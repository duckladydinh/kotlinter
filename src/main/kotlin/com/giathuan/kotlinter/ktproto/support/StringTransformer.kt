package com.giathuan.kotlinter.ktproto.support

object StringTransformer {
  fun variableCase(text: String): String = text.first().lowercase() + text.drop(1)

  fun startsWithUpperCase(text: String): Boolean = text.first().isUpperCase()

  fun unwrapBracket(text: String): String = text.drop(1).dropLast(1)

  fun toKtClassNameOrOriginal(text: String): String =
      if (startsWithUpperCase(text)) text + "Kt" else text
}
