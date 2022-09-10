package com.giathuan.kotlinter.ktproto.support

import com.giathuan.kotlinter.ktproto.support.JavaProtoConstants.BUILD_CALL
import com.giathuan.kotlinter.ktproto.support.JavaProtoConstants.MESSAGE_LITE_TYPENAME
import com.giathuan.kotlinter.ktproto.support.JavaProtoConstants.NEW_BUILDER_CALL_NAME
import com.giathuan.kotlinter.ktproto.support.JavaProtoConstants.TO_BUILDER_CALL
import com.giathuan.kotlinter.ktproto.support.JavaProtoConstants.TO_BUILDER_CALL_NAME
import org.jetbrains.kotlin.idea.core.resolveType
import org.jetbrains.kotlin.idea.debugger.sequence.psi.callName
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.supertypes
import org.jetbrains.kotlinx.serialization.compiler.backend.common.serialName

data class ParsedJavaProtoBuildExpressionInfo(
    val parts: List<KtExpression>,
    val builderCreatorType: JavaBuilderCreatorType,
    val builderCreatorIndex: Int
) {
  companion object {
    fun parseBuildExpression(
        element: KtDotQualifiedExpression
    ): ParsedJavaProtoBuildExpressionInfo {
      val text = element.text
      val buildCallText =
          element.callExpression?.text
              ?: throw invalid("The last part is not a call expression: $text")

      if (buildCallText != BUILD_CALL) {
        throw invalid("It misses .build() call: $text")
      }

      val messageType = element.resolveType()
      val messageTypeSerialName = messageType?.serialName() ?: throw invalid("The type is null")
      if (!messageType.isSubclassOf(MESSAGE_LITE_TYPENAME)) {
        throw invalid("The type is not MessageLite: $text")
      }

      if (!text.contains(NEW_BUILDER_CALL_NAME) && !text.contains(TO_BUILDER_CALL)) {
        throw invalid("It misses builder calls: $element")
      }

      val parts = splitDotQualifiedExpression(element)
      var builderCreatorIndex = -1
      var builderCreatorType: JavaBuilderCreatorType? = null
      for (i in parts.size - 2 downTo 0) {
        val part = parts[i]
        if (part !is KtCallExpression) {
          throw invalid("Expected a call expression: $part")
        }

        val partType = part.resolveType()
        if (!partType.isBuilderOf(messageTypeSerialName)) {
          throw invalid(
              "Expected to be builder of $messageTypeSerialName but was ${partType?.serialName()}")
        }

        val callName = part.callName()
        val numArgs = part.valueArguments.size
        if ((callName == TO_BUILDER_CALL_NAME) && (numArgs == 0)) {
          builderCreatorIndex = i
          builderCreatorType = JavaBuilderCreatorType.COPY_FROM_TO_BUILDER
          break
        }
        if (callName == NEW_BUILDER_CALL_NAME) {
          if (numArgs == 0) {
            builderCreatorIndex = i
            builderCreatorType = JavaBuilderCreatorType.FRESH
            break
          }
          if (numArgs == 1) {
            builderCreatorIndex = i
            builderCreatorType = JavaBuilderCreatorType.COPY_FROM_NEW_BUILDER
            break
          }
        }
      }
      if (builderCreatorType == null ||
          builderCreatorIndex <= 0 ||
          builderCreatorIndex >= parts.size) {
        throw invalid("Couldn't find builder creator call in correct location: $element")
      }
      return ParsedJavaProtoBuildExpressionInfo(parts, builderCreatorType, builderCreatorIndex)
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

    private fun invalid(reason: String) =
        IllegalArgumentException("Invalid Java proto build expression: $reason")

    private fun KotlinType?.isSubclassOf(className: String): Boolean =
        this?.supertypes()?.any { it.serialName() == className } == true

    private fun KotlinType?.isBuilderOf(className: String): Boolean =
        this?.serialName() == "$className.Builder"
  }
}
