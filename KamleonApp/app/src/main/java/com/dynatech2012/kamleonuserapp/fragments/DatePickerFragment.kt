package com.dynatech2012.kamleonuserapp.fragments

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.extensions.addYears
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.ozcanalasalvar.datepicker.view.datepicker.DateChangeListener
import com.ozcanalasalvar.datepicker.view.datepicker.DatePicker
import com.ozcanalasalvar.datepicker.view.datepicker.OnDatePickerTouchListener
import java.util.Date
import java.util.GregorianCalendar

class DatePickerFragment : BottomSheetDialogFragment() {
    private lateinit var behavior: BottomSheetBehavior<View>
    private val viewModel: MainViewModel by activityViewModels()
    var dismissListener: BottomFragmentDismissListener? = null
    private lateinit var dateChangeListener: DateChangeListener
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

        initObservers()

        return v
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        //val dialog = NonDraggableBottomSheetDialog(requireContext())
        dialog.setOnShowListener {

            setupBottomSheet(it)//, dialog)
        }
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return dialog
    }

    private fun setupBottomSheet(
        dialogInterface: DialogInterface//,
        //dialog: NonDraggableBottomSheetDialog
    ) {
        val bottomSheetDialog = dialogInterface as BottomSheetDialog
        val bottomSheet = bottomSheetDialog.findViewById<View>(
            com.google.android.material.R.id.design_bottom_sheet
        )
            ?: return
        behavior = BottomSheetBehavior.from(bottomSheet)
        behavior.isDraggable = false
        //behavior.state = BottomSheetBehavior.STATE_EXPANDED

        /*behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                    dialog.setAllowDragging(false)
                } else {
                    dialog.setAllowDragging(true)
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // Handle slide
            }
        })*/
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)
    }

    private fun setupDataPicker(v: View) {
        pickerV = v.findViewById(R.id.datePicker)
        pickerV.setDate(dateValue.time)
        pickerV.setMaxxDate(Date().addYears(-14).time)
        pickerV.setDateChangeListener(dateChangeListener)
        Log.d(TAG, "ACTIONN onDateChanged first date: $dateValue")
        pickerV.setDatePickerTouchListener(object : OnDatePickerTouchListener {
            override fun onDatePickerTouchDown() {
                behavior.isDraggable = false
                Log.d(TAG, "ACTIONN_DOWN draggable to false")
            }

            override fun onDatePickerTouchUp() {
                behavior.isDraggable = true
                Log.d(TAG, "ACTIONN_UP draggable to true")
            }

        })
    }
    override fun onCancel(dialog: DialogInterface) {
        Log.e(TAG, "onCancel")
        super.onCancel(dialog)
        dismissListener?.onDismissFragment()
    }

    private fun onSave() {
        Log.d(TAG, "onSave")
        pickerV.getDateSelected().let { dateSelected ->
            Log.d(TAG, "onSave date: year: ${dateSelected.year}, month: ${dateSelected.month}, day: ${dateSelected.day}")
            val date = GregorianCalendar(dateSelected.year, dateSelected.month, dateSelected.day).time
            Log.d(TAG, "onSave date: $date")
            viewModel.changeBirth(date)
        }
    }

    fun enableSaveButton(enable: Boolean) {
        view?.findViewById<TextView>(R.id.tvSave)?.isEnabled = enable
    }

    private fun initObservers() {
        viewModel.userUpdated.observe(this, this::onUserDateUpdated)
    }
    private fun onUserDateUpdated(updated: Boolean) {
        if (updated)
        {
            dismiss()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(dismissListener: BottomFragmentDismissListener, dateChangeListener: DateChangeListener, value: Date) =
            DatePickerFragment().apply {
                Log.d(TAG, "onDateChanged first date: $value")
                this.dismissListener = dismissListener
                this.dateChangeListener = dateChangeListener
                dateValue = value
            }
        val TAG = DatePickerFragment::class.simpleName
    }
}

class NonDraggableBottomSheetDialog(context: Context) : BottomSheetDialog(context) {
    private var allowDragging = false

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (!allowDragging) {
            true
        } else super.onTouchEvent(event)
    }

    fun setAllowDragging(allowDragging: Boolean) {
        this.allowDragging = allowDragging
    }
}