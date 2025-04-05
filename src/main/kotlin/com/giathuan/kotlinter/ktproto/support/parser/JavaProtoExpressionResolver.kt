package com.giathuan.kotlinter.ktproto.support.parser

import com.giathuan.kotlinter.ktproto.support.model.JavaProtoConstants
import com.giathuan.kotlinter.ktproto.support.model.JavaProtoConstants.MESSAGE_LITE_OR_BUILDER_TYPENAME
import com.giathuan.kotlinter.ktproto.support.model.JavaProtoConstants.MESSAGE_LITE_TYPENAME
import com.giathuan.kotlinter.ktproto.support.model.JavaProtoConstants.NEW_BUILDER_CALL_NAME
import com.giathuan.kotlinter.ktproto.support.model.JavaProtoConstants.TO_BUILDER_CALL
import com.giathuan.kotlinter.ktproto.support.model.JavaProtoExpressionParsedData
import com.giathuan.kotlinter.ktproto.support.model.JavaProtoExpressionType
import com.giathuan.kotlinter.ktproto.support.model.KtProtoCreatorExpression
import com.giathuan.kotlinter.ktproto.support.model.KtProtoCreatorExpression.Companion.buildKtProtoCreatorFunc
import com.giathuan.kotlinter.ktproto.support.parser.DotQualifiedExpressionSplitter.splitDotQualifiedExpression
import com.giathuan.kotlinter.ktproto.support.parser.SetterResolver.generateSingleSetter
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.KtValueArgumentList

/** Utilities to resolve different Java proto expressions. */
object JavaProtoExpressionResolver {
  /** Returns myMessage {} for MyMessage.getDefaultInstance(). */
  fun KaSession.parseJavaGetDefaultInstanceExpression(
    element: KtDotQualifiedExpression
  ): KtProtoCreatorExpression {
    val text = element.text
    val getDefaultInstantExpressionCallText =
      element.callExpression?.text ?: throw invalid("The last part is not a call expression: $text")

    if (getDefaultInstantExpressionCallText != JavaProtoConstants.GET_DEFAULT_INSTANCE_CALL) {
      throw invalid("It misses .getDefaultInstance() call: $text")
    }

    if (element.expressionType?.isSubtypeOf(MESSAGE_LITE_TYPENAME) != true) {
      throw invalid("The type is not MessageLite: $text")
    }

    val parts = splitDotQualifiedExpression(element)
    val ktCreatorFunc = buildKtProtoCreatorFunc(parts, simpleTypeNameIndex = parts.lastIndex - 1)
    return object : KtProtoCreatorExpression {
      override fun getCreatorFunc(): String = ktCreatorFunc

      override fun getSettersCode(): String = ""
    }
  }

  /**
   * Returns [JavaProtoExpressionParsedData] for a valid Java proto builder expression like
   * MyMessage.newBuilder().setSomething(x).build().
   */
  fun parseJavaProtoBuildExpression(
    element: KtDotQualifiedExpression
  ): JavaProtoExpressionParsedData =
    analyze(element) {
      val text = element.text
      val buildCallText =
        element.callExpression?.text
          ?: throw invalid("The last part is not a call expression: $text")

      if (buildCallText != JavaProtoConstants.BUILD_CALL) {
        throw invalid("It misses .build() call: $text")
      }

      if (element.expressionType?.isSubtypeOf(MESSAGE_LITE_TYPENAME) != true) {
        throw invalid("The type is not MessageLite: $text")
      }

      if (!text.contains(NEW_BUILDER_CALL_NAME) && !text.contains(TO_BUILDER_CALL)) {
        throw invalid("It misses builder calls: $element")
      }

      val builderType =
        ClassId.fromString(element.expressionType.toString().trimEnd('!') + ".Builder")
      val parts = splitDotQualifiedExpression(element)
      for (i in parts.size - 2 downTo 1) {
        val part = parts[i]
        if (part !is KtCallExpression) {
          throw invalid("Expected a call expression: $part")
        }

        if (part.expressionType?.isSubtypeOf(builderType) != true) {
          throw invalid("Expected to be builder of $builderType but was ${part.expressionType}")
        }

        // Stops if encounters .newBuilder() or .newBuilder(anotherMessage).
        val builderCreatorType =
          JavaProtoExpressionType.toJavaBuilderExpressionType(part) ?: continue
        return JavaProtoExpressionParsedData(parts, builderCreatorType, builderCreatorIndex = i)
      }
      throw invalid("Couldn't find builder creator call in correct location: $element")
    }

  /** Returns true for x if it's inside a setter MyMessage.newBuilder().setSomething(x). */
  fun KaSession.isArgumentOfSomeProtoSetter(element: KtDotQualifiedExpression): Boolean {
    try {
      val parent = element.parent as KtValueArgument
      val grandParent = parent.parent as KtValueArgumentList
      if (grandParent.arguments.size != 1) {
        return false
      }
      val setter = grandParent.parent as KtCallExpression
      generateSingleSetter(setter, avoidThisExpression = false)
    } catch (_: Throwable) {
      return false
    }
    return true
  }

  /**
   * Returns true if it's an expression like MyMessage.newBuilder().setSomething(x) where .build()
   * is missing.
   */
  fun KaSession.isJavaProtoMissingBuildExpression(element: PsiElement): Boolean {
    try {
      if (element !is KtDotQualifiedExpression) {
        return false
      }
      if (element.expressionType?.isSubtypeOf(MESSAGE_LITE_OR_BUILDER_TYPENAME) != true) {
        return false
      }

      val text = element.text
      if (!text.contains(NEW_BUILDER_CALL_NAME) && !text.contains(TO_BUILDER_CALL)) {
        return false
      }

      val lastSetter = element.callExpression as KtCallExpression
      generateSingleSetter(lastSetter, avoidThisExpression = false)
    } catch (_: Throwable) {
      return false
    }
    return true
  }

  private fun invalid(reason: String) =
    IllegalArgumentException("Invalid Java proto build expression: $reason")
}
