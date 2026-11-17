package com.dynatech2012.kamleonuserapp.fragments

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
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
import com.dynatech2012.kamleonuserapp.views.KamleonFormLayout
import com.dynatech2012.kamleonuserapp.views.KamleonRoundButton
import com.dynatech2012.kamleonuserapp.views.KamleonSwitchRow
import com.dynatech2012.kamleonuserapp.views.KamleonTextField
import com.dynatech2012.kamleonuserapp.views.KamleonTextLink
import com.dynatech2012.kamleonuserapp.views.kamleonBody
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
    val darkGrey = colorResource(R.color.kamleon_dark_grey)
    val grey60 = colorResource(R.color.kamleon_secondary_grey_60)
    val emailFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }
    var submitted by remember { mutableStateOf(false) }
    val emailError = if (submitted && email.isBlank()) stringResource(R.string.field_error_email_required) else null
    val passwordError =
        if (submitted && password.isBlank()) stringResource(R.string.field_error_password_required) else null

    // The button is never silently disabled: pressing it with a blank field explains what is
    // missing next to that field and moves focus there.
    val submit: () -> Unit = {
        when {
            isLoading -> Unit
            email.isBlank() -> { submitted = true; emailFocus.requestFocus() }
            password.isBlank() -> { submitted = true; passwordFocus.requestFocus() }
            else -> onSignIn()
        }
    }

    KamleonFormLayout(
        bottomBar = { imeVisible ->
            KamleonRoundButton(
                text = stringResource(R.string.login_btn_signin),
                onClick = submit,
                enabled = true,
                loading = isLoading,
                testTag = "login_sign_in"
            )
            // Secondary action hidden while typing so the keyboard leaves room for the fields.
            AnimatedVisibility(visible = !imeVisible) {
                KamleonSwitchRow(
                    prompt = stringResource(R.string.login_not_have_account),
                    action = stringResource(R.string.login_signup),
                    onClick = onRegister,
                    actionTestTag = "login_register"
                )
            }
        }
    ) {
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
                errorText = emailError,
                focusRequester = emailFocus,
                testTag = "login_email"
            )
            KamleonTextField(
                value = password,
                onValueChange = onPasswordChange,
                placeholder = stringResource(R.string.placeholder_password),
                secure = true,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                errorText = passwordError,
                focusRequester = passwordFocus,
                onImeAction = submit,
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
