package com.giathuan.kotlinter.ktproto.support.utility

import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.supertypes
import org.jetbrains.kotlinx.serialization.compiler.backend.common.serialName

/** Utilities to verify [KotlinType]. */
object KtTypeVerifier {
  /**
   * Returns true if the [KotlinType] is a subclass of the class referenced by its fully qualified
   * name.
   */
  fun KotlinType?.isSubclassOf(qualifiedClassName: String): Boolean =
    this?.supertypes()?.any { it.serialName() == qualifiedClassName } == true

  /**
   * Returns true if the [KotlinType] is a proto builder of the class referenced by its fully
   * qualified name.
   */
  fun KotlinType?.isBuilderOf(qualifiedClassName: String): Boolean =
    this?.serialName() == "$qualifiedClassName.Builder"
}
