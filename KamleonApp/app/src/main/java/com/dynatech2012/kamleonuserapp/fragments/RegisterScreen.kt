package com.dynatech2012.kamleonuserapp.fragments

import android.util.Patterns
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
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
import com.dynatech2012.kamleonuserapp.views.kamleonTitle

private const val MIN_PASSWORD_LENGTH = 6

/**
 * Compose version of `activity_register.xml`. Layout margins are the XML's literal values (90dp
 * header offset, 20dp before the form, 44dp side padding, 12dp between fields, ...); typography
 * and fields come from the shared Kamleon components.
 *
 * Feedback: a validation result from submitting ([error]) is shown under the form and outlines the
 * offending field(s) in red. While typing, the screen also explains on its own why the form cannot
 * be sent yet (invalid email after leaving the field, short password, passwords that differ).
 *
 * When [recovering] is true an authenticated account without a profile is being completed: the
 * email is fixed, the password fields are hidden and the button reads "Complete profile".
 */
@Composable
fun RegisterScreen(
    firstName: String,
    lastName: String,
    email: String,
    password: String,
    repeatPassword: String,
    recovering: Boolean,
    @StringRes error: Int?,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRepeatPasswordChange: (String) -> Unit,
    onCreate: () -> Unit,
    onLogin: () -> Unit
) {
    val canCreate = firstName.isNotBlank() && lastName.isNotBlank() && email.isNotBlank() &&
        (recovering || (password.isNotBlank() && repeatPassword.isNotBlank() && password == repeatPassword))
    val darkGrey = colorResource(R.color.kamleon_dark_grey)
    val grey60 = colorResource(R.color.kamleon_secondary_grey_60)

    // A field counts as "visited" once it has had focus and lost it; only then is it judged live.
    var emailVisited by remember { mutableStateOf(false) }
    var passwordVisited by remember { mutableStateOf(false) }
    var emailHadFocus by remember { mutableStateOf(false) }
    var passwordHadFocus by remember { mutableStateOf(false) }

    val emailInvalid = email.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    val passwordShort = password.isNotEmpty() && password.length < MIN_PASSWORD_LENGTH
    val passwordsDiffer = password.isNotEmpty() && repeatPassword.isNotEmpty() && password != repeatPassword

    val liveError: Int? = when {
        emailVisited && emailInvalid -> R.string.signup_error_email_pattern
        passwordVisited && passwordShort -> R.string.signup_error_password
        passwordsDiffer -> R.string.signup_error_password_mismatch
        else -> null
    }
    val message = error ?: liveError

    val nameError = error == R.string.signup_error_empty_fields
    val emailError = error == R.string.signup_error_email_pattern ||
        (error == null && emailVisited && emailInvalid)
    val passwordError = error == R.string.signup_error_password ||
        (error == null && passwordVisited && passwordShort)
    val repeatError = passwordsDiffer

    // Root uses ?attr/colorSurface, which the theme maps to white.
    BoxWithConstraints(Modifier.fillMaxSize().background(colorResource(R.color.white))) {
        val viewportHeight = maxHeight
        // Scrolls when the keyboard or large fonts leave too little room; otherwise the button
        // stays pinned to the bottom like the XML.
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .heightIn(min = viewportHeight)
        ) {
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
                    text = stringResource(R.string.signup_title),
                    style = kamleonTitle(),
                    color = darkGrey,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = stringResource(R.string.signup_desc),
                    style = kamleonBody(),
                    color = darkGrey,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Column(Modifier.fillMaxWidth().padding(top = 20.dp).padding(horizontal = 44.dp)) {
                KamleonTextField(
                    value = firstName,
                    onValueChange = onFirstNameChange,
                    placeholder = stringResource(R.string.placeholder_first_name),
                    capitalization = KeyboardCapitalization.Words,
                    contentType = ContentType.PersonFirstName,
                    isError = nameError && firstName.isBlank(),
                    testTag = "register_first_name"
                )
                KamleonTextField(
                    value = lastName,
                    onValueChange = onLastNameChange,
                    placeholder = stringResource(R.string.placeholder_last_name),
                    capitalization = KeyboardCapitalization.Words,
                    contentType = ContentType.PersonLastName,
                    isError = nameError && lastName.isBlank(),
                    modifier = Modifier.padding(top = 12.dp),
                    testTag = "register_last_name"
                )
                KamleonTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    placeholder = stringResource(R.string.placeholder_email),
                    keyboardType = KeyboardType.Email,
                    autoCorrect = false,
                    contentType = ContentType.EmailAddress,
                    enabled = !recovering,
                    isError = emailError,
                    onFocusChange = { focused ->
                        if (focused) emailHadFocus = true else if (emailHadFocus) emailVisited = true
                    },
                    modifier = Modifier.padding(top = 12.dp),
                    testTag = "register_email"
                )
                if (!recovering) {
                    KamleonTextField(
                        value = password,
                        onValueChange = onPasswordChange,
                        placeholder = stringResource(R.string.placeholder_password),
                        secure = true,
                        keyboardType = KeyboardType.Password,
                        autoCorrect = false,
                        contentType = ContentType.NewPassword,
                        isError = passwordError,
                        onFocusChange = { focused ->
                            if (focused) passwordHadFocus = true else if (passwordHadFocus) passwordVisited = true
                        },
                        modifier = Modifier.padding(top = 12.dp),
                        testTag = "register_password"
                    )
                    KamleonTextField(
                        value = repeatPassword,
                        onValueChange = onRepeatPasswordChange,
                        placeholder = stringResource(R.string.placeholder_password_repeat),
                        secure = true,
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                        autoCorrect = false,
                        contentType = ContentType.NewPassword,
                        isError = repeatError,
                        modifier = Modifier.padding(top = 12.dp),
                        testTag = "register_repeat_password"
                    )
                }
                if (message != null) {
                    Row(
                        modifier = Modifier.padding(top = 12.dp, start = 8.dp).testTag("register_error"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(R.drawable.app_icon_error),
                            contentDescription = null
                        )
                        Text(
                            text = stringResource(message),
                            style = kamleonBodySmall(),
                            color = colorResource(R.color.kmln_red),
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Column(Modifier.fillMaxWidth().padding(horizontal = 44.dp).padding(bottom = 40.dp)) {
                KamleonRoundButton(
                    text = stringResource(
                        if (recovering) R.string.signup_complete_profile else R.string.signup_btn_create_account
                    ),
                    onClick = onCreate,
                    enabled = canCreate,
                    testTag = "register_create"
                )
                Row(
                    modifier = Modifier.padding(top = 16.dp).align(Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.signup_have_account),
                        style = kamleonBodySmall(),
                        color = grey60
                    )
                    KamleonTextLink(
                        text = stringResource(R.string.signup_login),
                        onClick = onLogin,
                        style = kamleonBodySmall(),
                        color = colorResource(R.color.kamleon_blue),
                        underline = true,
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier.testTag("register_login")
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun RegisterScreenPreview() {
    AppTheme(useDarkTheme = false) {
        RegisterScreen(
            firstName = "", lastName = "", email = "", password = "", repeatPassword = "",
            recovering = false, error = R.string.signup_error_password,
            onFirstNameChange = {}, onLastNameChange = {}, onEmailChange = {},
            onPasswordChange = {}, onRepeatPasswordChange = {}, onCreate = {}, onLogin = {}
        )
    }
}
