package com.giathuan.kotlinter.ktproto

import com.giathuan.kotlinter.ktproto.support.fix.ExpressionReplacerQuickFix
import com.giathuan.kotlinter.ktproto.support.parser.JavaProtoExpressionResolver.parseJavaGetDefaultInstanceExpression
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.ui.MultipleCheckboxOptionsPanel
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.idea.codeinsight.api.classic.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.psi.dotQualifiedExpressionVisitor
import javax.swing.JComponent

private val logger = Logger.getInstance(KtProtoGetDefaultInstanceInspection::class.java)

/**
 * An IntelliJ inspection to detect Java proto empty creation like `MyMessage.getDefaultInstance()`
 * and suggest transformation to Kotlin DSL like `myMessage {}`.
 */
class KtProtoGetDefaultInstanceInspection(@JvmField var enableLogging: Boolean = false) :
  AbstractKotlinInspection() {
  override fun createOptionsPanel(): JComponent =
    MultipleCheckboxOptionsPanel(this).apply {
      addCheckbox("Enable logging (expensive)", "enableLogging")
    }

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
    dotQualifiedExpressionVisitor { element ->
      analyze(element) {
        val dsl =
          try {
            parseJavaGetDefaultInstanceExpression(element).text()
          } catch (e: Throwable) {
            if (enableLogging) {
              logger.info(
                "Kotlinter: Failed to parse Java .getDefaultInstance() expression: ```\n${element.text}\n``` ",
                e,
              )
            }
            return@dotQualifiedExpressionVisitor
          }
        holder.registerProblem(
          element.originalElement,
          "Kotlinter: Better DSL for .getDefaultInstance() is available in Kotlin",
          ExpressionReplacerQuickFix(dsl, "Kotlinter: Transform .getDefaultInstance() to Kotlin DS"),
        )
      }
    }
}
