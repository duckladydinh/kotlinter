package com.giathuan.kotlinter.ktproto.support.model

import org.jetbrains.kotlin.psi.KtExpression

/**
 * Parsed data about a normal proto expression with builder creation (newBuilder, toBuilder) call.
 */
data class JavaProtoExpressionParsedData(
  val parts: List<KtExpression>,
  val javaProtoBuildExpressionType: JavaProtoExpressionType,
  val builderCreatorIndex: Int,
)
