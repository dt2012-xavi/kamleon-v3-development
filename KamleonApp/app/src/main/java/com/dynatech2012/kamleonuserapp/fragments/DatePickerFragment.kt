package com.dynatech2012.kamleonuserapp.fragments

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.repositories.FirestoreRepository
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel
import com.ozcanalasalvar.datepicker.view.datepicker.DatePicker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.GregorianCalendar

class DatePickerFragment : BottomSheetDialogFragment() {
    private val viewModel: MainViewModel by activityViewModels()
    var dismissListener: BottomFragmentDismissListener? = null
    private lateinit var pickerV: DatePicker
    var dateValue: Date = Date()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val v = inflater.inflate(R.layout.fragment_date_picker, container, false)
        setupDataPicker(v)

        v.findViewById<TextView>(R.id.tvSave).setOnClickListener {
            //dismiss()
            onSave()
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
            com.google.android.material.R.id.design_bottom_sheet
        )
            ?: return
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)
    }

    private fun setupDataPicker(v: View) {
        pickerV = v.findViewById(R.id.datePicker)
        pickerV.setDate(dateValue.time)
    }
    override fun onCancel(dialog: DialogInterface) {
        Log.e(TAG, "onCancel")
        super.onCancel(dialog)
        dismissListener?.onDismissFragment()
    }

    private fun onSave() {
        Log.d(TAG, "onSave")
        pickerV.getDateSelected().let { dateSelected ->
            val date = GregorianCalendar(dateSelected.year, dateSelected.month - 1, dateSelected.day).getTime()
            viewModel.changeBirth(date)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(listener: BottomFragmentDismissListener, value: Date) =
            DatePickerFragment().apply {
                dismissListener = listener
                dateValue = value
            }
        val TAG = DatePickerFragment::class.simpleName
    }
}