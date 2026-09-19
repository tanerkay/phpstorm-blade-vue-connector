package bladevueconnector.inspections

import bladevueconnector.blade.BladePsi
import bladevueconnector.resolve.ComponentTagResolver
import com.intellij.codeInsight.daemon.HighlightDisplayKey
import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.profile.codeInspection.InspectionProjectProfileManager
import com.intellij.psi.PsiElement

/**
 * Hides "Unknown HTML tag" on component tags: recognised ones, and ones the unregistered component
 * inspection reports itself.
 */
class BladeVueUnknownTagSuppressor : InspectionSuppressor {
    override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean {
        if (toolId != UNKNOWN_TAG_TOOL_ID) {
            return false
        }

        val tag = BladePsi.tagOfNameToken(element) ?: return false
        if (!BladePsi.isBladeFile(tag.containingFile) || ComponentTagResolver.componentName(tag) == null) {
            return false
        }

        return try {
            ComponentTagResolver.isRecognised(ComponentTagResolver.resolve(tag)) || isReportedByUnregisteredInspection(element)
        } catch (_: IndexNotReadyException) {
            false
        }
    }

    override fun getSuppressActions(element: PsiElement?, toolId: String): Array<SuppressQuickFix> = SuppressQuickFix.EMPTY_ARRAY

    private fun isReportedByUnregisteredInspection(element: PsiElement): Boolean {
        val tag = BladePsi.tagOfNameToken(element) ?: return false
        val profile = InspectionProjectProfileManager.getInstance(element.project).currentProfile
        val key = HighlightDisplayKey.find(BladeVueUnregisteredComponentInspection.SHORT_NAME) ?: return false
        if (!profile.isToolEnabled(key, element)) {
            return false
        }

        val inspection = profile.getUnwrappedTool(BladeVueUnregisteredComponentInspection.SHORT_NAME, element)
            as? BladeVueUnregisteredComponentInspection
            ?: return false

        return UnregisteredTagPolicy.problem(tag, inspection) != null
    }

    private companion object {
        const val UNKNOWN_TAG_TOOL_ID = "HtmlUnknownTag"
    }
}
