package bladevueconnector.search

import bladevueconnector.BladeVueTestCase
import bladevueconnector.names.ComponentNames
import bladevueconnector.navigation.BladeVueTagReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.indexing.FileBasedIndex

class BladeVueFindUsagesTest : BladeVueTestCase() {
    fun testFindUsagesOfVueFileListsBladeTags() {
        copyProject("vite-vue3")
        myFixture.addFileToProject("resources/views/forms/edit.blade.php", "<div>\n    <text-input></text-input>\n</div>")
        myFixture.addFileToProject("resources/views/forms/show.blade.php", "<TextInput></TextInput>")

        val usageFiles = myFixture.findUsages(projectFile("resources/js/components/FormTextInput.vue"))
            .mapNotNull { it.virtualFile?.name }
            .sorted()

        assertEquals(listOf("app.js", "edit.blade.php", "show.blade.php"), usageFiles)
        assertEquals(
            listOf("edit.blade.php:text-input", "show.blade.php:TextInput"),
            bladeReferences("resources/js/components/FormTextInput.vue")
                .map { "${it.element.containingFile.name}:${it.canonicalText}" }
                .sorted(),
        )
    }

    fun testFindUsagesFollowsRegisteredNameWithDigits() {
        copyProject("vite-vue3")
        myFixture.addFileToProject("resources/views/steps/details.blade.php", "<step-2-details></step-2-details>")

        assertEquals(1, bladeReferences("resources/js/components/Step2Details.vue").size)
    }

    fun testFindUsagesFollowsAliasedRegistration() {
        copyProject("vite-vue3")
        myFixture.addFileToProject("resources/views/upload.blade.php", "<file-upload-v2></file-upload-v2>")

        assertEquals(1, bladeReferences("resources/js/components/MultiFileUploader.vue").size)
    }

    fun testFindUsagesIncludesFallbackResolvedTags() {
        copyProject("vite-vue3")
        myFixture.addFileToProject("resources/views/table.blade.php", "<data-table></data-table>")

        assertEquals(1, bladeReferences("resources/js/components/DataTable.vue").size)
    }

    fun testTagRegisteredToAnotherFileIsNotAUsage() {
        copyProject("vite-vue3")
        myFixture.addFileToProject("resources/js/components/other/TextInput.vue", "<template><div/></template>")
        myFixture.addFileToProject("resources/views/forms/edit.blade.php", "<text-input></text-input>")

        assertEmpty(bladeReferences("resources/js/components/other/TextInput.vue"))
        assertEquals(1, bladeReferences("resources/js/components/FormTextInput.vue").size)
    }

    fun testRenamingVueFileKeepsBladeTagAndRegisteredName() {
        copyProject("vite-vue3")
        val view = myFixture.addFileToProject("resources/views/forms/edit.blade.php", "<text-input></text-input>")

        myFixture.renameElement(projectFile("resources/js/components/FormTextInput.vue"), "FormInputText.vue")

        assertEquals("<text-input></text-input>", view.text)
        val app = projectFile("resources/js/app.js").text
        assertTrue(app.contains("from './components/FormInputText.vue'"))
        assertTrue(app.contains("app.component('TextInput', "))
        assertEquals(1, bladeReferences("resources/js/components/FormInputText.vue").size)
    }

    fun testIndexStoresComponentTagKeysOnly() {
        myFixture.addFileToProject(
            "resources/views/mixed.blade.php",
            "<div><Step2Details></Step2Details><x-alert></x-alert><livewire:counter /><span></span></div>",
        )

        val index = FileBasedIndex.getInstance()
        val scope = GlobalSearchScope.projectScope(project)

        assertEquals(1, index.getContainingFiles(BladeVueTagIndex.NAME, ComponentNames.key("step-2-details"), scope).size)
        assertEmpty(index.getContainingFiles(BladeVueTagIndex.NAME, ComponentNames.key("x-alert"), scope))
        assertEmpty(index.getContainingFiles(BladeVueTagIndex.NAME, ComponentNames.key("span"), scope))
    }

    /** References found by this plugin; in tests the Vue plugin also resolves some Blade tags itself. */
    private fun bladeReferences(vuePath: String): List<BladeVueTagReference> =
        ReferencesSearch.search(projectFile(vuePath)).findAll().filterIsInstance<BladeVueTagReference>()
}
