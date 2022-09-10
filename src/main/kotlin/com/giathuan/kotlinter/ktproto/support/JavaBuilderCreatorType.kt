package com.giathuan.kotlinter.ktproto.support

enum class JavaBuilderCreatorType {
  /** Example: newBuilder(). */
  FRESH,

  /** Example: newBuilder(anotherMessage). */
  COPY_FROM_NEW_BUILDER,

  /** Example: anotherMessage.toBuilder(). */
  COPY_FROM_TO_BUILDER
}
