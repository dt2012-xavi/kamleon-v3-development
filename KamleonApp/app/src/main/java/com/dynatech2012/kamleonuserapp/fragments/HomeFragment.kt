package com.dynatech2012.kamleonuserapp.fragments

import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.NavHostFragment
import coil.load
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.base.BaseFragment
import com.dynatech2012.kamleonuserapp.database.MeasureData
import com.dynatech2012.kamleonuserapp.databinding.ActivityHomeBinding
import com.dynatech2012.kamleonuserapp.extensions.formatTime
import com.dynatech2012.kamleonuserapp.models.CustomUser
import com.dynatech2012.kamleonuserapp.models.Invitation
import com.dynatech2012.kamleonuserapp.models.MeasurePrecision
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
        //viewModel.getUserMeasures()
        viewModel.getInvitations()
    }

    override fun initEvent() {
        Log.d(TAG, "settings clicked 0")
        binding.cvHomeProfile.isClickable = true
        binding.cvHomeProfile.setOnClickListener {
            Log.d(TAG, "settings clicked 1")
            val navHostFragment = requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_fragment_main) as NavHostFragment
            val navController = navHostFragment.navController
            //Log.d(TAG, "frgmts ${navHostFragment.childFragmentManager.fragments[0]}")
            Log.d(TAG, "settings clicked 2")
            navController.navigate(R.id.action_tabFragment_to_settingFragment)
            //navController.popBackStack(R.id.tabFragment, true)
        }
    }
    private fun initObservers() {
        viewModel.userData.observe(this, this::onUserDataChanged)
        viewModel.userImageDrawable.observe(this, this::onUserImageChanged)
        viewModel.lastMeasure.observe(this, this::onLastMeasureChanged)
        viewModel.pendingInvitations.observe(this, this::onGetPendingInvitations)
    }

    private fun onUserDataChanged(userData: CustomUser) {
        Log.d(TAG, "on user data changed")
        val initials = getString(R.string.user_name_initials, userData.name.substring(0, 1).uppercase(), userData.lastName.substring(0, 1).uppercase())
        binding.tvHomeProfileName.text = initials
    }

    private fun onUserImageChanged(drawable: Drawable?) {
        Log.d(TAG, "image changed")
        if (drawable == null) {
            binding.ivHomeProfile.setImageDrawable(null)
            binding.ivHomeProfile.visibility = View.INVISIBLE
            return
        }
        binding.ivHomeProfile.load(drawable)
        binding.ivHomeProfile.visibility = View.VISIBLE
    }

    private fun onLastMeasureChanged(measure: MeasureData) {
        Log.d(TAG, "got measures 6 changed")
        val date = Date(measure.analysisDate)
        //val format = SimpleDateFormat("d MMMM yyy", Locale.US)
        //val dateString = format.format(date)
        val dateString = date.formatTime.uppercase()
        binding.tvHomeLastSample.text = getString(R.string.home_last_sample_message, dateString)
        setupTips(measure)
        if (!measure.isPrecise) {
            binding.lastMeasure.text = getString(R.string.default_value)
            binding.tvHomeUnit.visibility = View.GONE
            binding.homeProgressBar.progress = 0

            binding.homeBg.setImageResource(R.drawable.bg_low_volume)
            binding.tvHomeMessage.text = getString(R.string.analytic_volume_low)
            return
        }
        binding.lastMeasure.text = measure.score.toString()
        binding.homeProgressBar.progress = measure.score
        binding.tvHomeUnit.visibility = View.VISIBLE
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
    }

    private fun onGetPendingInvitations(invitations: ArrayList<Invitation>) {
        if (invitations.isNotEmpty())
            binding.ivHomeNotiBadge.visibility = View.VISIBLE
        else binding.ivHomeNotiBadge.visibility = View.GONE
    }

    private fun setupTips(measure: MeasureData?) {
        val recommendationType = RecommendationType.HOME
        binding.cvHome.setContent {
            ViewPager(recommendationType = recommendationType, measure = measure, modifier = Modifier)
        }
    }

    companion object {
        val TAG: String = HomeFragment::class.java.simpleName
    }
}
