package com.dynatech2012.kamleonuserapp.fragments

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel
import com.ozcanalasalvar.datepicker.view.datapicker.DataPicker

class DataPickerFragment : BottomSheetDialogFragment() {
    private val viewModel: MainViewModel by activityViewModels()
    var dismissListener: BottomFragmentDismissListener? = null
    var dataValue: String? = null
    var dataType: String = ""
    var bottomSheetBehavior: BottomSheetBehavior<View>? = null
    private lateinit var pickerV: DataPicker

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_data_picker, container, false)
        setupDataPicker(v)

        v.findViewById<TextView>(R.id.tvSave).setOnClickListener {
            onSave()
        }

        initObservers()

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
        Log.d("DataPickerFragment", "helooo pre set up data picker")
        val pickerView = v.findViewById<DataPicker>(R.id.dataPicker)
        pickerV = pickerView
        val  aryRet = ArrayList<String>()
        if (dataType == "height") {
            for (height in 160 .. 190) {
                aryRet.add("$height")
            }
            pickerView.setValueUnit("cm")
            pickerView.setValueWidth(70)
            Log.d("DataPickerFragment", "helooo pre $dataValue")
            pickerView.setShowDecimal(true)
            dataValue?.let { pickerView.setValue(it) }
        } else if (dataType == "weight") {
            for (weight in 45 .. 120) {
                aryRet.add("$weight")
            }
            pickerView.setValueUnit("kg")
            pickerView.setValueWidth(50)
            pickerView.setShowDecimal(true)
            dataValue?.let { pickerView.setValue(it) }
        } else if (dataType == "gender") {
            val arrRes = resources.getStringArray(R.array.onboard_gender_values)
            aryRet.addAll(arrRes)
            dataValue?.let { pickerView.setValue(it) }
        }

        pickerView.setValues(aryRet)
    }

    override fun onCancel(dialog: DialogInterface) {
        Log.e("BottomSheet", "onCancel")
        super.onCancel(dialog)
        dismissListener?.onDismissFragment()
    }

    private fun onSave() {
        Log.d("BottomSheet", "onSave")
        when (dataType) {
            "height" -> {
                pickerV.getSelectedValue()?.let { newHeight ->
                    viewModel.changeHeight(newHeight)
                }
            }
            "weight" -> {
                pickerV.getSelectedValue()?.let { newWeight ->
                    viewModel.changeWeight(newWeight)
                }
            }
            "gender" -> {
                pickerV.getSelectedValue()?.let { newGender ->
                    viewModel.changeGender(newGender)
                }
            }
        }
    }

    private fun initObservers() {
        viewModel.userUpdated.observe(this, this::onUserDataUpdated)

    }

    private fun onUserDataUpdated(updated: Boolean) {
        if (updated)
        {
            dismiss()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(listener: BottomFragmentDismissListener, type: String, value: String?) =
            DataPickerFragment().apply {
                dismissListener = listener
                dataValue = value
                dataType = type
            }
        val TAG = DataPickerFragment::class.simpleName
    }
}