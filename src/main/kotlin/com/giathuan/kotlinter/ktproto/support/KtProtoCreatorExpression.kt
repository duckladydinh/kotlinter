package com.giathuan.kotlinter.ktproto.support

import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

interface KtProtoCreatorExpression {
  fun getCreatorFunc(): String

  fun getSettersCode(): String

  fun text(): String = """${getCreatorFunc()} {
		  ${getSettersCode()}
		  }
		  """.trimIndent()

  companion object {
    fun buildKtProtoCreatorFun(parts: List<KtExpression>, buildCreatorIndex: Int): String {
      val typeSimpleName = parts[buildCreatorIndex - 1].text
      if (!StringTransformer.startsWithUpperCase(typeSimpleName)) {
        throw IllegalArgumentException(
            "The simple message name must starts with an upper case: $typeSimpleName")
      }
      val builder = StringBuilder()
      if (buildCreatorIndex > 1) {
        val prefix =
            parts
                .slice(0 until buildCreatorIndex - 1)
                .map { (it as KtNameReferenceExpression).text }
                .joinToString(".") { StringTransformer.toKtClassNameOrOriginal(it) }
                .trim()
        builder.append(prefix)
        builder.append(".")
      }
      builder.append(StringTransformer.variableCase(typeSimpleName))
      return builder.toString()
    }
  }
}
