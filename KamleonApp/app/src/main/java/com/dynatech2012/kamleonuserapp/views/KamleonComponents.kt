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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.application.MatTheme

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

/** Large headings on the left-aligned flow screens (privacy): Regular 32sp. */
@Composable
fun kamleonDisplay() = circular(R.dimen.ts_32)

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
 * A disabled field (`enabled = false`) is read-only and greyed out.
 *
 * Keyboard actions are the defaults: Next moves focus to the following field and Done hides the
 * keyboard.
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
    testTag: String? = null
) {
    var secureOn by rememberSaveable { mutableStateOf(true) }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(dimensionResource(R.dimen.input_text_box_corner_radius))
    val textStyle = kamleonBodySmall().copy(
        color = colorResource(if (enabled) R.color.kamleon_secondary_grey_70 else R.color.kamleon_secondary_grey_60)
    )
    val eyeSize = Modifier.size(
        dimensionResource(R.dimen.input_text_box_eye_width),
        dimensionResource(R.dimen.input_text_box_eye_height)
    )

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        enabled = enabled,
        textStyle = textStyle,
        interactionSource = interaction,
        cursorBrush = SolidColor(colorResource(R.color.kamleon_blue)),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        visualTransformation = if (secure && secureOn) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = modifier
            .fillMaxWidth()
            .height(dimensionResource(R.dimen.input_text_box_height))
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
                            if (focused) colorResource(R.color.kamleon_blue) else colorResource(R.color.kamleon_outline)
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
}

/**
 * Primary pill button: filled teal, translucent when pressed, outlined when disabled. While
 * [loading] the label is replaced by a small spinner and the button is inert.
 */
@Composable
fun KamleonRoundButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    testTag: String? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val active = enabled && !loading
    val shape = RoundedCornerShape(dimensionResource(R.dimen.round_blue_button_corner_radius))
    val grey40 = colorResource(R.color.kamleon_secondary_grey_40)
    val container = when {
        !active -> Color.Transparent
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
