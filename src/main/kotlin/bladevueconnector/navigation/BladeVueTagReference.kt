package bladevueconnector.navigation

import bladevueconnector.resolve.ComponentTagResolver
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.xml.XmlTag

/**
 * A soft reference from a component tag's start name to its component, which is what Find Usages
 * reports.
 *
 * Renaming or moving the component never rewrites the tag: the tag name follows the registration, not
 * the file name.
 */
class BladeVueTagReference(tag: XmlTag, range: TextRange) : PsiPolyVariantReferenceBase<XmlTag>(tag, range, true) {
    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> =
        PsiElementResolveResult.createResults(ComponentTagResolver.targets(ComponentTagResolver.resolve(element)))

    override fun handleElementRename(newElementName: String): PsiElement = element

    override fun bindToElement(element: PsiElement): PsiElement = this.element

    override fun getVariants(): Array<Any> = emptyArray()
}
