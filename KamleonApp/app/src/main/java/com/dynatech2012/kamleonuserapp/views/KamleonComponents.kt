package com.dynatech2012.kamleonuserapp.views

import androidx.annotation.DimenRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.application.MatTheme

// Compose counterparts of the XML styles (CircularRegularFont, CircularBookFont,
// RoundBlueButtonTextStyle, TextAppearance.Kamleon.Headline6) and of the custom views
// (KamleonInputBox, bg_btn_round_blue). Sizes and colors come from the same dimen and
// color resources so XML and Compose screens stay identical.

/** Reads an `sp` dimen resource as a [TextUnit], honouring the user's font scale like XML does. */
@Composable
fun spResource(@DimenRes id: Int): TextUnit {
    val dp = dimensionResource(id)
    return with(LocalDensity.current) { dp.toSp() }
}

// XML TextViews include font padding by default; Compose does not. Keep it so line boxes match.
@Suppress("DEPRECATION")
private val XmlFontPadding = PlatformTextStyle(includeFontPadding = true)

@Composable
fun kamleonTextStyle(
    fontFamily: FontFamily,
    size: TextUnit,
    weight: FontWeight = FontWeight.Normal
): TextStyle = TextStyle(
    fontFamily = fontFamily,
    fontWeight = weight,
    fontSize = size,
    platformStyle = XmlFontPadding
)

/** `style="@style/CircularRegularFont"`. */
@Composable
fun kamleonRegular(size: TextUnit) = kamleonTextStyle(MatTheme.fontFamily, size)

/**
 * `style="@style/CircularBookFont"`.
 *
 * Measured on device (SM-A556B, API 36): TextViews using the XML Book style are drawn with the
 * Regular face (glyph widths match Regular pixel for pixel; the hinted Book TTF is not what shows).
 * Compose uses the same face so both UIs look identical. If the XML is ever fixed to render the
 * real Book face, switch this to `MatTheme.fontFamilyBook`.
 */
@Composable
fun kamleonBook(size: TextUnit) = kamleonTextStyle(MatTheme.fontFamily, size)

/** `textAppearance="@style/TextAppearance.Kamleon.Headline6"` (bold, 20sp, Circular family). */
@Composable
fun kamleonHeadline6() =
    kamleonTextStyle(MatTheme.fontFamily, spResource(R.dimen.ts_20), FontWeight.Bold)

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
    contentPadding: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.foundation.layout.PaddingValues(0.dp)
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
 * Port of `KamleonInputBox` (`layout_kamleon_input_box.xml`): a pill with the label on the left
 * and the right-aligned value, plus the arrow/eye toggle for secure inputs.
 */
@Composable
fun KamleonInputBox(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    secure: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    testTag: String? = null
) {
    var secureOn by rememberSaveable { mutableStateOf(true) }
    val grey60 = colorResource(R.color.kamleon_secondary_grey_60)
    val textSize = spResource(R.dimen.ts_14)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(dimensionResource(R.dimen.input_text_box_height))
            .background(
                colorResource(R.color.color_fa),
                RoundedCornerShape(dimensionResource(R.dimen.input_text_box_corner_radius))
            )
            .padding(horizontal = dimensionResource(R.dimen.input_text_box_padding_h))
    ) {
        Text(
            text = placeholder,
            style = kamleonBook(textSize),
            color = grey60,
            modifier = Modifier.align(Alignment.CenterStart)
        )
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                // The XML EditText keeps the platform default font, not Circular.
                textStyle = kamleonTextStyle(FontFamily.Default, textSize).copy(
                    color = colorResource(R.color.kamleon_secondary_grey_70),
                    textAlign = TextAlign.End
                ),
                cursorBrush = SolidColor(colorResource(R.color.kamleon_blue)),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
                // Default keyboard actions: Next moves focus to the following field and Done hides
                // the keyboard, like the XML EditTexts (overriding them would break Next).
                visualTransformation = if (secure && secureOn) PasswordVisualTransformation() else VisualTransformation.None,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 50.dp)
                    .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            )
            if (secure) {
                Row(
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(R.drawable.icn_secure_input_arrow),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(
                            dimensionResource(R.dimen.input_text_box_eye_width),
                            dimensionResource(R.dimen.input_text_box_eye_height)
                        )
                    )
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
        }
    }
}

/**
 * Port of the `bg_btn_round_blue` button: filled, translucent when pressed, outlined when
 * disabled. While [loading] the label is replaced by a small spinner and the button is inert.
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
    // The XML disabled outline (bg_btn_round_blue_disabled) is drawn lighter than its grey40
    // stroke color on device (measured #E4E4E4 over white), so match what is actually shown.
    val disabledStroke = Color(0xFFE4E4E4)
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
            .then(if (!active) Modifier.border(BorderStroke(1.dp, disabledStroke), shape) else Modifier)
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
            // Matches the 16dp progressBarStyleSmall tinted grey40 in activity_login.xml.
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = grey40,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                style = kamleonTextStyle(
                    MatTheme.fontFamily,
                    spResource(R.dimen.round_blue_button_text_size),
                    FontWeight.Medium
                ),
                color = if (active) colorResource(R.color.white) else grey40
            )
        }
    }
}
