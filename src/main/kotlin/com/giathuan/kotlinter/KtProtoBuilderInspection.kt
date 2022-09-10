package com.giathuan.kotlinter

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.ui.MultipleCheckboxOptionsPanel
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.core.resolveType
import org.jetbrains.kotlin.idea.debugger.sequence.psi.callName
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.idea.refactoring.getLineNumber
import org.jetbrains.kotlin.idea.util.CommentSaver
import org.jetbrains.kotlin.j2k.isInSingleLine
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.dotQualifiedExpressionVisitor
import org.jetbrains.kotlin.psi.psiUtil.getPrevSiblingIgnoringWhitespace
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.supertypes
import org.jetbrains.kotlinx.serialization.compiler.backend.common.serialName
import javax.swing.JComponent

class KtProtoBuilderInspection(@JvmField var avoidThisExpression: Boolean = false) :
    AbstractKotlinInspection() {

  override fun createOptionsPanel(): JComponent =
      MultipleCheckboxOptionsPanel(this).apply {
        addCheckbox("Avoid using `this.` expression", "avoidThisExpression")
      }

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
      dotQualifiedExpressionVisitor { element ->
        if (!element.resolveType().isChildOf(MESSAGE_LITE_TYPENAME) ||
            element.callExpression?.text != BUILD_CALL ||
            !element.text.contains(NEW_BUILDER_CALL_NAME)) {
          return@dotQualifiedExpressionVisitor
        }
        try {
          val dsl = buildProtoDsl(element)
          holder.registerProblem(
              element.originalElement,
              "Kotlinter: Better DSL for proto builder is available in Kotlin",
              ProblemHighlightType.WARNING,
              DslQuickFix(dsl))
        } catch (_: Throwable) {}
      }

  private fun buildProtoDsl(element: KtDotQualifiedExpression): String {
    val parts = splitDotQualifiedExpression(element)
    val newBuilderIndex =
        parts.indexOfFirst { it is KtCallExpression && "${it.callName()}()" == NEW_BUILDER_CALL }
    if (newBuilderIndex <= 0) {
      throw IllegalArgumentException("Can't find suitable `$NEW_BUILDER_CALL` in `${element.text}`")
    }
    val newBuilderCall = parts[newBuilderIndex] as KtCallExpression

    if (newBuilderCall.valueArguments.isEmpty()) {
      val setters = buildSetters(parts, newBuilderIndex)
      val typeName = buildTypeName(parts, newBuilderIndex)
      return """$typeName {
               $setters
               }
           """.trimIndent()
    }

    if (newBuilderCall.valueArguments.size == 1) {
      val inBracketExpression = newBuilderCall.lastChild
      val parent =
          if (inBracketExpression.isInSingleLine()) inBracketExpression.text.drop(1).dropLast(1)
          else inBracketExpression.text
      val setters = buildSetters(parts, newBuilderIndex)
      return """${parent.trimEnd()}.copy {
               $setters
               }
           """.trimIndent()
    }

    throw IllegalArgumentException("This is not a $NEW_BUILDER_CALL call: ${newBuilderCall.text}")
  }

  private fun buildSetters(
      parts: List<KtExpression>,
      newBuilderIndex: Int,
  ): String {
    val builder = StringBuilder()
    for (setter in
        parts.slice(newBuilderIndex + 1 until parts.size).map { it as KtCallExpression }) {
      val precedingCommentBlock = getPrecedingDotCallComments(setter)
      if (precedingCommentBlock.block.isNotBlank()) {
        if (precedingCommentBlock.firstCommentNotInItsOwnLine && builder.endsWith("\n")) {
          builder.deleteCharAt(builder.lastIndex)
        }
        builder.append(precedingCommentBlock.block)
        builder.append("\n")
      }
      if (setter.text == BUILD_CALL) {
        break
      }

      val fieldName = setter.setterFieldName()

      val simplifiedValue = setter.valueArguments[0].text
      if (!avoidThisExpression || fieldName == simplifiedValue) {
        builder.append("this.")
      }

      val rawValue = setter.lastChild.text.drop(1).dropLast(1)
      builder.append("$fieldName = $rawValue\n")
    }

    return builder.toString().trimEnd()
  }

  private fun buildTypeName(parts: List<KtExpression>, newBuilderIndex: Int): String {
    val ktMessageCreator = parts[newBuilderIndex - 1].text
    if (!ktMessageCreator.startsWithUpperCase()) {
      throw IllegalArgumentException(
          "The first part on the right of `$NEW_BUILDER_CALL` must starts with an uppercase")
    }
    if (newBuilderIndex == 1) {
      return ktMessageCreator.variableCase()
    }

    val prefix =
        parts
            .slice(0 until newBuilderIndex - 1)
            .map { (it as KtNameReferenceExpression).text }
            .joinToString(".") {
              if (it.startsWithUpperCase()) {
                it + "Kt"
              } else {
                it
              }
            }
    return "$prefix.${ktMessageCreator.variableCase()}"
  }

  class DslQuickFix(private val dsl: String) : LocalQuickFix {
    override fun getName(): String = "Kotlinter: Transform to Kotlin DSL"

    override fun getFamilyName(): String = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
      val element = descriptor.psiElement as KtDotQualifiedExpression
      val commentSaver = CommentSaver(element)
      val factory = KtPsiFactory(project)
      val expression = factory.createExpression(dsl)
      commentSaver.restore(expression)
      element.replace(expression)
    }
  }

  companion object {
    private const val MESSAGE_LITE_TYPENAME = "com.google.protobuf.MessageLite"
    private const val ADD_ALL_PREFIX = "addAll"
    private const val ADD_PREFIX = "add"
    private const val SET_PREFIX = "set"
    private const val NEW_BUILDER_CALL = "newBuilder()"
    private const val NEW_BUILDER_CALL_NAME = "newBuilder"
    private const val BUILD_CALL = "build()"

    private fun String.startsWithUpperCase(): Boolean {
      return this.first().isUpperCase()
    }

    private fun String.variableCase(): String = this.first().lowercase() + this.drop(1)

    private fun KotlinType?.isChildOf(className: String): Boolean {
      return this != null && this.supertypes().any { it.serialName() == className }
    }

    private fun KtCallExpression.setterFieldName(): String {
      if (this.valueArguments.size != 1) {
        throw IllegalArgumentException("Expect exactly 1 argument, not ${this.valueArguments.size}")
      }
      val text = this.callName()
      if (text.startsWith(ADD_ALL_PREFIX) && text.length >= 7 && text[6].isUpperCase()) {
        return text.drop(6).variableCase()
      }
      if ((text.startsWith(SET_PREFIX) || text.startsWith(ADD_PREFIX)) &&
          text.length >= 4 &&
          text[3].isUpperCase()) {
        return text.drop(3).variableCase()
      }
      throw IllegalArgumentException("Definitely not a setter: $text")
    }

    private data class PrecedingCommentBlock(
        val block: String,
        val firstCommentNotInItsOwnLine: Boolean
    )

    private fun getPrecedingDotCallComments(setter: KtCallExpression): PrecedingCommentBlock {
      var node = setter.prevSibling
      val reversedComments = mutableListOf<PsiComment>()
      while (node.getPrevSiblingIgnoringWhitespace() is PsiComment) {
        node = node.getPrevSiblingIgnoringWhitespace()
        reversedComments += node as PsiComment
      }

      node = node.getPrevSiblingIgnoringWhitespace()
      val commentBlockBuilder = StringBuilder()
      val comments = reversedComments.asReversed()
      for (i in comments.indices) {
        if (i > 0 &&
            comments[i].getLineNumber(start = true) !=
                comments[i - 1].getLineNumber(start = false)) {
          commentBlockBuilder.append("\n")
        }
        commentBlockBuilder.append(comments[i].text)
      }

      val firstCommentNotInItsOwnLine =
          comments.isNotEmpty() &&
              node != null &&
              comments.first().isInSingleLine() &&
              comments.first().getLineNumber(start = true) == node.getLineNumber(start = false)

      return PrecedingCommentBlock(commentBlockBuilder.toString(), firstCommentNotInItsOwnLine)
    }

    private fun splitDotQualifiedExpression(element: KtDotQualifiedExpression): List<KtExpression> {
      var node = element
      if (element.selectorExpression == null) {
        return emptyList()
      }
      val parts = mutableListOf(element.selectorExpression as KtExpression)
      while (node.receiverExpression is KtDotQualifiedExpression) {
        node = node.receiverExpression as KtDotQualifiedExpression
        parts += node.selectorExpression ?: return emptyList()
      }
      parts += node.receiverExpression
      return parts.asReversed()
    }
  }
}
