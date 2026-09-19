package bladevueconnector

import bladevueconnector.inspections.BladeVueUnregisteredComponentInspection
import com.intellij.codeInspection.LocalInspectionEP
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class PluginDescriptorTest : BasePlatformTestCase() {
    /**
     * With a `<resource-bundle>` in plugin.xml, the IDE reads the description through the bundle, which
     * strips every `&` as a mnemonic marker and shows `&lt;text-input&gt;` as `lt;text-inputgt;`.
     */
    fun testDescriptionKeepsEscapedTags() {
        val description = PluginManagerCore.getPlugin(PluginId.getId("blade-vue-connector"))!!.description!!

        assertTrue(description, description.contains("<code>&lt;text-input&gt;</code>"))
    }

    fun testInspectionDisplayNameResolvesFromItsBundle() {
        val extension = LocalInspectionEP.LOCAL_INSPECTION.extensionList
            .single { it.shortName == BladeVueUnregisteredComponentInspection.SHORT_NAME }

        assertEquals("Unregistered Vue component", extension.getDisplayName())
        assertEquals("Blade Vue Connector", extension.getGroupDisplayName())
    }
}
