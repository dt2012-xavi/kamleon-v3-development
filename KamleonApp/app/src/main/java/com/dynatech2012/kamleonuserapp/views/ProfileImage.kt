package com.dynatech2012.kamleonuserapp.views

import android.view.View
import android.widget.ImageView
import coil.ImageLoader
import coil.dispose
import coil.load

/** The photo to show for an account, or null when only the initials should be on screen. */
internal fun profileImageUrlOf(imageUrl: String?): String? = imageUrl?.trim()?.takeIf { it.isNotEmpty() }

/**
 * Shows the profile photo in this view with the app's shared [ImageLoader]. The view starts hidden (the
 * initials are behind it): it appears when a photo has loaded, and is hidden again when there is none.
 * A failed load leaves whatever the view already showed, so a refresh that fails offline does not
 * blank an avatar that was already on screen.
 */
internal fun ImageView.showProfileImage(url: String?, loader: ImageLoader) {
    if (url == null) {
        dispose()
        setImageDrawable(null)
        visibility = View.INVISIBLE
        return
    }
    load(url, loader) {
        listener(onSuccess = { _, _ -> visibility = View.VISIBLE })
    }
}

/**
 * The photo is always uploaded to the same storage path, so after a new upload the old copy may still be
 * cached under an unchanged address. Forgetting what is cached makes the next load fetch the new one.
 */
internal fun ImageLoader.forgetCachedImages(url: String?) {
    memoryCache?.clear()
    if (url != null) diskCache?.remove(url)
}
