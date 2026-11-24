package com.dynatech2012.kamleonuserapp.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dynatech2012.kamleonuserapp.R

/**
 * Round back button of a screen header: a 36dp white circle with an 8dp shadow, the Compose twin of
 * the `Widget.Kamleon.HeaderButton` style.
 */
@Composable
fun HeaderBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.nav_back)
    Box(
        modifier = modifier
            .size(36.dp)
            .shadow(8.dp, CircleShape)
            .background(colorResource(R.color.white), CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        // The chevron is drawn slightly left of centre, as in the XML headers.
        Image(
            painter = painterResource(R.drawable.ic_arrow_back_ios_new_24),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(20.dp).padding(end = 3.dp)
        )
    }
}
