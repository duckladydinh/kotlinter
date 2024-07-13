package com.giathuan.kotlinter.ktproto.support.fix

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.util.CommentSaver
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtPsiFactory

/**
 * A generic fix that input the replacement expression as a string, converts it to KtExpression and
 * applies.
 */
class ExpressionReplacerQuickFix(private val text: String, private val name: String) :
  LocalQuickFix {
  override fun getName(): String = name

  override fun getFamilyName(): String = name

  override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
    val element = descriptor.psiElement as KtDotQualifiedExpression
    val commentSaver = CommentSaver(element)
    val factory = KtPsiFactory(project)
    val expression = factory.createExpression(text)
    commentSaver.restore(expression)
    element.replace(expression)
  }
}
