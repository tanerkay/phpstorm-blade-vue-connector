package bladevueconnector.navigation

import bladevueconnector.blade.BladePsi
import bladevueconnector.names.ComponentNames
import com.intellij.patterns.XmlPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.xml.XmlTag
import com.intellij.util.ProcessingContext

class BladeVueReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(XmlPatterns.xmlTag(), TagProvider)
    }

    private object TagProvider : PsiReferenceProvider() {
        override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
            val tag = element as? XmlTag ?: return PsiReference.EMPTY_ARRAY
            if (!BladePsi.isBladeFile(tag.containingFile)) {
                return PsiReference.EMPTY_ARRAY
            }

            val name = BladePsi.startName(tag) ?: return PsiReference.EMPTY_ARRAY
            if (!ComponentNames.looksLikeComponent(name.text)) {
                return PsiReference.EMPTY_ARRAY
            }

            return arrayOf(BladeVueTagReference(tag, name.textRange.shiftLeft(tag.textRange.startOffset)))
        }
    }
}
