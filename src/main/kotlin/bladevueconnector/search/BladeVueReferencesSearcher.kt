package bladevueconnector.search

import bladevueconnector.blade.BladePsi
import bladevueconnector.names.ComponentNames
import bladevueconnector.navigation.BladeVueTagReference
import bladevueconnector.registry.BladeVueComponentRegistry
import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.PsiSearchScopeUtil
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlTag
import com.intellij.util.Processor
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.php.blade.BladeFileType

/** Finds Blade tags referring to a `.vue` file, under every name it is registered as. */
class BladeVueReferencesSearcher : QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>(true) {
    override fun processQuery(parameters: ReferencesSearch.SearchParameters, consumer: Processor<in PsiReference>) {
        val target = parameters.elementToSearch as? PsiFile ?: return
        val targetFile = target.virtualFile ?: return
        if (targetFile.extension != "vue") {
            return
        }

        val project = parameters.project
        if (DumbService.isDumb(project)) {
            return
        }

        val snapshot = BladeVueComponentRegistry.getInstance(project).snapshot()
        val keys = (snapshot.targeting(targetFile).map { it.name } + targetFile.nameWithoutExtension)
            .map { ComponentNames.key(it) }
            .toSet()

        val scope = parameters.effectiveSearchScope
        val psiManager = PsiManager.getInstance(project)

        for (file in bladeFilesContaining(keys, scope)) {
            ProgressManager.checkCanceled()

            val root = psiManager.findFile(file)?.let { BladePsi.htmlRoot(it) } ?: continue
            val completed = PsiTreeUtil.processElements(root, XmlTag::class.java) { tag ->
                val name = BladePsi.startName(tag)?.text
                if (name == null || ComponentNames.key(name) !in keys || !PsiSearchScopeUtil.isInScope(scope, tag)) {
                    return@processElements true
                }

                tag.references
                    .filterIsInstance<BladeVueTagReference>()
                    .filter { it.isReferenceTo(target) }
                    .all { consumer.process(it) }
            }

            if (!completed) {
                return
            }
        }
    }

    private fun bladeFilesContaining(keys: Set<String>, scope: SearchScope): Collection<VirtualFile> {
        return when (scope) {
            is GlobalSearchScope -> {
                val bladeScope = GlobalSearchScope.getScopeRestrictedByFileTypes(scope, BladeFileType.INSTANCE)

                keys.flatMap { FileBasedIndex.getInstance().getContainingFiles(BladeVueTagIndex.NAME, it, bladeScope) }.toSet()
            }

            is LocalSearchScope -> scope.virtualFiles.filter { it.fileType == BladeFileType.INSTANCE }
            else -> emptyList()
        }
    }
}
