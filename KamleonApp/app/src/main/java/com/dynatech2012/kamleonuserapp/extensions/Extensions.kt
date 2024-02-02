package com.dynatech2012.kamleonuserapp.extensions

import android.content.res.Resources
import android.util.Log
import com.dynatech2012.kamleonuserapp.models.Invitation
import com.dynatech2012.kamleonuserapp.views.graph.KamleonUtils
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
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

fun String.capitalize(): String {
    return this.replaceFirstChar { it.uppercase() }
}



private const val DEFAULT_FORMAT_DATE_WITHOUT_TIME = "MMM dd, yyyy"

fun Date.formatDate(formatStr: String? = KamleonUtils.DEFAULT_FORMAT_DATE_WITHOUT_TIME): String {
    return SimpleDateFormat(formatStr, Locale.US).format(
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
    val calendar = Calendar.getInstance()
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

fun Date.day(): Int {
    return Calendar.getInstance().apply {
        time = this@day
    }.get(Calendar.DAY_OF_MONTH)
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
        Log.d("Date", "isLastWeek: ${calendar.time}, ${sevenDaysAgo.time} = ${calendar.after(sevenDaysAgo)}")
        return calendar.after(sevenDaysAgo)
    }

val Date.formatTime: String
    get() {
        val date = this
        // If it's today, display the time (e.g., "19:00").
        if (date.isToday) {
            val dateFormater = SimpleDateFormat("HH:mm", Locale.US)
            return dateFormater.format(date)
        }
        // If it's yesterday, display "Yesterday".
        else if (date.isYesterday) {
            val dateFormater = SimpleDateFormat("HH:mm", Locale.US)
            val stringDate = dateFormater.format(date)
            return "Yesterday $stringDate"
        }
        // If it's within the last week, display the day name (e.g., "Tuesday").
        else if (date.isLastWeek) {
            Log.d(Invitation.TAG, "invitationTime: isLastWeek: $date")
            val dateFormater = SimpleDateFormat("EEEE 'at' HH:mm", Locale.US)
            return dateFormater.format(date)
        }
        // If it's more than one week ago, display the full date.
        else {
            Log.d(Invitation.TAG, "invitationTime: is old: $date")
            val dateFormater = SimpleDateFormat("d MMMM yyyy", Locale.US)
            return dateFormater.format(date)
        }

    }



val Int.dp: Int get() = (this / Resources.getSystem().displayMetrics.density).toInt()
val Int.px: Int get() = (this * Resources.getSystem().displayMetrics.density).toInt()
