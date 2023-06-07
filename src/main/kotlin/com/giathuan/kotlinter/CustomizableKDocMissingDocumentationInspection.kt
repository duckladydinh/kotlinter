package com.giathuan.kotlinter

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.ui.MultipleCheckboxOptionsPanel
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import com.siyeh.ig.psiutils.TestUtils
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.isOverridable
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.codeinsight.api.classic.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.codeinsight.utils.findExistingEditor
import org.jetbrains.kotlin.idea.core.unblockDocument
import org.jetbrains.kotlin.idea.inspections.describe
import org.jetbrains.kotlin.idea.kdoc.KDocElementFactory
import org.jetbrains.kotlin.idea.kdoc.findKDoc
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.namedDeclarationVisitor
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyPublicApi
import javax.swing.JComponent

class CustomizableKDocMissingDocumentationInspection(
    @JvmField var ignoreTests: Boolean = false,
    @JvmField var ignoreOverrideElements: Boolean = true,
    @JvmField var ignoreNonOverridableProperties: Boolean = true
) : AbstractKotlinInspection() {

  override fun createOptionsPanel(): JComponent {
    val panel = MultipleCheckboxOptionsPanel(this)
    panel.addCheckbox("Ignore tests", "ignoreTests")
    panel.addCheckbox("Ignore override elements", "ignoreOverrideElements")
    panel.addCheckbox("Ignore non-overridable properties", "ignoreNonOverridableProperties")
    return panel
  }

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
      namedDeclarationVisitor { element ->
        if (TestUtils.isInTestSourceContent(element) &&
            (ignoreTests || element.containingFile.name.endsWith(KOTLIN_TEST_FILE_SUFFIX))) {
          return@namedDeclarationVisitor
        }
        val nameIdentifier = element.nameIdentifier
        if (nameIdentifier != null) {
          if (ignoreOverrideElements &&
              element.modifierList?.hasModifier(KtTokens.OVERRIDE_KEYWORD) == true) {
            return@namedDeclarationVisitor
          }
          if (element.findKDoc {
            DescriptorToSourceUtilsIde.getAnyDeclaration(element.project, it)
          } == null) {
            val descriptor =
                element.resolveToDescriptorIfAny() as? DeclarationDescriptorWithVisibility
                    ?: return@namedDeclarationVisitor
            if (ignoreNonOverridableProperties &&
                descriptor is PropertyDescriptor &&
                !descriptor.isOverridable) {
              return@namedDeclarationVisitor
            }
            if (descriptor.isEffectivelyPublicApi) {
              val message =
                  element.describe()?.let { "Kotlinter: $it is missing documentation" }
                      ?: "Kotlinter: Missing documentation"
              holder.registerProblem(nameIdentifier, message, AddDocumentationFix())
            }
          }
        }
      }

  private class AddDocumentationFix : LocalQuickFix {
    override fun getName(): String = "Kotlinter: Add documentation"

    override fun getFamilyName(): String = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
      try {
        val declaration =
            descriptor.psiElement.getParentOfType<KtNamedDeclaration>(true)
                ?: throw IllegalStateException("Can't find declaration")

        declaration.addBefore(
            KDocElementFactory(project).createKDocFromText("/**\n*\n*/\n"), declaration.firstChild)

        val editor = descriptor.psiElement.findExistingEditor() ?: return

        // If we just add whitespace
        // /**
        //  *[HERE]
        // it will be erased by formatter, so following code adds it right way and moves caret then
        editor.unblockDocument()

        val section = declaration.firstChild.getChildOfType<KDocSection>() ?: return
        val asterisk = section.firstChild

        editor.caretModel.moveToOffset(asterisk.endOffset)
        EditorModificationUtil.insertStringAtCaret(editor, " ")
      } catch (_: Throwable) {}
    }
  }

  companion object {

    private const val KOTLIN_TEST_FILE_SUFFIX = "Test.kt"
  }
}
