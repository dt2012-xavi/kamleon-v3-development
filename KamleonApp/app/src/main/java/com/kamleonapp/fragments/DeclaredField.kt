package com.kamleonapp.fragments

import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior
import java.lang.reflect.Field
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

//fun KClass<*>.getDeclaredFieldOrNull(vararg names: String): Field? {
//    names.forEach {
//        return try{
//            getDeclaredField(it).apply {
//                isAccessible = true
//            }
//        } catch (noSuchFieldException: NoSuchFieldException) {
//            null
//        }
//    }
//
//    return null
//}
//
//class DeclaredField<T: Any?> (name: String) {
//    private val field = BottomSheetBehavior::class.getDeclaredFieldOrNull(name)
//
//    operator fun <V: View> getValue(bottomSheetBehavior: BottomSheetBehavior<V>, property: KProperty<*>): T? {
//        @Suppress("UNCHECKED_CAST")
//        return field?.get(bottomSheetBehavior) as? T
//    }
//
//    operator fun <V: View> setValue(bottomSheetBehavior: BottomSheetBehavior<V>, property: KProperty<*>, value: T) {
//        field?.set(bottomSheetBehavior, value)
//    }
//}