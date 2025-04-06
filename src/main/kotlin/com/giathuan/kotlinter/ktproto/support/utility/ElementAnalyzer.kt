package com.giathuan.kotlinter.ktproto.support.utility

import com.intellij.psi.PsiElement

/** Static utilities for analyzing [PsiElement]. */
object ElementAnalyzer {
  /** Returns true of the expression is written on a single line. */
  fun PsiElement.inSingleLine(): Boolean {
    val doc = containingFile?.viewProvider?.document ?: return false
    return with(textRange) {
      isEmpty ||
        (endOffset <= doc.textLength &&
          startOffset >= 0 &&
          endOffset <= doc.getLineEndOffset(doc.getLineNumber(startOffset)))
    }
  }
}
