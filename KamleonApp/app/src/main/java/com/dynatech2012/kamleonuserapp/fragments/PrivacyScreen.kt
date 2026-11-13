package com.dynatech2012.kamleonuserapp.fragments

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.application.AppTheme
import com.dynatech2012.kamleonuserapp.constants.UrlConstants
import com.dynatech2012.kamleonuserapp.views.KamleonRoundButton
import com.dynatech2012.kamleonuserapp.views.kamleonBody
import com.dynatech2012.kamleonuserapp.views.kamleonBodyBold
import com.dynatech2012.kamleonuserapp.views.kamleonBodySmall
import com.dynatech2012.kamleonuserapp.views.kamleonDisplay

private const val SUPPORT_EMAIL = "info@kamleon.com"

/**
 * Compose version of `activity_privacy.xml`. Layout margins are the XML's literal values (90dp
 * before the title, 36dp side padding for the content, 44dp for the buttons, 50dp under "Next").
 * Links open through [onOpenUrl] and the support address through [onEmail].
 */
@Composable
fun PrivacyScreen(
    onBack: () -> Unit,
    onNext: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onEmail: (String) -> Unit
) {
    // Saveable so the choices survive rotation and process death.
    var policy by rememberSaveable { mutableStateOf(false) }
    var terms by rememberSaveable { mutableStateOf(false) }
    var consent by rememberSaveable { mutableStateOf(false) }
    val allAccepted = policy && terms && consent

    val darkGrey = colorResource(R.color.kamleon_dark_grey)
    val grey70 = colorResource(R.color.kamleon_secondary_grey_70)
    val linkTargets = listOf(
        stringResource(R.string.privacy_word_terms) to UrlConstants.URL_TERMS,
        stringResource(R.string.privacy_word_privacy) to UrlConstants.URL_POLICY_APP,
        stringResource(R.string.privacy_word_consent) to UrlConstants.URL_CONSENT
    )
    val item1 = linkedText(stringResource(R.string.privacy_item1), linkTargets, onOpenUrl)
    val item2 = linkedText(stringResource(R.string.privacy_item2), linkTargets, onOpenUrl)
    val item3 = linkedText(stringResource(R.string.privacy_item3), linkTargets, onOpenUrl)
    val note = linkedText(
        stringResource(R.string.privacy_note),
        listOf(SUPPORT_EMAIL to SUPPORT_EMAIL),
        onEmail
    )

    Box(Modifier.fillMaxSize().background(colorResource(R.color.bg_color_activity))) {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 90.dp)
                    .padding(horizontal = 36.dp)
            ) {
                Text(
                    text = stringResource(R.string.privacy_title),
                    style = kamleonDisplay(),
                    color = darkGrey,
                    modifier = Modifier.padding(top = 8.dp)
                )
                PrivacyItem(policy, { policy = it }, item1, grey70, Modifier.padding(top = 24.dp), "privacy_item_policy")
                PrivacyItem(terms, { terms = it }, item2, grey70, Modifier.padding(top = 18.dp), "privacy_item_terms")
                PrivacyItem(consent, { consent = it }, item3, grey70, Modifier.padding(top = 18.dp), "privacy_item_consent")
                Text(
                    text = note,
                    style = kamleonBodySmall(),
                    color = colorResource(R.color.kamleon_secondary_grey_50),
                    modifier = Modifier.padding(top = 24.dp, end = 16.dp).padding(vertical = 4.dp)
                )
            }

            Column(Modifier.fillMaxWidth().padding(horizontal = 44.dp)) {
                AcceptAll(
                    visible = !allAccepted,
                    onClick = { policy = true; terms = true; consent = true },
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                KamleonRoundButton(
                    text = stringResource(R.string.privacy_button_next),
                    onClick = onNext,
                    enabled = allAccepted,
                    modifier = Modifier.padding(bottom = 50.dp),
                    testTag = "privacy_next"
                )
            }
        }

        // ImageButton 48dp at the top start, 16dp from the top and start; icn_nav_back centered.
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = dimensionResource(R.dimen.nav_back_button_left), top = dimensionResource(R.dimen.nav_back_button_top))
                .size(dimensionResource(R.dimen.nav_back_button_size))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClick = onBack
                )
                .testTag("privacy_back"),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.icn_nav_back),
                contentDescription = null,
                contentScale = ContentScale.None
            )
        }
    }
}

/** One consent row: round check icon plus text; the whole row toggles, links inside open pages. */
@Composable
private fun PrivacyItem(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    text: AnnotatedString,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier,
    testTag: String
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Checkbox,
                onValueChange = onCheckedChange
            )
            .padding(start = 16.dp)
            .testTag(testTag)
    ) {
        // 60px xxhdpi icons => 20dp.
        Image(
            painter = painterResource(if (checked) R.drawable.icn_circular_check else R.drawable.icn_circular_uncheck),
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = text,
            style = kamleonBody(),
            color = color,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 2.dp)
        )
    }
}

/** "Accept All" text button; keeps its space while hidden so the layout does not jump. */
@Composable
private fun AcceptAll(visible: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Text(
        text = stringResource(R.string.privacy_button_accept_all),
        style = kamleonBodyBold(),
        color = colorResource(if (pressed) R.color.kamleon_blue_pressed else R.color.kamleon_blue),
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (visible) 1f else 0f)
            .clickable(
                enabled = visible,
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(vertical = 10.dp)
            .testTag("privacy_accept_all")
    )
}

// Links are bold and underlined. The real Bold face has underline metrics that detach the line from
// the text and taller line boxes, so (like the XML's fake bold) bold is synthesized on the Regular
// face by using a family that only contains it.
private val LinkFontFamily = FontFamily(Font(R.font.circular_pro_regular, FontWeight.Normal))

/** Builds [full] with each word in [links] turned into a bold, underlined link to its target. */
@Composable
private fun linkedText(full: String, links: List<Pair<String, String>>, onLink: (String) -> Unit): AnnotatedString =
    buildAnnotatedString {
        append(full)
        for ((word, target) in links) {
            val start = full.indexOf(word)
            if (start >= 0) {
                addLink(
                    LinkAnnotation.Clickable(
                        tag = target,
                        styles = TextLinkStyles(
                            SpanStyle(
                                fontFamily = LinkFontFamily,
                                fontWeight = FontWeight.Bold,
                                textDecoration = TextDecoration.Underline
                            )
                        ),
                        linkInteractionListener = { onLink(target) }
                    ),
                    start,
                    start + word.length
                )
            }
        }
    }

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun PrivacyScreenPreview() {
    AppTheme(useDarkTheme = false) {
        PrivacyScreen(onBack = {}, onNext = {}, onOpenUrl = {}, onEmail = {})
    }
}
