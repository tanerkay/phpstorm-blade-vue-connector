package bladevueconnector.inspections

import bladevueconnector.BladeVueBundle
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.modcommand.ModCommand
import com.intellij.modcommand.ModCommandQuickFix
import com.intellij.openapi.project.Project

/** Adds `^prefix-` to the inspection's ignored tag patterns, for tags of a component library. */
class IgnoreTagPrefixFix private constructor(
    private val inspection: BladeVueUnregisteredComponentInspection,
    private val prefix: String,
) : ModCommandQuickFix() {
    override fun getFamilyName(): String = BladeVueBundle.message("inspection.unregistered.fix.ignore.family")

    override fun getName(): String = BladeVueBundle.message("inspection.unregistered.fix.ignore.prefix", prefix)

    override fun perform(project: Project, descriptor: ProblemDescriptor): ModCommand {
        val pattern = "^$prefix"

        return ModCommand.updateInspectionOption(descriptor.psiElement, inspection) { updated ->
            if (pattern !in updated.ignoredTagPatterns) {
                updated.ignoredTagPatterns.add(pattern)
            }
        }
    }

    companion object {
        fun forTag(inspection: BladeVueUnregisteredComponentInspection, tagName: String): IgnoreTagPrefixFix? {
            val separator = tagName.indexOf('-')
            if (separator <= 0) {
                return null
            }

            return IgnoreTagPrefixFix(inspection, tagName.substring(0, separator + 1))
        }
    }
}
