package com.dynatech2012.kamleonuserapp.views

import androidx.annotation.DimenRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.application.MatTheme
import com.dynatech2012.kamleonuserapp.security.PasswordStrength

// Shared building blocks for the Compose screens: one type scale, one text field, one button and
// one text link. Sizes and colors come from the existing dimen/color resources.
//
// Typography: every text uses the Circular family through [MatTheme.fontFamily] (Regular, Medium,
// Bold). The XML screens also end up on those faces: the theme text style (TextAppearance.Kamleon)
// sets `app:fontFamily`, which AppCompat prioritises over the `android:fontFamily` of the
// CircularBookFont / CircularMediumFont styles, so "Book" text is actually drawn as Regular. The
// Book face is therefore not part of the Compose scale; reintroduce it deliberately if wanted.

/** Reads an `sp` dimen resource as a [TextUnit], honouring the user's font scale. */
@Composable
fun spResource(@DimenRes id: Int): TextUnit {
    val dp = dimensionResource(id)
    return with(LocalDensity.current) { dp.toSp() }
}

// XML TextViews include font padding by default; Compose does not. Keep it so line boxes match
// the XML screens that are still around.
@Suppress("DEPRECATION")
private val XmlFontPadding = PlatformTextStyle(includeFontPadding = true)

@Composable
private fun circular(@DimenRes size: Int, weight: FontWeight = FontWeight.Normal): TextStyle = TextStyle(
    fontFamily = MatTheme.fontFamily,
    fontWeight = weight,
    fontSize = spResource(size),
    platformStyle = XmlFontPadding
)

/** Screen titles: Bold 20sp. */
@Composable
fun kamleonTitle() = circular(R.dimen.ts_20, FontWeight.Bold)

/** Descriptions and primary body copy: Regular 16sp. */
@Composable
fun kamleonBody() = circular(R.dimen.ts_16)

/** Emphasised body copy such as inline links and text buttons: Bold 16sp. */
@Composable
fun kamleonBodyBold() = circular(R.dimen.ts_16, FontWeight.Bold)

/** Secondary body copy and field text: Regular 14sp. */
@Composable
fun kamleonBodySmall() = circular(R.dimen.ts_14)

/** Links, hints and captions: Regular 13sp (replaces the 12sp used by the XML screens). */
@Composable
fun kamleonCaption() = circular(R.dimen.ts_13)

/** Button labels: Medium, size from `round_blue_button_text_size`. */
@Composable
fun kamleonButtonText() = circular(R.dimen.round_blue_button_text_size, FontWeight.Medium)

/**
 * Text that darkens while pressed, like the `color_text_button` selector used by XML text links.
 * Callers pass the resting and pressed colors.
 */
@Composable
fun KamleonTextLink(
    text: String,
    onClick: () -> Unit,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    pressedColor: Color = color,
    underline: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Text(
        text = text,
        style = style,
        color = if (pressed) pressedColor else color,
        textDecoration = if (underline) TextDecoration.Underline else null,
        modifier = modifier
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(contentPadding)
    )
}

/**
 * The app's single-line text field: a pill with the placeholder inside (it disappears as soon as
 * there is text), left-aligned text, a visible outline (so it never vanishes on light backgrounds)
 * that turns teal while focused, and an eye toggle when [secure].
 *
 * A disabled field (`enabled = false`) is read-only and greyed out; `isError` draws a red outline.
 * [errorText] shows the message right under the field (red outline included), announces it to
 * accessibility services and scrolls it into view when the field has focus, so it never ends up
 * hidden behind the keyboard. [supportingContent] (e.g. a strength meter) takes the error's place
 * while there is no error and is kept in view the same way. [contentType] marks the field for autofill, [onFocusChange] reports
 * focus changes and [focusRequester] lets the screen move focus here (e.g. to the first invalid
 * field).
 *
 * Keyboard actions are the defaults (Next moves focus to the following field, Done hides the
 * keyboard) unless [onImeAction] is given: then any action (Done/Go/Send, and Next for keyboards
 * that lag behind the focus change) calls it, e.g. to submit the form from the last field.
 */
@Composable
fun KamleonTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    secure: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    enabled: Boolean = true,
    isError: Boolean = false,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    autoCorrect: Boolean = true,
    contentType: ContentType? = null,
    onFocusChange: ((Boolean) -> Unit)? = null,
    errorText: String? = null,
    focusRequester: FocusRequester? = null,
    onImeAction: (() -> Unit)? = null,
    supportingContent: (@Composable () -> Unit)? = null,
    testTag: String? = null
) {
    var secureOn by rememberSaveable { mutableStateOf(true) }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    if (onFocusChange != null) {
        LaunchedEffect(focused) { onFocusChange(focused) }
    }
    val bringIntoView = remember { BringIntoViewRequester() }
    val showError = isError || errorText != null
    // Keep the whole block (field plus its error or supporting content) in view while the field
    // has focus, with some breathing room below, so neither ends up under the keyboard or the fixed
    // action bar. Re-run when the block changes height (e.g. a strength bar replacing the hint).
    var blockSize by remember { mutableStateOf(IntSize.Zero) }
    val extraBelow = with(LocalDensity.current) { 14.dp.toPx() }
    LaunchedEffect(errorText, focused, blockSize.height) {
        if (focused && blockSize.height > 0 && (errorText != null || supportingContent != null)) {
            bringIntoView.bringIntoView(Rect(0f, 0f, blockSize.width.toFloat(), blockSize.height + extraBelow))
        }
    }
    val shape = RoundedCornerShape(dimensionResource(R.dimen.input_text_box_corner_radius))
    val textStyle = kamleonBodySmall().copy(
        color = colorResource(if (enabled) R.color.kamleon_secondary_grey_70 else R.color.kamleon_secondary_grey_60)
    )
    val eyeSize = Modifier.size(
        dimensionResource(R.dimen.input_text_box_eye_width),
        dimensionResource(R.dimen.input_text_box_eye_height)
    )

    Column(modifier.onSizeChanged { blockSize = it }.bringIntoViewRequester(bringIntoView)) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            enabled = enabled,
            textStyle = textStyle,
            interactionSource = interaction,
            cursorBrush = SolidColor(colorResource(R.color.kamleon_blue)),
            keyboardOptions = KeyboardOptions(
                capitalization = capitalization,
                autoCorrectEnabled = autoCorrect,
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            keyboardActions = if (onImeAction != null) {
                // Next too: the Samsung keyboard can still be showing (and sending) the previous field's
                // action right after focus moves here.
                KeyboardActions(
                    onDone = { onImeAction() },
                    onGo = { onImeAction() },
                    onSend = { onImeAction() },
                    onNext = { onImeAction() }
                )
            } else {
                KeyboardActions.Default
            },
            visualTransformation = if (secure && secureOn) PasswordVisualTransformation() else VisualTransformation.None,
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(R.dimen.input_text_box_height))
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .then(if (errorText != null) Modifier.semantics { error(errorText) } else Modifier)
                .then(if (contentType != null) Modifier.semantics { this.contentType = contentType } else Modifier)
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimensionResource(R.dimen.input_text_box_height))
                        .clip(shape)
                        .background(colorResource(R.color.color_fa))
                        .border(
                            BorderStroke(
                                1.dp,
                                when {
                                    showError -> colorResource(R.color.kmln_red)
                                    focused -> colorResource(R.color.kamleon_blue)
                                    else -> colorResource(R.color.kamleon_outline)
                                }
                            ),
                            shape
                        )
                        .padding(horizontal = dimensionResource(R.dimen.input_text_box_padding_h) + 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = kamleonBodySmall(),
                                color = colorResource(R.color.kamleon_secondary_grey_60)
                            )
                        }
                        innerTextField()
                    }
                    if (secure) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                // Tappable but not a keyboard focus stop: otherwise the IME "Next" action
                                // from the password field lands on the eye instead of the next field.
                                .focusProperties { canFocus = false }
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    role = Role.Button,
                                    onClickLabel = stringResource(
                                        if (secureOn) R.string.auth_show_password else R.string.auth_hide_password
                                    )
                                ) { secureOn = !secureOn }
                                .padding(start = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(if (secureOn) R.drawable.ic_eye_crossed else R.drawable.ic_eye_open),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = eyeSize
                            )
                        }
                    }
                }
            }
        )
        if (errorText != null) {
            Text(
                text = errorText,
                style = kamleonCaption(),
                color = colorResource(R.color.kmln_red),
                modifier = Modifier
                    .padding(start = dimensionResource(R.dimen.input_text_box_padding_h) + 4.dp, top = 4.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite }
                    .then(if (testTag != null) Modifier.testTag(testTag + "_error") else Modifier)
            )
        } else {
            supportingContent?.invoke()
        }
    }
}

/**
 * Primary pill button: filled teal, translucent when pressed, outlined when disabled. While
 * [loading] the label is replaced by a small spinner and the button is inert. With [destructive]
 * it is red instead of teal (confirming something that cannot be undone).
 */
@Composable
fun KamleonRoundButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    destructive: Boolean = false,
    testTag: String? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val active = enabled && !loading
    val shape = RoundedCornerShape(dimensionResource(R.dimen.round_blue_button_corner_radius))
    val grey40 = colorResource(R.color.kamleon_secondary_grey_40)
    val container = when {
        !active -> Color.Transparent
        destructive -> colorResource(R.color.kmln_red).copy(alpha = if (pressed) 0.8f else 1f)
        pressed -> colorResource(R.color.kamleon_blue_pressed)
        else -> colorResource(R.color.kamleon_blue)
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(dimensionResource(R.dimen.round_blue_button_height))
            .clip(shape)
            .background(container)
            .then(
                if (!active) Modifier.border(BorderStroke(1.dp, colorResource(R.color.kamleon_outline)), shape)
                else Modifier
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = active,
                role = Role.Button,
                onClick = onClick
            )
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = grey40,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                style = kamleonButtonText(),
                color = if (active) colorResource(R.color.white) else grey40
            )
        }
    }
}

/** Thin linear progress bar (4dp, square ends): [progress] in 0..1, teal over a light track. */
@Composable
fun KamleonProgressBar(progress: Float, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), label = "progress")
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(colorResource(R.color.kamleon_progress_track))
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .fillMaxHeight()
                .background(colorResource(R.color.kamleon_blue))
        )
    }
}

/**
 * Layout shared by the auth forms: the [content] scrolls, while the primary action stays in a
 * fixed [bottomBar] that always remains on screen (above the keyboard when it is open), so the
 * button is never buried under the IME. [bottomBar] receives whether the keyboard is visible so it
 * can hide secondary content (e.g. the "Register" link) and give the fields the space.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KamleonFormLayout(
    bottomBar: @Composable ColumnScope.(imeVisible: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    barHorizontalPadding: Dp = 44.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val imeVisible = WindowInsets.isImeVisible
    Column(modifier.fillMaxSize().background(colorResource(R.color.white))) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), content = content)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = barHorizontalPadding)
                .padding(top = 8.dp, bottom = if (imeVisible) 12.dp else 40.dp),
            content = { bottomBar(imeVisible) }
        )
    }
}

/**
 * Password strength feedback shown under a password field: the usage hint while it is empty, then
 * a three-segment bar with a label. [strength] null (not scored yet) keeps the hint. [inset] is the
 * horizontal indent; it defaults to the text inset of [KamleonTextField].
 */
@Composable
fun KamleonPasswordStrengthMeter(
    password: String,
    strength: PasswordStrength?,
    modifier: Modifier = Modifier,
    inset: Dp = dimensionResource(R.dimen.input_text_box_padding_h) + 4.dp
) {
    if (password.isEmpty() || strength == null) {
        Text(
            text = stringResource(R.string.password_hint),
            style = kamleonCaption(),
            color = colorResource(R.color.kamleon_secondary_grey_60),
            modifier = modifier.padding(start = inset, top = 4.dp)
        )
        return
    }
    val (filled, color, label) = when (strength) {
        PasswordStrength.WEAK -> Triple(1, colorResource(R.color.kmln_red), R.string.password_strength_weak)
        PasswordStrength.GOOD -> Triple(2, colorResource(R.color.kamleon_strength_good), R.string.password_strength_good)
        PasswordStrength.STRONG -> Triple(3, colorResource(R.color.kamleon_blue), R.string.password_strength_strong)
    }
    val text = stringResource(label)
    Column(
        modifier = modifier
            .padding(start = inset, end = inset, top = 6.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(3) { index ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (index < filled) color else colorResource(R.color.kamleon_progress_track))
                )
            }
        }
        Text(text = text, style = kamleonCaption(), color = color, modifier = Modifier.padding(top = 4.dp))
    }
}

/**
 * "Prompt + link" row (e.g. "Don't you have an account yet? Register") centred under the primary
 * button. It wraps to a second line instead of squeezing the link when the font is large.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KamleonSwitchRow(prompt: String, action: String, onClick: () -> Unit, modifier: Modifier = Modifier, actionTestTag: String? = null) {
    FlowRow(
        modifier = modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.Center,
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = prompt, style = kamleonBodySmall(), color = colorResource(R.color.kamleon_secondary_grey_60))
        KamleonTextLink(
            text = action,
            onClick = onClick,
            style = kamleonBodySmall(),
            color = colorResource(R.color.kamleon_blue),
            underline = true,
            contentPadding = PaddingValues(horizontal = 10.dp),
            modifier = if (actionTestTag != null) Modifier.testTag(actionTestTag) else Modifier
        )
    }
}
