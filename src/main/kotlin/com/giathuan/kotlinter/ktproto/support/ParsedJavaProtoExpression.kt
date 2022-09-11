package com.giathuan.kotlinter.ktproto.support

import org.jetbrains.kotlin.psi.KtExpression

data class ParsedJavaProtoExpression(
    val parts: List<KtExpression>,
    val javaProtoBuildExpressionType: JavaProtoExpressionType,
    val builderCreatorIndex: Int
)
