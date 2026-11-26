package com.dynatech2012.kamleonuserapp.fragments

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.application.AppTheme
import com.dynatech2012.kamleonuserapp.views.KamleonRoundButton
import com.dynatech2012.kamleonuserapp.views.KamleonTextField
import com.dynatech2012.kamleonuserapp.views.KamleonTextLink
import com.dynatech2012.kamleonuserapp.views.kamleonBody
import com.dynatech2012.kamleonuserapp.views.kamleonCaption
import com.dynatech2012.kamleonuserapp.views.kamleonTitle

/**
 * The "Trouble logging in?" screen. Layout margins are the old XML's literal values (90dp
 * header offset, 44dp side padding, 52dp before the input, ...); typography and the text field
 * come from the shared Kamleon components.
 */
@Composable
fun ForgotPwdScreen(
    email: String,
    onEmailChange: (String) -> Unit,
    onSend: () -> Unit,
    onClose: () -> Unit,
    onContactSupport: () -> Unit
) {
    val darkGrey = colorResource(R.color.kamleon_dark_grey)
    val grey60 = colorResource(R.color.kamleon_secondary_grey_60)
    val canSend = email.isNotBlank()

    // The XML root uses @color/bg_color_activity (translucent), so the activity shows through.
    Box(Modifier.fillMaxSize().background(colorResource(R.color.bg_color_activity))) {
        // The content can outgrow the window with large fonts or an open keyboard: scroll.
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 90.dp)
                .padding(horizontal = 44.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // img_logo.png is 274x209px in drawable-xxhdpi (3x) => 91.33x69.67dp.
            Image(
                painter = painterResource(R.drawable.img_logo),
                contentDescription = null,
                modifier = Modifier.size(width = (274f / 3f).dp, height = (209f / 3f).dp)
            )
            Text(
                text = stringResource(R.string.forgotpwd_title),
                style = kamleonTitle(),
                color = darkGrey,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = stringResource(R.string.forgotpwd_desc),
                style = kamleonBody(),
                color = darkGrey,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
            KamleonTextField(
                value = email,
                onValueChange = onEmailChange,
                placeholder = stringResource(R.string.placeholder_email),
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done,
                modifier = Modifier.padding(top = 52.dp),
                testTag = "forgot_email"
            )
            KamleonRoundButton(
                text = stringResource(R.string.forgotpwd_send),
                onClick = onSend,
                enabled = canSend,
                modifier = Modifier.padding(top = 16.dp),
                testTag = "forgot_send"
            )
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.forgotpwd_can_not_reset),
                    style = kamleonCaption(),
                    color = grey60
                )
                // Someone who cannot get back in needs support, not a second account.
                KamleonTextLink(
                    text = stringResource(R.string.forgotpwd_contact),
                    onClick = onContactSupport,
                    style = kamleonCaption(),
                    color = colorResource(R.color.kamleon_blue),
                    pressedColor = colorResource(R.color.kamleon_secondary_grey_70),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.padding(top = 4.dp).testTag("forgot_contact")
                )
            }
        }

        // ImageButton 48dp at the top end, 16dp from the top; icn_cross (31px, nodpi) centered.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = dimensionResource(R.dimen.nav_back_button_top))
                .size(dimensionResource(R.dimen.nav_back_button_size))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClick = onClose
                )
                .testTag("forgot_close"),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.icn_cross),
                contentDescription = null,
                contentScale = ContentScale.None
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun ForgotPwdScreenPreview() {
    AppTheme(useDarkTheme = false) {
        ForgotPwdScreen(
            email = "",
            onEmailChange = {},
            onSend = {},
            onClose = {},
            onContactSupport = {}
        )
    }
}
