package com.dynatech2012.kamleonuserapp.fragments

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import com.dynatech2012.kamleonuserapp.views.kamleonBodySmall
import com.dynatech2012.kamleonuserapp.views.kamleonCaption
import com.dynatech2012.kamleonuserapp.views.kamleonTitle

/**
 * Compose version of `activity_login.xml`. Layout margins are the XML's literal values (44dp side
 * padding, 90dp header offset, 40dp bottom padding, ...); typography and the text field come from
 * the shared Kamleon components.
 */
@Composable
fun LoginScreen(
    email: String,
    password: String,
    isLoading: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSignIn: () -> Unit,
    onForgotPassword: () -> Unit,
    onRegister: () -> Unit
) {
    val canSignIn = email.isNotBlank() && password.isNotBlank()
    val darkGrey = colorResource(R.color.kamleon_dark_grey)
    val grey60 = colorResource(R.color.kamleon_secondary_grey_60)

    // Root uses ?attr/colorSurface, which the theme maps to white.
    BoxWithConstraints(Modifier.fillMaxSize().background(colorResource(R.color.white))) {
        val viewportHeight = maxHeight
        // With enough room the layout equals the XML (button pinned to the bottom). When the
        // keyboard shrinks the window the content scrolls instead of letting the pinned button
        // cover the fields.
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .heightIn(min = viewportHeight)
        ) {
        Column(Modifier.fillMaxWidth()) {
            // loginScreenHeaderLayout
            Column(
                modifier = Modifier.padding(top = 90.dp).align(Alignment.CenterHorizontally),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // img_logo.png is 274x209px in drawable-xxhdpi (3x) => 91.33x69.67dp.
                Image(
                    painter = painterResource(R.drawable.img_logo),
                    contentDescription = null,
                    modifier = Modifier.size(width = (274f / 3f).dp, height = (209f / 3f).dp)
                )
                Text(
                    text = stringResource(R.string.login_title),
                    style = kamleonTitle(),
                    color = darkGrey,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = stringResource(R.string.login_desc),
                    style = kamleonBody(),
                    color = darkGrey,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Form block
            Column(Modifier.fillMaxWidth().padding(top = 40.dp).padding(horizontal = 44.dp)) {
                KamleonTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    placeholder = stringResource(R.string.placeholder_email),
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                    testTag = "login_email"
                )
                KamleonTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    placeholder = stringResource(R.string.placeholder_password),
                    secure = true,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                    modifier = Modifier.padding(top = 12.dp),
                    testTag = "login_password"
                )
                KamleonTextLink(
                    text = stringResource(R.string.login_forgot_pwd),
                    onClick = onForgotPassword,
                    style = kamleonCaption(),
                    color = grey60,
                    pressedColor = colorResource(R.color.kamleon_secondary_grey_70),
                    underline = true,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier.padding(top = 12.dp).testTag("login_forgot_password")
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // Bottom block, pushed to the bottom of the screen.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 44.dp)
                .padding(bottom = 40.dp)
        ) {
            KamleonRoundButton(
                text = stringResource(R.string.login_btn_signin),
                onClick = onSignIn,
                enabled = canSignIn,
                loading = isLoading,
                testTag = "login_sign_in"
            )
            Row(
                modifier = Modifier.padding(top = 16.dp).align(Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.login_not_have_account),
                    style = kamleonBodySmall(),
                    color = grey60
                )
                KamleonTextLink(
                    text = stringResource(R.string.login_signup),
                    onClick = onRegister,
                    style = kamleonBodySmall(),
                    color = colorResource(R.color.kamleon_blue),
                    underline = true,
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier.testTag("login_register")
                )
            }
        }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun LoginScreenPreview() {
    AppTheme(useDarkTheme = false) {
        LoginScreen(
            email = "",
            password = "",
            isLoading = false,
            onEmailChange = {},
            onPasswordChange = {},
            onSignIn = {},
            onForgotPassword = {},
            onRegister = {}
        )
    }
}
