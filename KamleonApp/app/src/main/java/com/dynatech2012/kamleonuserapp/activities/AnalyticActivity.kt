package com.dynatech2012.kamleonuserapp.activities

import android.content.Intent
import android.view.View
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.base.BaseActivity
import com.dynatech2012.kamleonuserapp.databinding.ActivityAnalyticBinding
//import com.dynatech2012.kamleonuserapp.databinding.ActivityAnalyticBinding
import com.dynatech2012.kamleonuserapp.fragments.BottomFragmentDismissListener
import com.dynatech2012.kamleonuserapp.fragments.ScanIntroFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AnalyticActivity : BaseActivity<ActivityAnalyticBinding>() {
    override fun setBinding(): ActivityAnalyticBinding = ActivityAnalyticBinding.inflate(layoutInflater)


    private val onDismissScanIntro = object : BottomFragmentDismissListener {
        override fun onDismissFragment() {
        }
    }

    override fun initView() {
        binding.layoutTabAnalytic.setOnClickListener {
            selectTab(1)
        }
        binding.layoutTabHome.setOnClickListener {
//            selectTab(0)
            startActivity(Intent(this, SettingActivity::class.java))
        }
        binding.layoutTabQr.setOnClickListener {
            selectTab(2)
            val thread: Thread = object : Thread() {
                override fun run() {
                    try {
                        sleep(400)
                    } catch (e: InterruptedException) {
                    }
                    runOnUiThread {
                        showQRIntroFragment()
                    }
                }
            }
            thread.start()
        }

        selectTab(1)
    }

    override fun initEvent() {
        /*
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
        */

        binding.btnNavProfile.isClickable = true
        binding.btnNavProfile.setOnClickListener {
            startActivity(Intent(this, SettingActivity::class.java))
        }
    }

    private fun openGraphView(type: Int) {
        val toGraph = Intent(this, GraphicsActivity::class.java)
        toGraph.putExtra(GraphicsActivity.EXTRA_KEY_TYPE, type)
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