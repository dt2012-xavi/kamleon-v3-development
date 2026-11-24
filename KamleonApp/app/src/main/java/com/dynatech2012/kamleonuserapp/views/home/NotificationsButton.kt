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
 * The bell next to the profile button on Home: just the dark icon, with a red dot on its top right corner
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
            .size(ButtonSize.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_bell),
            contentDescription = null,
            // Same dark grey as the rest of Home's content over the gradient.
            tint = colorResource(R.color.kamleon_dark_grey),
            modifier = Modifier.offset(y = BellShiftY).size(BellSize),
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

private const val ButtonSize = 36f
private val BellSize = 22.dp
private val DotSize = 8.dp
// In ic_bell (24 units) the outline's top right corner is at about (17.8, 3) and the clapper ends at 22.75.
// The dot's centre sits on that corner, so it overlaps the bell.
private const val BellScale = 22f / 24f
private const val DotCentreX = 19.4f * BellScale
private const val DotCentreY = 3.2f * BellScale
// Bell and dot together run from the dot's top to the clapper; shift the bell down so that whole set,
// not just the bell, is centred on the profile button next to it.
private const val BellTop = (ButtonSize - 22f) / 2
private const val SetTopInBell = DotCentreY - 4f
private const val SetBottomInBell = 22.75f * BellScale
private val BellShiftY = (ButtonSize / 2 - (BellTop + (SetTopInBell + SetBottomInBell) / 2)).dp
private val DotOffsetX = ((ButtonSize - 22f) / 2 + DotCentreX).dp - DotSize / 2
private val DotOffsetY = (BellTop + DotCentreY).dp + BellShiftY - DotSize / 2
