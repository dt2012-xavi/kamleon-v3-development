package com.dynatech2012.kamleonuserapp.fragments

import android.content.Intent
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.load
import coil.size.Scale
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.activities.InitActivity
import com.dynatech2012.kamleonuserapp.activities.MainActivity
import com.dynatech2012.kamleonuserapp.base.BaseFragment
import com.dynatech2012.kamleonuserapp.databinding.ActivityLoginBinding
import com.dynatech2012.kamleonuserapp.databinding.ActivitySplashBinding
import com.dynatech2012.kamleonuserapp.viewmodels.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SplashFragment : BaseFragment<ActivitySplashBinding>() {
    private val viewModel: AuthViewModel by activityViewModels()
    override fun setBinding(): ActivitySplashBinding = ActivitySplashBinding.inflate(layoutInflater)

    override fun initView() {
        Log.d(TAG, "init activity splash fragment")
        viewModel.resetLogged()
        viewModel.checkLogin()

        val imageLoader = ImageLoader.Builder(binding.root.context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
        binding.ivSplashGif.load(R.drawable.splash, imageLoader = imageLoader) {
            lifecycleScope.launch {
                delay(2000)
                if (viewModel.alreadyLogged) {
                    startActivity(Intent(requireContext(), MainActivity::class.java))
                } else {
                    findNavController().navigate(R.id.action_splashFragment_to_loginFragment)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "init activity splash on resume")
        if (viewModel.alreadySplash) {
            viewModel.resetLogged()
            viewModel.checkLogin()
            if (viewModel.alreadyLogged) {
                Log.d(TAG, "already logged")
                startActivity(Intent(requireContext(), MainActivity::class.java))
            } else {
                Log.d(TAG, "not logged, go to login")
                findNavController().navigate(R.id.action_splashFragment_to_loginFragment)
            }
        }
        viewModel.alreadySplash = true
    }

    override fun initEvent() { }

    companion object {
        val TAG: String = SplashFragment::class.java.simpleName
    }
}