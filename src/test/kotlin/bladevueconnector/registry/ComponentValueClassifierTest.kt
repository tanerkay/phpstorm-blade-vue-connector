package bladevueconnector.registry

import bladevueconnector.BladeVueTestCase
import com.intellij.lang.javascript.psi.JSCallExpression
import com.intellij.lang.javascript.psi.JSExpression
import com.intellij.psi.util.PsiTreeUtil

class ComponentValueClassifierTest : BladeVueTestCase() {
    fun testDefaultImportIsModule() {
        val source = classifySecondArgument(
            "import FormTextInput from './components/FormTextInput.vue'\napp.component('TextInput', FormTextInput)",
        )

        assertEquals("./components/FormTextInput.vue", (source as ComponentSource.Module).specifier)
    }

    fun testImportFromPackageIsLibrary() {
        val source = classifySecondArgument("import vSelect from 'vue-select'\napp.component('v-select', vSelect)")

        assertEquals(ComponentSource.Library("vue-select"), source)
    }

    fun testNamedImportWithAliasIsLibrary() {
        val source = classifySecondArgument("import { VueSelect as vSelect } from 'vue-select'\napp.component('v-select', vSelect)")

        assertEquals(ComponentSource.Library("vue-select"), source)
    }

    fun testNamespaceImportIsUnknown() {
        val source = classifySecondArgument("import * as Lib from './lib'\napp.component('lib', Lib)")

        assertEquals(ComponentSource.Unknown, source)
    }

    fun testVariableHoldingAsyncComponentIsModule() {
        val source = classifySecondArgument(
            "const ActivityFeed = defineAsyncComponent(() => import('./components/ActivityFeed.vue'))\napp.component('ActivityFeed', ActivityFeed)",
        )

        assertEquals("./components/ActivityFeed.vue", (source as ComponentSource.Module).specifier)
    }

    fun testInlineDefinitionIsInline() {
        val source = classifySecondArgument("app.component('inline-thing', defineComponent({ template: '<div/>' }))")

        assertEquals(ComponentSource.Inline, source)
    }

    private fun classifySecondArgument(text: String): ComponentSource {
        val file = myFixture.addFileToProject("resources/js/app.js", text)
        val call = PsiTreeUtil.findChildrenOfType(file, JSCallExpression::class.java).single { it.arguments.size == 2 }
        val argument: JSExpression = call.arguments[1]

        return ComponentValueClassifier.classify(argument)
    }
}
