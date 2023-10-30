package com.kamleonapp.fragments

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kamleonapp.R
import com.kamleonapp.activities.OnboardingActivity
import com.ozcanalasalvar.datepicker.view.datapicker.DataPicker
import com.ozcanalasalvar.datepicker.view.datepicker.DatePicker

class DataPickerFragment : BottomSheetDialogFragment() {

    var dismissListener: BottomFragmentDismissListener? = null
    var dataType: String = ""
    var bottomSheetBehavior: BottomSheetBehavior<View>? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_data_picker, container, false)
        setupDataPicker(v)

        v.findViewById<TextView>(R.id.tvSave).setOnClickListener {
            dismiss()
        }
        return v
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener { setupBottomSheet(it) }
//        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        return dialog
    }

    private fun setupBottomSheet(dialogInterface: DialogInterface) {
        val bottomSheetDialog = dialogInterface as BottomSheetDialog
        val bottomSheet = bottomSheetDialog.findViewById<View>(
            com.google.android.material.R.id.design_bottom_sheet
        )
            ?: return

//        val contentLayout = v.findViewById<ConstraintLayout>(R.id.layoutContent)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior?.isHideable = true
        bottomSheetBehavior?.skipCollapsed = false

        bottomSheet.setBackgroundColor(Color.TRANSPARENT)
    }

    private fun setupDataPicker(v: View) {
        val pickerView = v.findViewById<DataPicker>(R.id.dataPicker)
        val  aryRet = ArrayList<String>()
        if (dataType == "height") {
            for (height in 150 .. 190) {
                aryRet.add("$height")
            }
            pickerView.setValueUnit("cm")
            pickerView.setValueWidth(75)
        } else if (dataType == "weight") {
            for (weight in 40 .. 120) {
                aryRet.add("$weight")
            }
            pickerView.setValueUnit("kg")
            pickerView.setValueWidth(75)
        } else if (dataType == "gender") {
            val arrRes = resources.getStringArray(R.array.onboard_gender_values)
            aryRet.addAll(arrRes)
        }

        pickerView.setValues(aryRet)
    }

    override fun onCancel(dialog: DialogInterface) {
        Log.e("BottomSheet", "onCancel")
        super.onCancel(dialog)
        dismissListener?.onDismissFragment()
    }

    companion object {
        @JvmStatic
        fun newInstance(listener: BottomFragmentDismissListener, type: String) =
            DataPickerFragment().apply {
                dismissListener = listener
                dataType = type
            }
    }
}