package com.dynatech2012.kamleonuserapp.fragments

import android.view.View
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import coil.ImageLoader
import coil.decode.ImageDecoderDecoder
import coil.load
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.application.AppTheme

/**
 * Compose version of `activity_splash.xml`: the animated logo (a 1920x1080 GIF, center-cropped to a
 * square) at 60% of the width, centred on the surface color. The GIF is drawn by Coil into an
 * `ImageView`: Compose has no animated-GIF painter and the project does not use coil-compose.
 * minSdk is 30, so the platform ImageDecoder is always available.
 */
@Composable
fun SplashScreen() {
    val context = LocalContext.current
    // One loader for the screen's lifetime; Coil decodes the GIF at the view's size.
    val imageLoader = remember(context) {
        ImageLoader.Builder(context).components { add(ImageDecoderDecoder.Factory()) }.build()
    }
    Box(
        modifier = Modifier.fillMaxSize().background(colorResource(R.color.white)),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                ImageView(ctx).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    // Decorative: the screen has no content for screen readers.
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    setImageResource(R.drawable.app_icon_foreground)
                    load(R.drawable.splash, imageLoader)
                }
            },
            modifier = Modifier.fillMaxWidth(0.6f).aspectRatio(1f)
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun SplashScreenPreview() {
    AppTheme(useDarkTheme = false) { SplashScreen() }
}
