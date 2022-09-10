package com.giathuan.kotlinter.ktproto.support

import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression

object DotQualifiedExpressionSplitter {
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
