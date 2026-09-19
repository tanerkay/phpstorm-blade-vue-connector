package bladevueconnector.inspections

import bladevueconnector.BladeVueTestCase
import com.intellij.codeInspection.htmlInspections.HtmlUnknownTagInspection
import com.intellij.modcommand.ActionContext
import com.intellij.modcommand.ModCommand
import com.intellij.modcommand.ModUpdateSystemOptions
import com.intellij.openapi.application.ReadAction

class BladeVueUnregisteredComponentInspectionTest : BladeVueTestCase() {
    fun testUnregisteredTagWithoutFileIsReported() {
        copyProject("vite-vue3")
        enableInspection()
        openBlade(
            "resources/views/edit.blade.php",
            "<div>\n    <<warning descr=\"<missing-widget> looks like a Vue component but is not registered in any Vue app\">missing-widget</warning>></missing-widget>\n</div>",
        )

        myFixture.checkHighlighting(true, false, true)
    }

    fun testUnregisteredTagWithSameNamedFileMentionsTheFile() {
        copyProject("vite-vue3")
        enableInspection()
        openBlade(
            "resources/views/table.blade.php",
            "<<warning descr=\"Vue component <data-table> is not registered in any Vue app (DataTable.vue exists)\">data-table</warning>></data-table>",
        )

        myFixture.checkHighlighting(true, false, true)
    }

    fun testFileRegisteredUnderAnotherNameIsMentioned() {
        copyProject("vite-vue3")
        enableInspection()
        openBlade(
            "resources/views/table.blade.php",
            "<<warning descr=\"Vue component <form-text-input> is not registered in any Vue app (FormTextInput.vue is registered as 'TextInput')\">form-text-input</warning>></form-text-input>",
        )

        myFixture.checkHighlighting(true, false, true)
    }

    fun testRegisteredAndNonComponentTagsAreNotReported() {
        copyProject("vite-vue3")
        enableInspection()
        openBlade(
            "resources/views/edit.blade.php",
            """
            <div>
                <text-input value="{{ ${'$'}email }}" @if(${'$'}locked) disabled @endif></text-input>
                <v-select></v-select>
                <x-alert type="error"></x-alert>
                <livewire:counter />
                <flux:button>Save</flux:button>
                <svg><linearGradient id="g"></linearGradient></svg>
                <keep-alive></keep-alive>
            </div>
            """.trimIndent(),
        )

        myFixture.checkHighlighting(true, false, true)
    }

    fun testProjectWithoutRegistrationsIsNotChecked() {
        enableInspection()
        openBlade("resources/views/edit.blade.php", "<missing-widget></missing-widget>")

        myFixture.checkHighlighting(true, false, true)
    }

    fun testDynamicRegistrationsSilenceTheInspection() {
        copyProject("dynamic")
        enableInspection()
        openBlade("resources/views/nav.blade.php", "<b-nav-item></b-nav-item>\n<unused-panel></unused-panel>")

        myFixture.checkHighlighting(true, false, true)
    }

    fun testDynamicRegistrationsOptionReportsAnyway() {
        copyProject("dynamic")
        enableInspection { it.reportWhenDynamicRegistrationsExist = true }
        openBlade(
            "resources/views/nav.blade.php",
            "<<warning descr=\"<b-nav-item> looks like a Vue component but is not registered in any Vue app\">b-nav-item</warning>></b-nav-item>",
        )

        myFixture.checkHighlighting(true, false, true)
    }

    fun testPluginInstallsSilenceOnlyTagsWithoutFile() {
        copyProject("mix-vue2")
        enableInspection()
        myFixture.addFileToProject("resources/js/components/StrayPanel.vue", "<template><div/></template>")
        openBlade(
            "resources/views/table.blade.php",
            "<portal-target></portal-target>\n<<warning descr=\"Vue component <stray-panel> is not registered in any Vue app (StrayPanel.vue exists)\">stray-panel</warning>></stray-panel>",
        )

        myFixture.checkHighlighting(true, false, true)
    }

    fun testIgnoredTagPatternsOption() {
        copyProject("vite-vue3")
        enableInspection { it.ignoredTagPatterns.add("^b-") }
        openBlade("resources/views/nav.blade.php", "<b-nav-item></b-nav-item>")

        myFixture.checkHighlighting(true, false, true)
    }

    fun testVendorViewsAreExcluded() {
        copyProject("vite-vue3")
        enableInspection()
        openBlade("resources/views/vendor/package/panel.blade.php", "<package-widget></package-widget>")

        myFixture.checkHighlighting(true, false, true)
    }

    /** Checks the option update the fix asks for; light tests can't apply it, since their profile has no registered tools. */
    fun testIgnorePrefixQuickFixAddsPattern() {
        copyProject("vite-vue3")
        enableInspection()
        openBlade("resources/views/nav.blade.php", "<b-nav-<caret>item></b-nav-item>")

        val action = myFixture.findSingleIntention("Ignore tags starting with 'b-'").asModCommandAction()!!
        val command = ReadAction.compute<ModCommand, RuntimeException> {
            action.perform(ActionContext.from(myFixture.editor, myFixture.file))
        }

        val option = (command as ModUpdateSystemOptions).options().single()
        assertTrue(option.bindId(), option.bindId().endsWith("BladeVueUnregisteredComponent.options.ignoredTagPatterns"))
        assertEquals(listOf("^b-"), option.newValue())
    }

    fun testUnknownHtmlTagHiddenWhereThisInspectionReports() {
        copyProject("vite-vue3")
        enableInspection()
        myFixture.enableInspections(HtmlUnknownTagInspection())
        openBlade(
            "resources/views/edit.blade.php",
            "<<warning descr=\"<missing-widget> looks like a Vue component but is not registered in any Vue app\">missing-widget</warning>></missing-widget>",
        )

        myFixture.checkHighlighting(true, false, true)
    }

    private fun enableInspection(configure: (BladeVueUnregisteredComponentInspection) -> Unit = {}) {
        myFixture.enableInspections(BladeVueUnregisteredComponentInspection().also(configure))
    }
}
