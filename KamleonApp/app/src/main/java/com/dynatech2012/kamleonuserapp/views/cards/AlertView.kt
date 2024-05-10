package com.dynatech2012.kamleonuserapp.views.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.dynatech2012.kamleonuserapp.R
import kotlinx.coroutines.delay


class AlertViewPreviewProvider :
    PreviewParameterProvider<Pair<() -> Unit, Triple<String, String, String>>> {
    override val values: Sequence<Pair<() -> Unit, Triple<String, String, String>>> = sequenceOf(
        Pair({}, Triple("Preview Title", "This is a preview of the AlertView.", "Button Text"))
    )
}

@Preview(showBackground = true)
@Composable
fun AlertViewPreview(@PreviewParameter(AlertViewPreviewProvider::class) params: Pair<() -> Unit, Triple<String, String, String>>) {
    val (onDismissRequest, dialogParams) = params
    val (dialogTitle, dialogText, buttonText) = dialogParams
    AlertView(
        onDismissRequest = onDismissRequest,
        dialogTitle = dialogTitle,
        dialogText = dialogText,
        buttonText = buttonText
    )
}

@Composable
fun AlertView(
    onDismissRequest: () -> Unit,
    dialogTitle: String,
    dialogText: String,
    buttonText: String,
    onButtonClick: ((() -> Unit) -> Unit) = {}
) {
    LaunchedEffect(key1 = "key1", block =
    {
        //delay(3000)
        //onDismissRequest()
    })
    Dialog(onDismissRequest = {  }) {
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
                Button(
                    onClick = { onButtonClick(onDismissRequest) },
                    modifier = Modifier.padding(8.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.kamleon_blue),
                        contentColor = colorResource(id = R.color.white),
                        disabledContainerColor = colorResource(id = R.color.kamleon_secondary_grey_40),
                        disabledContentColor = colorResource(id = R.color.kamleon_blue),
                    ),
                ) {
                    Text(
                        text = buttonText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }

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

