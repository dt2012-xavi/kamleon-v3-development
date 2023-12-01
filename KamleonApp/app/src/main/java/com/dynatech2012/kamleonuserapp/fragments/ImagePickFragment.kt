package com.dynatech2012.kamleonuserapp.fragments

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.activityViewModels
import coil.load
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.constants.Constants
import com.dynatech2012.kamleonuserapp.databinding.FragmentImagePickBinding
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel

class ImagePickFragment : BottomSheetDialogFragment() {

    var dismissListener: BottomFragmentDismissListener? = null
    private val viewModel: MainViewModel by activityViewModels()
    lateinit var binding: FragmentImagePickBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_image_pick, container, false)
        binding = FragmentImagePickBinding.bind(v)
        v.findViewById<LinearLayout>(R.id.layoutLibrary).setOnClickListener {
            activity?.supportFragmentManager?.setFragmentResult(Constants.PICK_IMAGE, Bundle().apply {
                putBoolean(Constants.PICK_IMAGE_BUNDLE, true)
            })

        }
        v.findViewById<LinearLayout>(R.id.layoutCamera).setOnClickListener {
            activity?.supportFragmentManager?.setFragmentResult(Constants.TAKE_IMAGE, Bundle().apply {
                putBoolean(Constants.TAKE_IMAGE_BUNDLE, true)
            })
        }
        v.findViewById<LinearLayout>(R.id.layoutRemove).setOnClickListener {
            viewModel.removeUserImage()
        }
        viewModel.userData.value?.imageUrl?.let {
            if (it.isNotBlank())
                binding.imageProfile.load(it) {
                    placeholder(R.drawable.image_profile)
                }
        }
        viewModel.userImageUri.observe(viewLifecycleOwner, this::onProfileImagePrevUriChanged)
        viewModel.userImageUri.observe(viewLifecycleOwner, this::onProfileImageUriChanged)
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

    private fun onProfileImagePrevUriChanged(uri: Uri?) {
        Log.d(TAG, "Got image profile changed")
        binding.imageProfile.setImageURI(uri)
    }
    private fun onProfileImageUriChanged(uri: Uri?) {
        Log.d(TAG, "Got image profile update")
        dismiss()
    }

    companion object {
        @JvmStatic
        fun newInstance(listener: BottomFragmentDismissListener) =
            ImagePickFragment().apply {
                dismissListener = listener
            }

        val TAG: String = ImagePickFragment::class.java.simpleName
    }
}