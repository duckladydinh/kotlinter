package com.giathuan.kotlinter.ktproto.support.model

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.base.psi.getLineNumber
import org.jetbrains.kotlin.j2k.isInSingleLine
import org.jetbrains.kotlin.psi.psiUtil.getPrevSiblingIgnoringWhitespace

/** A model for preceding comments of some element. */
data class PrecedingCommentsBlock(val block: String, val firstCommentNotStartingNewLine: Boolean) {
  companion object {
    /** Returns [PrecedingCommentsBlock] for a [PsiElement] */
    fun query(element: PsiElement): PrecedingCommentsBlock {
      val reversedComments = mutableListOf<PsiComment>()
      var node = element.originalElement
      while (node.getPrevSiblingIgnoringWhitespace() is PsiComment) {
        node = node.getPrevSiblingIgnoringWhitespace()
        reversedComments += node as PsiComment
      }
      node = node.getPrevSiblingIgnoringWhitespace()

      val comments = reversedComments.asReversed()
      val builder = StringBuilder()
      for (i in comments.indices) {
        if (
          i > 0 &&
          comments[i].getLineNumber(start = true) != comments[i - 1].getLineNumber(start = false)
        ) {
          builder.append("\n")
        }
        builder.append(comments[i].text)
      }

      val firstCommentNotStartingNewLine =
        comments.isNotEmpty() &&
            node != null &&
            comments.first().isInSingleLine() &&
            comments.first().getLineNumber(start = true) == node.getLineNumber(start = false)

      return PrecedingCommentsBlock(builder.toString(), firstCommentNotStartingNewLine)
    }
  }
}
