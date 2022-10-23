package com.giathuan.kotlinter.ktproto.support.parser

import com.giathuan.kotlinter.ktproto.support.model.JavaProtoConstants
import com.giathuan.kotlinter.ktproto.support.model.PrecedingCommentsBlock
import com.giathuan.kotlinter.ktproto.support.parser.JavaProtoExpressionResolver.isJavaProtoMissingBuildExpression
import com.giathuan.kotlinter.ktproto.support.utility.KtTypeVerifier.isSubclassOf
import com.giathuan.kotlinter.ktproto.support.utility.StringTransformer
import com.giathuan.kotlinter.ktproto.support.utility.StringTransformer.unwrapRoundBracket
import org.jetbrains.kotlin.idea.core.resolveType
import org.jetbrains.kotlin.idea.debugger.sequence.psi.callName
import org.jetbrains.kotlin.psi.KtCallExpression
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
      val javaSetter = parts[i] as KtCallExpression
      // Since func is inside a dot-qualified call chain, its previous sibling is a dot.
      val precedingCommentsBlock = PrecedingCommentsBlock.query(javaSetter.prevSibling)
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

      builder.append(generateSingleSetter(javaSetter, avoidThisExpression))
      builder.append("\n")
    }

    return builder.toString().trimEnd()
  }

  /** Generates Kotlin DSL for a single Java setter. */
  fun generateSingleSetter(javaSetter: KtCallExpression, avoidThisExpression: Boolean): String {
    // Special case for .setExtension(e, x).
    val callName = javaSetter.callName()
    if (callName == "setExtension" &&
        javaSetter.valueArguments.size == 2 &&
        (javaSetter.valueArguments[0].lastChild as KtExpression)
            .resolveType()
            .isSubclassOf(JavaProtoConstants.GENERATED_EXTENSION_TYPENAME)) {
      return "this[${javaSetter.valueArguments[0].text.trim()}] = ${javaSetter.valueArguments[1].text.trim()}"
    }

    // Throw if it's not a normal setter.
    if (javaSetter.valueArguments.size != 1) {
      throw IllegalArgumentException("Expect exactly 1 argument: ${javaSetter.text}")
    }

    // Prepare an empty string builder for storing the result.
    val builder = StringBuilder()

    // Add `this.` if needed.
    val fieldName = extractFieldNameFromCallName(callName)
    val isArgProtoBuilderMissingBuild =
        isJavaProtoMissingBuildExpression(javaSetter.valueArguments[0].lastChild)
    if (!avoidThisExpression ||
        (fieldName == javaSetter.valueArguments[0].text && !isArgProtoBuilderMissingBuild)) {
      builder.append("this.")
    }

    // Transform into basic setter.
    val rawValue = unwrapRoundBracket(javaSetter.lastChild.text)
    builder.append(fieldName)
    builder.append(" ")
    if (callName.startsWith(JavaProtoConstants.ADD_PREFIX)) {
      builder.append("+")
    }
    builder.append("= ")
    builder.append(rawValue)

    // Add `.build` if needed.
    if (isArgProtoBuilderMissingBuild) {
      builder.append("\n.build()")
    }

    return builder.toString()
  }

  private fun extractFieldNameFromCallName(callName: String): String {
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
