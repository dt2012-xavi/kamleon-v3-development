package com.dynatech2012.kamleonuserapp.views.home

import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.ImageLoader
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.application.MatTheme
import com.dynatech2012.kamleonuserapp.views.showProfileImage

/** Who is signed in, as the profile button needs it. */
data class HomeProfile(
    val initials: String? = null,
    val imageUrl: String? = null,
    val imageLoader: ImageLoader? = null,
    val hasNotifications: Boolean = false,
)

/**
 * The round profile button at the top right of the tab screens: the user's photo, or their initials
 * until it loads, with a dot when there are pending invitations. Opens the settings.
 */
@Composable
fun ProfileButton(profile: HomeProfile, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.home_profile_button)
    Box(modifier = modifier.size(36.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(colorResource(R.color.white))
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = label },
        ) {
            Text(
                text = profile.initials ?: stringResource(R.string.default_value),
                fontSize = 16.sp,
                fontFamily = MatTheme.fontFamily,
                color = colorResource(R.color.kamleon_secondary_grey_60),
                modifier = Modifier.clearAndSetSemantics { },
            )
            // The initials stay behind the photo until it has loaded.
            val loader = profile.imageLoader
            if (loader != null) {
                AndroidView(
                    factory = { context -> ImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_CROP } },
                    // Only reload when the address changes, not on every recomposition of the screen.
                    update = { view ->
                        if (view.tag != profile.imageUrl) {
                            view.tag = profile.imageUrl
                            view.showProfileImage(profile.imageUrl, loader)
                        }
                    },
                    modifier = Modifier.size(36.dp),
                )
            }
        }
        if (profile.hasNotifications) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 2.dp)
                    .size(8.dp)
                    .background(colorResource(R.color.kamleon_blue), CircleShape),
            )
        }
    }
}
