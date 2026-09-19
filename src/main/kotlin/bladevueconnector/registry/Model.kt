package bladevueconnector.registry

import bladevueconnector.names.ComponentNames
import com.intellij.lang.javascript.psi.JSElement
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import java.util.concurrent.ConcurrentHashMap

enum class RegistrationKind {
    /** `app.component('Name', X)` or `Vue.component('name', X)`. */
    Global,

    /** A key of `components: {}` on `createApp({...})` or `new Vue({...})`. */
    RootLocal,
}

/** Where a registered component is defined, as far as the registering file shows. */
sealed interface ComponentSource {
    /**
     * A module path such as `./components/X.vue` or `@/components/X`.
     *
     * @property resolveVia the `from` clause, `import()` call or `require()` argument, used for the
     * JavaScript plugin's own module resolution.
     */
    data class Module(val specifier: String, val resolveVia: JSElement) : ComponentSource

    /** A package from node_modules, e.g. `vue-select`. */
    data class Library(val packageName: String) : ComponentSource

    /** Defined in the registering file, e.g. `defineComponent({...})`. */
    data object Inline : ComponentSource

    data object Unknown : ComponentSource
}

/** A registration as found in one file, before module paths are resolved. */
class RawRegistration(
    val name: String,
    val kind: RegistrationKind,
    val nameElement: PsiElement,
    val source: ComponentSource,
)

class FileRegistrations(
    val registrations: List<RawRegistration>,
    /** `.component(...)` calls or `components` entries whose name can't be read statically. */
    val dynamicSites: List<PsiElement>,
    /** Packages installed with `.use(...)` that may register components of their own. */
    val pluginInstalls: List<String>,
)

/**
 * @property target the resolved `.vue` (or script) file, null for libraries and unresolved paths.
 */
class Registration(
    val name: String,
    val kind: RegistrationKind,
    val nameElement: PsiElement,
    val source: ComponentSource,
    val target: PsiFile?,
)

class RegistrySnapshot(
    val registrations: List<Registration>,
    val dynamicSites: List<PsiElement>,
    val pluginInstalls: Set<String>,
) {
    private val byKey: Map<String, List<Registration>> = registrations.groupBy { ComponentNames.key(it.name) }

    private val byTarget: Map<VirtualFile, List<Registration>> = registrations
        .filter { it.target?.virtualFile != null }
        .groupBy { it.target!!.virtualFile }

    fun lookup(tagName: String): List<Registration> = byKey[ComponentNames.key(tagName)].orEmpty()

    fun targeting(file: VirtualFile): List<Registration> = byTarget[file].orEmpty()

    /** Same-named `.vue` files per tag key; file renames also invalidate the snapshot. */
    internal val fallbackFiles: ConcurrentHashMap<String, List<PsiFile>> = ConcurrentHashMap()

    companion object {
        val EMPTY: RegistrySnapshot = RegistrySnapshot(emptyList(), emptyList(), emptySet())
    }
}
