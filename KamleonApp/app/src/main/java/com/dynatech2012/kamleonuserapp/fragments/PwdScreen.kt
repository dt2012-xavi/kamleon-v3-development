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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.views.HeaderBackButton
import com.dynatech2012.kamleonuserapp.application.AppTheme
import com.dynatech2012.kamleonuserapp.security.PasswordLength
import com.dynatech2012.kamleonuserapp.security.PasswordStrength
import com.dynatech2012.kamleonuserapp.views.KamleonFormLayout
import com.dynatech2012.kamleonuserapp.views.KamleonPasswordStrengthMeter
import com.dynatech2012.kamleonuserapp.views.KamleonRoundButton
import com.dynatech2012.kamleonuserapp.views.KamleonTextLink
import com.dynatech2012.kamleonuserapp.views.toAnnotatedMarkup
import com.dynatech2012.kamleonuserapp.views.kamleonBodySmall
import com.dynatech2012.kamleonuserapp.views.kamleonCaption

/**
 * Compose version of `activity_pwd.xml` (Settings > change password). It keeps the settings look
 * 1:1: round back button, bold uppercase title and shadowed cards whose rows show the label on the
 * left and the value on the right (current password alone; new and confirm sharing a card with a
 * divider). Literal margins come from the XML; 12sp text becomes 13sp like the other Compose screens.
 *
 * Feedback follows Login/Register: every problem appears right under the row that causes it (red
 * label plus message), pressing Save with something missing or invalid moves focus to the first
 * offending field instead of failing silently, Done on the last field submits, and a strength bar
 * sits under the new password. [currentError] and [newError] are verdicts reported back by the
 * ViewModel (wrong current password, policy rejection); they are cleared by the caller when the
 * field changes. [checking] shows the button spinner while the change runs. Save lives in a fixed
 * bar that stays above the keyboard. Changing [formKey] resets the "already submitted" state, e.g.
 * after a successful change clears the fields.
 */
@Composable
fun PwdScreen(
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
    onForgotPassword: () -> Unit,
    onBack: () -> Unit,
    formKey: Int = 0
) {
    var submitted by remember(formKey) { mutableStateOf(false) }
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

    // Verdicts coming back from the ViewModel: take the user to the field to fix.
    LaunchedEffect(currentError) { if (currentError != null) currentFocus.requestFocus() }
    LaunchedEffect(newError) { if (newError != null) newFocus.requestFocus() }

    val horizontal = dimensionResource(R.dimen.margin_horizontal_header)
    Column(Modifier.fillMaxSize().background(colorResource(R.color.white))) {
        HeaderBackButton(
            onClick = onBack,
            modifier = Modifier.padding(
                start = horizontal,
                top = dimensionResource(R.dimen.margin_top_header),
                bottom = dimensionResource(R.dimen.margin_bottom_header)
            ).testTag("pwd_back")
        )
        KamleonFormLayout(
            barHorizontalPadding = horizontal,
            bottomBar = {
                KamleonRoundButton(
                    text = stringResource(R.string.setting_button_save),
                    onClick = submit,
                    enabled = true,
                    loading = checking,
                    testTag = "pwd_save"
                )
            }
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = horizontal)) {
                Text(
                    text = stringResource(R.string.pwd_title),
                    style = kamleonBodySmall().copy(fontWeight = FontWeight.Bold),
                    color = colorResource(R.color.text_color_category),
                    modifier = Modifier.padding(top = 16.dp)
                )
                Column(Modifier.padding(vertical = 8.dp)) {
                    // The XML card is 44dp tall with the row centred; it grows to fit an error.
                    SettingsCard(
                        modifier = Modifier.heightIn(min = 44.dp),
                        verticalPadding = 4.dp
                    ) {
                        PasswordRow(
                            label = stringResource(R.string.pwd_password),
                            value = currentPassword,
                            onValueChange = onCurrentPasswordChange,
                            contentType = ContentType.Password,
                            errorText = fieldErrors.current?.let { stringResource(it) },
                            focusRequester = currentFocus,
                            testTag = "pwd_current"
                        )
                    }
                    SettingsCard(modifier = Modifier.padding(top = 16.dp), verticalPadding = 10.dp) {
                        PasswordRow(
                            label = stringResource(R.string.pwd_new),
                            value = newPassword,
                            onValueChange = onNewPasswordChange,
                            contentType = ContentType.NewPassword,
                            errorText = fieldErrors.new?.let { stringResource(it) },
                            focusRequester = newFocus,
                            supportingContent = {
                                KamleonPasswordStrengthMeter(
                                    password = newPassword,
                                    strength = passwordStrength,
                                    inset = 0.dp,
                                    modifier = Modifier.padding(bottom = 4.dp).testTag("pwd_new_strength")
                                )
                            },
                            testTag = "pwd_new"
                        )
                        // 1px divider, 8dp in from each side, 4dp above and below.
                        val onePx = with(LocalDensity.current) { 1.toDp() }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = dimensionResource(R.dimen.setting_item_padding_h), vertical = 4.dp)
                                .height(onePx)
                                .background(colorResource(R.color.kamleon_secondary_grey_40))
                        )
                        PasswordRow(
                            label = stringResource(R.string.pwd_confirm),
                            value = confirmPassword,
                            onValueChange = onConfirmPasswordChange,
                            contentType = ContentType.NewPassword,
                            errorText = fieldErrors.confirm?.let { stringResource(it) },
                            focusRequester = confirmFocus,
                            imeAction = ImeAction.Done,
                            onImeAction = submit,
                            testTag = "pwd_confirm"
                        )
                    }
                    KamleonTextLink(
                        text = stringResource(R.string.login_forgot_pwd),
                        onClick = onForgotPassword,
                        style = kamleonCaption(),
                        color = colorResource(R.color.kamleon_secondary_grey_60),
                        pressedColor = colorResource(R.color.kamleon_secondary_grey_70),
                        underline = true,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier.padding(top = 12.dp).testTag("pwd_forgot")
                    )
                    Text(
                        text = annotatedStringResource(R.string.pwd_desc),
                        style = kamleonCaption(),
                        color = colorResource(R.color.kamleon_dark_grey_50),
                        modifier = Modifier.padding(top = 16.dp).padding(horizontal = 16.dp)
                    )
                }
            }
            // Breathing room so the last block can scroll clear of the fixed button bar.
            Spacer(Modifier.height(12.dp))
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

/**
 * Settings card (`bg_setting_list_shadow`): #FAFAFA rounded rectangle over six 1dp rings of
 * increasingly dark translucent black that fake a soft shadow, 16dp horizontal padding inside.
 */
@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    verticalPadding: Dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val radius = dimensionResource(R.dimen.setting_option_list_radius)
    val rings = listOf(
        R.color.shadow_black_0, R.color.shadow_black_1, R.color.shadow_black_2,
        R.color.shadow_black_3, R.color.shadow_black_4, R.color.shadow_black_5
    ).map { colorResource(it) }
    var layered: Modifier = Modifier
    // Each layer-list item insets the next by 1dp on every side.
    rings.forEach { ring ->
        layered = layered.background(ring, RoundedCornerShape(radius)).padding(1.dp)
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(layered)
            .background(colorResource(R.color.color_fa), RoundedCornerShape(radius))
            .padding(horizontal = 16.dp - 6.dp, vertical = (verticalPadding - 6.dp).coerceAtLeast(0.dp)),
        verticalArrangement = Arrangement.Center,
        content = content
    )
}

/**
 * One settings row: bold label on the left, the password right-aligned in the remaining space and
 * an eye toggle (tappable but not a keyboard focus stop). The error message or [supportingContent]
 * go under the row and are kept in view while the field has focus, like [KamleonTextField].
 */
@Composable
private fun PasswordRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    contentType: ContentType,
    errorText: String?,
    focusRequester: FocusRequester,
    testTag: String,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null,
    supportingContent: (@Composable () -> Unit)? = null
) {
    var secureOn by rememberSaveable { mutableStateOf(true) }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val bringIntoView = remember { BringIntoViewRequester() }
    var blockSize by remember { mutableStateOf(IntSize.Zero) }
    val extraBelow = with(LocalDensity.current) { 14.dp.toPx() }
    LaunchedEffect(errorText, focused, blockSize.height) {
        if (focused && blockSize.height > 0 && (errorText != null || supportingContent != null)) {
            bringIntoView.bringIntoView(Rect(0f, 0f, blockSize.width.toFloat(), blockSize.height + extraBelow))
        }
    }
    val red = colorResource(R.color.kmln_red)
    val labelStyle = kamleonBodySmall().copy(fontWeight = FontWeight.Bold)

    Column(Modifier.onSizeChanged { blockSize = it }.bringIntoViewRequester(bringIntoView)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = labelStyle,
                color = if (errorText != null) red else colorResource(R.color.kamleon_dark_grey)
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = kamleonBodySmall().copy(
                    color = colorResource(R.color.kamleon_secondary_grey_70),
                    textAlign = TextAlign.End
                ),
                interactionSource = interaction,
                cursorBrush = SolidColor(colorResource(R.color.kamleon_blue)),
                keyboardOptions = KeyboardOptions(
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Password,
                    imeAction = imeAction
                ),
                keyboardActions = if (onImeAction != null) {
                    // Next too: the Samsung keyboard can still be showing (and sending) the previous
                    // field's action right after focus moves here.
                    KeyboardActions(
                        onDone = { onImeAction() },
                        onGo = { onImeAction() },
                        onSend = { onImeAction() },
                        onNext = { onImeAction() }
                    )
                } else {
                    KeyboardActions.Default
                },
                visualTransformation = if (secureOn) PasswordVisualTransformation() else VisualTransformation.None,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
                    .focusRequester(focusRequester)
                    .semantics {
                        this.contentDescription = label
                        this.contentType = contentType
                        if (errorText != null) error(errorText)
                    }
                    .testTag(testTag)
            )
            Box(
                // XML: 12dp gap before a 20x10dp eye. The tappable area adds a little room around it
                // without making the row taller.
                modifier = Modifier
                    .padding(start = 4.dp)
                    .focusProperties { canFocus = false }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClickLabel = stringResource(
                            if (secureOn) R.string.auth_show_password else R.string.auth_hide_password
                        )
                    ) { secureOn = !secureOn }
                    .padding(start = 8.dp, top = 4.dp, bottom = 4.dp)
                    .testTag(testTag + "_eye"),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(if (secureOn) R.drawable.ic_eye_crossed else R.drawable.ic_eye_open),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(
                        dimensionResource(R.dimen.input_text_box_eye_width),
                        dimensionResource(R.dimen.input_text_box_eye_height)
                    )
                )
            }
        }
        if (errorText != null) {
            Text(
                text = errorText,
                style = kamleonCaption(),
                color = red,
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite }
                    .testTag(testTag + "_error")
            )
        } else {
            supportingContent?.invoke()
        }
    }
}

/** A string resource keeping its bold and underline markup, which `stringResource` drops. */
@Composable
private fun annotatedStringResource(@StringRes id: Int): AnnotatedString {
    val text = LocalResources.current.getText(id)
    return remember(text) { text.toAnnotatedMarkup() }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun PwdScreenPreview() {
    AppTheme(useDarkTheme = false) {
        PwdScreen(
            currentPassword = "secret", newPassword = "new-pass-1", confirmPassword = "new-pass",
            currentError = null, newError = null, checking = false,
            passwordStrength = PasswordStrength.GOOD,
            onCurrentPasswordChange = {}, onNewPasswordChange = {}, onConfirmPasswordChange = {},
            onSave = {}, onForgotPassword = {}, onBack = {}
        )
    }
}
