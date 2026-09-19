package bladevueconnector.highlighting

import bladevueconnector.blade.BladePsi
import bladevueconnector.resolve.ComponentTagResolver
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlTag

/** Colours the start and end names of recognised component tags. */
class BladeVueTagAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val tag = element as? XmlTag ?: return
        if (!BladePsi.isBladeFile(holder.currentAnnotationSession.file)) {
            return
        }

        if (ComponentTagResolver.componentName(tag) == null) {
            return
        }

        if (!ComponentTagResolver.isRecognised(ComponentTagResolver.resolve(tag))) {
            return
        }

        listOfNotNull(BladePsi.startName(tag), BladePsi.endName(tag)).forEach { name ->
            holder
                .newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(name)
                .textAttributes(BladeVueColors.COMPONENT_TAG)
                .create()
        }
    }
}
