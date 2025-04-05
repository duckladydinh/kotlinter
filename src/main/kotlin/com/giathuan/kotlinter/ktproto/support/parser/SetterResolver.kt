package com.giathuan.kotlinter.ktproto.support.parser

import com.giathuan.kotlinter.ktproto.support.model.JavaProtoConstants.ADD_ALL_PREFIX
import com.giathuan.kotlinter.ktproto.support.model.JavaProtoConstants.ADD_PREFIX
import com.giathuan.kotlinter.ktproto.support.model.JavaProtoConstants.GENERATED_EXTENSION_TYPENAME
import com.giathuan.kotlinter.ktproto.support.model.JavaProtoConstants.SET_PREFIX
import com.giathuan.kotlinter.ktproto.support.model.PrecedingCommentsBlock
import com.giathuan.kotlinter.ktproto.support.parser.JavaProtoExpressionResolver.isJavaProtoMissingBuildExpression
import com.giathuan.kotlinter.ktproto.support.utility.StringTransformer.unwrapRoundBracket
import com.giathuan.kotlinter.ktproto.support.utility.StringTransformer.variableCase
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression

/** Utilities to handle proto setters. */
object SetterResolver {
  /** Returns unformatted DSL for proto setters only. */
  fun KaSession.buildSettersCode(
    parts: List<KtExpression>,
    firstSetterIndex: Int,
    avoidThisExpression: Boolean,
  ): String {
    val builder = StringBuilder()
    for (i in firstSetterIndex..<parts.size) {
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
  fun KaSession.generateSingleSetter(
    javaSetter: KtCallExpression,
    avoidThisExpression: Boolean,
  ): String {
    // Special case for .setExtension(e, x).
    val callName = javaSetter.calleeExpression!!.text
    if (
      callName == "setExtension" &&
      javaSetter.valueArguments.size == 2 &&
      (javaSetter.valueArguments[0].lastChild as KtExpression)
        .expressionType
        ?.isSubtypeOf(GENERATED_EXTENSION_TYPENAME) == true
    ) {
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
    if (
      !avoidThisExpression ||
      (fieldName == javaSetter.valueArguments[0].text && !isArgProtoBuilderMissingBuild)
    ) {
      builder.append("this.")
    }

    // Transform into basic setter.
    val rawValue = unwrapRoundBracket(javaSetter.lastChild.text)
    builder.append(fieldName)
    builder.append(" ")
    if (callName.startsWith(ADD_PREFIX)) {
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
    if (
      callName.startsWith(ADD_ALL_PREFIX) &&
      callName.length > ADD_ALL_PREFIX.length &&
      callName[ADD_ALL_PREFIX.length].isUpperCase()
    ) {
      return variableCase(callName.drop(ADD_ALL_PREFIX.length))
    }
    if (
    // SET_PREFIX and ADD_PREFIX have the same length.
      (callName.startsWith(SET_PREFIX) || callName.startsWith(ADD_PREFIX)) &&
      callName.length > ADD_PREFIX.length &&
      callName[ADD_PREFIX.length].isUpperCase()
    ) {
      return variableCase(callName.drop(ADD_PREFIX.length))
    }
    throw IllegalArgumentException("Definitely not a setter: $callName")
  }
}
