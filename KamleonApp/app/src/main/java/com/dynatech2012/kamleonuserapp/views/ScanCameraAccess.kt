package com.dynatech2012.kamleonuserapp.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dynatech2012.kamleonuserapp.R

/** Whether the scanner can use the camera, and if not, what the user can do about it. */
enum class CameraAccess {
    Granted,
    /** Not allowed yet, and Android will still ask. */
    Ask,
    /** Denied for good: only the app settings can allow it. */
    Blocked,
    /** The phone has no camera to scan with. */
    NoCamera,
}

/**
 * [askedBefore]: the app already asked once. Android stops showing the rationale flag after a
 * definite "no", which is how a refusal for good is told apart from one where it will ask again.
 */
fun cameraAccessOf(hasCamera: Boolean, granted: Boolean, askedBefore: Boolean, showRationale: Boolean): CameraAccess =
    when {
        !hasCamera -> CameraAccess.NoCamera
        granted -> CameraAccess.Granted
        askedBefore && !showRationale -> CameraAccess.Blocked
        else -> CameraAccess.Ask
    }

/**
 * What the scanner shows instead of the camera when it can't use it: why, and the one thing that
 * helps (ask again, or open the app settings). Light on the scanner's dark background.
 */
@Composable
fun CameraAccessPanel(access: CameraAccess, onAllow: () -> Unit, onOpenSettings: () -> Unit) {
    if (access == CameraAccess.Granted) return
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .padding(bottom = 120.dp)
            .testTag("camera_access")
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_camera),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(
                if (access == CameraAccess.NoCamera) R.string.scan_no_camera_title else R.string.scan_camera_denied_title
            ),
            style = kamleonTitle(),
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(
                when (access) {
                    CameraAccess.Blocked -> R.string.scan_camera_blocked_message
                    CameraAccess.NoCamera -> R.string.scan_no_camera_message
                    else -> R.string.scan_camera_denied_message
                }
            ),
            style = kamleonBody(),
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        when (access) {
            CameraAccess.Ask -> {
                Spacer(Modifier.height(24.dp))
                KamleonRoundButton(
                    text = stringResource(R.string.scan_allow_camera),
                    onClick = onAllow,
                    enabled = true,
                    testTag = "camera_allow"
                )
            }
            CameraAccess.Blocked -> {
                Spacer(Modifier.height(24.dp))
                KamleonRoundButton(
                    text = stringResource(R.string.setting_noti_open_settings),
                    onClick = onOpenSettings,
                    enabled = true,
                    testTag = "camera_settings"
                )
            }
            else -> Unit
        }
    }
}
