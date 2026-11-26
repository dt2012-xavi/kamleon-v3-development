package com.dynatech2012.kamleonuserapp.extensions

import android.content.res.Resources
import com.dynatech2012.kamleonuserapp.R
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun String.sha256(): String {
    return hashString(this, "SHA-256")
}

private fun hashString(input: String, algorithm: String): String {
    return MessageDigest
        .getInstance(algorithm)
        .digest(input.toByteArray())
        .fold("") { str, it -> str + "%02x".format(it) }
}

fun String.toDate(pattern: String, locale: Locale = Locale.US): Date =
    SimpleDateFormat(pattern, locale).parse(this) ?: Date(0)

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
    val calendar = Calendar.getInstance()
    calendar.time = this
    return calendar.get(Calendar.DAY_OF_WEEK)
}

fun Date.beginningOfWeek(): Date {
    val weekDay = this.getWeekDay()
    return this.addDays(-(weekDay - 1)).beginningOfDay
}

fun Date.with(weekday: Int = -1): Date {
    val calendar = Calendar.getInstance()
    calendar.time = this
    if (weekday > -1) calendar.set(Calendar.WEEK_OF_MONTH, weekday)
    return calendar.time
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

fun Date.year(): Int {
    return Calendar.getInstance().apply {
        time = this@year
    }.get(Calendar.YEAR)
}
fun Date.month(): Int {
    return Calendar.getInstance().apply {
        time = this@month
    }.get(Calendar.MONTH)
}

val Date.isToday: Boolean
    get() = Calendar.getInstance().apply {
        time = this@isToday
    }.let {
        it.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR) &&
                it.get(Calendar.DAY_OF_YEAR) == Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
    }

val Date.isYesterday: Boolean
    get() = Calendar.getInstance().apply {
        time = this@isYesterday
    }.let {
        it.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR) &&
                it.get(Calendar.DAY_OF_YEAR) == Calendar.getInstance().get(Calendar.DAY_OF_YEAR) - 1
    }

val Date.isLastWeek: Boolean
    get() = Calendar.getInstance().apply {
        time = this@isLastWeek
    }.let {
        val calendar = Calendar.getInstance()
        calendar.time = this

        // Get the current date
        val sevenDaysAgo = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -7)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // Check if date is after the beginning
        return calendar.after(sevenDaysAgo)
    }

/** "19:00": the time in 24-hour format. */
fun Date.timeText(locale: Locale = Locale.getDefault()): String = SimpleDateFormat("HH:mm", locale).format(this)

/**
 * When a sample was taken, in the phone's language: "today at 19:00", "yesterday at 19:00", "Tuesday
 * at 19:00" within the last week, or the date ("November 24, 2026") before that.
 */
fun Date.sampleTimeText(resources: Resources, locale: Locale = Locale.getDefault()): String = when {
    isToday -> resources.getString(R.string.sample_time_today, timeText(locale))
    isYesterday -> resources.getString(R.string.sample_time_yesterday, timeText(locale))
    isLastWeek -> resources.getString(
        R.string.sample_time_weekday, SimpleDateFormat("EEEE", locale).format(this), timeText(locale)
    )
    else -> localDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale))
}

/** The date in the phone's language, medium length: "Nov 24, 2026", "24 nov 2026". */
fun Date.mediumDateText(locale: Locale = Locale.getDefault()): String =
    localDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))

/** The month's short name in the phone's language, as said on its own: "Nov", "nov.". */
fun Date.shortMonthText(locale: Locale = Locale.getDefault()): String =
    localDate().format(DateTimeFormatter.ofPattern("LLL", locale))

/** The year: "2026". */
fun Date.yearText(): String = localDate().year.toString()

private fun Date.localDate(): LocalDate = toInstant().atZone(ZoneId.systemDefault()).toLocalDate()

val Int.dp: Int get() = (this / Resources.getSystem().displayMetrics.density).toInt()
val Int.px: Int get() = (this * Resources.getSystem().displayMetrics.density).toInt()


/** Day of the week as the old Date.getDay(): 0 is Sunday. */
val Date.dayOfWeekIndex: Int
    get() = Calendar.getInstance().apply { time = this@dayOfWeekIndex }.get(Calendar.DAY_OF_WEEK) - 1

/** Month as the old Date.getMonth(): 0 is January. */
val Date.monthIndex: Int
    get() = Calendar.getInstance().apply { time = this@monthIndex }.get(Calendar.MONTH)

/** Day of the month as the old Date.getDate(): 1 to 31. */
val Date.dayOfMonth: Int
    get() = Calendar.getInstance().apply { time = this@dayOfMonth }.get(Calendar.DAY_OF_MONTH)
