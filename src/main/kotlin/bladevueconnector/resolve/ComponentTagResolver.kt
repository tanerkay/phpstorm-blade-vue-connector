package bladevueconnector.resolve

import bladevueconnector.blade.BladePsi
import bladevueconnector.names.ComponentNames
import bladevueconnector.registry.BladeVueComponentRegistry
import bladevueconnector.registry.Registration
import bladevueconnector.registry.RegistrySnapshot
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.xml.XmlTag

sealed interface TagResolution {
    data class Registered(val registrations: List<Registration>, val fallbackFiles: List<PsiFile>) : TagResolution

    /** Not registered, but a `.vue` file with the same name exists. */
    data class Fallback(val files: List<PsiFile>) : TagResolution

    data object Unknown : TagResolution
}

object ComponentTagResolver {
    /** The tag's start name, when it can refer to a component. */
    fun componentName(tag: XmlTag): String? =
        BladePsi.startName(tag)?.text?.takeIf { ComponentNames.looksLikeComponent(it) }

    fun resolve(tag: XmlTag): TagResolution {
        val name = componentName(tag) ?: return TagResolution.Unknown
        val registry = BladeVueComponentRegistry.getInstance(tag.project)

        return resolve(registry, registry.snapshot(), name)
    }

    fun resolve(registry: BladeVueComponentRegistry, snapshot: RegistrySnapshot, name: String): TagResolution {
        val registrations = snapshot.lookup(name)
        if (registrations.isNotEmpty()) {
            val fallbackFiles = if (registrations.none { it.target != null }) registry.fallbackFiles(snapshot, name) else emptyList()

            return TagResolution.Registered(registrations, fallbackFiles)
        }

        val files = registry.fallbackFiles(snapshot, name)
        if (files.isNotEmpty()) {
            return TagResolution.Fallback(files)
        }

        return TagResolution.Unknown
    }

    /**
     * Where Ctrl+click goes: the component files of the registrations, or the registration itself for
     * library components, then same-named files when no registration points at a file.
     */
    fun targets(resolution: TagResolution): List<PsiElement> {
        return when (resolution) {
            is TagResolution.Registered -> {
                val registered = resolution.registrations.map { it.target ?: it.nameElement }

                (registered + resolution.fallbackFiles).filter { it.isValid }.distinct()
            }

            is TagResolution.Fallback -> resolution.files.filter { it.isValid }
            TagResolution.Unknown -> emptyList()
        }
    }

    fun isRecognised(resolution: TagResolution): Boolean = resolution != TagResolution.Unknown
}
