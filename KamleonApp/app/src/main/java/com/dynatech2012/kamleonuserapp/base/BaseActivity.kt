package com.dynatech2012.kamleonuserapp.base

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnLayout
import androidx.fragment.app.FragmentManager
import androidx.viewbinding.ViewBinding
import com.dynatech2012.kamleonuserapp.activities.MainActivity
import com.dynatech2012.kamleonuserapp.utils.DebugLog

abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {
    protected lateinit var binding: VB
    private val keyboard: InputMethodManager by lazy {
        getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        binding = setBinding()

        setContentView(binding.root)
        initView()
        initEvent()
    }

    private fun configureSystemBars() {
        // Transparent bars with dark icons: the activity background (and the screen behind it)
        // shows through, so there is no visual break between content and system bars.
        // Light-only for now; a dark theme will need SystemBarStyle.auto(...) later.
        val style = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        enableEdgeToEdge(
            statusBarStyle = style,
            navigationBarStyle = style,
        )
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
            DebugLog.e("BaseActivity") { "Ignore exception" }
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