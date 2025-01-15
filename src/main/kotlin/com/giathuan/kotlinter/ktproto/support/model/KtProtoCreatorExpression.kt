package com.giathuan.kotlinter.ktproto.support.model

import com.giathuan.kotlinter.ktproto.support.utility.StringTransformer
import com.giathuan.kotlinter.ktproto.support.utility.StringTransformer.toKtClassNameOrOriginal
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

/** An interface for expressions like MyMessage.newBuilder.setSomething(x).build(). */
interface KtProtoCreatorExpression {
  /** Returns myMessage for expressions like MyMessage.newBuilder.build(). */
  fun getCreatorFunc(): String

  /**
   * Returns `this.something = x` for expressions like MyMessage.newBuilder.setSomething(x).build().
   */
  fun getSettersCode(): String

  /** Returns the unformatted DSL. */
  fun text(): String {
    if (getSettersCode().isBlank()) {
      return "${getCreatorFunc()} {}"
    }
    return """${getCreatorFunc()} {
		  ${getSettersCode()}
		  }
		  """
      .trimIndent()
  }

  companion object {
    /** Returns com.package.MyProtosKt.myMessage for com.package.MyProto.MyMessage. */
    fun buildKtProtoCreatorFunc(parts: List<KtExpression>, simpleTypeNameIndex: Int): String {
      val typeSimpleName = parts[simpleTypeNameIndex].text
      if (!StringTransformer.startsWithUpperCase(typeSimpleName)) {
        throw IllegalArgumentException(
          "The simple message name must starts with an upper case: $typeSimpleName"
        )
      }
      val builder = StringBuilder()
      if (simpleTypeNameIndex > 0) {
        val prefix =
          parts
            .slice(0..<simpleTypeNameIndex)
            .map { (it as KtNameReferenceExpression).text }
            .joinToString(".") { toKtClassNameOrOriginal(it) }
            .trim()
        builder.append(prefix)
        builder.append(".")
      }
      builder.append(StringTransformer.variableCase(typeSimpleName))
      return builder.toString()
    }
  }
}
