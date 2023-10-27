package com.kamleonapp.activities

import android.content.Intent
import android.view.View
import com.kamleonapp.R
import com.kamleonapp.base.BaseActivity
import com.kamleonapp.databinding.ActivityAnalyticBinding
import com.kamleonapp.fragments.BottomFragmentDismissListener
import com.kamleonapp.fragments.ScanIntroFragment


class AnalyticActivity : BaseActivity<ActivityAnalyticBinding>() {
    override fun setBinding(): ActivityAnalyticBinding = ActivityAnalyticBinding.inflate(layoutInflater)

    private val onDismissScanIntro = object : BottomFragmentDismissListener {
        override fun onDismissFragment() {
            selectTab(2)
        }
    }

    override fun initView() {
        binding.layoutTabAnalytic.setOnClickListener {
            selectTab(1)
        }
        binding.layoutTabHome.setOnClickListener {
            selectTab(0)
        }
        binding.layoutTabQr.setOnClickListener {
            showQRIntroFragment()
        }

        selectTab(1)
    }

    override fun initEvent() {
        binding.listItemHydration.isClickable = true
        binding.listItemHydration.setOnClickListener {
            openGraphView(0)
        }
        binding.listItemElectrolyte.isClickable = true
        binding.listItemElectrolyte.setOnClickListener {
            openGraphView(1)
        }
        binding.listItemVolume.isClickable = true
        binding.listItemVolume.setOnClickListener {
            openGraphView(2)
        }
    }

    private fun openGraphView(type: Int) {
        val toGraph = Intent(this, MainActivity::class.java)
        toGraph.putExtra(MainActivity.EXTRA_KEY_TYPE, type)
        startActivity(toGraph)
    }

    private fun selectTab(tabIndex: Int) {
        binding.layoutContentAnalytics.visibility = View.GONE
        binding.layoutContentQR.visibility = View.GONE

        if (tabIndex == 0) {
            binding.layoutTabHome.background = getDrawable(R.drawable.bg_tab_button_shadow)
//            binding.layoutTabHome.setBackgroundResource(R.drawable.bg_setting_option_shadow)
            binding.layoutTabAnalytic.background = null
            binding.layoutTabQr.background = null
        } else if (tabIndex == 1) {
            binding.layoutTabAnalytic.setBackgroundResource(R.drawable.bg_tab_button_shadow)
            binding.layoutTabHome.background = null
            binding.layoutTabQr.background = null

            binding.layoutContentAnalytics.visibility = View.VISIBLE
        } else {
            binding.layoutTabQr.setBackgroundResource(R.drawable.bg_tab_button_shadow)
            binding.layoutTabAnalytic.background = null
            binding.layoutTabHome.background = null

            binding.layoutContentQR.visibility = View.VISIBLE
        }
    }

    private fun showQRIntroFragment() {
        val qrFragment = ScanIntroFragment.newInstance(onDismissScanIntro)
        qrFragment.show(supportFragmentManager, "QR")
    }
}