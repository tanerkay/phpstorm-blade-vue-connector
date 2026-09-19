package bladevueconnector.search

import bladevueconnector.names.ComponentNames
import com.intellij.util.indexing.DataIndexer
import com.intellij.util.indexing.DefaultFileTypeSpecificInputFilter
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID
import com.intellij.util.indexing.ScalarIndexExtension
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import com.jetbrains.php.blade.BladeFileType

/**
 * Blade views by the component tags they contain, keyed by [ComponentNames.key].
 *
 * The word index can't serve Find Usages here: it splits `verify-2fa-token` into three words.
 */
class BladeVueTagIndex : ScalarIndexExtension<String>() {
    override fun getName(): ID<String, Void> = NAME

    override fun getIndexer(): DataIndexer<String, Void, FileContent> = DataIndexer { input ->
        tagStart
            .findAll(input.contentAsText)
            .map { it.groupValues[1] }
            .filter { ComponentNames.looksLikeComponent(it) }
            .associate { ComponentNames.key(it) to null }
    }

    override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE

    override fun getVersion(): Int = ComponentNames.INDEX_VERSION

    override fun getInputFilter(): FileBasedIndex.InputFilter = DefaultFileTypeSpecificInputFilter(BladeFileType.INSTANCE)

    override fun dependsOnFileContent(): Boolean = true

    companion object {
        val NAME: ID<String, Void> = ID.create("bladevueconnector.component.tags")

        private val tagStart = Regex("<([A-Za-z][A-Za-z0-9_.:-]*)")
    }
}
