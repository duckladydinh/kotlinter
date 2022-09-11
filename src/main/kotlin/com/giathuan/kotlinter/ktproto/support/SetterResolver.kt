package com.giathuan.kotlinter.ktproto.support

import com.giathuan.kotlinter.ktproto.support.JavaProtoExpressionResolver.isJavaProtoMissingBuildExpression
import org.jetbrains.kotlin.idea.debugger.sequence.psi.callName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression

object SetterResolver {
  fun buildSettersCode(
      parts: List<KtExpression>,
      buildCreatorIndex: Int,
      avoidThisExpression: Boolean
  ): String {
    val builder = StringBuilder()
    for (i in buildCreatorIndex + 1 until parts.size) {
      val funcCall = parts[i] as KtCallExpression
      // Since func is inside a dot-qualified call chain, its previous sibling is a dot.
      val precedingCommentsBlock = PrecedingCommentsBlock.query(funcCall.prevSibling)
      if (precedingCommentsBlock.block.isNotBlank()) {
        if (precedingCommentsBlock.firstCommentNotStartingNewLine && builder.endsWith("\n")) {
          builder.deleteCharAt(builder.lastIndex)
        }
        builder.append(precedingCommentsBlock.block)
        builder.append("\n")
      }

      // Stops if it's the .build() call.
      if (i == parts.lastIndex) {
        continue
      }

      val fieldName = getFieldFromValidSetterOrThrows(funcCall)

      val callArg = funcCall.valueArguments[0]
      val callArgInnerExpression = callArg.lastChild
      val isArgProtoBuilder =
          callArgInnerExpression is KtDotQualifiedExpression &&
              isJavaProtoMissingBuildExpression(callArgInnerExpression)

      val simplifiedValue = callArg.text
      if (!avoidThisExpression || (fieldName == simplifiedValue && !isArgProtoBuilder)) {
        builder.append("this.")
      }

      val rawValue = StringTransformer.unwrapBracket(funcCall.lastChild.text.trim()).trim()
      builder.append(fieldName)
      builder.append(" ")
      if (funcCall.callName().startsWith(JavaProtoConstants.ADD_PREFIX)) {
        builder.append("+")
      }
      builder.append("= ")
      builder.append(rawValue)
      if (isArgProtoBuilder) {
        builder.append("\n.build()")
      }
      builder.append("\n")
    }

    return builder.toString().trimEnd()
  }

  fun getFieldFromValidSetterOrThrows(setter: KtCallExpression): String {
    if (setter.valueArguments.size != 1) {
      throw IllegalArgumentException("Expect exactly 1 argument: ${setter.text}")
    }
    val callName = setter.callName()
    if (callName.startsWith(JavaProtoConstants.ADD_ALL_PREFIX) &&
        callName.length > 6 &&
        callName[6].isUpperCase()) {
      return StringTransformer.variableCase(callName.drop(6))
    }
    if ((callName.startsWith(JavaProtoConstants.SET_PREFIX) ||
        callName.startsWith(JavaProtoConstants.ADD_PREFIX)) &&
        callName.length > 3 &&
        callName[3].isUpperCase()) {
      return StringTransformer.variableCase(callName.drop(3))
    }
    throw IllegalArgumentException("Definitely not a setter: $callName")
  }
}
