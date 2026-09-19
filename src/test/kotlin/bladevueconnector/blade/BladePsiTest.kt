package bladevueconnector.blade

import bladevueconnector.BladeVueTestCase
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlTag

class BladePsiTest : BladeVueTestCase() {
    fun testTagsWithBladeEchoesAndDirectivesLiveInTheHtmlTree() {
        val file = openBlade(
            "resources/views/edit.blade.php",
            """
            @extends('layouts.app')

            @section('content')
                <text-input :value="{{ ${'$'}email }}" @if(${'$'}locked) disabled @endif
                            @input="update">
                </text-input>
            @endsection
            """.trimIndent(),
        )

        val root = BladePsi.htmlRoot(file)
        assertNotNull(root)

        val tag = PsiTreeUtil.findChildrenOfType(root, XmlTag::class.java).single { it.name == "text-input" }
        assertEquals("text-input", BladePsi.startName(tag)?.text)
        assertEquals("text-input", BladePsi.endName(tag)?.text)
    }

    fun testTagWithNameAtFindsStartAndEndNames() {
        val text = "<div>\n    <file-upload-v2 url=\"{{ route('upload') }}\"></file-upload-v2>\n</div>"
        val file = openBlade("resources/views/upload.blade.php", text)

        val start = BladePsi.tagWithNameAt(file, text.indexOf("file-upload-v2") + 3)
        val end = BladePsi.tagWithNameAt(file, text.lastIndexOf("file-upload-v2") + 3)
        val attribute = BladePsi.tagWithNameAt(file, text.indexOf("url"))

        assertEquals("file-upload-v2", start?.name)
        assertSame(start, end)
        assertNull(attribute)
    }

    fun testHtmlRootIsNullOutsideBladeFiles() {
        val file = myFixture.addFileToProject("resources/js/app.js", "const a = 1")

        assertNull(BladePsi.htmlRoot(file))
        assertFalse(BladePsi.isBladeFile(file))
    }
}
