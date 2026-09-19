package bladevueconnector.registry

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope

/**
 * Project files that can hold component registrations or components: source files, not dependencies
 * or build output.
 */
class SourceScope(private val project: Project) : DelegatingGlobalSearchScope(ProjectScope.getContentScope(project)) {
    override fun contains(file: VirtualFile): Boolean = super.contains(file) && isSource(project, file)

    companion object {
        private const val MAX_SCRIPT_LENGTH = 512 * 1024

        private val excludedTopLevelDirectories = listOf("vendor/", "public/", "storage/", "bootstrap/ssr/")

        fun isSource(project: Project, file: VirtualFile): Boolean {
            if (file.path.split('/').contains("node_modules")) {
                return false
            }

            if (file.name.endsWith(".min.js")) {
                return false
            }

            if (file.extension != "vue" && file.length > MAX_SCRIPT_LENGTH) {
                return false
            }

            val contentRoot = ProjectFileIndex.getInstance(project).getContentRootForFile(file) ?: return true
            val relativePath = VfsUtilCore.getRelativePath(file, contentRoot) ?: return true

            return excludedTopLevelDirectories.none { relativePath.startsWith(it) }
        }

        fun of(project: Project): GlobalSearchScope = SourceScope(project)
    }
}
