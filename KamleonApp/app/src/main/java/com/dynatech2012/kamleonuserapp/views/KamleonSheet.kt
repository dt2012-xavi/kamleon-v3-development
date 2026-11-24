package com.dynatech2012.kamleonuserapp.views

import android.content.Intent
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.application.AppTheme
import coil.ImageLoader
import coil.decode.ImageDecoderDecoder
import coil.load
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/** What the user did with a [KamleonSheet]. */
enum class SheetResult { PRIMARY, SECONDARY, TERTIARY, DISMISSED }

/**
 * Content of a [KamleonSheet]. [message] and [title] keep bold and underline markup. With
 * [destructive] the primary action is red. A sheet that is not [dismissible] can only be left
 * through its buttons. [secondary] and [tertiary] are text links under the primary button. [linkUrl], when set, opens that URL when the message is tapped.
 * [image], when set, is an illustration (an animated GIF plays) shown above the title on a dark card.
 */
data class SheetSpec(
    val message: CharSequence,
    val primary: String,
    val title: CharSequence? = null,
    val secondary: CharSequence? = null,
    val tertiary: CharSequence? = null,
    val destructive: Boolean = false,
    val dismissible: Boolean = true,
    val linkUrl: String? = null,
    @DrawableRes val image: Int? = null
) {
    internal fun toBundle(requestKey: String) = Bundle().apply {
        putString(ARG_KEY, requestKey)
        putCharSequence(ARG_MESSAGE, message)
        putString(ARG_PRIMARY, primary)
        putCharSequence(ARG_TITLE, title)
        putCharSequence(ARG_SECONDARY, secondary)
        putCharSequence(ARG_TERTIARY, tertiary)
        putBoolean(ARG_DESTRUCTIVE, destructive)
        putBoolean(ARG_DISMISSIBLE, dismissible)
        putString(ARG_LINK, linkUrl)
        image?.let { putInt(ARG_IMAGE, it) }
    }

    internal companion object {
        const val ARG_KEY = "sheet_key"
        private const val ARG_MESSAGE = "sheet_message"
        private const val ARG_PRIMARY = "sheet_primary"
        private const val ARG_TITLE = "sheet_title"
        private const val ARG_SECONDARY = "sheet_secondary"
        private const val ARG_TERTIARY = "sheet_tertiary"
        private const val ARG_DESTRUCTIVE = "sheet_destructive"
        private const val ARG_DISMISSIBLE = "sheet_dismissible"
        private const val ARG_LINK = "sheet_link"
        private const val ARG_IMAGE = "sheet_image"

        fun fromBundle(b: Bundle) = SheetSpec(
            message = b.getCharSequence(ARG_MESSAGE) ?: "",
            primary = b.getString(ARG_PRIMARY).orEmpty(),
            title = b.getCharSequence(ARG_TITLE),
            secondary = b.getCharSequence(ARG_SECONDARY),
            tertiary = b.getCharSequence(ARG_TERTIARY),
            destructive = b.getBoolean(ARG_DESTRUCTIVE),
            dismissible = b.getBoolean(ARG_DISMISSIBLE, true),
            linkUrl = b.getString(ARG_LINK),
            image = if (b.containsKey(ARG_IMAGE)) b.getInt(ARG_IMAGE) else null
        )
    }
}

/** Bold and underline spans of a resource string, as Compose styles. */
internal fun CharSequence.toAnnotatedMarkup(): AnnotatedString = buildAnnotatedString {
    append(this@toAnnotatedMarkup.toString())
    val text = this@toAnnotatedMarkup
    if (text is Spanned) {
        text.getSpans(0, text.length, Any::class.java).forEach { span ->
            val start = text.getSpanStart(span)
            val end = text.getSpanEnd(span)
            when {
                span is StyleSpan && span.style == Typeface.BOLD ->
                    addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
                span is UnderlineSpan ->
                    addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)
            }
        }
    }
}

private fun CharSequence.hasUnderline() =
    this is Spanned && getSpans(0, length, UnderlineSpan::class.java).isNotEmpty()

/**
 * The sheet's look: white surface with rounded top corners, optional title, a scrollable message
 * (large fonts never push the buttons off screen), the primary button and an optional text-link
 * secondary action. The drag handle only shows when the sheet can be dismissed.
 */
@Composable
fun KamleonSheetContent(spec: SheetSpec, onAction: (SheetResult) -> Unit) {
    val context = LocalContext.current
    val darkGrey = colorResource(R.color.kamleon_dark_grey)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(R.color.white), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(top = 12.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (spec.dismissible) {
            Box(
                Modifier
                    .size(width = 36.dp, height = 4.dp)
                    .background(colorResource(R.color.kamleon_secondary_grey_40), RoundedCornerShape(2.dp))
                    .testTag("sheet_handle")
            )
            Spacer(Modifier.height(16.dp))
        } else {
            Spacer(Modifier.height(8.dp))
        }
        spec.image?.let { image ->
            SheetIllustration(image)
            Spacer(Modifier.height(20.dp))
        }
        spec.title?.let { title ->
            val markup = remember(title) { title.toAnnotatedMarkup() }
            Text(
                text = markup,
                style = kamleonTitle(),
                color = darkGrey,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).testTag("sheet_title")
            )
        }
        val message = remember(spec.message) { spec.message.toAnnotatedMarkup() }
        // A sheet with only a title has no message block, so no blank line is left over.
        if (message.isNotEmpty()) {
            // The sheet can be dragged, so the scrolling message must hand its gestures to the sheet's
            // behaviour through the nested-scroll interop instead of being intercepted by it.
            Column(
                Modifier
                    .weight(1f, fill = false)
                    .nestedScroll(rememberNestedScrollInteropConnection())
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = message,
                    style = kamleonBody(),
                    color = darkGrey,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (spec.linkUrl != null) {
                                Modifier.clickable {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, spec.linkUrl.toUri()))
                                }
                            } else Modifier
                        )
                        .testTag("sheet_message")
                )
            }
            Spacer(Modifier.height(24.dp))
        }
        KamleonRoundButton(
            text = spec.primary,
            onClick = { onAction(SheetResult.PRIMARY) },
            enabled = true,
            destructive = spec.destructive,
            testTag = "sheet_primary"
        )
        spec.secondary?.let { secondary ->
            KamleonTextLink(
                text = secondary.toString(),
                underline = secondary.hasUnderline(),
                onClick = { onAction(SheetResult.SECONDARY) },
                style = kamleonBodyBold(),
                color = colorResource(R.color.kamleon_blue),
                pressedColor = colorResource(R.color.kamleon_blue_pressed),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                modifier = Modifier.padding(top = 8.dp).testTag("sheet_secondary")
            )
        }
        spec.tertiary?.let { tertiary ->
            KamleonTextLink(
                text = tertiary.toString(),
                underline = tertiary.hasUnderline(),
                onClick = { onAction(SheetResult.TERTIARY) },
                style = kamleonBodyBold(),
                color = colorResource(R.color.kamleon_blue),
                pressedColor = colorResource(R.color.kamleon_blue_pressed),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                modifier = Modifier.testTag("sheet_tertiary")
            )
        }
    }
}

/**
 * A sheet's illustration: the drawable (GIFs animate) filling the width of a dark rounded card,
 * cropped to its top [ILLUSTRATION_CROP] so tall artwork does not push the buttons down.
 */
@Composable
private fun SheetIllustration(@DrawableRes image: Int) {
    val context = LocalContext.current
    val loader = remember(context) {
        ImageLoader.Builder(context).components { add(ImageDecoderDecoder.Factory()) }.build()
    }
    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colorResource(R.color.kamleon_dark_grey))
            .padding(top = 12.dp)
    ) {
        AndroidView(
            factory = { ctx ->
                ImageView(ctx).apply {
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    scaleType = ImageView.ScaleType.MATRIX
                    // Fill the width and keep the top: the drawing starts at the top of the artwork.
                    addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
                        val view = v as ImageView
                        val width = view.drawable?.intrinsicWidth ?: return@addOnLayoutChangeListener
                        if (width <= 0) return@addOnLayoutChangeListener
                        val scale = view.width.toFloat() / width
                        view.imageMatrix = Matrix().apply { setScale(scale, scale) }
                    }
                    load(image, loader) { listener(onSuccess = { _, _ -> requestLayout() }) }
                }
            },
            modifier = Modifier.width(ILLUSTRATION_WIDTH).aspectRatio(ILLUSTRATION_CROP)
        )
    }
}

private val ILLUSTRATION_WIDTH = 190.dp
/** Width over height of the part of the artwork that is shown (the scanner animation: 370 x 520 px). */
private const val ILLUSTRATION_CROP = 370f / 520f

/**
 * A modal bottom sheet that replaces the old dialogs. It reports what the user did through the
 * Fragment Result API (see [onKamleonSheetResult]) exactly once, then closes itself. A sheet that
 * is not dismissible has no handle, cannot be dragged and ignores Back and outside touches.
 */
class KamleonSheet : BottomSheetDialogFragment() {
    private var delivered = false
    private val spec: SheetSpec by lazy { SheetSpec.fromBundle(requireArguments()) }
    private val requestKey: String get() = requireArguments().getString(SheetSpec.ARG_KEY).orEmpty()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        delivered = savedInstanceState?.getBoolean(STATE_DELIVERED) ?: false
        isCancelable = spec.dismissible
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_DELIVERED, delivered)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { AppTheme(useDarkTheme = false) { KamleonSheetContent(spec, ::deliver) } }
    }

    override fun onStart() {
        super.onStart()
        val sheetDialog = dialog as? BottomSheetDialog ?: return
        // The content draws its own rounded surface.
        sheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.setBackgroundColor(Color.TRANSPARENT)
        // Always fully expanded: the collapsed peek height can be shorter than the content, which
        // would leave the buttons below the screen edge until the user drags the sheet up.
        sheetDialog.behavior.apply {
            skipCollapsed = true
            state = BottomSheetBehavior.STATE_EXPANDED
            if (!spec.dismissible) isDraggable = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tag?.let { PendingSheets.release(parentFragmentManager, it) }
    }

    override fun onCancel(dialog: android.content.DialogInterface) {
        super.onCancel(dialog)
        report(SheetResult.DISMISSED)
    }

    private fun deliver(result: SheetResult) {
        if (report(result)) dismissAllowingStateLoss()
    }

    /** Sends [result] unless one was already sent; returns whether it did. */
    private fun report(result: SheetResult): Boolean {
        if (delivered) return false
        delivered = true
        parentFragmentManager.setFragmentResult(requestKey, bundleOf(RESULT to result.name))
        return true
    }

    internal companion object {
        const val RESULT = "sheet_result"
        private const val STATE_DELIVERED = "sheet_delivered"
    }
}

/** Tags of sheets that were requested and have not been destroyed yet, per manager (main thread only). */
private object PendingSheets {
    private val tags = java.util.WeakHashMap<FragmentManager, MutableSet<String>>()

    fun claim(fm: FragmentManager, tag: String) = tags.getOrPut(fm) { mutableSetOf() }.add(tag)

    fun release(fm: FragmentManager, tag: String) {
        tags[fm]?.remove(tag)
    }
}

/** Shows a [KamleonSheet]; a sheet with the same [requestKey] already on screen is kept. */
fun FragmentManager.showKamleonSheet(requestKey: String, spec: SheetSpec) {
    val tag = "kamleon_sheet_$requestKey"
    // findFragmentByTag misses a sheet whose transaction has not run yet, so those are tracked too.
    if (findFragmentByTag(tag) != null || !PendingSheets.claim(this, tag)) return
    val sheet = KamleonSheet().apply { arguments = spec.toBundle(requestKey) }
    // Allow state loss: sheets are shown from LiveData callbacks that can arrive after onStop.
    beginTransaction().add(sheet, tag).commitAllowingStateLoss()
}

/** Closes the sheet shown with [requestKey] from code, if it is on screen. No result is delivered. */
fun FragmentManager.dismissKamleonSheet(requestKey: String) {
    (findFragmentByTag("kamleon_sheet_$requestKey") as? KamleonSheet)?.dismissAllowingStateLoss()
}

private fun Bundle.toSheetResult() =
    SheetResult.valueOf(getString(KamleonSheet.RESULT) ?: SheetResult.DISMISSED.name)

/**
 * Listens for the result of sheets shown with `childFragmentManager.showKamleonSheet(requestKey, …)`.
 * Call it from `onViewCreated`.
 */
fun Fragment.onKamleonSheetResult(requestKey: String, onResult: (SheetResult) -> Unit) {
    childFragmentManager.setFragmentResultListener(requestKey, viewLifecycleOwner) { _, bundle ->
        onResult(bundle.toSheetResult())
    }
}

/**
 * Listens for the result of sheets shown with `supportFragmentManager.showKamleonSheet(requestKey, …)`.
 * Call it from `onCreate`.
 */
fun FragmentActivity.onKamleonSheetResult(requestKey: String, onResult: (SheetResult) -> Unit) {
    supportFragmentManager.setFragmentResultListener(requestKey, this) { _, bundle ->
        onResult(bundle.toSheetResult())
    }
}
