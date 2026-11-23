package com.dynatech2012.kamleonuserapp.views.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dynatech2012.kamleonuserapp.R

/**
 * The bell next to the profile button on Home: just the icon, with a red dot on its top right corner
 * when there are pending invitations. Opens the invitations.
 */
@Composable
fun NotificationsButton(hasNotifications: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(
        if (hasNotifications) R.string.home_notifications_button_new else R.string.home_notifications_button
    )
    // Same size as the profile button, so both line up; the whole square is the touch target.
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(36.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_bell),
            contentDescription = null,
            tint = colorResource(R.color.kamleon_secondary_grey_60),
            modifier = Modifier.size(BellSize),
        )
        if (hasNotifications) {
            // Over the bell's top right corner, as in the design.
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = DotOffsetX, y = DotOffsetY)
                    .size(DotSize)
                    .background(colorResource(R.color.color_red), CircleShape),
            )
        }
    }
}

private val BellSize = 26.dp
private val DotSize = 10.dp
// The bell is centred in 36 dp, so it starts at 5 dp; in ic_bell its outline ends about 23.5 dp across
// and 1 dp down at this size. The dot's centre sits 2.5 dp in from that corner, so it overlaps the bell.
private val DotOffsetX = 5.dp + 21.dp - DotSize / 2
private val DotOffsetY = 5.dp + 3.5.dp - DotSize / 2
