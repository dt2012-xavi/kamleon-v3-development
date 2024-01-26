package com.dynatech2012.kamleonuserapp.fragments

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.load
import coil.size.Scale
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.databinding.FragmentScanIntroBinding
import com.dynatech2012.kamleonuserapp.extensions.px
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar

class ScanIntroFragment : BottomSheetDialogFragment() {

    var dismissListener: BottomFragmentDismissListener? = null
    lateinit var binding: FragmentScanIntroBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentScanIntroBinding.inflate(inflater, container, false)
        // show snackbar
        val imageLoader = ImageLoader.Builder(binding.root.context)
            .components {
                if (SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
        binding.ivAnimationQr.load(R.drawable.animation_qr, imageLoader = imageLoader) {
            scale(Scale.FIT)

        }

        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener { setupBottomSheet(it) }
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return dialog
    }

    private fun setupBottomSheet(dialogInterface: DialogInterface) {
        val bottomSheetDialog = dialogInterface as BottomSheetDialog
        val bottomSheet = bottomSheetDialog.findViewById<View>(
            com.google.android.material.R.id.design_bottom_sheet)
            ?: return
        //bottomSheet.layoutParams.height = 300.px
        val parent = view?.parent as View
        val params = parent.layoutParams as CoordinatorLayout.LayoutParams
        val behavior = params.behavior
        val bottomSheetBehavior = behavior as BottomSheetBehavior<*>?
        //
        bottomSheetBehavior?.peekHeight = 300.px//view?.measuredHeight ?: 0
        //bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        //bottomSheetBehavior?.isDraggable = false
        //bottomSheetBehavior?.isFitToContents = true
        //bottomSheetBehavior?.expandedOffset = 380.px
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)
    }

    override fun onCancel(dialog: DialogInterface) {
        Log.e("BottomSheet", "onCancel")
        super.onCancel(dialog)
        dismissListener?.onDismissFragment()
    }

    companion object {
        @JvmStatic
        fun newInstance(listener: BottomFragmentDismissListener) =
            ScanIntroFragment().apply {
                dismissListener = listener
            }

        val TAG: String = ScanIntroFragment::class.java.simpleName
    }

}