package bladevueconnector.registry

import com.intellij.lang.ecmascript6.psi.ES6FromClause
import com.intellij.lang.ecmascript6.psi.ES6ImportCall
import com.intellij.lang.javascript.buildTools.npm.PackageJsonUtil
import com.intellij.lang.javascript.psi.JSLiteralExpression
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiPolyVariantReference

/** Resolves the module path of a registered component to its file. */
object ModulePathResolver {
    private val extensions = listOf("", ".vue", ".js", ".ts", ".jsx", ".tsx", ".mjs", "/index.vue", "/index.js", "/index.ts")

    /** Directories that `@/` and `~/` conventionally point at in Laravel projects. */
    private val aliasRoots = listOf("resources/js", "resources/assets/js", "resources/ts", "src")

    fun resolve(source: ComponentSource.Module): PsiFile? {
        val context = source.resolveVia.containingFile?.originalFile?.virtualFile ?: return null
        val manager = source.resolveVia.manager
        val specifier = source.specifier

        if (specifier.startsWith("./") || specifier.startsWith("../")) {
            return probe(context.parent, specifier)?.let(manager::findFile)
        }

        val resolved = resolveWithJavaScriptPlugin(source.resolveVia)
        if (resolved != null) {
            return resolved
        }

        if (specifier.startsWith("@/") || specifier.startsWith("~/")) {
            val packageRoot = PackageJsonUtil.findUpPackageJson(context)?.parent ?: return null

            return aliasRoots
                .asSequence()
                .mapNotNull { packageRoot.findFileByRelativePath(it) }
                .firstNotNullOfOrNull { probe(it, specifier.substring(2)) }
                ?.let(manager::findFile)
        }

        return null
    }

    private fun probe(directory: VirtualFile?, path: String): VirtualFile? {
        if (directory == null) {
            return null
        }

        return extensions
            .asSequence()
            .mapNotNull { directory.findFileByRelativePath(path + it) }
            .firstOrNull { !it.isDirectory }
    }

    /** Uses the IDE's resolution, which knows Vite/webpack aliases and tsconfig paths. */
    private fun resolveWithJavaScriptPlugin(element: PsiElement): PsiFile? {
        val targets: Collection<PsiElement> = when (element) {
            is ES6FromClause -> element.resolveReferencedElements()
            is ES6ImportCall -> element.resolveReferencedElements()
            is JSLiteralExpression -> element.references
                .filterIsInstance<PsiPolyVariantReference>()
                .flatMap { reference -> reference.multiResolve(false).mapNotNull { it.element } }
            else -> emptyList()
        }

        val files = targets
            .mapNotNull { if (it is PsiFileSystemItem) it as? PsiFile else it.containingFile }
            .map { it.originalFile }
            .filter { it.virtualFile != null && !it.name.endsWith(".d.ts") }
            .distinct()

        return files.firstOrNull { it.virtualFile.extension == "vue" } ?: files.firstOrNull()
    }
}
