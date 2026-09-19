package bladevueconnector.navigation

import bladevueconnector.BladeVueTestCase
import bladevueconnector.blade.BladePsi
import com.intellij.codeInsight.navigation.actions.GotoDeclarationAction
import com.intellij.lang.javascript.psi.JSLiteralExpression
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlTag

class BladeVueNavigationTest : BladeVueTestCase() {
    fun testStartTagNavigatesToRegisteredFile() {
        copyProject("vite-vue3")
        openBlade(
            "resources/views/forms/edit.blade.php",
            """
            @extends('layouts.app')

            @section('content')
                <text-<caret>input :value="{{ ${'$'}email }}" @if(${'$'}locked) disabled @endif
                            @input="update">
                </text-input>
            @endsection
            """.trimIndent(),
        )

        assertEquals(listOf("FormTextInput.vue"), targetFileNames())
    }

    fun testEndTagNavigates() {
        copyProject("vite-vue3")
        openBlade("resources/views/upload.blade.php", "<file-upload-v2 url=\"{{ route('upload') }}\"></file-upload-<caret>v2>")

        assertEquals(listOf("MultiFileUploader.vue"), targetFileNames())
    }

    fun testTagInLayoutNavigates() {
        copyProject("vite-vue3")
        openBlade("resources/views/layouts/app.blade.php", "<div id=\"app\">\n    <status-<caret>message></status-message>\n    @yield('content')\n</div>")

        assertEquals(listOf("AlertBanner.vue"), targetFileNames())
    }

    fun testUnregisteredTagFallsBackToSameNamedFile() {
        copyProject("vite-vue3")
        openBlade("resources/views/table.blade.php", "<data-<caret>table></data-table>")

        assertEquals(listOf("DataTable.vue"), targetFileNames())
    }

    fun testLibraryComponentNavigatesToRegistration() {
        copyProject("vite-vue3")
        openBlade("resources/views/select.blade.php", "<v-<caret>select :options=\"options\"></v-select>")

        val target = targets().single()

        assertTrue(target is JSLiteralExpression)
        assertEquals("'v-select'", target.text)
    }

    fun testBladeComponentTagIsNotHandled() {
        copyProject("vite-vue3")
        openBlade("resources/views/alert.blade.php", "<x-<caret>alert type=\"error\"></x-alert>")

        assertFalse(targets().any { it is PsiFile && it.name.endsWith(".vue") })
    }

    fun testReferenceIsSoftAndResolvesToComponentFile() {
        copyProject("vite-vue3")
        val file = openBlade("resources/views/feed.blade.php", "<activity-feed :items=\"@json(${'$'}items)\"></activity-feed>")

        val tag = PsiTreeUtil.findChildOfType(BladePsi.htmlRoot(file), XmlTag::class.java)!!
        val reference = tag.references.filterIsInstance<BladeVueTagReference>().single()

        assertTrue(reference.isSoft)
        assertEquals("activity-feed", reference.canonicalText)
        assertEquals("ActivityFeed.vue", (reference.multiResolve(false).single().element as PsiFile).name)
    }

    private fun targets() = GotoDeclarationAction.findAllTargetElements(project, myFixture.editor, myFixture.caretOffset).toList()

    private fun targetFileNames() = targets().map { (it as PsiFile).name }
}
