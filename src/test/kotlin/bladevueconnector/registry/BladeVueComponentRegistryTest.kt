package bladevueconnector.registry

import bladevueconnector.BladeVueTestCase
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiDocumentManager

class BladeVueComponentRegistryTest : BladeVueTestCase() {
    fun testGlobalRegistrationWithDifferentImportName() {
        copyProject("vite-vue3")

        val registration = lookupSingle("text-input")

        assertEquals(RegistrationKind.Global, registration.kind)
        assertEquals("FormTextInput.vue", registration.target?.name)
    }

    fun testKebabRegistrationOfDifferentlyNamedComponent() {
        copyProject("vite-vue3")

        assertEquals("MultiFileUploader.vue", lookupSingle("file-upload-v2").target?.name)
        assertEquals("AlertBanner.vue", lookupSingle("status-message").target?.name)
    }

    fun testLibraryRegistrationHasNoTarget() {
        copyProject("vite-vue3")

        val registration = lookupSingle("v-select")

        assertEquals(ComponentSource.Library("vue-select"), registration.source)
        assertNull(registration.target)
    }

    fun testRootLocalAsyncComponents() {
        copyProject("vite-vue3")

        val shorthand = lookupSingle("activity-feed")
        assertEquals(RegistrationKind.RootLocal, shorthand.kind)
        assertEquals("ActivityFeed.vue", shorthand.target?.name)

        assertEquals("SearchPanel.vue", lookupSingle("search-box").target?.name)
    }

    fun testAtAliasResolvesToResourcesJs() {
        copyProject("vite-vue3")

        assertEquals("AtAlias.vue", lookupSingle("at-alias").target?.name)
    }

    fun testRequireDefaultAndTemplateLiteralName() {
        copyProject("vite-vue3")

        assertEquals("Step2Details.vue", lookupSingle("step-2-details").target?.name)
        assertEquals("UserCard.vue", lookupSingle("user-card").target?.name)
    }

    fun testUnregisteredComponentIsNotInSnapshot() {
        copyProject("vite-vue3")

        assertEmpty(snapshot().lookup("data-table"))
    }

    fun testVue2RequireWithAndWithoutExtension() {
        copyProject("mix-vue2")

        assertEquals("ContactForm.vue", lookupSingle("contact-form").target?.name)
        assertEquals("DataTable.vue", lookupSingle("data-table").target?.name)
        assertEquals(ComponentSource.Library("vue-flatpickr-component"), lookupSingle("flat-pickr").source)
    }

    fun testNewVueComponentsOption() {
        copyProject("mix-vue2")

        assertEquals("SummaryTable.vue", lookupSingle("summary-table").target?.name)
        assertEquals("PaymentForm.vue", lookupSingle("payment-form").target?.name)
    }

    fun testPluginInstallsExcludeKnownPluginsWithoutComponents() {
        copyProject("mix-vue2")

        assertEquals(setOf("portal-vue"), snapshot().pluginInstalls)
    }

    fun testTypeScriptOptionsObjectInVariable() {
        copyProject("local-components")

        assertEquals("APIKeyList.vue", lookupSingle("api-key-list").target?.name)
        assertEquals("ProfileSettingsDialog.vue", lookupSingle("account-settings-modal").target?.name)
    }

    fun testLoopRegistrationIsDynamicSite() {
        copyProject("dynamic")

        val snapshot = snapshot()

        assertEquals(1, snapshot.dynamicSites.size)
        assertEquals("LoginDialog.vue", lookupSingle("login-dialog").target?.name)
    }

    fun testDependenciesAndBuildOutputIgnored() {
        copyProject("vite-vue3")
        myFixture.addFileToProject("public/build/assets/app-abc.js", "app.component('zzz-bundle', X)")
        myFixture.addFileToProject("node_modules/pkg/index.js", "app.component('zzz-module', X)")
        myFixture.addFileToProject("vendor/pkg/resources/js/app.js", "app.component('zzz-vendor', X)")

        assertEmpty(snapshot().lookup("zzz-bundle"))
        assertEmpty(snapshot().lookup("zzz-module"))
        assertEmpty(snapshot().lookup("zzz-vendor"))
    }

    fun testSnapshotFollowsEdits() {
        copyProject("vite-vue3")
        assertEmpty(snapshot().lookup("late-comer"))

        val app = projectFile("resources/js/app.js")
        val document = PsiDocumentManager.getInstance(project).getDocument(app)!!
        WriteCommandAction.runWriteCommandAction(project) {
            document.insertString(document.textLength, "\napp.component('LateComer', AlertBanner)\n")
            PsiDocumentManager.getInstance(project).commitDocument(document)
        }

        assertEquals("AlertBanner.vue", lookupSingle("late-comer").target?.name)
    }

    fun testFallbackFilesIgnoreDependencies() {
        copyProject("vite-vue3")
        myFixture.addFileToProject("node_modules/pkg/DataTable.vue", "<template><div/></template>")
        myFixture.addFileToProject("vendor/pkg/DataTable.vue", "<template><div/></template>")

        val registry = BladeVueComponentRegistry.getInstance(project)
        val files = registry.fallbackFiles(registry.snapshot(), "data-table")

        assertEquals(listOf("/src/resources/js/components/DataTable.vue"), files.map { it.virtualFile.path })
    }

    private fun snapshot(): RegistrySnapshot = BladeVueComponentRegistry.getInstance(project).snapshot()

    private fun lookupSingle(tagName: String): Registration = snapshot().lookup(tagName).single()
}
