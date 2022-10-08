package com.giathuan.kotlinter.ktproto.support.model

/** Common Java proto constants. */
object JavaProtoConstants {
  const val MESSAGE_LITE_TYPENAME = "com.google.protobuf.MessageLite"
  const val MESSAGE_LITE_OR_BUILDER_TYPENAME = "com.google.protobuf.MessageLiteOrBuilder"

  const val BUILD_CALL = "build()"
  const val GET_DEFAULT_INSTANCE_CALL = "getDefaultInstance()"

  const val ADD_ALL_PREFIX = "addAll"
  const val ADD_PREFIX = "add"
  const val SET_PREFIX = "set"

  const val NEW_BUILDER_CALL_NAME = "newBuilder"
  const val TO_BUILDER_CALL = "toBuilder()"
  const val TO_BUILDER_CALL_NAME = "toBuilder"
}
