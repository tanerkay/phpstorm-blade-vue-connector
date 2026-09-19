package bladevueconnector.highlighting

import bladevueconnector.BladeVueTestCase
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInspection.htmlInspections.HtmlUnknownTagInspection
import com.intellij.openapi.util.TextRange

class BladeVueHighlightingTest : BladeVueTestCase() {
    fun testRecognisedTagNamesAreColoured() {
        copyProject("vite-vue3")
        val text = "<div>\n    <text-input :value=\"{{ ${'$'}email }}\"></text-input>\n    <span></span>\n</div>"
        openBlade("resources/views/edit.blade.php", text)

        val ranges = myFixture.doHighlighting()
            .filter { it.forcedTextAttributesKey == BladeVueColors.COMPONENT_TAG }
            .map { TextRange(it.startOffset, it.endOffset) }
            .sortedBy { it.startOffset }

        val start = text.indexOf("text-input")
        val end = text.lastIndexOf("text-input")
        assertEquals(
            listOf(TextRange(start, start + "text-input".length), TextRange(end, end + "text-input".length)),
            ranges,
        )
    }

    fun testUnrecognisedTagIsNotColoured() {
        copyProject("vite-vue3")
        openBlade("resources/views/edit.blade.php", "<missing-widget></missing-widget>")

        assertEmpty(myFixture.doHighlighting().filter { it.forcedTextAttributesKey == BladeVueColors.COMPONENT_TAG })
    }

    fun testUnknownHtmlTagSuppressedForRegisteredAndFallbackTags() {
        copyProject("vite-vue3")
        myFixture.enableInspections(HtmlUnknownTagInspection())
        openBlade("resources/views/edit.blade.php", "<div>\n    <file-upload-v2></file-upload-v2>\n    <data-table></data-table>\n</div>")

        assertEmpty(unknownTagWarnings())
    }

    fun testUnknownHtmlTagStillReportedWhenPluginInspectionIsOff() {
        copyProject("vite-vue3")
        myFixture.enableInspections(HtmlUnknownTagInspection())
        openBlade("resources/views/edit.blade.php", "<div>\n    <missing-widget></missing-widget>\n</div>")

        assertNotEmpty(unknownTagWarnings())
    }

    fun testColorSettingsPageMarksComponentTagInDemoText() {
        val page = BladeVueColorSettingsPage()

        assertEquals(mapOf("component" to BladeVueColors.COMPONENT_TAG), page.additionalHighlightingTagToDescriptorMap)
        assertTrue(page.demoText.contains("<<component>text-input</component>"))
    }

    private fun unknownTagWarnings(): List<HighlightInfo> =
        myFixture.doHighlighting().filter { it.description?.contains("Unknown html tag", ignoreCase = true) == true }
}
