package com.giathuan.kotlinter.ktproto

import com.giathuan.kotlinter.ktproto.support.fix.ExpressionReplacerQuickFix
import com.giathuan.kotlinter.ktproto.support.model.JavaProtoExpressionParsedData
import com.giathuan.kotlinter.ktproto.support.model.JavaProtoExpressionType
import com.giathuan.kotlinter.ktproto.support.model.KtProtoCopyExpression
import com.giathuan.kotlinter.ktproto.support.model.KtProtoCreatorExpression
import com.giathuan.kotlinter.ktproto.support.model.KtProtoCreatorExpression.Companion.buildKtProtoCreatorFunc
import com.giathuan.kotlinter.ktproto.support.parser.JavaProtoExpressionResolver.parseJavaProtoBuildExpression
import com.giathuan.kotlinter.ktproto.support.parser.SetterResolver.buildSettersCode
import com.giathuan.kotlinter.ktproto.support.utility.StringTransformer.unwrapRoundBracket
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.ui.MultipleCheckboxOptionsPanel
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.idea.codeinsight.api.classic.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.nj2k.isInSingleLine
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.dotQualifiedExpressionVisitor
import javax.swing.JComponent

/**
 * An IntelliJ inspection to detect Java proto builder expression like
 * `MyMessage.newBuilder().setSomething(x).build()` and suggest transformation to Kotlin DSL like
 * `myMessage { this.something = x }`.
 */
class KtProtoCreationInspection(@JvmField var avoidThisExpression: Boolean = false) :
  AbstractKotlinInspection() {

  override fun createOptionsPanel(): JComponent =
    MultipleCheckboxOptionsPanel(this).apply {
      addCheckbox("Avoid using `this.` expression (not recommended)", "avoidThisExpression")
    }

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
    dotQualifiedExpressionVisitor { element ->
      analyze(element) {
        val dsl =
          try {
            val javaProtoExpressionParsedData = parseJavaProtoBuildExpression(element)
            buildKtProtoDslFromJavaBuildExpression(javaProtoExpressionParsedData)
          } catch (_: Throwable) {
            return@dotQualifiedExpressionVisitor
          }
        holder.registerProblem(
          element.originalElement,
          "Kotlinter: Better DSL for proto builder is available in Kotlin",
          ExpressionReplacerQuickFix(dsl, name = "Kotlinter: Transform to Kotlin DSL"),
        )
      }
    }

  private fun KaSession.buildKtProtoDslFromJavaBuildExpression(
    javaProtoExpressionParsedData: JavaProtoExpressionParsedData
  ): String {
    val (parts, buildCreatorType, buildCreatorIndex) = javaProtoExpressionParsedData
    val settersCode =
      buildSettersCode(parts, firstSetterIndex = buildCreatorIndex + 1, avoidThisExpression)
    when (buildCreatorType) {
      JavaProtoExpressionType.BUILD_FROM_NEW_BUILDER_EMPTY -> {
        val ktCreatorFunc =
          buildKtProtoCreatorFunc(parts, simpleTypeNameIndex = buildCreatorIndex - 1)
        return object : KtProtoCreatorExpression {
          override fun getCreatorFunc(): String = ktCreatorFunc

          override fun getSettersCode(): String = settersCode
        }
          .text()
      }

      JavaProtoExpressionType.BUILD_FROM_NEW_BUILDER_SOURCE -> {
        val argWithBracket = (parts[buildCreatorIndex] as KtCallExpression).lastChild
        val copySrc =
          if (argWithBracket.isInSingleLine()) unwrapRoundBracket(argWithBracket.text)
          else argWithBracket.text.trim()
        return object : KtProtoCopyExpression {
          override fun getCopySource(): String = copySrc

          override fun getSettersCode(): String = settersCode
        }
          .text()
      }

      JavaProtoExpressionType.BUILD_FROM_TO_BUILDER_EMPTY -> {
        val copySrc = parts.slice(0..<buildCreatorIndex).joinToString(".") { it.text }
        return object : KtProtoCopyExpression {
          override fun getCopySource(): String = copySrc

          override fun getSettersCode(): String = settersCode
        }
          .text()
      }
    }
  }
}
