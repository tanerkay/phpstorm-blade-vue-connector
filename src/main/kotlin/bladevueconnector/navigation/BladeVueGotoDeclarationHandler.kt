package bladevueconnector.navigation

import bladevueconnector.blade.BladePsi
import bladevueconnector.resolve.ComponentTagResolver
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement

/**
 * Ctrl+click on a component tag's start or end name.
 *
 * Runs before reference resolution, where the platform's tag name reference resolves an unknown tag to
 * itself.
 */
class BladeVueGotoDeclarationHandler : GotoDeclarationHandler {
    override fun getGotoDeclarationTargets(sourceElement: PsiElement?, offset: Int, editor: Editor?): Array<PsiElement>? {
        val file = sourceElement?.containingFile ?: return null
        if (!BladePsi.isBladeFile(file)) {
            return null
        }

        val tag = BladePsi.tagWithNameAt(file, offset) ?: return null
        val targets = ComponentTagResolver.targets(ComponentTagResolver.resolve(tag))

        return targets.takeIf { it.isNotEmpty() }?.toTypedArray()
    }
}
