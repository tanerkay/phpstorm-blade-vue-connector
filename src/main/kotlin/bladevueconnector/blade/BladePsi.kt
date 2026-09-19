package bladevueconnector.blade

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.psi.xml.XmlToken
import com.intellij.xml.util.XmlTagUtil
import com.jetbrains.php.blade.BladeLanguage

/**
 * Blade files have two PSI trees: the Blade tree and the HTML (template data) tree. Tags only exist in
 * the HTML tree.
 */
object BladePsi {
    fun isBladeFile(file: PsiFile): Boolean = file.viewProvider.baseLanguage == BladeLanguage.INSTANCE

    fun htmlRoot(file: PsiFile): XmlFile? {
        val viewProvider = file.viewProvider
        if (viewProvider.baseLanguage != BladeLanguage.INSTANCE) {
            return null
        }

        return viewProvider.allFiles.firstOrNull { it is XmlFile && it.language != viewProvider.baseLanguage } as? XmlFile
    }

    /**
     * The tag whose start or end name token is at [offset], looked up in the HTML tree whichever tree
     * the caller holds.
     */
    fun tagWithNameAt(file: PsiFile, offset: Int): XmlTag? {
        val root = htmlRoot(file) ?: return null

        return listOf(offset, offset - 1)
            .asSequence()
            .filter { it >= 0 }
            .mapNotNull { root.findElementAt(it) }
            .firstNotNullOfOrNull { tagOfNameToken(it) }
    }

    fun tagOfNameToken(element: PsiElement): XmlTag? {
        val tag = element.parent as? XmlTag ?: return null

        return tag.takeIf { element == startName(it) || element == endName(it) }
    }

    fun startName(tag: XmlTag): XmlToken? = XmlTagUtil.getStartTagNameElement(tag)

    fun endName(tag: XmlTag): XmlToken? = XmlTagUtil.getEndTagNameElement(tag)
}
