package com.dynatech2012.kamleonuserapp.views.cards

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynatech2012.kamleonuserapp.R

@Composable
fun PremiumModalView(modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            //.wrapContentHeight()
            .fillMaxSize()
            .background(color = colorResource(id = R.color.color_fa))
            .padding(horizontal = 40.dp),
            //.padding(top = 24.dp, bottom = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            modifier = Modifier
                .padding(4.dp)
                .size(48.dp),
            painter = painterResource(
                id = R.drawable.padlock
            ),
            contentDescription = "",
        )
        Text(
            modifier = Modifier
                .padding(8.dp),
            text = stringResource(id = R.string.premium_recommendation_modal_title),
            fontSize = dimensionResource(R.dimen.ts_24).value.sp,
            fontWeight = FontWeight.Bold,
            color = colorResource(id = R.color.kamleon_dark_grey)
        )
        Text(
            modifier = Modifier
                .padding(bottom = 14.dp),
            text = stringResource(id = R.string.premium_recommendation_modal_subtitle),
            fontSize = dimensionResource(R.dimen.ts_14).value.sp,
            color = colorResource(id = R.color.kamleon_dark_grey)
        )
        Button(
            onClick = {
                onClick()
            },
            modifier = Modifier
                .fillMaxWidth(),
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
                text = stringResource(id = R.string.premium_recommendation_modal_button)
            )
        }
    }
}