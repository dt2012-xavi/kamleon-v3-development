package com.dynatech2012.kamleonuserapp.base

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnLayout
import androidx.fragment.app.FragmentManager
import androidx.viewbinding.ViewBinding
import com.dynatech2012.kamleonuserapp.activities.MainActivity

abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {
    protected lateinit var binding: VB
    private val keyboard: InputMethodManager by lazy {
        getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = setBinding()

        setContentView(binding.root)
        initView()
        initEvent()
    }

    fun getSupportedFragManager() : FragmentManager? {
        return supportFragmentManager
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
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    fun openWebBrowser(with: String) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(with))
        startActivity(browserIntent)
    }
}