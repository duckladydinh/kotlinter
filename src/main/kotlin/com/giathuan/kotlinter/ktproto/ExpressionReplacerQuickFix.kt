package com.giathuan.kotlinter.ktproto

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.util.CommentSaver
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtPsiFactory

class ExpressionReplacerQuickFix(private val text: String) : LocalQuickFix {
  override fun getName(): String = "Kotlinter: Transform to Kotlin DSL"

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
