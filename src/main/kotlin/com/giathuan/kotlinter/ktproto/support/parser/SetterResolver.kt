package com.giathuan.kotlinter.ktproto.support.parser

import com.giathuan.kotlinter.ktproto.support.model.JavaProtoConstants
import com.giathuan.kotlinter.ktproto.support.model.PrecedingCommentsBlock
import com.giathuan.kotlinter.ktproto.support.parser.JavaProtoExpressionResolver.isJavaProtoMissingBuildExpression
import com.giathuan.kotlinter.ktproto.support.utility.StringTransformer
import com.giathuan.kotlinter.ktproto.support.utility.StringTransformer.unwrapRoundBracket
import org.jetbrains.kotlin.idea.debugger.sequence.psi.callName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression

/** Utilities to handle proto setters. */
object SetterResolver {
  /** Returns unformatted DSL for proto setters only. */
  fun buildSettersCode(
      parts: List<KtExpression>,
      firstSetterIndex: Int,
      avoidThisExpression: Boolean
  ): String {
    val builder = StringBuilder()
    for (i in firstSetterIndex until parts.size) {
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

      val rawValue = unwrapRoundBracket(funcCall.lastChild.text)
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

  /** Returns something for .setSomething(x) or throws otherwise. */
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
