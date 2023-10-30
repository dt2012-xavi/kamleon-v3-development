package com.kamleonapp.fragments

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kamleonapp.R

class ImagePickFragment : BottomSheetDialogFragment() {

    var dismissListener: BottomFragmentDismissListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_image_pick, container, false)

        v.findViewById<LinearLayout>(R.id.layoutLibrary).setOnClickListener {
            dismiss()
        }
        v.findViewById<LinearLayout>(R.id.layoutCamera).setOnClickListener {
            dismiss()
        }
        v.findViewById<LinearLayout>(R.id.layoutRemove).setOnClickListener {
            dismiss()
        }
        return v
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
            ImagePickFragment().apply {
                dismissListener = listener
            }
    }
}