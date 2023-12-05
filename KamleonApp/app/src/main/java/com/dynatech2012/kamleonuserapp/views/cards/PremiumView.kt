package com.dynatech2012.kamleonuserapp.views.cards

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynatech2012.kamleonuserapp.R

@Composable
fun PremiumView(modifier: Modifier, onClick: () -> Unit) {
    val openDialog = remember { mutableStateOf(false)  }
    Column (
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        colorResource(id = R.color.premium_gradient_1),
                        colorResource(id = R.color.premium_gradient_1),
                        colorResource(id = R.color.premium_gradient_2)
                    ),
                    start = Offset.Zero,
                    end = Offset.Infinite,

                    )
            )
            .padding(32.dp)
    ){
        Row (
            verticalAlignment = Alignment.CenterVertically
        ){
            Image(
                modifier = Modifier
                    .height(40.dp),
                painter = painterResource(
                id = R.drawable.kamleon),
                contentDescription = ""
            )
            Spacer(modifier = Modifier
                .weight(1f)
            )
            Image(
                modifier = Modifier
                    .padding(10.dp)
                    .clickable { onClick() },
                painter = painterResource(
                id = R.drawable.ic_close_24),
                contentDescription = "")
        }
        Text(
            modifier = Modifier
                .padding(top = 24.dp, bottom = 4.dp),
            text = stringResource(id = R.string.premium_landpage_title),
            fontSize = dimensionResource(R.dimen.ts_24).value.sp,
            fontWeight = FontWeight.Bold,
            color = colorResource(id = R.color.kamleon_dark_grey))
        Text(
            modifier = Modifier
                .padding(bottom = 30.dp),
            text = stringResource(id = R.string.premium_landpage_subtitle),
            fontSize = dimensionResource(R.dimen.ts_14).value.sp,
            color = colorResource(id = R.color.kamleon_dark_grey))
        PremiumListItemView(
            title = stringResource(id = R.string.premium_landpage_title1),
            subtitle = stringResource(id = R.string.premium_landpage_subtitle1)
        )
        PremiumListItemView(
            title = stringResource(id = R.string.premium_landpage_title2),
            subtitle = stringResource(id = R.string.premium_landpage_subtitle2)
        )
        PremiumListItemView(
            title = stringResource(id = R.string.premium_landpage_title3),
            subtitle = stringResource(id = R.string.premium_landpage_subtitle3)
        )
        PremiumListItemView(
            title = stringResource(id = R.string.premium_landpage_title4),
            subtitle = stringResource(id = R.string.premium_landpage_subtitle4)
        )
        PremiumListItemView(
            title = stringResource(id = R.string.premium_landpage_title5),
            subtitle = stringResource(id = R.string.premium_landpage_subtitle5)
        )
        PremiumListItemView(
            title = stringResource(id = R.string.premium_landpage_title6),
            subtitle = stringResource(id = R.string.premium_landpage_subtitle6)
        )

        Button(
            onClick = {
                openDialog.value = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(id = R.color.kamleon_blue),
                contentColor = colorResource(id = R.color.color_fa),
                disabledContainerColor = colorResource(id = R.color.kamleon_secondary_grey_40),
                disabledContentColor = colorResource(id = R.color.kamleon_secondary_grey_40),
            )
        ) {
            Text(
                modifier = Modifier
                    .padding(6.dp),
                text = stringResource(id = R.string.premium_landpage_button)
            )
        }
        Spacer(modifier = Modifier
            .weight(1f)
        )


        if (openDialog.value) {
            AlertView(
                dialogTitle = stringResource(id = R.string.premium_alert_title),
                dialogText = stringResource(id = R.string.premium_alert_message),
                onDismissRequest =
                {
                    openDialog.value = false
                    onClick()
                },
            )
        }
    }
}

@Composable
fun PremiumListItemView(title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            modifier = Modifier
                .size(36.dp),
            painter = painterResource(
                id = R.drawable.ic_check_24),
            contentDescription = "",
            colorFilter = ColorFilter.tint(color = colorResource(id = R.color.kamleon_blue))
        )
                    Spacer(modifier = Modifier
            .width(10.dp)
        )
        Column {
            Text(
                text = title,
                fontSize = dimensionResource(R.dimen.ts_14).value.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.kamleon_dark_grey))
            Text(
                text = subtitle,
                fontSize = dimensionResource(R.dimen.ts_12).value.sp,
                color = colorResource(id = R.color.kamleon_dark_grey))
        }
    }
}