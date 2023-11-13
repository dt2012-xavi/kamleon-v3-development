package com.dynatech2012.kamleonuserapp.base

import android.content.Context.INPUT_METHOD_SERVICE
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewbinding.ViewBinding

abstract class BaseFragment<VB : ViewBinding> : Fragment() {
    protected lateinit var binding: VB
    private val keyboard: InputMethodManager by lazy {
        activity?.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreate(savedInstanceState)
        binding = setBinding()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initEvent()
    }

    fun getSupportedFragManager() : FragmentManager? {
        return activity?.supportFragmentManager
    }

    abstract fun setBinding(): VB

    abstract fun initView()

    abstract fun initEvent()

    private fun hideSoftInput() {
        try {
            keyboard.hideSoftInputFromWindow(binding.root.windowToken, 0)
        } catch (e: Exception) {
            Log.e("BaseActivity","Ignore exception")
        }
    }
    fun hideKeyboardDismissFocus() {
        hideSoftInput()
        binding.root.clearFocus()
    }

    fun showSoftKeyboard(view: View) {
        if (view.requestFocus()) {
            val imm = activity?.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    fun openWebBrowser(with: String) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(with))
        startActivity(browserIntent)
    }
}