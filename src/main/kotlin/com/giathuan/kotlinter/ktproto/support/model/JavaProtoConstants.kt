package com.giathuan.kotlinter.ktproto.support.model

import org.jetbrains.kotlin.name.ClassId

/** Common Java proto constants. */
object JavaProtoConstants {
  private const val PROTO_PACKAGE = "com/google/protobuf"
  val MESSAGE_LITE_TYPENAME = ClassId.fromString("$PROTO_PACKAGE/MessageLite")
  val MESSAGE_LITE_BUILDER_TYPENAME = ClassId.fromString("$PROTO_PACKAGE/MessageLite.Builder")
  val GENERATED_EXTENSION_TYPENAME = ClassId.fromString("$PROTO_PACKAGE/ExtensionLite")

  const val BUILD_CALL = "build()"
  const val GET_DEFAULT_INSTANCE_CALL = "getDefaultInstance()"

  const val ADD_ALL_PREFIX = "addAll"
  const val ADD_PREFIX = "add"
  const val SET_PREFIX = "set"

  const val NEW_BUILDER_CALL_NAME = "newBuilder"
  const val TO_BUILDER_CALL = "toBuilder()"
  const val TO_BUILDER_CALL_NAME = "toBuilder"
}
