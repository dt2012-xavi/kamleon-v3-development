package com.dynatech2012.kamleonuserapp.fragments

import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import androidx.compose.ui.Modifier
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.NavHostFragment
import coil.load
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.base.BaseFragment
import com.dynatech2012.kamleonuserapp.database.MeasureData
import com.dynatech2012.kamleonuserapp.databinding.ActivityHomeBinding
import com.dynatech2012.kamleonuserapp.extensions.formatTime
import com.dynatech2012.kamleonuserapp.models.CustomUser
import com.dynatech2012.kamleonuserapp.models.RecommendationType
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel
import com.dynatech2012.kamleonuserapp.views.cards.ViewPager
import dagger.hilt.android.AndroidEntryPoint
import java.util.Date


@AndroidEntryPoint
class HomeFragment : BaseFragment<ActivityHomeBinding>() {
    private val viewModel: MainViewModel by activityViewModels()
    override fun setBinding(): ActivityHomeBinding = ActivityHomeBinding.inflate(layoutInflater)

    override fun initView() {
        setupTips(null)

        //selectTab(0)

        initObservers()
        viewModel.getUserData()
        Log.d(TAG, "got measures -2")
        viewModel.getUserMeasures()

    }

    override fun initEvent() {
        binding.ivHomeProfile.isClickable = true
        binding.ivHomeProfile.setOnClickListener {
            val navHostFragment = requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_fragment_main) as NavHostFragment
            val navController = navHostFragment.navController
            //Log.d(TAG, "frgmts ${navHostFragment.childFragmentManager.fragments[0]}")
            navController.navigate(R.id.action_tabFragment_to_settingFragment)
            //navController.popBackStack(R.id.tabFragment, true)
        }
    }
    private fun initObservers() {
        viewModel.userData.observe(this, this::onUserDataChanged)
        viewModel.userImageDrawable.observe(this, this::onUserImageChanged)
        viewModel.lastMeasure.observe(this, this::onLastMeasureChanged)
    }

    private fun onUserDataChanged(userData: CustomUser) {
        Log.d(SettingFragment.TAG, "on user data changed")
        val initials = getString(R.string.user_name_initials, userData.name.substring(0, 1).uppercase(), userData.lastName.substring(0, 1).uppercase())
        binding.tvHomeProfileName.text = initials
    }

    private fun onUserImageChanged(drawable: Drawable?) {
        Log.d(TAG, "image changed")
        drawable?.let {
            binding.ivHomeProfile.load(drawable)
            binding.ivHomeProfile.visibility = View.VISIBLE
            return
        }
    }

    private fun onLastMeasureChanged(measure: MeasureData) {
        Log.d(TAG, "got measures 6 changed")
        binding.lastMeasure.text = measure.score.toString()
        binding.homeProgressBar.progress = measure.score
        val date = Date(measure.analysisDate)
        //val format = SimpleDateFormat("d MMMM yyy", Locale.getDefault())
        //val dateString = format.format(date)
        val dateString = date.formatTime.uppercase()
        binding.tvHomeLastSample.text = getString(R.string.home_last_sample_message, dateString)
        when (measure.hydrationLevel) {
            MeasureData.HydrationLevel.VERYHYDRATED -> {
                binding.homeBg.setImageResource(R.drawable.bg_very_hydrated)
                binding.tvHomeMessage.text = getString(R.string.analytic_very_hydrated)
            }
            MeasureData.HydrationLevel.HYDRATED -> {
                binding.homeBg.setImageResource(R.drawable.bg_hydrated)
                binding.tvHomeMessage.text = getString(R.string.analytic_hydrated)
            }
            MeasureData.HydrationLevel.DEHYDRATED -> {
                binding.homeBg.setImageResource(R.drawable.bg_dehydrated)
                binding.tvHomeMessage.text = getString(R.string.analytic_dehydrated)
            }
            MeasureData.HydrationLevel.VERYDEHYDRATED -> {
                binding.homeBg.setImageResource(R.drawable.bg_severely_dehydrated)
                binding.tvHomeMessage.text = getString(R.string.analytic_severely_dehydrated)
            }
        }
        setupTips(measure.hydrationLevel)
    }


    private fun setupTips(hydrationLevel: MeasureData.HydrationLevel?) {
        binding.cvHome.setContent {
            ViewPager(recommendationType = RecommendationType.HOME, hydrationLevel = hydrationLevel, modifier = Modifier)
        }
    }

    companion object {
        val TAG: String = HomeFragment::class.java.simpleName
    }
}
