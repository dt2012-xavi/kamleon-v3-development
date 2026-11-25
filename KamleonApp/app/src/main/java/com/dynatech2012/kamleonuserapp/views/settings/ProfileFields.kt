package com.dynatech2012.kamleonuserapp.views.settings

import java.util.Locale
import kotlin.math.roundToInt

/**
 * The fields of Settings edited in a profile sheet: Personal data (Name groups the name and the
 * surname) and the PIN.
 */
enum class ProfileField { NAME, GENDER, BIRTH, HEIGHT, WEIGHT, PIN }

/** How Personal data shows and picks heights and weights. */
object ProfileValues {
    /** What a row shows when the value was never set. */
    const val EMPTY = "-"

    val HEIGHT_RANGE = 100..250
    val WEIGHT_RANGE = 30..250
    const val DEFAULT_HEIGHT = 170
    const val DEFAULT_WEIGHT = "70.0"

    /** A set value: not null and above zero (older profiles saved 0 for "not set"). */
    private fun Float?.orUnset(): Float? = this?.takeIf { it > 0f }

    /** The height in whole centimetres, rounded; null when not set. */
    fun height(height: Float?): Int? = height.orUnset()?.roundToInt()

    /** The weight with one decimal, without a trailing ".0" ("70", "70.5"); null when not set. */
    fun weightText(weight: Float?): String? {
        val tenths = ((weight.orUnset() ?: return null) * 10).roundToInt()
        return if (tenths % 10 == 0) (tenths / 10).toString()
        else String.format(Locale.US, "%d.%d", tenths / 10, tenths % 10)
    }

    /** Where the height wheel opens: the saved height, else [DEFAULT_HEIGHT]. */
    fun heightStart(height: Float?): Int = height(height) ?: DEFAULT_HEIGHT

    /** Where the weight wheel opens, always with one decimal ("70.0"): the saved weight, else [DEFAULT_WEIGHT]. */
    fun weightStart(weight: Float?): String {
        val tenths = ((weight.orUnset() ?: return DEFAULT_WEIGHT) * 10).roundToInt()
        return String.format(Locale.US, "%d.%d", tenths / 10, tenths % 10)
    }

    /** The wheel's whole values: [range], stretched so a saved value outside it can still be shown. */
    fun wheelValues(range: IntRange, start: Int): List<String> =
        (minOf(range.first, start)..maxOf(range.last, start)).map { it.toString() }

    const val PIN_LENGTH = 6

    /** What a PIN field keeps of typed or pasted text: digits only, at most [PIN_LENGTH]. */
    fun pinInput(text: String): String = text.filter { it.isDigit() }.take(PIN_LENGTH)

    /** Whether [pin] is a whole PIN. */
    fun isCompletePin(pin: String): Boolean = pin.length == PIN_LENGTH && pin.all { it.isDigit() }
}
