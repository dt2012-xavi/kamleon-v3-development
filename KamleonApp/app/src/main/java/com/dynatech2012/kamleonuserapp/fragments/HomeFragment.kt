package com.dynatech2012.kamleonuserapp.fragments

import android.content.res.Resources.getSystem
import android.net.Uri
import android.util.Log
import android.view.View
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat.getDrawable
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import coil.load
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.adapters.HomeItemAdapter
import com.dynatech2012.kamleonuserapp.base.BaseFragment
import com.dynatech2012.kamleonuserapp.databinding.ActivityHomeBinding
import com.dynatech2012.kamleonuserapp.models.CustomUser
import com.dynatech2012.kamleonuserapp.database.MeasureData
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel
import com.dynatech2012.kamleonuserapp.views.cards.TipType
import com.dynatech2012.kamleonuserapp.views.cards.ViewPager
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Locale


@AndroidEntryPoint
class HomeFragment : BaseFragment<ActivityHomeBinding>() {
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var viewPagerCallback: ViewPager2.OnPageChangeCallback
    private lateinit var homeItemAdapter: HomeItemAdapter
    override fun setBinding(): ActivityHomeBinding = ActivityHomeBinding.inflate(layoutInflater)


    private val onDismissScanIntro = object : BottomFragmentDismissListener {
        override fun onDismissFragment() {
        }
    }

    override fun initView() {
        binding.layoutTabAnalytic.setOnClickListener {
            selectTab(1)
            findNavController().navigate(R.id.action_homeFragment_to_analyticFragment)
        }

        binding.layoutTabQr.setOnClickListener {
            selectTab(2)
            val thread: Thread = object : Thread() {
                override fun run() {
                    try {
                        sleep(400)
                    } catch (e: InterruptedException) {
                    }
                    activity?.runOnUiThread {
                        showQRIntroFragment()
                    }
                }
            }
            thread.start()
        }

        setupTips()

        selectTab(0)

        initObservers()
        viewModel.getUserData()
        Log.d(TAG, "got measures -2")
        viewModel.getUserMeasures()
    }

    override fun initEvent() {
        binding.btnNavProfile.isClickable = true
        binding.btnNavProfile.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_settingFragment)
        }
    }
    private fun initObservers() {
        viewModel.userImage.observe(this, this::onProfileImageUriChanged)
        viewModel.userData.observe(this, this::onUserDataChanged)
        viewModel.lastMeasure.observe(this, this::onLastMeasureChanged)
    }

    private fun selectTab(tabIndex: Int) {
        //binding.layoutContentAnalytics.visibility = View.GONE
        binding.layoutContentQR.visibility = View.GONE

        if (tabIndex == 0) {
            binding.layoutTabHome.background = getDrawable(requireContext(), R.drawable.bg_tab_button_shadow)
//            binding.layoutTabHome.setBackgroundResource(R.drawable.bg_setting_option_shadow)
            binding.layoutTabAnalytic.background = null
            binding.layoutTabQr.background = null
        } else if (tabIndex == 1) {
            binding.layoutTabAnalytic.setBackgroundResource(R.drawable.bg_tab_button_shadow)
            binding.layoutTabHome.background = null
            binding.layoutTabQr.background = null

            //binding.layoutContentHome.visibility = View.VISIBLE
        } else {
            binding.layoutTabQr.setBackgroundResource(R.drawable.bg_tab_button_shadow)
            binding.layoutTabAnalytic.background = null
            binding.layoutTabHome.background = null

            binding.layoutContentQR.visibility = View.VISIBLE
        }
    }

    private fun onUserDataChanged(userData: CustomUser) {
        if (userData.imageUrl.isNotBlank())
            binding.btnNavProfile.load(userData.imageUrl) {
                placeholder(R.drawable.image_profile)
            }
    }

    private fun onProfileImageUriChanged(uri: Uri?) {
        Log.d(SettingFragment.TAG, "Got image profile")
        binding.btnNavProfile.setImageURI(uri)
    }

    private fun onLastMeasureChanged(measure: MeasureData) {
        Log.d(TAG, "got measures 6 changed")
        binding.lastMeasure.text = measure.score.toString()
        binding.homeProgressBar.progress = measure.score
        val date = measure.analysisDate
        val format = SimpleDateFormat("d MMMM yyy", Locale.getDefault())
        val dateString = format.format(date)
        binding.tvHomeLastSample.text = getString(R.string.last_sample_message, dateString)
        when (measure.hydrationLevel) {
            MeasureData.HydrationLevel.VERYHYDRATED -> {
                binding.homeBg.setImageResource(R.drawable.bg_very_hydrated)
                binding.tvHomeMessage.text = getString(R.string.very_hydrated)
            }
            MeasureData.HydrationLevel.HYDRATED -> {
                binding.homeBg.setImageResource(R.drawable.bg_hydrated)
                binding.tvHomeMessage.text = getString(R.string.hydrated)
            }
            MeasureData.HydrationLevel.DEHYDRATED -> {
                binding.homeBg.setImageResource(R.drawable.bg_dehydrated)
                binding.tvHomeMessage.text = getString(R.string.dehydrated)
            }
            MeasureData.HydrationLevel.VERYDEHYDRATED -> {
                binding.homeBg.setImageResource(R.drawable.bg_severely_dehydrated)
                binding.tvHomeMessage.text = getString(R.string.severely_dehydrated)
            }
        }
    }


    private fun setupTips() {
        /*
        homeItemAdapter = HomeItemAdapter(this)
        homeItemAdapter.addFragment(HomeItemFragment.newInstance(0))
        homeItemAdapter.addFragment(HomeItemFragment.newInstance(1))
        val viewPager = binding.viewPager
        viewPager.clipToPadding = false
        viewPager.clipChildren = false
        viewPager.offscreenPageLimit = 2
        val offsetPx = 48.px
        val pageMarginPx = 8.px
        val marginTransformer = MarginPageTransformer(pageMarginPx)
        viewPager.setPageTransformer(marginTransformer)
        viewPager.setPadding(offsetPx, 0, offsetPx, 0)
        viewPager.adapter = homeItemAdapter
        */

        /*
        val rv = binding.rvHome
        val item1 = HomeListAdapter.HomeListItemModel("title 1", "Your hydration level is good")
        val item2 = HomeListAdapter.HomeListItemModel("title 2", "Your hydration level is good")
        val layoutManager = LinearLayoutManager(requireContext())
        layoutManager.orientation = RecyclerView.HORIZONTAL
        val adapter = HomeListAdapter(arrayListOf(item1, item2))
        rv.adapter = adapter
        rv.layoutManager = layoutManager
        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(rv)
        rv.adapter = adapter
        */

        binding.cvHome.setContent {
            ViewPager(tipType = TipType.HOME, modifier = Modifier) {
                findNavController().navigate(R.id.action_homeFragment_to_analyticFragment)
            }
        }
    }

    private fun showQRIntroFragment() {
        val qrFragment = ScanIntroFragment.newInstance(onDismissScanIntro)
        qrFragment.show(parentFragmentManager, "QR")
    }

    override fun onDestroy() {
        //viewPager.unregisterOnPageChangeCallback(viewPagerCallback)
        super.onDestroy()
    }

    companion object {
        val TAG: String = HomeFragment::class.java.simpleName
    }
}

val Int.dp: Int get() = (this / getSystem().displayMetrics.density).toInt()
val Int.px: Int get() = (this * getSystem().displayMetrics.density).toInt()