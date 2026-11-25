package com.dynatech2012.kamleonuserapp.fragments

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.constants.Constants
import com.dynatech2012.kamleonuserapp.databinding.FragmentImagePickBinding
import com.dynatech2012.kamleonuserapp.models.CustomUser
import com.dynatech2012.kamleonuserapp.models.observeEvent
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel
import com.dynatech2012.kamleonuserapp.views.showActionFailedSheet
import com.dynatech2012.kamleonuserapp.views.showProfileImage

/**
 * The light sheet to change the profile photo: choose one from the library, take one, or remove
 * the current one (only offered when there is one). While a new photo uploads, a ring turns around
 * the preview and the options are disabled; the sheet closes itself when the upload ends.
 */
class ImagePickFragment : BottomSheetDialogFragment() {

    var dismissListener: BottomFragmentDismissListener? = null
    private val viewModel: MainViewModel by activityViewModels()
    private var _binding: FragmentImagePickBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentImagePickBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.layoutLibrary.setOnClickListener {
            activity?.supportFragmentManager?.setFragmentResult(
                Constants.PICK_IMAGE, Bundle().apply { putBoolean(Constants.PICK_IMAGE_BUNDLE, true) }
            )
        }
        binding.layoutCamera.setOnClickListener {
            activity?.supportFragmentManager?.setFragmentResult(
                Constants.TAKE_IMAGE, Bundle().apply { putBoolean(Constants.TAKE_IMAGE_BUNDLE, true) }
            )
        }
        binding.layoutRemove.setOnClickListener { viewModel.removeUserImage() }

        viewModel.userData.observe(viewLifecycleOwner, this::onUserDataChanged)
        viewModel.userImageUrl.observe(viewLifecycleOwner, this::onUserImageChanged)
        viewModel.userImagePrevUri.observe(viewLifecycleOwner, this::onProfileImagePrevUriChanged)
        viewModel.userImageUploading.observe(viewLifecycleOwner, this::onUploadingChanged)
        viewModel.userImageFailed.observeEvent(viewLifecycleOwner) {
            showActionFailedSheet(it.failure)
        }
        viewModel.userImageUri.observeEvent(viewLifecycleOwner) {
            dismissListener?.onDismissFragment()
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        val sheetDialog = dialog as? BottomSheetDialog ?: return
        // The layout draws its own rounded surface.
        sheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.setBackgroundColor(Color.TRANSPARENT)
        sheetDialog.behavior.apply {
            skipCollapsed = true
            state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun onUserDataChanged(userData: CustomUser) {
        binding.tvSettingsPickProfileName.text = getString(
            R.string.user_name_initials,
            userData.name.take(1).uppercase(),
            userData.lastName.take(1).uppercase()
        )
    }

    private fun onUserImageChanged(url: String?) {
        binding.ivSettingsPickProfileImage.showProfileImage(url, viewModel.imageLoader)
        // Nothing to remove without a photo.
        binding.layoutRemove.visibility = if (url.isNullOrBlank()) View.GONE else View.VISIBLE
    }

    private fun onUploadingChanged(uploading: Boolean) {
        binding.piSettingsPickUpload.visibility = if (uploading) View.VISIBLE else View.GONE
        listOf(binding.layoutLibrary, binding.layoutCamera, binding.layoutRemove).forEach {
            it.isEnabled = !uploading
            it.alpha = if (uploading) DISABLED_ALPHA else 1f
        }
    }

    private fun onProfileImagePrevUriChanged(uri: Uri?) {
        if (uri == null)
            // No picked photo (none yet, removed, or the upload failed): show the saved avatar, if any.
            binding.ivSettingsPickProfileImage.showProfileImage(viewModel.userImageUrl.value, viewModel.imageLoader)
        else {
            binding.ivSettingsPickProfileImage.visibility = View.VISIBLE
            binding.ivSettingsPickProfileImage.setImageURI(uri)
        }
    }

    companion object {
        private const val DISABLED_ALPHA = 0.4f

        @JvmStatic
        fun newInstance(listener: BottomFragmentDismissListener) =
            ImagePickFragment().apply {
                dismissListener = listener
            }

        val TAG: String = ImagePickFragment::class.java.simpleName
    }
}
