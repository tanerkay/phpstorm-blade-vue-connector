package bladevueconnector

import com.intellij.psi.PsiFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase

abstract class BladeVueTestCase : BasePlatformTestCase() {
    override fun getTestDataPath(): String = "src/test/testData"

    protected fun copyProject(name: String) {
        myFixture.copyDirectoryToProject(name, "")
    }

    /** Adds a Blade view at [path] and opens it, with the caret at `<caret>` when present. */
    protected fun openBlade(path: String, text: String): PsiFile {
        val caret = text.indexOf(CARET)
        val file = myFixture.addFileToProject(path, text.replace(CARET, ""))
        myFixture.configureFromExistingVirtualFile(file.virtualFile)
        if (caret >= 0) {
            myFixture.editor.caretModel.moveToOffset(caret)
        }

        return file
    }

    protected fun projectFile(path: String): PsiFile {
        val virtualFile = myFixture.findFileInTempDir(path) ?: error("No file at $path")

        return psiManager.findFile(virtualFile) ?: error("No PSI for $path")
    }

    private companion object {
        const val CARET = "<caret>"
    }
}
