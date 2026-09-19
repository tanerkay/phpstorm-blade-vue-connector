package bladevueconnector

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.PropertyKey

private const val BUNDLE = "messages.BladeVueBundle"

object BladeVueBundle : DynamicBundle(BladeVueBundle::class.java, BUNDLE) {
    @Nls
    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String = getMessage(key, *params)
}
