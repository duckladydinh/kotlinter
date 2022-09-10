package com.giathuan.kotlinter.ktproto.support

import com.giathuan.kotlinter.ktproto.support.DotQualifiedExpressionSplitter.splitDotQualifiedExpression
import com.giathuan.kotlinter.ktproto.support.JavaProtoBuildExpressionType.Companion.toJavaBuilderExpressionType
import com.giathuan.kotlinter.ktproto.support.JavaProtoConstants.BUILD_CALL
import com.giathuan.kotlinter.ktproto.support.JavaProtoConstants.MESSAGE_LITE_TYPENAME
import com.giathuan.kotlinter.ktproto.support.JavaProtoConstants.NEW_BUILDER_CALL_NAME
import com.giathuan.kotlinter.ktproto.support.JavaProtoConstants.TO_BUILDER_CALL
import com.giathuan.kotlinter.ktproto.support.KtTypeVerifier.isBuilderOf
import com.giathuan.kotlinter.ktproto.support.KtTypeVerifier.isSubclassOf
import org.jetbrains.kotlin.idea.core.resolveType
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlinx.serialization.compiler.backend.common.serialName

data class ParsedJavaProtoBuildExpressionInfo(
    val parts: List<KtExpression>,
    val javaProtoBuildExpressionType: JavaProtoBuildExpressionType,
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

        val builderCreatorType = toJavaBuilderExpressionType(part) ?: continue
        return ParsedJavaProtoBuildExpressionInfo(
            parts, builderCreatorType, builderCreatorIndex = i)
      }
      throw invalid("Couldn't find builder creator call in correct location: $element")
    }

    private fun invalid(reason: String) =
        IllegalArgumentException("Invalid Java proto build expression: $reason")
  }
}
