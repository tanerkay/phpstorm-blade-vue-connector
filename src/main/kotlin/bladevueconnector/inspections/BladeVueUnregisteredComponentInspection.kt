package bladevueconnector.inspections

import bladevueconnector.BladeVueBundle
import bladevueconnector.blade.BladePsi
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.options.OptPane
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.XmlElementVisitor
import com.intellij.psi.xml.XmlTag

class BladeVueUnregisteredComponentInspection : LocalInspectionTool() {
    /** Regular expressions; a tag matching any of them is never reported. */
    @JvmField
    var ignoredTagPatterns: MutableList<String> = mutableListOf()

    /** Ant-style paths relative to the content root. */
    @JvmField
    var excludedPaths: MutableList<String> = mutableListOf("resources/views/vendor/**")

    @JvmField
    var reportWhenDynamicRegistrationsExist: Boolean = false

    @JvmField
    var reportWhenPluginsInstalled: Boolean = false

    override fun getOptionsPane(): OptPane = OptPane.pane(
        OptPane.stringList("ignoredTagPatterns", BladeVueBundle.message("inspection.unregistered.option.ignored.tags")),
        OptPane.stringList("excludedPaths", BladeVueBundle.message("inspection.unregistered.option.excluded.paths")),
        OptPane.checkbox("reportWhenDynamicRegistrationsExist", BladeVueBundle.message("inspection.unregistered.option.dynamic")),
        OptPane.checkbox("reportWhenPluginsInstalled", BladeVueBundle.message("inspection.unregistered.option.plugins")),
    )

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (!BladePsi.isBladeFile(holder.file)) {
            return PsiElementVisitor.EMPTY_VISITOR
        }

        return object : XmlElementVisitor() {
            override fun visitXmlTag(tag: XmlTag) {
                val message = UnregisteredTagPolicy.problem(tag, this@BladeVueUnregisteredComponentInspection) ?: return
                val name = BladePsi.startName(tag) ?: return
                val fixes = listOfNotNull(IgnoreTagPrefixFix.forTag(this@BladeVueUnregisteredComponentInspection, name.text)).toTypedArray<LocalQuickFix>()

                holder.registerProblem(name, message, *fixes)
            }
        }
    }

    companion object {
        const val SHORT_NAME: String = "BladeVueUnregisteredComponent"
    }
}
