package com.dynatech2012.kamleonuserapp.fragments

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.application.AppTheme
import com.dynatech2012.kamleonuserapp.views.AuthPrimaryButton
import com.dynatech2012.kamleonuserapp.views.AuthScreenLayout
import com.dynatech2012.kamleonuserapp.views.AuthTextField

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
    AuthScreenLayout {
        Spacer(Modifier.height(dimensionResource(R.dimen.auth_top_padding)))
        Image(
            painter = painterResource(R.drawable.img_logo),
            contentDescription = null,
            modifier = Modifier.size(dimensionResource(R.dimen.auth_top_padding))
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.auth_field_spacing)))
        Text(
            text = stringResource(R.string.login_title),
            style = MaterialTheme.typography.displaySmall,
            color = colorResource(R.color.kamleon_dark_grey)
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.auth_field_spacing)))
        Text(
            text = stringResource(R.string.login_desc),
            style = MaterialTheme.typography.bodyLarge,
            color = colorResource(R.color.kamleon_dark_grey),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.auth_section_spacing)))
        AuthTextField(
            value = email,
            onValueChange = onEmailChange,
            label = stringResource(R.string.placeholder_email),
            testTag = "login_email",
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.auth_field_spacing)))
        AuthTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = stringResource(R.string.placeholder_password),
            testTag = "login_password",
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            password = true,
            onImeAction = { if (canSignIn && !isLoading) onSignIn() }
        )
        TextButton(
            onClick = onForgotPassword,
            modifier = Modifier.align(Alignment.End).testTag("login_forgot_password"),
            colors = ButtonDefaults.textButtonColors(contentColor = colorResource(R.color.kamleon_blue_dark))
        ) {
            Text(stringResource(R.string.login_forgot_pwd))
        }
        Spacer(Modifier.height(dimensionResource(R.dimen.auth_section_spacing)))
        AuthPrimaryButton(
            text = stringResource(R.string.login_btn_signin),
            onClick = onSignIn,
            enabled = canSignIn,
            loading = isLoading,
            testTag = "login_sign_in"
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.auth_field_spacing)))
        Text(
            text = stringResource(R.string.login_not_have_account),
            style = MaterialTheme.typography.bodyMedium,
            color = colorResource(R.color.kamleon_secondary_grey_60),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        TextButton(
            onClick = onRegister,
            modifier = Modifier.testTag("login_register"),
            colors = ButtonDefaults.textButtonColors(contentColor = colorResource(R.color.kamleon_blue_dark))
        ) {
            Text(stringResource(R.string.login_signup))
        }
        Spacer(Modifier.height(dimensionResource(R.dimen.auth_section_spacing)))
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
