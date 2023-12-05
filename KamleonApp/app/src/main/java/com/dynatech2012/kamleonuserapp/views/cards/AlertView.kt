package com.dynatech2012.kamleonuserapp.views.cards

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.dynatech2012.kamleonuserapp.R
import com.ozcanalasalvar.datepicker.ui.theme.PickerTheme.colors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AlertView
            (onDismissRequest: () -> Unit,
             dialogTitle: String,
             dialogText: String,
)
{
    LaunchedEffect(key1 = "key1", block =
    {
        delay(3000)
        onDismissRequest()
    })
    Dialog(onDismissRequest = { onDismissRequest() },) {
        // Draw a rectangle shape with rounded corners inside the dialog
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                //.height(375.dp)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                    containerColor = colorResource(id = R.color.kamleon_dark_grey),
                ),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = dialogTitle,
                    modifier = Modifier.padding(bottom = 16.dp),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(id = R.color.color_fa),
                    )
                Text(
                    text = dialogText,
                    modifier = Modifier.padding(bottom = 16.dp),
                    fontSize = 16.sp,
                    color = colorResource(id = R.color.color_fa),
                    )
            }
        }
    }
}


/*

Column (
    modifier = modifier
        .fillMaxWidth()
        .background(
            color = colorResource(id = R.color.white),
            shape = RoundedCornerShape(16.dp)
        )
        .padding(16.dp)
) {
    Text(
        text = stringResource(id = R.string.premium_alert_title),
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.size(12.dp))
    Text(
        text = stringResource(id = R.string.premium_alert_message),
        fontSize = 16.sp
    )
    Spacer(modifier = Modifier.size(12.dp))
    Button(
        onClick = onClick,
        modifier = Modifier
            .padding(12.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = colorResource(id = R.color.kamleon_blue),
            contentColor = colorResource(id = R.color.white)
        )
    ) {
        Text(
            modifier = Modifier.padding(8.dp),
            text = stringResource(id = R.string.premium_alert_ok),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold)
    }
}

 */

