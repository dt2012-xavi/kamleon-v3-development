package com.ozcanalasalvar.datepicker.compose.datepicker

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ozcanalasalvar.datepicker.compose.component.SelectorView
import com.ozcanalasalvar.datepicker.utils.daysOfDate
import com.ozcanalasalvar.datepicker.utils.monthsOfDate
import com.ozcanalasalvar.datepicker.utils.withDay
import com.ozcanalasalvar.datepicker.utils.withMonth
import com.ozcanalasalvar.datepicker.utils.withYear
import com.ozcanalasalvar.datepicker.model.Date
import com.ozcanalasalvar.datepicker.ui.theme.colorDarkPrimary
import com.ozcanalasalvar.datepicker.ui.theme.colorDarkTextPrimary
import com.ozcanalasalvar.datepicker.ui.theme.colorLightPrimary
import com.ozcanalasalvar.datepicker.ui.theme.colorLightTextPrimary
import com.ozcanalasalvar.datepicker.ui.theme.darkPallet
import com.ozcanalasalvar.datepicker.ui.theme.lightPallet
import com.ozcanalasalvar.datepicker.utils.DateUtils
import com.ozcanalasalvar.wheelview.WheelView
import com.ozcanalasalvar.wheelview.SelectorOptions
import java.text.DateFormatSymbols


@Composable
fun WheelDatePicker(
    modifier: Modifier = Modifier,
    offset: Int = 4,
    yearsRange: IntRange = IntRange(1923, 2121),
    startDate: Date = Date(DateUtils.getCurrentTime()),
    //toDate: Date = Date(DateUtils.getCurrentTime()),
    maxDate: Date = Date(DateUtils.getCurrentTime()),
    textSize: Int = 16,
    textBold: Boolean = false,
    selectorEffectEnabled: Boolean = true,
    onDateChanged: (Int, Int, Int, Long) -> Unit = { _, _, _, _ -> },
    darkModeEnabled: Boolean = true,
) {

    var selectedDate by remember { mutableStateOf(startDate) }
    //var scrollToDate by remember { mutableStateOf(toDate) }

    val months = selectedDate.monthsOfDate()

    val days = selectedDate.daysOfDate()

    val years = mutableListOf<Int>().apply {
        for (year in yearsRange) {
            add(year)
        }
    }

    val maxMonth = if (selectedDate.year == maxDate.year) maxDate.month else 12
    val maxDay = if (selectedDate.year == maxDate.year && selectedDate.month == maxDate.month) maxDate.day else 31

    LaunchedEffect(selectedDate) {
        onDateChanged(selectedDate.day, selectedDate.month, selectedDate.year, selectedDate.date)
    }

    /*
    LaunchedEffect(scrollToDate) {
        onDateChanged(selectedDate.day, selectedDate.month, selectedDate.year, selectedDate.date)
    }
    */

    val fontSize = maxOf(13, minOf(28, textSize))


    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max)
            .background(if (darkModeEnabled) colorDarkPrimary else colorLightPrimary),
        contentAlignment = Alignment.Center
    ) {

        val height = (fontSize + 14).dp


        Row(
            modifier = Modifier
                .nestedScroll(rememberNestedScrollInteropConnection())
                .fillMaxSize()
                .padding(start = 10.dp, end = 10.dp)
        ) {
            val maxIndexMonth = maxMonth - 1
            val maxIndexDay = maxDay - 1
            Log.d("TAG", "ssss maxDate: $maxDate _ maxIndexMonth: $maxIndexMonth")
            Log.d("TAG", "ssss maxDate: $maxDate _ maxIndexDay: $maxIndexDay")
            WheelView(modifier = Modifier.weight(7f),
                itemSize = DpSize(150.dp, height),
                selection = maxOf(months.indexOf(selectedDate.month), 0),
                maxIndex = maxIndexMonth,
                itemCount = months.size,
                rowOffset = offset,
                selectorOption = SelectorOptions().copy(selectEffectEnabled = selectorEffectEnabled, enabled = false),
                onFocusItem = {
                    selectedDate = selectedDate.withMonth(months[it])
                },
                content = {
                    Log.d("TAG", "ssss month: ${months[it]} _ ${DateFormatSymbols().months[months[it]]}")
                    Text(
                        text = DateFormatSymbols().months[months[it]],
                        textAlign = TextAlign.Start,
                        modifier = Modifier.width(120.dp),
                        fontSize = fontSize.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (darkModeEnabled) colorDarkTextPrimary else colorLightTextPrimary
                    )
                })

            key(days.size) {
                WheelView(modifier = Modifier.weight(2f),
                    itemSize = DpSize(150.dp, height),
                    selection = maxOf(days.indexOf(selectedDate.day), 0),
                    maxIndex = maxDay - 1,
                    itemCount = days.size,
                    rowOffset = offset,
                    selectorOption = SelectorOptions().copy(selectEffectEnabled = selectorEffectEnabled, enabled = false),
                    onFocusItem = {
                        selectedDate = selectedDate.withDay(days[it])
                    },
                    content = {
                        Text(
                            text = days[it].toString(),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(50.dp),
                            fontSize = fontSize.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (darkModeEnabled) colorDarkTextPrimary else colorLightTextPrimary
                        )
                    })
            }

            WheelView(modifier = Modifier.weight(3f),
                itemSize = DpSize(150.dp, height),
                selection = years.indexOf(selectedDate.year),
                itemCount = years.size,
                rowOffset = offset,
                isEndless = false,//years.size > offset * 2,
                selectorOption = SelectorOptions().copy(selectEffectEnabled = selectorEffectEnabled, enabled = false),
                onFocusItem = {
                    selectedDate = selectedDate.withYear(years[it])
                },
                content = {
                    Text(
                        text = years[it].toString(),
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(100.dp),
                        fontSize = fontSize.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (darkModeEnabled) colorDarkTextPrimary else colorLightTextPrimary
                    )
                })
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (darkModeEnabled) darkPallet else lightPallet
                    )
                ),
        ) {}

        SelectorView(darkModeEnabled= darkModeEnabled, offset = offset)



    }


}


@Preview
@Composable
fun DatePickerPreview() {
    WheelDatePicker(onDateChanged = { _, _, _, _ -> })
}