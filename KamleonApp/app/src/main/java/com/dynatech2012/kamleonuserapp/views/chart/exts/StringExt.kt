package com.dynatech2012.kamleonuserapp.views.chart.exts

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun String.toDate(pattern: String, locale: Locale = Locale.getDefault()): Date =
    SimpleDateFormat(pattern, locale).parse(this)
