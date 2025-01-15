package com.giathuan.kotlinter.ktproto.support.model

import org.jetbrains.kotlin.psi.KtCallExpression

/** Different types of proto builder creation. */
enum class JavaProtoExpressionType {
  /** Example: newBuilder().build() */
  BUILD_FROM_NEW_BUILDER_EMPTY,

  /** Example: newBuilder(anotherMessage). */
  BUILD_FROM_NEW_BUILDER_SOURCE,

  /** Example: anotherMessage.toBuilder(). */
  BUILD_FROM_TO_BUILDER_EMPTY;

  companion object {
    /** Returns [JavaProtoExpressionType] for the given builder creation call. */
    fun toJavaBuilderExpressionType(part: KtCallExpression): JavaProtoExpressionType? {
      val callName = part.calleeExpression!!.text
      val numArgs = part.valueArguments.size
      if ((callName == JavaProtoConstants.TO_BUILDER_CALL_NAME) && (numArgs == 0)) {
        return BUILD_FROM_TO_BUILDER_EMPTY
      }
      if (callName == JavaProtoConstants.NEW_BUILDER_CALL_NAME) {
        if (numArgs == 0) {
          return BUILD_FROM_NEW_BUILDER_EMPTY
        }
        if (numArgs == 1) {
          return BUILD_FROM_NEW_BUILDER_SOURCE
        }
      }
      return null
    }
  }
}
