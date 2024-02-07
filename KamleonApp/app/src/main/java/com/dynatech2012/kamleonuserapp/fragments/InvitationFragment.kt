package com.dynatech2012.kamleonuserapp.fragments

import android.app.Dialog
import android.content.DialogInterface
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.adapters.NewInvitationListAdapter
import com.dynatech2012.kamleonuserapp.adapters.OldInvitationListAdapter
import com.dynatech2012.kamleonuserapp.databinding.FragmentInvitationBinding
import com.dynatech2012.kamleonuserapp.extensions.px
import com.dynatech2012.kamleonuserapp.models.Invitation
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class InvitationFragment : BottomSheetDialogFragment() {

    var dismissListener: BottomFragmentDismissListener? = null
    private val viewModel: MainViewModel by activityViewModels()
    lateinit var binding: FragmentInvitationBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        binding = FragmentInvitationBinding.inflate(inflater, container, false)
        setupAdapter()
        initEvent()
        initObservers()
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener { setupBottomSheet(it) }
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return dialog
    }

    private fun initEvent() {
        binding.llSeeAll.setOnClickListener {
            binding.tvTimeRange.text = getString(R.string.noti_all_time)
            val adapter = binding.rvOldInvitationList.adapter as OldInvitationListAdapter
            //adapter.addDataSource(viewModel.oldInvitations.value as ArrayList<Invitation>)
            adapter.submitList(viewModel.oldInvitations.value as ArrayList<Invitation>)
            binding.llSeeAll.visibility = View.GONE
        }
    }

    private fun initObservers() {
        viewModel.pendingInvitations.observe(this, this::onGetPendingInvitations)
        viewModel.recentInvitations.observe(this, this::onGetRecentInvitations)
        viewModel.oldInvitations.observe(this, this::onGetOldInvitations)
        viewModel.recentInvitationModified.observe(this, this::onRecentInvitationModified)
    }

    private fun onRecentInvitationModified(isModified: Boolean?) {
        if (isModified == true) {
            viewModel.resetGettingInvitationsAfterModifyingOne()
            dismiss()
            dismissListener?.onDismissFragment()
        }
    }

    private fun onGetPendingInvitations(it: List<Invitation>) {
        val adapter = binding.rvNewInvitationList.adapter as NewInvitationListAdapter
        //adapter.setDataSource(it as ArrayList<Invitation>)
        Log.d(TAG, "HHH on get invitations pending: ${it.size}")
        adapter.submitList(it.toList())
        if (it.isNotEmpty())
            binding.vDivider.visibility = View.VISIBLE
    }

    private fun onGetRecentInvitations(it: List<Invitation>) {
        val adapter = binding.rvOldInvitationList.adapter as OldInvitationListAdapter
        adapter.submitList(it.toList())
        //adapter.setDataSource(it as ArrayList<Invitation>)
    }

    private fun onGetOldInvitations(it: List<Invitation>) {
        binding.llSeeAll.visibility = View.VISIBLE
        //val adapter = binding.rvOldInvitationList.adapter as OldInvitationListAdapter
        //adapter.setDataSource(it as ArrayList<Invitation>)
    }

    private fun setupAdapter() {
        val adapterNewInvitations = NewInvitationListAdapter()
        adapterNewInvitations.setNotificationListItemViewListener(object :
            NewInvitationListAdapter.NotificationListItemViewListener {
            override fun onClick(invitation: Invitation, accepted: Boolean) {
                Log.d(TAG, "onClick")
                if (accepted)
                    viewModel.acceptInvitation(invitation.id)
                else
                    viewModel.rejectInvitation(invitation.id)
                dismissListener?.onDismissFragment()
            }
        })
        binding.rvNewInvitationList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvNewInvitationList.adapter = adapterNewInvitations
        val adapterOldInvitations = OldInvitationListAdapter()
        binding.rvOldInvitationList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvOldInvitationList.adapter = adapterOldInvitations
    }

    private fun setupBottomSheet(dialogInterface: DialogInterface) {
        val bottomSheetDialog = dialogInterface as BottomSheetDialog
        val bottomSheet = bottomSheetDialog.findViewById<View>(
            com.google.android.material.R.id.design_bottom_sheet)
            ?: return

        bottomSheet.layoutParams.height = getScreenHeight - 70.px//ViewGroup.LayoutParams.MATCH_PARENT

        // Collapsed height
        val parent = view?.parent as View
        val params = parent.layoutParams as CoordinatorLayout.LayoutParams
        val behavior = params.behavior
        val bottomSheetBehavior = behavior as BottomSheetBehavior<*>?
        bottomSheetBehavior?.peekHeight = 360.px//view?.measuredHeight ?: 0
        //bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED

        // Collapsed height
        /*
        val displayMetrics = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getMetrics(displayMetrics)
        bottomSheet.minimumHeight = (displayMetrics.heightPixels * 0.85).toInt()//180
        //bottomSheet.minimumHeight = (getScreenHeight * 0.85).toInt()//180
        */

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

val getScreenHeight: Int = Resources.getSystem().displayMetrics.heightPixels
