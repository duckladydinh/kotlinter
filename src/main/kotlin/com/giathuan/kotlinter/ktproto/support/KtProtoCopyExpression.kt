package com.giathuan.kotlinter.ktproto.support

interface KtProtoCopyExpression {
  /**
   * Returns a text of the source to copy data from, could be an expression or a variable, so better
   * just returns the text.
   */
  fun getCopySource(): String

  fun getSettersCode(): String

  fun text(): String =
      """${getCopySource()}.copy {
		  ${getSettersCode()}
		  }
		  """.trimIndent()
}
