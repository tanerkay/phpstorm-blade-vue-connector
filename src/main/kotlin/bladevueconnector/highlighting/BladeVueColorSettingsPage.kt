package bladevueconnector.highlighting

import bladevueconnector.BladeVueBundle
import com.intellij.lang.html.HTMLLanguage
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import javax.swing.Icon

class BladeVueColorSettingsPage : ColorSettingsPage {
    override fun getDisplayName(): String = BladeVueBundle.message("color.settings.display.name")

    override fun getIcon(): Icon? = null

    override fun getHighlighter(): SyntaxHighlighter =
        SyntaxHighlighterFactory.getSyntaxHighlighter(HTMLLanguage.INSTANCE, null, null)

    override fun getDemoText(): String =
        """
        <form>
            <<component>text-input</component> name="email" :required="true"></<component>text-input</component>>
            <span>Not a component</span>
        </form>
        """.trimIndent()

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey> =
        mapOf("component" to BladeVueColors.COMPONENT_TAG)

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> =
        arrayOf(AttributesDescriptor(BladeVueBundle.message("color.settings.component.tag"), BladeVueColors.COMPONENT_TAG))

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY
}
