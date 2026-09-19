package bladevueconnector.inspections

import bladevueconnector.BladeVueBundle
import bladevueconnector.registry.BladeVueComponentRegistry
import bladevueconnector.resolve.ComponentTagResolver
import bladevueconnector.resolve.TagResolution
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.xml.XmlTag

/**
 * Decides whether a tag is reported as an unregistered component. Errs on the side of silence: a
 * project with no registrations, with registrations the plugin can't read, or with component libraries
 * installed produces no warnings for tags it can't account for.
 */
object UnregisteredTagPolicy {
    /** The problem message, or null when the tag isn't reported. */
    fun problem(tag: XmlTag, options: BladeVueUnregisteredComponentInspection): String? {
        val name = ComponentTagResolver.componentName(tag) ?: return null

        if (isExcludedPath(tag, options.excludedPaths)) {
            return null
        }

        if (options.ignoredTagPatterns.any { pattern -> regex(pattern)?.containsMatchIn(name) == true }) {
            return null
        }

        val registry = BladeVueComponentRegistry.getInstance(tag.project)
        val snapshot = registry.snapshot()
        if (snapshot.registrations.isEmpty()) {
            return null
        }

        val resolution = ComponentTagResolver.resolve(registry, snapshot, name)
        if (resolution is TagResolution.Registered) {
            return null
        }

        if (snapshot.dynamicSites.isNotEmpty() && !options.reportWhenDynamicRegistrationsExist) {
            return null
        }

        if (resolution is TagResolution.Fallback) {
            val file = resolution.files.first()
            val registeredAs = snapshot.targeting(file.virtualFile).firstOrNull()?.name

            return if (registeredAs == null) {
                BladeVueBundle.message("inspection.unregistered.found.file", name, file.name)
            } else {
                BladeVueBundle.message("inspection.unregistered.registered.as", name, file.name, registeredAs)
            }
        }

        if (snapshot.pluginInstalls.isNotEmpty() && !options.reportWhenPluginsInstalled) {
            return null
        }

        return BladeVueBundle.message("inspection.unregistered.no.file", name)
    }

    private fun isExcludedPath(tag: XmlTag, patterns: List<String>): Boolean {
        val file = tag.containingFile?.originalFile?.virtualFile ?: return false
        val contentRoot = ProjectFileIndex.getInstance(tag.project).getContentRootForFile(file) ?: return false
        val relativePath = VfsUtilCore.getRelativePath(file, contentRoot) ?: return false

        return patterns.any { pattern -> regex(FileUtil.convertAntToRegexp(pattern.trim()))?.matches(relativePath) == true }
    }

    /** Null for an invalid pattern typed into the options. */
    private fun regex(pattern: String): Regex? {
        return try {
            Regex(pattern)
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
