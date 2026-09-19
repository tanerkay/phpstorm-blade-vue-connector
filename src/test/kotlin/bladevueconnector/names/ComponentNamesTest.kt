package bladevueconnector.names

import junit.framework.TestCase

class ComponentNamesTest : TestCase() {
    fun testKebabTagMatchesPascalRegistration() {
        assertEquals(ComponentNames.key("TextInput"), ComponentNames.key("text-input"))
    }

    fun testDigitSegmentMatches() {
        assertEquals(ComponentNames.key("Step2Details"), ComponentNames.key("step-2-details"))
        assertEquals(ComponentNames.key("RatingScaleR1"), ComponentNames.key("rating-scale-r1"))
    }

    fun testCamelCaseRegistrationMatches() {
        assertEquals(ComponentNames.key("flatPickr"), ComponentNames.key("flat-pickr"))
    }

    fun testSingleWordMatchesIgnoringCase() {
        assertEquals(ComponentNames.key("notes"), ComponentNames.key("Notes"))
    }

    fun testDifferentNamesDoNotMatch() {
        assertFalse(ComponentNames.key("data-table") == ComponentNames.key("contact-form"))
    }

    fun testCamelizePascalAndHyphenate() {
        assertEquals("textInput", ComponentNames.camelize("text-input"))
        assertEquals("TextInput", ComponentNames.pascal("text-input"))
        assertEquals("text-input", ComponentNames.hyphenate("TextInput"))
        assertEquals("file-upload-v2", ComponentNames.hyphenate("file-upload-v2"))
    }

    fun testLooksLikeComponentRejectsHtmlSvgMathMlAndBuiltIns() {
        listOf("div", "DIV", "select", "font-face", "missing-glyph", "linearGradient", "annotation-xml", "math")
            .forEach { assertFalse(it, ComponentNames.looksLikeComponent(it)) }
        listOf("component", "keep-alive", "KeepAlive", "transition-group", "router-link", "RouterView")
            .forEach { assertFalse(it, ComponentNames.looksLikeComponent(it)) }
    }

    fun testLooksLikeComponentRejectsBladeLivewireAndFluxTags() {
        listOf("x-alert", "X-Alert", "x-slot:title", "x-mail::message", "livewire:counter", "flux:button", "x-icons.plus")
            .forEach { assertFalse(it, ComponentNames.looksLikeComponent(it)) }
    }

    fun testLooksLikeComponentRejectsOddNames() {
        listOf("", "1-thing", "_foo", "lowercase", "BR")
            .forEach { assertFalse(it, ComponentNames.looksLikeComponent(it)) }
    }

    fun testLooksLikeComponentAcceptsHyphenatedAndPascalCaseNames() {
        listOf("text-input", "file-upload-v2", "step-2-details", "b-nav-item", "Notes", "AlertBanner")
            .forEach { assertTrue(it, ComponentNames.looksLikeComponent(it)) }
    }
}
