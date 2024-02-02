package com.dynatech2012.kamleonuserapp.fragments

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
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
import com.dynatech2012.kamleonuserapp.models.CustomUser
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel

class ImagePickFragment : BottomSheetDialogFragment() {

    var dismissListener: BottomFragmentDismissListener? = null
    private val viewModel: MainViewModel by activityViewModels()
    lateinit var binding: FragmentImagePickBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "image pick dialog oncreate view 1")
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
        /*
        viewModel.userData.value?.imageUrl?.let {
            if (it.isNotBlank())
                binding.ivSettingsPickProfileImage.load(it) {
                    placeholder(R.drawable.image_profile)
                }
        }
        */

        viewModel.userImageDrawable.observe(this, this::onUserImageChanged)
        viewModel.userImagePrevUri.observe(viewLifecycleOwner, this::onProfileImagePrevUriChanged)
        viewModel.userImageUri.observe(viewLifecycleOwner, this::onProfileImageUriChanged)
        viewModel.userData.observe(this, this::onUserDataChanged)
        return v
    }

    private fun onUserDataChanged(userData: CustomUser) {
        val initials = getString(R.string.user_name_initials, userData.name.substring(0, 1).uppercase(), userData.lastName.substring(0, 1).uppercase())
        binding.tvSettingsPickProfileName.text = initials
    }

    private fun onUserImageChanged(drawable: Drawable?) {
        Log.d(HomeFragment.TAG, "image changed")
        if (drawable == null) {
            binding.ivSettingsPickProfileImage.setImageDrawable(null)
            binding.ivSettingsPickProfileImage.visibility = View.INVISIBLE
            return
        }
        binding.ivSettingsPickProfileImage.load(drawable)
        binding.ivSettingsPickProfileImage.visibility = View.VISIBLE
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Log.d(TAG, "image pick dialog oncreate dialog 1")
        val dialog = super.onCreateDialog(savedInstanceState)
        //dialog.setOnShowListener { setupBottomSheet(it) }
        //dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        Log.d(TAG, "image pick dialog oncreate dialog 2")
        //isCancelable = true
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
        Log.e("BottomSheet", "image pick dialog onCancel")
        super.onCancel(dialog)
        //dismissListener?.onDismissFragment()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        Log.d(TAG, "image pick dialog onDismiss")
    }

    private fun onProfileImagePrevUriChanged(uri: Uri?) {
        Log.d(TAG, "Got image profile changed")
        if (uri == null)
            binding.ivSettingsPickProfileImage.visibility = View.INVISIBLE
        else {
            binding.ivSettingsPickProfileImage.visibility = View.VISIBLE
            binding.ivSettingsPickProfileImage.setImageURI(uri)
        }
    }
    private fun onProfileImageUriChanged(uri: Uri?) {
        Log.d(TAG, "Got image profile update")
        /*dialog?.setOnCancelListener {  }
        onCancel(dialog)*/
        dismissListener?.onDismissFragment()
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