package com.giathuan.kotlinter.ktproto.support.utility

/** Utilities to handle common string transformations for protos. */
object StringTransformer {
  /** Returns myMessage for MyMessage. */
  fun variableCase(text: String): String = text.first().lowercase() + text.drop(1)

  /** Returns true if starts with an upper case letter. */
  fun startsWithUpperCase(text: String): Boolean = text.first().isUpperCase()

  /** Returns x for (x) */
  fun unwrapRoundBracket(text: String): String {
    val trimmed = text.trim()
    if (trimmed.startsWith('(') && trimmed.endsWith(')')) {
      return text.drop(1).dropLast(1).trim()
    }
    return trimmed
  }

  /**
   * Returns MyMessageKt for MyMessage OR original if not a message name (not starting with an upper
   * case letter).
   */
  fun toKtClassNameOrOriginal(text: String): String =
      if (startsWithUpperCase(text)) text + "Kt" else text
}
