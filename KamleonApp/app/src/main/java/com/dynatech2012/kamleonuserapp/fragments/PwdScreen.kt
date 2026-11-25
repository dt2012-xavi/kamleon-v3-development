package com.dynatech2012.kamleonuserapp.fragments

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.security.PasswordLength
import com.dynatech2012.kamleonuserapp.security.PasswordStrength
import com.dynatech2012.kamleonuserapp.views.KamleonPasswordStrengthMeter
import com.dynatech2012.kamleonuserapp.views.KamleonTextField
import com.dynatech2012.kamleonuserapp.views.settings.FieldLabel
import com.dynatech2012.kamleonuserapp.views.settings.SettingsSheetFrame
import com.dynatech2012.kamleonuserapp.views.KamleonTextLink
import com.dynatech2012.kamleonuserapp.views.kamleonBodySmall
import com.dynatech2012.kamleonuserapp.views.kamleonCaption

/**
 * The Settings sheet to change the password: the current password with the "Forgot password?"
 * link under it, the new one with its strength meter, its confirmation and Save, inside the frame
 * every Settings sheet shares.
 *
 * Every problem appears right under the row that causes it, pressing Save with something missing
 * or invalid moves focus to the first offending field instead of failing silently, and Done on the
 * last field submits. [currentError] and [newError] are verdicts reported back by the ViewModel
 * (wrong current password, policy rejection); the caller clears them when the field changes.
 * [checking] shows the button spinner while the change runs.
 */
@Composable
fun PasswordSheetContent(
    currentPassword: String,
    newPassword: String,
    confirmPassword: String,
    @StringRes currentError: Int?,
    @StringRes newError: Int?,
    checking: Boolean,
    passwordStrength: PasswordStrength?,
    onCurrentPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSave: () -> Unit,
    onForgotPassword: () -> Unit
) {
    var submitted by remember { mutableStateOf(false) }
    val currentFocus = remember { FocusRequester() }
    val newFocus = remember { FocusRequester() }
    val confirmFocus = remember { FocusRequester() }

    fun errors(force: Boolean) = pwdFieldErrors(
        current = currentPassword,
        new = newPassword,
        confirm = confirmPassword,
        strength = passwordStrength,
        showBlank = force,
        force = force,
        currentServerError = currentError,
        newServerError = newError
    )
    // Before the first submit only the live checks show; afterwards every rule is judged live, so
    // the message that blocked the submit stays next to its field until it is fixed.
    val fieldErrors = errors(force = submitted)

    val submit: () -> Unit = submit@{
        if (checking) return@submit
        val e = errors(force = true)
        submitted = true
        when {
            e.current != null -> currentFocus.requestFocus()
            e.new != null -> newFocus.requestFocus()
            e.confirm != null -> confirmFocus.requestFocus()
            else -> onSave()
        }
    }

    LaunchedEffect(Unit) { currentFocus.requestFocus() }
    // Verdicts coming back from the ViewModel: take the user to the field to fix.
    LaunchedEffect(currentError) { if (currentError != null) currentFocus.requestFocus() }
    LaunchedEffect(newError) { if (newError != null) newFocus.requestFocus() }

    SettingsSheetFrame(
        title = stringResource(R.string.profile_sheet_title_password),
        saveEnabled = true,
        saving = checking,
        onSave = submit,
        saveTestTag = "pwd_save"
    ) {
        // The sheet can be dragged, so the scrolling form must hand its gestures to the sheet's
        // behaviour through the nested-scroll interop.
        // Takes only the room left above Save, so Save stays visible over the keyboard.
        Column(
            Modifier
                .weight(1f, fill = false)
                .fillMaxWidth()
                .nestedScroll(rememberNestedScrollInteropConnection())
                .verticalScroll(rememberScrollState())
        ) {
            FieldLabel(stringResource(R.string.pwd_password))
            KamleonTextField(
                value = currentPassword,
                onValueChange = onCurrentPasswordChange,
                placeholder = stringResource(R.string.placeholder_password),
                secure = true,
                keyboardType = KeyboardType.Password,
                autoCorrect = false,
                contentType = ContentType.Password,
                errorText = fieldErrors.current?.let { stringResource(it) },
                focusRequester = currentFocus,
                testTag = "pwd_current"
            )
            // Right under the password it is about.
            KamleonTextLink(
                text = stringResource(R.string.login_forgot_pwd),
                onClick = onForgotPassword,
                style = kamleonCaption(),
                color = colorResource(R.color.kamleon_secondary_grey_60),
                pressedColor = colorResource(R.color.kamleon_secondary_grey_70),
                underline = true,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                modifier = Modifier.testTag("pwd_forgot")
            )
            Spacer(Modifier.height(8.dp))
            FieldLabel(stringResource(R.string.pwd_new))
            KamleonTextField(
                value = newPassword,
                onValueChange = onNewPasswordChange,
                placeholder = stringResource(R.string.placeholder_password),
                secure = true,
                keyboardType = KeyboardType.Password,
                autoCorrect = false,
                contentType = ContentType.NewPassword,
                errorText = fieldErrors.new?.let { stringResource(it) },
                focusRequester = newFocus,
                supportingContent = {
                    KamleonPasswordStrengthMeter(
                        password = newPassword,
                        strength = passwordStrength,
                        modifier = Modifier.testTag("pwd_new_strength")
                    )
                },
                testTag = "pwd_new"
            )
            Spacer(Modifier.height(16.dp))
            FieldLabel(stringResource(R.string.pwd_confirm))
            KamleonTextField(
                value = confirmPassword,
                onValueChange = onConfirmPasswordChange,
                placeholder = stringResource(R.string.placeholder_password_repeat),
                secure = true,
                keyboardType = KeyboardType.Password,
                autoCorrect = false,
                contentType = ContentType.NewPassword,
                errorText = fieldErrors.confirm?.let { stringResource(it) },
                focusRequester = confirmFocus,
                imeAction = ImeAction.Done,
                onImeAction = submit,
                testTag = "pwd_confirm"
            )
        }
    }
}

/** Messages per field (null = fine). */
internal data class PwdFieldErrors(
    @StringRes val current: Int?,
    @StringRes val new: Int?,
    @StringRes val confirm: Int?
)

/**
 * Field validation for the change-password form. [showBlank] reports empty fields (after the first
 * submit); [force] applies the checks that are only judged on submit (length, weak strength).
 * Server verdicts take precedence over local rules once the field is filled in.
 */
internal fun pwdFieldErrors(
    current: String,
    new: String,
    confirm: String,
    strength: PasswordStrength?,
    showBlank: Boolean,
    force: Boolean,
    @StringRes currentServerError: Int? = null,
    @StringRes newServerError: Int? = null
): PwdFieldErrors = PwdFieldErrors(
    current = when {
        showBlank && current.isEmpty() -> R.string.field_error_password_required
        else -> currentServerError
    },
    new = when {
        showBlank && new.isEmpty() -> R.string.pwd_error_new_required
        new.isNotEmpty() && new == current -> R.string.pwd_error_same_as_current
        newServerError != null -> newServerError
        force && new.length < PasswordLength.MIN -> R.string.password_error_too_short
        force && new.length > PasswordLength.MAX -> R.string.password_error_too_long
        force && strength == PasswordStrength.WEAK -> R.string.password_error_weak
        else -> null
    },
    confirm = when {
        showBlank && confirm.isEmpty() -> R.string.signup_error_repeat_required
        new.isNotEmpty() && confirm.isNotEmpty() && new != confirm -> R.string.pwd_error_dontmatch
        else -> null
    }
)
