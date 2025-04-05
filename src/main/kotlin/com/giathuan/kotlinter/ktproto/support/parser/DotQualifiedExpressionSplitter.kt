package com.giathuan.kotlinter.ktproto.support.parser

import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression

/** Utilities for splitting [KtDotQualifiedExpression]. */
object DotQualifiedExpressionSplitter {
  /** Split dot qualified expressions like a.b.c.d().e into [a, b, c, d(), e]. */
  fun splitDotQualifiedExpression(element: KtDotQualifiedExpression): List<KtExpression> {
    var node = element
    if (element.selectorExpression == null) {
      return emptyList()
    }
    val parts = mutableListOf(element.selectorExpression as KtExpression)
    while (node.receiverExpression is KtDotQualifiedExpression) {
      node = node.receiverExpression as KtDotQualifiedExpression
      parts += node.selectorExpression ?: return emptyList()
    }
    parts += node.receiverExpression
    return parts.asReversed()
  }
}
