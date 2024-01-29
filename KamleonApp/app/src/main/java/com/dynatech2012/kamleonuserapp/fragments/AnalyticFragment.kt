package com.dynatech2012.kamleonuserapp.fragments

import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.NavHostFragment
import coil.load
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.base.BaseFragment
import com.dynatech2012.kamleonuserapp.database.MeasureData
import com.dynatech2012.kamleonuserapp.databinding.ActivityAnalyticBinding
import com.dynatech2012.kamleonuserapp.extensions.formatTime
import com.dynatech2012.kamleonuserapp.models.CustomUser
import com.dynatech2012.kamleonuserapp.models.MeasurePrecision
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Date

@AndroidEntryPoint
class AnalyticFragment : BaseFragment<ActivityAnalyticBinding>() {
    private val viewModel: MainViewModel by activityViewModels()
    override fun setBinding(): ActivityAnalyticBinding = ActivityAnalyticBinding.inflate(layoutInflater)

    override fun initView() {
        initObservers()
        viewModel.getUserData()
        //viewModel.getUserMeasures()
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
        binding.cvAnalyticProfile.isClickable = true
        binding.cvAnalyticProfile.setOnClickListener {
            val navHostFragment = requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_fragment_main) as NavHostFragment
            val navController = navHostFragment.navController
            navController.navigate(R.id.action_tabFragment_to_settingFragment)
        }
    }

    private fun openGraphView(type: Int) {
        viewModel.graphicType = type
        val navHostFragment = requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_fragment_main) as NavHostFragment
        val navController = navHostFragment.navController
        navController.navigate(R.id.action_tabFragment_to_graphicsFragment)
        //findNavController().navigate(R.id.action_analyticFragment_to_graphicsFragment)
    }

    private fun initObservers() {
        onLastMeasureChanged(viewModel.lastMeasure.value)
        viewModel.userData.observe(this, this::onUserDataChanged)
        viewModel.userImageDrawable.observe(this, this::onUserImageChanged)
        viewModel.lastMeasure.observe(this, this::onLastMeasureChanged)
    }

    private fun onUserDataChanged(userData: CustomUser) {
        Log.d(SettingFragment.TAG, "on user data changed")
        val initials = getString(R.string.user_name_initials, userData.name.substring(0, 1).uppercase(), userData.lastName.substring(0, 1).uppercase())
        binding.tvAnalyticProfileName.text = initials
    }

    private fun onUserImageChanged(drawable: Drawable?) {
        Log.d(HomeFragment.TAG, "image changed")
        if (drawable == null) {
            binding.ivAnalyticProfile.setImageDrawable(null)
            binding.ivAnalyticProfile.visibility = View.INVISIBLE
            return
        }
        drawable?.let {
            binding.ivAnalyticProfile.load(drawable)
            binding.ivAnalyticProfile.visibility = View.VISIBLE
            return
        }
    }

    private fun onLastMeasureChanged(measureData: MeasureData?) {
        Log.d(TAG, "onLastMeasureChanged: $measureData")
        val cvHy = binding.cvAnalyticHydration
        val score = measureData?.score
        cvHy.value = if (measureData?.precision == MeasurePrecision.Bad) null else score
        cvHy.subtitle = when (score) {
            null -> getString(R.string.analytic_no_data)
            in 0..30 -> getString(R.string.analytic_severely_dehydrated)
            in 31..64 -> getString(R.string.analytic_dehydrated)
            in 65..90 -> getString(R.string.analytic_hydrated)
            else -> getString(R.string.analytic_very_hydrated)
        }
        cvHy.description = when (score) {
            null -> ""
            in 0..64 -> getString(R.string.analytic_hydratation_under)
            else -> getString(R.string.analytic_hydratation_above)
        }
        cvHy.onClick = { openGraphView(0) }
        val el = measureData?.msCond?.toInt()// ?: 0
        val cvEl = binding.cvAnalyticElectrol
        cvEl.value = if (measureData?.precision == MeasurePrecision.Bad) null else el
        cvEl.subtitle = when (el) {
            null -> getString(R.string.analytic_no_data)
            in 0..4 -> getString(R.string.analytic_elec_hypo)
            in 5..19 -> getString(R.string.analytic_elec_normal)
            else -> getString(R.string.analytic_elec_hyper)
        }
        cvEl.description = when (el) {
            null -> ""
            in 0..4 -> getString(R.string.analytic_elec_under)
            in 5..19 -> getString(R.string.analytic_elec_optimal)
            else -> getString(R.string.analytic_elec_above)
        }
        cvEl.onClick = { openGraphView(1) }

        val cvVol = binding.cvAnalyticVolume
        val vol = measureData?.vol?.toInt()
        cvEl.value = if (measureData?.precision == MeasurePrecision.Bad) null else vol
        cvVol.subtitle = when (vol) {
            null -> getString(R.string.analytic_volume_insuff_title)
            in 0..149 -> getString(R.string.analytic_volume_low)
            in 150..249 -> getString(R.string.analytic_volume_medium)
            else -> getString(R.string.analytic_volume_high)
        }
        cvVol.description = when (vol) {
            null -> getString(R.string.analytic_volume_insuff_desc)
            in 0..149 -> getString(R.string.analytic_volume_insuff_desc)
            else -> getString(R.string.analytic_volume_good)
        }
        cvVol.onClick = { openGraphView(2) }

        val cvNew = binding.cvAnalyticEmpty
        cvNew.onClick = { }

        measureData?.analysisDate?.let {
            val date = Date(it)
            val dateString = date.formatTime.uppercase()
            binding.tvAnalyticLastSample.text = getString(R.string.home_last_sample_message, dateString)
        }
    }

    companion object {
        val TAG: String = AnalyticFragment::class.java.simpleName
    }
}