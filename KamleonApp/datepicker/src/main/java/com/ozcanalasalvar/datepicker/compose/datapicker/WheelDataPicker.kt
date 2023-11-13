package com.ozcanalasalvar.datepicker.compose.datapicker

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ozcanalasalvar.datepicker.compose.component.SelectorView
import com.ozcanalasalvar.datepicker.ui.theme.colorDarkPrimary
import com.ozcanalasalvar.datepicker.ui.theme.colorDarkTextPrimary
import com.ozcanalasalvar.datepicker.ui.theme.colorLightPrimary
import com.ozcanalasalvar.datepicker.ui.theme.colorLightTextPrimary
import com.ozcanalasalvar.datepicker.ui.theme.darkPallet
import com.ozcanalasalvar.datepicker.ui.theme.lightPallet
import com.ozcanalasalvar.wheelview.WheelView
import com.ozcanalasalvar.wheelview.SelectorOptions


@Composable
fun WheelDataPicker(
    offset: Int = 4,
    selectorEffectEnabled: Boolean = true,
    valueUnit: String = "",
    startValue: String = "",
    showDecimal: Boolean = false,
    values: List<String> = arrayListOf("value1", "value2"),
    textSize: Int = 16,
    textBold: Boolean = false,
    onValueChanged: (String, String?) -> Unit = { _, _ -> },
    darkModeEnabled: Boolean = true,
    valueWidth: Int = 350
) {

    var selectedValue by remember { mutableStateOf(startValue.split(".")[0]) }
    var selectedDecimal by remember { mutableStateOf(if (startValue.contains(".")) startValue.split(".")[1] else "" ) }
    val decimalValues = mutableListOf<Int>().apply {
        for (decimalValue in IntRange(0, 9)) {
            add(decimalValue)
        }
    }

    val fontSize = maxOf(13, minOf(29, textSize))

    LaunchedEffect(selectedValue, selectedDecimal) {
        if (showDecimal) {
            onValueChanged("$selectedValue.$selectedDecimal", valueUnit)
        } else {
            onValueChanged(selectedValue, valueUnit)
        }
    }

    Box(
        modifier = Modifier
            .wrapContentSize()
            .height(IntrinsicSize.Max)
            .background(if (darkModeEnabled) colorDarkPrimary else colorLightPrimary),
        contentAlignment = Alignment.Center
    ) {

        val height=( fontSize + 10) .dp


        Row(
            modifier = Modifier
                .nestedScroll(rememberNestedScrollInteropConnection())
                .wrapContentSize()
                .padding(start = 20.dp, end = 20.dp)
                .align(Alignment.Center)
        ) {


            WheelView(//modifier = Modifier.weight(3f)
                modifier = Modifier.width(valueWidth.dp),
                itemSize = DpSize(valueWidth.dp, height),
                selection = maxOf(0, values.indexOf(selectedValue)),
                itemCount = values.size,
                rowOffset = offset,
                isEndless = false,
                selectorOption = SelectorOptions().copy(selectEffectEnabled = selectorEffectEnabled, enabled = false),
                onFocusItem = {
                    selectedValue = values[it]
                },
                content = {
                    Text(
                        text = if (values.isNotEmpty()) values[it] else "",
//                        textAlign = if (valueUnit.isEmpty()) TextAlign.Center else TextAlign.End,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(valueWidth.dp),
                        fontSize = fontSize.sp,
                        fontWeight = if (textBold) FontWeight.Bold else FontWeight.Normal,
                        color = if (darkModeEnabled) colorDarkTextPrimary else colorLightTextPrimary
                    )
                })
            if (showDecimal) {
                WheelView(modifier = Modifier.width(10.dp),
                    itemSize = DpSize(10.dp, height),
                    selection = 0,
                    itemCount = 1,
                    rowOffset = offset,
                    isEndless = false,
                    selectorOption = SelectorOptions().copy(
                        selectEffectEnabled = false,
                        enabled = false
                    ),
                    onFocusItem = {

                    },
                    content = {
                        Text(
                            text = ".",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(10.dp),
                            fontSize = fontSize.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (darkModeEnabled) colorDarkTextPrimary else colorLightTextPrimary
                        )
                    })

                WheelView(modifier = Modifier.width(30.dp),
                    itemSize = DpSize(30.dp, height),
                    selection = decimalValues.indexOf(if (selectedDecimal.isEmpty()) 0 else selectedDecimal.toInt()),
                    itemCount = decimalValues.size,
                    rowOffset = offset,
                    isEndless = false,
                    selectorOption = SelectorOptions().copy(
                        selectEffectEnabled = selectorEffectEnabled,
                        enabled = false
                    ),
                    onFocusItem = {
//                    selectedValue = selectedValue.withDecimal(decimalValues[it])
                        selectedDecimal = decimalValues[it].toString()
                    },
                    content = {
                        Text(
                            text = decimalValues[it].toString(),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(30.dp),
                            fontSize = fontSize.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (darkModeEnabled) colorDarkTextPrimary else colorLightTextPrimary
                        )
                    })
            }

            if (valueUnit.isNotEmpty()) {
                WheelView(modifier = Modifier.width(50.dp),
                    itemSize = DpSize(50.dp, height),
                    selection = 0,
                    itemCount = 1,
                    rowOffset = offset,
                    isEndless = false,
                    selectorOption = SelectorOptions().copy(
                        selectEffectEnabled = selectorEffectEnabled,
                        enabled = false
                    ),
                    onFocusItem = {

                    },
                    content = {
                        Text(
                            text = valueUnit,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.width(50.dp),
                            fontSize = 16.sp,
                            color = if (darkModeEnabled) colorDarkTextPrimary else colorLightTextPrimary
                        )
                    })
            }
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