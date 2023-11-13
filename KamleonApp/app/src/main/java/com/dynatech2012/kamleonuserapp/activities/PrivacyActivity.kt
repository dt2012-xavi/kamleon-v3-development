package com.dynatech2012.kamleonuserapp.activities

import android.content.Intent
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.base.BaseActivity
import com.dynatech2012.kamleonuserapp.databinding.ActivityPrivacyBinding
import com.dynatech2012.kamleonuserapp.models.CustomUser
import com.dynatech2012.kamleonuserapp.viewmodels.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PrivacyActivity : BaseActivity<ActivityPrivacyBinding>() {
    private val viewModel: AuthViewModel by viewModels()
    override fun setBinding(): ActivityPrivacyBinding = ActivityPrivacyBinding.inflate(layoutInflater)

    private var acceptState = arrayOf(false, false, false)

    private fun privacyItemIcons(): Array<ImageView> { return arrayOf(binding.ivPrivacy1, binding.ivPrivacy2, binding.ivPrivacy3) }
    private fun privacyItemTextViews(): Array<TextView> { return arrayOf(binding.tvPrivacy1, binding.tvPrivacy2, binding.tvPrivacy3) }
    private fun privacyItemLayouts(): Array<LinearLayout> { return arrayOf(binding.privacyLayout1, binding.privacyLayout2, binding.privacyLayout3) }

    override fun initView() {
        val privacyItemStrRes = arrayOf(R.string.privacy_item1, R.string.privacy_item2, R.string.privacy_item3)
        for (itemIndex in 0 until privacyItemLayouts().size) {
            privacyItemIcons()[itemIndex].setImageResource(R.drawable.icn_circular_uncheck)

            privacyItemLayouts()[itemIndex].setOnClickListener {
                privacyItemViewClicked(itemIndex)
            }

            val spannableString = privacySpannableString(getString(privacyItemStrRes[itemIndex]))
            privacyItemTextViews()[itemIndex].setText(spannableString, TextView.BufferType.SPANNABLE)

            privacyItemTextViews()[itemIndex].movementMethod = LinkMovementMethod.getInstance()
        }
        getExtras()
        updatePrivacyItemViews()
        initObservers()
    }

    override fun initEvent() {
        binding.btnNavBack.setOnClickListener { finish() }
        binding.btnAcceptAll.setOnClickListener {
            acceptState = arrayOf(true, true, true)
            updatePrivacyItemViews()
        }
        binding.btnNext.setOnClickListener {
            Log.d(TAG, "Btn clicked")
            viewModel.signup()
        }
    }

    private fun getExtras()
    {
        viewModel.fName = intent.getStringExtra("fName")
        viewModel.lName = intent.getStringExtra("lName")
        viewModel.email = intent.getStringExtra("email")
        viewModel.pass = intent.getStringExtra("pass")
    }

    private fun privacySpannableString(textFull: String): SpannableString {
        val textTerms = getString(R.string.privacy_word_terms)
        val textPrivacy = getString(R.string.privacy_word_privacy)

        val spannableString = SpannableString(textFull)
        val clickableTerms: ClickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                openWebBrowser("https://termsuse.com")
            }

            override fun updateDrawState(ds: TextPaint) {
                ds.isUnderlineText = false
                ds.color = getColor(R.color.kamleon_blue)
            }
        }
        val clickablePolicy: ClickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                openWebBrowser("https://privacypolicy.com")
            }

            override fun updateDrawState(ds: TextPaint) {
                ds.isUnderlineText = false
                ds.color = getColor(R.color.kamleon_blue)
            }
        }


        val termsIndex = textFull.indexOf(textTerms)
        val privacyIndex = textFull.indexOf(textPrivacy)
        if (termsIndex >= 0) {
            spannableString.setSpan(clickableTerms, termsIndex, termsIndex + textTerms.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        if (privacyIndex >= 0) {
            spannableString.setSpan(clickablePolicy, privacyIndex, privacyIndex + textPrivacy.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        return spannableString
    }

    private fun privacyItemViewClicked(index: Int) {
        acceptState[index] = !acceptState[index]
        updatePrivacyItemViews()
    }

    private fun updatePrivacyItemViews() {
        for (index in 0 until privacyItemLayouts().size) {
            privacyItemIcons()[index].setImageResource(if (acceptState[index]) R.drawable.icn_circular_check else R.drawable.icn_circular_uncheck)
        }
        binding.btnAcceptAll.visibility = if (acceptState.contains(false)) View.VISIBLE else View.INVISIBLE
        binding.btnNext.isEnabled = (acceptState[0] && acceptState[1] && acceptState[2])
    }

    private fun initObservers() {
        viewModel.uiState.observe(this, this::startActivity)
    }

    private fun startActivity(state: Int) {
        Log.d(TAG, "Callback privacy")
        if (state == 2)
            startActivity(Intent(this, OnboardingActivity::class.java))
    }
    companion object {
        val TAG = PrivacyActivity::class.simpleName
    }
}