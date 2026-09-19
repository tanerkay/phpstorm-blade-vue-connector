package bladevueconnector.registry

import com.intellij.lang.ecmascript6.psi.ES6ImportCall
import com.intellij.lang.ecmascript6.psi.ES6ImportDeclaration
import com.intellij.lang.ecmascript6.psi.ES6ImportedBinding
import com.intellij.lang.javascript.psi.JSCallExpression
import com.intellij.lang.javascript.psi.JSExpression
import com.intellij.lang.javascript.psi.JSFunction
import com.intellij.lang.javascript.psi.JSLiteralExpression
import com.intellij.lang.javascript.psi.JSObjectLiteralExpression
import com.intellij.lang.javascript.psi.JSParenthesizedExpression
import com.intellij.lang.javascript.psi.JSReferenceExpression
import com.intellij.lang.javascript.psi.JSVariable
import com.intellij.lang.javascript.psi.util.JSStubBasedPsiTreeUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

/** Works out where the value passed to a component registration comes from. */
object ComponentValueClassifier {
    private const val MAX_DEPTH = 4

    fun classify(expression: JSExpression?): ComponentSource = classify(expression, 0)

    private fun classify(expression: JSExpression?, depth: Int): ComponentSource {
        val value = unwrap(expression) ?: return ComponentSource.Unknown
        if (depth > MAX_DEPTH) {
            return ComponentSource.Unknown
        }

        return when (value) {
            is JSReferenceExpression -> classifyReference(value, depth)
            is ES6ImportCall -> moduleOfImportCall(value)
            is JSCallExpression -> classifyCall(value, depth)
            is JSFunction -> moduleLoadedBy(value)
            is JSObjectLiteralExpression -> ComponentSource.Inline
            else -> ComponentSource.Unknown
        }
    }

    /** The package name when [expression] is an import or `require()` of a bare package. */
    fun packageName(expression: JSExpression?): String? {
        return when (val source = classify(expression)) {
            is ComponentSource.Library -> source.packageName
            else -> null
        }
    }

    fun isBarePackage(specifier: String): Boolean =
        !specifier.startsWith(".") && !specifier.startsWith("/") && !specifier.startsWith("@/") && !specifier.startsWith("~")

    private fun classifyReference(reference: JSReferenceExpression, depth: Int): ComponentSource {
        val qualifier = unwrap(reference.qualifier as? JSExpression)

        // require('./X.vue').default
        if (reference.referenceName == "default" && qualifier is JSCallExpression && qualifier.isRequireCall) {
            return moduleOfRequire(qualifier)
        }

        if (qualifier != null) {
            return ComponentSource.Unknown
        }

        val resolved = resolveLocally(reference) ?: return ComponentSource.Unknown

        val declaration = PsiTreeUtil.getParentOfType(resolved, ES6ImportDeclaration::class.java, false)
        if (declaration != null) {
            return moduleOfImport(declaration, resolved)
        }

        if (resolved is JSVariable) {
            return classify(resolved.initializer, depth + 1)
        }

        return ComponentSource.Unknown
    }

    private fun classifyCall(call: JSCallExpression, depth: Int): ComponentSource {
        if (call.isRequireCall) {
            return moduleOfRequire(call)
        }

        val callee = call.methodExpression as? JSReferenceExpression ?: return ComponentSource.Unknown
        val argument = call.arguments.firstOrNull()

        return when (callee.referenceName) {
            // defineAsyncComponent(() => import('./X.vue')) or defineAsyncComponent({ loader: ... })
            "defineAsyncComponent" -> when (val loader = unwrap(argument)) {
                is JSObjectLiteralExpression -> classify(loader.findProperty("loader")?.value, depth + 1)
                else -> classify(loader, depth + 1)
            }

            "defineComponent", "extend" -> ComponentSource.Inline
            else -> ComponentSource.Unknown
        }
    }

    private fun moduleOfImport(declaration: ES6ImportDeclaration, binding: PsiElement): ComponentSource {
        if (binding is ES6ImportedBinding && binding.isNamespaceImport) {
            return ComponentSource.Unknown
        }

        val fromClause = declaration.fromClause ?: return ComponentSource.Unknown
        val specifier = fromClause.referenceText?.let { StringUtil.unquoteString(it) } ?: return ComponentSource.Unknown
        if (isBarePackage(specifier)) {
            return ComponentSource.Library(specifier)
        }

        return ComponentSource.Module(specifier, fromClause)
    }

    private fun moduleOfRequire(call: JSCallExpression): ComponentSource {
        val literal = call.arguments.firstOrNull() as? JSLiteralExpression ?: return ComponentSource.Unknown
        val specifier = literal.stringValue ?: return ComponentSource.Unknown
        if (isBarePackage(specifier)) {
            return ComponentSource.Library(specifier)
        }

        return ComponentSource.Module(specifier, literal)
    }

    private fun moduleOfImportCall(call: ES6ImportCall): ComponentSource {
        val specifier = call.stringArgument?.stringValue ?: return ComponentSource.Unknown
        if (isBarePackage(specifier)) {
            return ComponentSource.Library(specifier)
        }

        return ComponentSource.Module(specifier, call)
    }

    /** `() => import('./X.vue')` or a Vue 2 async factory calling `require`. */
    private fun moduleLoadedBy(function: JSFunction): ComponentSource {
        val importCall = PsiTreeUtil.findChildOfType(function, ES6ImportCall::class.java)
        if (importCall != null) {
            return moduleOfImportCall(importCall)
        }

        val requireCall = PsiTreeUtil.findChildrenOfType(function, JSCallExpression::class.java).firstOrNull { it.isRequireCall }
        if (requireCall != null) {
            return moduleOfRequire(requireCall)
        }

        return ComponentSource.Unknown
    }

    /**
     * The declaration of an unqualified name in its file: an import binding or a variable. Plain
     * [JSReferenceExpression.resolve] goes through imports to the imported module instead.
     */
    fun resolveLocally(reference: JSReferenceExpression): PsiElement? {
        val name = reference.referenceName ?: return null

        return JSStubBasedPsiTreeUtil.resolveLocally(name, reference)
    }

    private fun unwrap(expression: PsiElement?): JSExpression? {
        var current = expression as? JSExpression
        while (current is JSParenthesizedExpression) {
            current = current.innerExpression
        }

        return current
    }
}
