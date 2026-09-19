package bladevueconnector.registry

import com.intellij.lang.javascript.psi.JSCallExpression
import com.intellij.lang.javascript.psi.JSExpression
import com.intellij.lang.javascript.psi.JSFile
import com.intellij.lang.javascript.psi.JSLiteralExpression
import com.intellij.lang.javascript.psi.JSNewExpression
import com.intellij.lang.javascript.psi.JSObjectLiteralExpression
import com.intellij.lang.javascript.psi.JSProperty
import com.intellij.lang.javascript.psi.JSRecursiveWalkingElementVisitor
import com.intellij.lang.javascript.psi.JSReferenceExpression
import com.intellij.lang.javascript.psi.JSVariable
import com.intellij.lang.javascript.psi.ecma6.JSStringTemplateExpression
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager

/** Reads the component registrations, dynamic registrations and plugin installs of one JS/TS file. */
object RegistrationExtractor {
    private val appFactories = setOf("createApp", "createSSRApp")

    /** Plugins commonly installed with `.use()` that don't register components. */
    private val pluginsWithoutComponents = setOf(
        "pinia",
        "vuex",
        "vue-router",
        "vue-i18n",
        "laravel-vue-i18n",
        "ziggy-js",
        "vue-axios",
        "vue-scrollto",
        "vue-moment",
        "vee-validate",
        "@sentry/vue",
    )

    fun registrationsIn(file: JSFile): FileRegistrations =
        CachedValuesManager.getCachedValue(file) {
            CachedValueProvider.Result.create(extract(file), file)
        }

    private fun extract(file: JSFile): FileRegistrations {
        val registrations = mutableListOf<RawRegistration>()
        val dynamicSites = mutableListOf<PsiElement>()
        val pluginInstalls = mutableListOf<String>()

        file.accept(object : JSRecursiveWalkingElementVisitor() {
            override fun visitJSCallExpression(node: JSCallExpression) {
                super.visitJSCallExpression(node)

                val callee = node.methodExpression as? JSReferenceExpression ?: return
                val arguments = node.arguments

                when {
                    node is JSNewExpression -> {
                        if (callee.referenceName == "Vue" && callee.qualifier == null) {
                            rootLocalRegistrations(arguments.firstOrNull(), registrations, dynamicSites)
                        }
                    }

                    callee.referenceName in appFactories -> {
                        rootLocalRegistrations(arguments.firstOrNull(), registrations, dynamicSites)
                    }

                    callee.referenceName == "component" && callee.qualifier != null && arguments.size >= 2 -> {
                        val name = staticString(arguments[0])
                        if (name == null) {
                            dynamicSites += node
                        } else {
                            registrations += RawRegistration(
                                name,
                                RegistrationKind.Global,
                                arguments[0],
                                ComponentValueClassifier.classify(arguments[1]),
                            )
                        }
                    }

                    callee.referenceName == "use" && callee.qualifier != null && arguments.isNotEmpty() -> {
                        val packageName = ComponentValueClassifier.packageName(arguments[0])
                        if (packageName != null && packageName !in pluginsWithoutComponents) {
                            pluginInstalls += packageName
                        }
                    }
                }
            }
        })

        return FileRegistrations(registrations, dynamicSites, pluginInstalls)
    }

    private fun rootLocalRegistrations(
        options: JSExpression?,
        registrations: MutableList<RawRegistration>,
        dynamicSites: MutableList<PsiElement>,
    ) {
        val components = objectLiteral(options)?.findProperty("components") ?: return

        val value = components.value
        if (value !is JSObjectLiteralExpression) {
            if (value != null) {
                dynamicSites += components
            }

            return
        }

        for (entry in value.propertiesIncludingSpreads) {
            val property = entry as? JSProperty
            val name = property?.name
            if (property == null || property.computedPropertyName != null || name == null) {
                dynamicSites += entry
                continue
            }

            registrations += RawRegistration(
                name,
                RegistrationKind.RootLocal,
                property,
                ComponentValueClassifier.classify(property.value),
            )
        }
    }

    /** An object literal, or a variable initialised with one. */
    private fun objectLiteral(expression: JSExpression?): JSObjectLiteralExpression? {
        return when (expression) {
            is JSObjectLiteralExpression -> expression
            is JSReferenceExpression -> {
                val variable = ComponentValueClassifier.resolveLocally(expression) as? JSVariable

                variable?.initializer as? JSObjectLiteralExpression
            }
            else -> null
        }
    }

    private fun staticString(expression: JSExpression): String? {
        return when {
            expression is JSStringTemplateExpression -> expression.stringValue.takeIf { expression.arguments.isEmpty() }
            expression is JSLiteralExpression && expression.isQuotedLiteral -> expression.stringValue
            else -> null
        }
    }
}
