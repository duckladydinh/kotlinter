package com.giathuan.kotlinter.ktproto

import com.giathuan.kotlinter.ktproto.support.JavaProtoExpressionResolver.isArgumentOfSomeProtoSetter
import com.giathuan.kotlinter.ktproto.support.JavaProtoExpressionResolver.isJavaProtoMissingBuildExpression
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.ui.MultipleCheckboxOptionsPanel
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.idea.util.CommentSaver
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.dotQualifiedExpressionVisitor
import javax.swing.JComponent

class KtProtoSetterUsingBuilderArgumentInspection(
    @JvmField var fixNonProtoSetterExpressions: Boolean = false
) : AbstractKotlinInspection() {

  override fun createOptionsPanel(): JComponent =
      MultipleCheckboxOptionsPanel(this).apply {
        addCheckbox("Fix expressions outside proto-setters", "fixNonProtoSetterExpressions")
      }

  override fun buildVisitor(
      holder: ProblemsHolder,
      isOnTheFly: Boolean,
  ): PsiElementVisitor = dotQualifiedExpressionVisitor { element ->
    if (element.parent is KtDotQualifiedExpression ||
        (!fixNonProtoSetterExpressions && !isArgumentOfSomeProtoSetter(element))) {
      return@dotQualifiedExpressionVisitor
    }
    if (isJavaProtoMissingBuildExpression(element)) {
      val lastSetter = element.callExpression as KtCallExpression
      holder.registerProblem(
          lastSetter.originalElement,
          "Kotlinter: You should add a .build()",
          ProblemHighlightType.WEAK_WARNING,
          AddBuildQuickFix())
    }
  }

  class AddBuildQuickFix : LocalQuickFix {
    override fun getName(): String = "Kotlinter: Add a .build()"

    override fun getFamilyName(): String = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
      val element = descriptor.psiElement as KtCallExpression
      val commentSaver = CommentSaver(element)
      val factory = KtPsiFactory(project)
      val expression = factory.createExpression("${element.text.trimEnd()}\n.build()")
      commentSaver.restore(expression)
      element.replace(expression)
    }
  }
}
