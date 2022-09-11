package com.giathuan.kotlinter.ktproto

import com.giathuan.kotlinter.ktproto.support.JavaProtoExpressionResolver.parseJavaProtoBuildExpression
import com.giathuan.kotlinter.ktproto.support.JavaProtoExpressionType
import com.giathuan.kotlinter.ktproto.support.KtProtoCopyExpression
import com.giathuan.kotlinter.ktproto.support.KtProtoCreatorExpression
import com.giathuan.kotlinter.ktproto.support.KtProtoCreatorExpression.Companion.buildKtProtoCreatorFun
import com.giathuan.kotlinter.ktproto.support.ParsedJavaProtoExpression
import com.giathuan.kotlinter.ktproto.support.SetterResolver.buildSettersCode
import com.giathuan.kotlinter.ktproto.support.StringTransformer.unwrapBracket
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.ui.MultipleCheckboxOptionsPanel
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.j2k.isInSingleLine
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.dotQualifiedExpressionVisitor
import javax.swing.JComponent

class KtProtoCreationInspection(@JvmField var avoidThisExpression: Boolean = false) :
    AbstractKotlinInspection() {

  override fun createOptionsPanel(): JComponent =
      MultipleCheckboxOptionsPanel(this).apply {
        addCheckbox("Avoid using `this.` expression (not recommended)", "avoidThisExpression")
      }

  override fun buildVisitor(
      holder: ProblemsHolder,
      isOnTheFly: Boolean,
  ): PsiElementVisitor = dotQualifiedExpressionVisitor { element ->
    val dsl =
        try {
          buildKtProtoDslFromJavaBuildExpression(parseJavaProtoBuildExpression(element))
        } catch (t: Throwable) {
          return@dotQualifiedExpressionVisitor
        }
    holder.registerProblem(
        element.originalElement,
        "Kotlinter: Better DSL for proto builder is available in Kotlin",
        ProblemHighlightType.WARNING,
        ExpressionReplacerQuickFix(dsl))
  }

  private fun buildKtProtoDslFromJavaBuildExpression(
      parsedJavaProtoExpression: ParsedJavaProtoExpression,
  ): String {
    val (parts, buildCreatorType, buildCreatorIndex) = parsedJavaProtoExpression
    val settersCode = buildSettersCode(parts, buildCreatorIndex, avoidThisExpression)
    when (buildCreatorType) {
      JavaProtoExpressionType.BUILD_FROM_NEW_BUILDER_EMPTY -> {
        val ktCreatorFunc = buildKtProtoCreatorFun(parts, buildCreatorIndex)
        return object : KtProtoCreatorExpression {
              override fun getCreatorFunc(): String = ktCreatorFunc
              override fun getSettersCode(): String = settersCode
            }
            .text()
      }
      JavaProtoExpressionType.BUILD_FROM_NEW_BUILDER_SOURCE -> {
        val argWithBracket = (parts[buildCreatorIndex] as KtCallExpression).lastChild
        val copySrc =
            if (argWithBracket.isInSingleLine()) unwrapBracket(argWithBracket.text.trim()).trim()
            else argWithBracket.text.trim()
        return object : KtProtoCopyExpression {
              override fun getCopySource(): String = copySrc
              override fun getSettersCode(): String = settersCode
            }
            .text()
      }
      JavaProtoExpressionType.BUILD_FROM_TO_BUILDER_EMPTY -> {
        val copySrc = parts.slice(0 until buildCreatorIndex).joinToString(".") { it.text }
        return object : KtProtoCopyExpression {
              override fun getCopySource(): String = copySrc
              override fun getSettersCode(): String = settersCode
            }
            .text()
      }
    }
  }
}
