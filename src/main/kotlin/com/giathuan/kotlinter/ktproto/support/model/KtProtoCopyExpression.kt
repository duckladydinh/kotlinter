package com.giathuan.kotlinter.ktproto.support.model

/**
 * An interface for expressions like MyMessage.newBuilder(anotherMessage).setSomething(x).build().
 */
interface KtProtoCopyExpression {
  /**
   * Returns a text of the source to copy data from, could be an expression or a variable, so better
   * just returns the text.
   */
  fun getCopySource(): String

  /**
   * Returns `this.something = x` for expressions like MyMessage.newBuilder.setSomething(x).build().
   */
  fun getSettersCode(): String

  /** Returns the unformatted DSL. */
  fun text(): String =
      """${getCopySource()}.copy {
		  ${getSettersCode()}
		  }
		  """
          .trimIndent()
}
