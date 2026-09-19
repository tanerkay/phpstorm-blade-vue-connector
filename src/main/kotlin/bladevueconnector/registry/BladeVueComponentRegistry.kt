package bladevueconnector.registry

import bladevueconnector.names.ComponentNames
import com.intellij.lang.javascript.psi.JSFile
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker

/** The Vue components registered anywhere in the project's JS/TS sources. */
@Service(Service.Level.PROJECT)
class BladeVueComponentRegistry(private val project: Project) {
    /**
     * @throws IndexNotReadyException while indexing, so that an empty result is never cached.
     */
    fun snapshot(): RegistrySnapshot {
        if (DumbService.isDumb(project)) {
            throw IndexNotReadyException.create()
        }

        return CachedValuesManager.getManager(project).getCachedValue(project, snapshotKey, ::computeSnapshot, false)
    }

    /** Same-named `.vue` files for a tag that isn't registered. */
    fun fallbackFiles(snapshot: RegistrySnapshot, tagName: String): List<PsiFile> =
        snapshot.fallbackFiles.computeIfAbsent(ComponentNames.key(tagName)) { findFilesNamedLike(tagName) }

    private fun findFilesNamedLike(tagName: String): List<PsiFile> {
        val names = linkedSetOf(
            ComponentNames.pascal(tagName) + ".vue",
            tagName + ".vue",
            ComponentNames.hyphenate(ComponentNames.pascal(tagName)) + ".vue",
        )
        val scope = SourceScope.of(project)
        val psiManager = PsiManager.getInstance(project)

        return names
            .flatMap { FilenameIndex.getVirtualFilesByName(it, false, scope) }
            .distinct()
            .mapNotNull { psiManager.findFile(it) }
    }

    private fun computeSnapshot(): CachedValueProvider.Result<RegistrySnapshot> {
        val registrations = mutableListOf<Registration>()
        val dynamicSites = mutableListOf<PsiElement>()
        val pluginInstalls = linkedSetOf<String>()

        for (file in candidateFiles()) {
            ProgressManager.checkCanceled()

            val found = RegistrationExtractor.registrationsIn(file)
            dynamicSites += found.dynamicSites
            pluginInstalls += found.pluginInstalls
            found.registrations.mapTo(registrations) { raw ->
                val target = (raw.source as? ComponentSource.Module)?.let(ModulePathResolver::resolve)

                Registration(raw.name, raw.kind, raw.nameElement, raw.source, target)
            }
        }

        val snapshot = RegistrySnapshot(registrations, dynamicSites, pluginInstalls)

        return CachedValueProvider.Result.create(
            snapshot,
            PsiModificationTracker.getInstance(project).forLanguages { it.isKindOf(JAVASCRIPT_LANGUAGE_ID) },
            VirtualFileManager.VFS_STRUCTURE_MODIFICATIONS,
            ProjectRootModificationTracker.getInstance(project),
        )
    }

    private fun candidateFiles(): Collection<JSFile> {
        val files = linkedSetOf<JSFile>()
        val scope = SourceScope.of(project)
        val searchHelper = PsiSearchHelper.getInstance(project)

        for (word in listOf("component", "components")) {
            searchHelper.processAllFilesWithWord(word, scope, { file ->
                if (file is JSFile) {
                    files += file
                }
                true
            }, true)
        }

        return files
    }

    companion object {
        /** TypeScript and other dialects are kinds of JavaScript. */
        private const val JAVASCRIPT_LANGUAGE_ID = "JavaScript"

        private val snapshotKey: Key<CachedValue<RegistrySnapshot>> = Key.create("bladevueconnector.registry.snapshot")

        fun getInstance(project: Project): BladeVueComponentRegistry = project.service()
    }
}
