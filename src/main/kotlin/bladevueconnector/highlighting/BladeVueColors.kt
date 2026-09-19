package bladevueconnector.highlighting

import com.intellij.openapi.editor.XmlHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey

object BladeVueColors {
    /** Defaults to the colour of custom tags, which is how components look in `.vue` files. */
    val COMPONENT_TAG: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("BLADE_VUE_COMPONENT_TAG", XmlHighlighterColors.HTML_CUSTOM_TAG_NAME)
}
