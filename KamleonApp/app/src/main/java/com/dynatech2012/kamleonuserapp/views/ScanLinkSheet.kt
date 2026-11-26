package com.dynatech2012.kamleonuserapp.views

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.application.AppTheme
import com.dynatech2012.kamleonuserapp.network.scanFailureText
import com.dynatech2012.kamleonuserapp.viewmodels.QrViewModel
import com.dynatech2012.kamleonuserapp.viewmodels.ScanLinkState
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

/**
 * One sheet for a scan being linked, read by the in-app scanner or by the phone's own camera (an app
 * link): it says the scan is being linked, then, in place, how it went, or that the link can't be used
 * with a way to scan again. It follows the activity's [QrViewModel] and tells its [Host] how it closed.
 */
@AndroidEntryPoint
class ScanLinkSheet : BottomSheetDialogFragment() {

    /** Implemented by the activity that shows the sheet. */
    fun interface Host {
        /** The sheet closed on [state]; [result] is the button, or DISMISSED. */
        fun onScanLinkClosed(state: ScanLinkState, result: SheetResult)
    }

    private val qrViewModel: QrViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppTheme(useDarkTheme = false) {
                    val state = qrViewModel.linkState.observeAsState().value
                    // Nothing to say (e.g. restored after the process died): leave.
                    LaunchedEffect(state == null) { if (state == null) dismissAllowingStateLoss() }
                    SideEffect { isCancelable = state != ScanLinkState.Linking }
                    if (state != null) ScanLinkContent(state, ::onAction)
                }
            }
        }

    override fun onStart() {
        super.onStart()
        matchScreenStatusBar()
        val sheetDialog = dialog as? BottomSheetDialog ?: return
        // The content draws its own rounded surface and is always fully expanded, like KamleonSheet.
        sheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.setBackgroundColor(Color.TRANSPARENT)
        sheetDialog.behavior.apply {
            skipCollapsed = true
            state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onCancel(dialog: android.content.DialogInterface) {
        super.onCancel(dialog)
        val state = qrViewModel.linkState.value
        qrViewModel.clearLinkState()
        if (state != null) (activity as? Host)?.onScanLinkClosed(state, SheetResult.DISMISSED)
    }

    private fun onAction(state: ScanLinkState, result: SheetResult) {
        qrViewModel.clearLinkState()
        dismissAllowingStateLoss()
        (activity as? Host)?.onScanLinkClosed(state, result)
    }

    companion object {
        private const val TAG = "scan_link_sheet"

        /** Shows the sheet unless it is already on screen (it then just follows the new state). */
        fun show(fragmentManager: FragmentManager) {
            if (fragmentManager.findFragmentByTag(TAG) != null) return
            ScanLinkSheet().show(fragmentManager, TAG)
        }
    }
}

@Composable
internal fun ScanLinkContent(state: ScanLinkState, onAction: (ScanLinkState, SheetResult) -> Unit) {
    when (state) {
        ScanLinkState.Linking -> LinkingContent()
        ScanLinkState.Linked -> KamleonSheetContent(
            SheetSpec(
                title = stringResource(R.string.dialog_scan_scanned),
                message = stringResource(R.string.dialog_scan_scanned_message),
                primary = stringResource(R.string.dialog_ok)
            )
        ) { onAction(state, it) }
        is ScanLinkState.Failed -> {
            val (title, message) = scanFailureText(state.failure)
            KamleonSheetContent(
                SheetSpec(
                    title = stringResource(title),
                    message = stringResource(message),
                    primary = stringResource(R.string.dialog_ok)
                )
            ) { onAction(state, it) }
        }
        ScanLinkState.Invalid -> KamleonSheetContent(
            SheetSpec(
                title = stringResource(R.string.scan_invalid_title),
                message = stringResource(R.string.scan_invalid_message),
                primary = stringResource(R.string.scan_again),
                secondary = stringResource(R.string.notifications_intro_later)
            )
        ) { onAction(state, it) }
    }
}

/** The sheet while the scan is linked: its title over a spinner, no way out until it answers. */
@Composable
private fun LinkingContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(R.color.white), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(top = 28.dp, bottom = 40.dp)
            .testTag("scan_linking")
    ) {
        Text(
            text = stringResource(R.string.scan_linking),
            style = kamleonTitle(),
            color = colorResource(R.color.kamleon_dark_grey),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))
        CircularProgressIndicator(
            color = colorResource(R.color.kamleon_blue),
            modifier = Modifier.size(40.dp)
        )
    }
}
