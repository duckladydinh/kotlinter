package com.giathuan.kotlinter.ktproto.support

import com.giathuan.kotlinter.ktproto.support.KtTypeVerifier.isBuilderOf
import com.giathuan.kotlinter.ktproto.support.KtTypeVerifier.isSubclassOf
import com.giathuan.kotlinter.ktproto.support.SetterResolver.getFieldFromValidSetterOrThrows
import org.jetbrains.kotlin.idea.core.resolveType
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlinx.serialization.compiler.backend.common.serialName

object JavaProtoExpressionResolver {
  fun parseJavaProtoBuildExpression(element: KtDotQualifiedExpression): ParsedJavaProtoExpression {
    val text = element.text
    val buildCallText =
        element.callExpression?.text
            ?: throw invalid("The last part is not a call expression: $text")

    if (buildCallText != JavaProtoConstants.BUILD_CALL) {
      throw invalid("It misses .build() call: $text")
    }

    val messageType = element.resolveType()
    val messageTypeSerialName = messageType?.serialName() ?: throw invalid("The type is null")
    if (!messageType.isSubclassOf(JavaProtoConstants.MESSAGE_LITE_TYPENAME)) {
      throw invalid("The type is not MessageLite: $text")
    }

    if (!text.contains(JavaProtoConstants.NEW_BUILDER_CALL_NAME) &&
        !text.contains(JavaProtoConstants.TO_BUILDER_CALL)) {
      throw invalid("It misses builder calls: $element")
    }

    val parts = DotQualifiedExpressionSplitter.splitDotQualifiedExpression(element)
    for (i in parts.size - 2 downTo 1) {
      val part = parts[i]
      if (part !is KtCallExpression) {
        throw invalid("Expected a call expression: $part")
      }

      val partType = part.resolveType()
      if (!partType.isBuilderOf(messageTypeSerialName)) {
        throw invalid(
            "Expected to be builder of $messageTypeSerialName but was ${partType?.serialName()}")
      }

      val builderCreatorType = JavaProtoExpressionType.toJavaBuilderExpressionType(part) ?: continue
      return ParsedJavaProtoExpression(parts, builderCreatorType, builderCreatorIndex = i)
    }
    throw invalid("Couldn't find builder creator call in correct location: $element")
  }

  fun isArgumentOfSomeProtoSetter(element: KtDotQualifiedExpression): Boolean {
    try {
      val parent = element.parent as KtValueArgument
      val grandParent = parent.parent as KtValueArgumentList
      if (grandParent.arguments.size != 1) {
        return false
      }
      val setter = grandParent.parent as KtCallExpression
      getFieldFromValidSetterOrThrows(setter)
    } catch (t: Throwable) {
      return false
    }
    return true
  }

  fun isJavaProtoMissingBuildExpression(element: KtDotQualifiedExpression): Boolean {
    try {
      val messageType = element.resolveType()
      if (!messageType.isSubclassOf(JavaProtoConstants.MESSAGE_LITE_OR_BUILDER_TYPENAME)) {
        return false
      }

      val text = element.text
      if (!text.contains(JavaProtoConstants.NEW_BUILDER_CALL_NAME) &&
          !text.contains(JavaProtoConstants.TO_BUILDER_CALL)) {
        return false
      }

      val lastSetter = element.callExpression as KtCallExpression
      getFieldFromValidSetterOrThrows(lastSetter)
    } catch (t: Throwable) {
      return false
    }
    return true
  }

  private fun invalid(reason: String) =
      IllegalArgumentException("Invalid Java proto build expression: $reason")
}
