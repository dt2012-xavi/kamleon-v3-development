package com.dynatech2012.kamleonuserapp.fragments

import android.util.Patterns
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.application.AppTheme
import com.dynatech2012.kamleonuserapp.security.PasswordLength
import com.dynatech2012.kamleonuserapp.security.PasswordStrength
import com.dynatech2012.kamleonuserapp.views.KamleonFormLayout
import com.dynatech2012.kamleonuserapp.views.KamleonPasswordStrengthMeter
import com.dynatech2012.kamleonuserapp.views.KamleonRoundButton
import com.dynatech2012.kamleonuserapp.views.KamleonSwitchRow
import com.dynatech2012.kamleonuserapp.views.KamleonTextField
import com.dynatech2012.kamleonuserapp.views.kamleonBody
import com.dynatech2012.kamleonuserapp.views.kamleonBodySmall
import com.dynatech2012.kamleonuserapp.views.kamleonTitle

/**
 * Compose version of `activity_register.xml`. Layout margins are the XML's literal values (90dp
 * header offset, 20dp before the form, 44dp side padding, 12dp between fields, ...); typography
 * and fields come from the shared Kamleon components.
 *
 * Feedback: every problem is shown right under the field that causes it (red outline plus message),
 * so it is never hidden behind the keyboard. Pressing the button with something missing or invalid
 * moves focus to the first offending field instead of failing silently; while typing, the screen
 * also explains on its own (invalid email after leaving the field, a strength bar under the
 * password, passwords that differ). The password policy (length 8..128, strength, breach check) is
 * enforced by the ViewModel; [error] carries its verdict and [checking] shows the button spinner
 * while it runs. [passwordStrength] feeds the strength bar and blocks weak passwords on submit. [error] is the result reported back by the ViewModel; errors that belong to no field
 * (expired session) appear above the button. The primary button sits in a fixed bar above the
 * keyboard and the "Log in" link is hidden while the keyboard is open.
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
    checking: Boolean = false,
    passwordStrength: PasswordStrength? = null,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRepeatPasswordChange: (String) -> Unit,
    onCreate: () -> Unit,
    onLogin: () -> Unit
) {
    val darkGrey = colorResource(R.color.kamleon_dark_grey)

    // A field counts as "visited" once it has had focus and lost it; only then is it judged live.
    var emailVisited by remember { mutableStateOf(false) }
    var emailHadFocus by remember { mutableStateOf(false) }
    // Set once the user pressed the button: blank fields are then reported next to the field.
    var submitted by remember { mutableStateOf(false) }

    val firstNameFocus = remember { FocusRequester() }
    val lastNameFocus = remember { FocusRequester() }
    val emailFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }
    val repeatFocus = remember { FocusRequester() }

    // Message per field (null = fine). [force] judges everything as if it had been visited and
    // submitted, which is what pressing the button does.
    fun errors(force: Boolean): RegisterFieldErrors {
        val showBlank = force || submitted
        val emailInvalid = email.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
        val passwordsDiffer = password.isNotEmpty() && repeatPassword.isNotEmpty() && password != repeatPassword
        return RegisterFieldErrors(
            firstName = if (showBlank && firstName.isBlank()) R.string.signup_error_first_name_required else null,
            lastName = if (showBlank && lastName.isBlank()) R.string.signup_error_last_name_required else null,
            email = when {
                showBlank && email.isBlank() -> R.string.field_error_email_required
                emailInvalid && (force || emailVisited || error == R.string.signup_error_email_pattern) ->
                    R.string.signup_error_email_pattern
                else -> null
            },
            password = when {
                recovering -> null
                showBlank && password.isEmpty() -> R.string.field_error_password_required
                // Policy result reported back by the ViewModel (length, strength, breach).
                error in PASSWORD_ERRORS -> error
                // On submit, anything the local rules already know is not acceptable.
                force && password.length < PasswordLength.MIN -> R.string.password_error_too_short
                force && password.length > PasswordLength.MAX -> R.string.password_error_too_long
                force && passwordStrength == PasswordStrength.WEAK -> R.string.password_error_weak
                else -> null
            },
            repeat = when {
                recovering -> null
                showBlank && repeatPassword.isEmpty() -> R.string.signup_error_repeat_required
                passwordsDiffer -> R.string.signup_error_password_mismatch
                else -> null
            }
        )
    }
    // Before the first submit only the live checks show; afterwards every rule is judged live, so
    // the message that blocked the submit stays next to its field until it is fixed.
    val fieldErrors = errors(force = submitted)

    // Pressing the button never fails silently: if something is wrong, say it next to the field
    // and move focus to the first offending one; otherwise submit.
    val submit: () -> Unit = submit@{
        if (checking) return@submit
        val e = errors(force = true)
        submitted = true
        emailVisited = true
        when {
            e.firstName != null -> firstNameFocus.requestFocus()
            e.lastName != null -> lastNameFocus.requestFocus()
            e.email != null -> emailFocus.requestFocus()
            e.password != null -> passwordFocus.requestFocus()
            e.repeat != null -> repeatFocus.requestFocus()
            else -> onCreate()
        }
    }

    // Validation results coming back from the ViewModel: take the user to the field to fix.
    LaunchedEffect(error) {
        when (error) {
            R.string.signup_error_email_pattern -> emailFocus.requestFocus()
            in PASSWORD_ERRORS -> passwordFocus.requestFocus()
            R.string.signup_error_empty_fields -> {
                submitted = true
                if (firstName.isBlank()) firstNameFocus.requestFocus() else lastNameFocus.requestFocus()
            }
        }
    }

    KamleonFormLayout(
        bottomBar = { imeVisible ->
            // Errors that belong to no field (e.g. expired session).
            if (error == R.string.signup_session_expired || error == R.string.signup_error_server_error) {
                Row(
                    modifier = Modifier.padding(bottom = 12.dp, start = 8.dp).testTag("register_error"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(painter = painterResource(R.drawable.app_icon_error), contentDescription = null)
                    Text(
                        text = stringResource(error),
                        style = kamleonBodySmall(),
                        color = colorResource(R.color.kmln_red),
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
            }
            KamleonRoundButton(
                text = stringResource(
                    if (recovering) R.string.signup_complete_profile else R.string.signup_btn_create_account
                ),
                onClick = submit,
                enabled = true,
                loading = checking,
                testTag = "register_create"
            )
            // Secondary action hidden while typing so the keyboard leaves room for the fields.
            AnimatedVisibility(visible = !imeVisible) {
                KamleonSwitchRow(
                    prompt = stringResource(R.string.signup_have_account),
                    action = stringResource(R.string.signup_login),
                    onClick = onLogin,
                    actionTestTag = "register_login"
                )
            }
        }
    ) {
        Column(
            modifier = Modifier.padding(top = 90.dp).padding(horizontal = 44.dp).align(Alignment.CenterHorizontally),
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
                errorText = fieldErrors.firstName?.let { stringResource(it) },
                focusRequester = firstNameFocus,
                enabled = !checking,
                testTag = "register_first_name"
            )
            KamleonTextField(
                value = lastName,
                onValueChange = onLastNameChange,
                placeholder = stringResource(R.string.placeholder_last_name),
                capitalization = KeyboardCapitalization.Words,
                contentType = ContentType.PersonLastName,
                errorText = fieldErrors.lastName?.let { stringResource(it) },
                focusRequester = lastNameFocus,
                // While recovering the email is fixed, so the surname is the last editable field.
                imeAction = if (recovering) ImeAction.Done else ImeAction.Next,
                onImeAction = if (recovering) submit else null,
                modifier = Modifier.padding(top = 12.dp),
                enabled = !checking,
                testTag = "register_last_name"
            )
            KamleonTextField(
                value = email,
                onValueChange = onEmailChange,
                placeholder = stringResource(R.string.placeholder_email),
                keyboardType = KeyboardType.Email,
                autoCorrect = false,
                contentType = ContentType.EmailAddress,
                enabled = !checking && !recovering,
                errorText = fieldErrors.email?.let { stringResource(it) },
                focusRequester = emailFocus,
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
                    errorText = fieldErrors.password?.let { stringResource(it) },
                    focusRequester = passwordFocus,
                    supportingContent = {
                        KamleonPasswordStrengthMeter(
                            password = password,
                            strength = passwordStrength,
                            modifier = Modifier.testTag("register_password_strength")
                        )
                    },
                    modifier = Modifier.padding(top = 12.dp),
                    enabled = !checking,
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
                    errorText = fieldErrors.repeat?.let { stringResource(it) },
                    focusRequester = repeatFocus,
                    onImeAction = submit,
                    modifier = Modifier.padding(top = 12.dp),
                    enabled = !checking,
                    testTag = "register_repeat_password"
                )
            }
        }
        // Breathing room so the last field's error can scroll clear of the fixed button bar.
        Spacer(Modifier.height(16.dp))
    }
}

private val PASSWORD_ERRORS = setOf(
    R.string.password_error_too_short,
    R.string.password_error_too_long,
    R.string.password_error_weak,
    R.string.password_error_breached
)

private class RegisterFieldErrors(
    @param:StringRes val firstName: Int?,
    @param:StringRes val lastName: Int?,
    @param:StringRes val email: Int?,
    @param:StringRes val password: Int?,
    @param:StringRes val repeat: Int?
)

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun RegisterScreenPreview() {
    AppTheme(useDarkTheme = false) {
        RegisterScreen(
            firstName = "", lastName = "", email = "", password = "", repeatPassword = "",
            recovering = false, error = R.string.password_error_weak,
            onFirstNameChange = {}, onLastNameChange = {}, onEmailChange = {},
            onPasswordChange = {}, onRepeatPasswordChange = {}, onCreate = {}, onLogin = {}
        )
    }
}
