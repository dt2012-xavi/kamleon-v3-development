package com.kamleonapp.views.chart.exts

import android.annotation.SuppressLint
import com.kamleonapp.views.chart.KamleonUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

private const val DEFAULT_FORMAT_DATE_WITHOUT_TIME = "MMM dd, yyyy"

@SuppressLint("SimpleDateFormat")
fun Date.formatDate(formatStr: String? = KamleonUtils.DEFAULT_FORMAT_DATE_WITHOUT_TIME): String {
    return SimpleDateFormat(formatStr).format(
        this
    )
}

fun Date.with(
    year: Int = -1,
    month: Int = -1,
    day: Int = -1,
    hour: Int = -1,
    minute: Int = -1,
    second: Int = -1,
    millisecond: Int = -1
): Date {
    val calendar = Calendar.getInstance()
    calendar.time = this
    if (year > -1) calendar.set(Calendar.YEAR, year)
    if (month > 0) calendar.set(Calendar.MONTH, month - 1)
    if (day > 0) calendar.set(Calendar.DATE, day)
    if (hour > -1) calendar.set(Calendar.HOUR_OF_DAY, hour)
    if (minute > -1) calendar.set(Calendar.MINUTE, minute)
    if (second > -1) calendar.set(Calendar.SECOND, second)
    if (millisecond > -1) calendar.set(Calendar.MILLISECOND, millisecond)
    return calendar.time
}

fun Date.getWeekDay() : Int {
    var calendar = Calendar.getInstance()
    calendar.time = this
    return calendar.get(Calendar.DAY_OF_WEEK)
}

fun Date.beginningOfWeek(): Date {
    val weekDay = this.getWeekDay()
    return this.addDays(-(weekDay - 1)).beginningOfDay
}

fun Date.endOfWeek(): Date {
    return this.beginningOfWeek().addDays(6).endOfDay
}

fun Date.with(weekday: Int = -1): Date {
    val calendar = Calendar.getInstance()
    calendar.time = this
    if (weekday > -1) calendar.set(Calendar.WEEK_OF_MONTH, weekday)
    return calendar.time
}

val Date.beginningOfYear: Date
    get() = with(month = 1, day = 1, hour = 0, minute = 0, second = 0, millisecond = 0)

fun Date.endOfYear(ignoreTime: Boolean = false): Date {
    return with(month = 12, day = 31, hour = if (ignoreTime) 0 else 23, minute = if (ignoreTime) 0 else 59, second = if (ignoreTime) 0 else 59)
}

val Date.beginningOfMonth: Date
    get() = with(day = 1, hour = 0, minute = 0, second = 0, millisecond = 0)

val Date.endOfMonth: Date
    get() = endOfMonth()

fun Date.endOfMonth(ignoreTime: Boolean = false): Date {
    val calendar = Calendar.getInstance()
    calendar.time = this
    val lastDay = calendar.getActualMaximum(Calendar.DATE)
    return with(day = lastDay, hour = if (ignoreTime) 0 else 23, minute = if (ignoreTime) 0 else 59, second = if (ignoreTime) 0 else 59)
}

val Date.beginningOfDay: Date
    get() = with(hour = 0, minute = 0, second = 0, millisecond = 0)

val Date.endOfDay: Date
    get() = with(hour = 23, minute = 59, second = 59, millisecond = 999)

/**
 * Add field date to current date
 */
fun Date.add(field: Int, amount: Int): Date {
    Calendar.getInstance().apply {
        time = this@add
        add(field, amount)
        return time
    }
}

fun Date.addYears(years: Int): Date{
    return add(Calendar.YEAR, years)
}
fun Date.addMonths(months: Int): Date {
    return add(Calendar.MONTH, months)
}
fun Date.addDays(days: Int): Date{
    return add(Calendar.DAY_OF_MONTH, days)
}
fun Date.addHours(hours: Int): Date{
    return add(Calendar.HOUR_OF_DAY, hours)
}
fun Date.addMinutes(minutes: Int): Date{
    return add(Calendar.MINUTE, minutes)
}
fun Date.addSeconds(seconds: Int): Date{
    return add(Calendar.SECOND, seconds)
}