package com.giathuan.kotlinter.ktproto.support

import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.supertypes
import org.jetbrains.kotlinx.serialization.compiler.backend.common.serialName

object KtTypeVerifier {
  fun KotlinType?.isSubclassOf(className: String): Boolean =
      this?.supertypes()?.any { it.serialName() == className } == true

  fun KotlinType?.isBuilderOf(className: String): Boolean =
      this?.serialName() == "$className.Builder"
}
