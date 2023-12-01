package com.dynatech2012.kamleonuserapp.fragments

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.adapters.InvitationListAdapter
import com.dynatech2012.kamleonuserapp.databinding.FragmentNotificationBinding
import com.dynatech2012.kamleonuserapp.models.Invitation
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InvitationFragment : BottomSheetDialogFragment() {

    var dismissListener: BottomFragmentDismissListener? = null
    private val viewModel: MainViewModel by activityViewModels()
    lateinit var binding: FragmentNotificationBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        binding = FragmentNotificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener { setupBottomSheet(it) }
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setupAdapter()
        viewModel.getInvitations()
        return dialog
    }

    private fun initObservers() {
        viewModel.pendingInvitations.observe(this, this::onGetPendingInvitations)
        viewModel.recentInvitations.observe(this, this::onGetPendingInvitations)
        viewModel.oldInvitations.observe(this, this::onGetPendingInvitations)
    }

    private fun onGetPendingInvitations(it: List<Invitation>) {
        Log.d(TAG, "onGetInvitations: $it")
    }

    private fun setupAdapter() {
        val adapter = InvitationListAdapter(arrayListOf())
        binding.rvInvitationList.adapter = adapter
    }

    private fun setupBottomSheet(dialogInterface: DialogInterface) {
        val bottomSheetDialog = dialogInterface as BottomSheetDialog
        val bottomSheet = bottomSheetDialog.findViewById<View>(
            com.google.android.material.R.id.design_bottom_sheet)
            ?: return

        val displayMetrics = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getMetrics(displayMetrics)

        bottomSheet.minimumHeight = (displayMetrics.heightPixels * 0.35).toInt()
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)
    }

    override fun onCancel(dialog: DialogInterface) {
        Log.d(TAG, "onCancel")
        super.onCancel(dialog)
        dismissListener?.onDismissFragment()
    }

    companion object {
        @JvmStatic
        fun newInstance(listener: BottomFragmentDismissListener) =
            InvitationFragment().apply {
                dismissListener = listener
            }
        val TAG: String = InvitationFragment::class.java.simpleName
    }
}