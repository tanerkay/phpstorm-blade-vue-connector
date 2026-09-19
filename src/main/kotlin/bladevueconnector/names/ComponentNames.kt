package bladevueconnector.names

/**
 * Vue component name spellings.
 *
 * Tags and registrations are matched on [key], which ignores case and hyphens, so `text-input`,
 * `TextInput` and `textInput` all match, as do `step-2-details` and `Step2Details`.
 * Vue's own hyphenate/camelize round trip doesn't handle digits that way.
 */
object ComponentNames {
    /** The Blade tag index stores the output of [key] and [looksLikeComponent]; bump when either changes. */
    const val INDEX_VERSION: Int = 1

    private val camelizePattern = Regex("-(\\w)")
    private val hyphenatePattern = Regex("\\B([A-Z])")

    fun key(name: String): String = name.filter { it != '-' }.lowercase()

    fun camelize(name: String): String = camelizePattern.replace(name) { it.groupValues[1].uppercase() }

    fun pascal(name: String): String = camelize(name).replaceFirstChar { it.uppercaseChar() }

    fun hyphenate(name: String): String = hyphenatePattern.replace(name, "-$1").lowercase()

    /**
     * Whether a tag name in a Blade view can refer to a Vue component.
     *
     * Rejects Blade (`x-`), Livewire/Flux and other namespaced tags, and HTML, SVG and MathML elements
     * and Vue built-ins. Accepts names with a hyphen, and PascalCase names.
     */
    fun looksLikeComponent(tagName: String): Boolean {
        if (tagName.isEmpty() || !tagName[0].isLetter()) {
            return false
        }

        if (tagName.contains(':') || tagName.contains('.')) {
            return false
        }

        if (tagName.startsWith("x-", ignoreCase = true)) {
            return false
        }

        if (KnownElements.contains(tagName)) {
            return false
        }

        if (tagName.contains('-')) {
            return true
        }

        return tagName[0].isUpperCase() && tagName.any { it.isLowerCase() }
    }
}
