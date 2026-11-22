package com.dynatech2012.kamleonuserapp.fragments

import android.graphics.Color
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.NavHostFragment
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.base.BaseFragment
import com.dynatech2012.kamleonuserapp.database.MeasureData
import com.dynatech2012.kamleonuserapp.databinding.ActivityHomeBinding
import com.dynatech2012.kamleonuserapp.extensions.formatTime
import com.dynatech2012.kamleonuserapp.models.CustomUser
import com.dynatech2012.kamleonuserapp.models.Invitation
import com.dynatech2012.kamleonuserapp.models.HomeStatus
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel
import com.dynatech2012.kamleonuserapp.views.cards.HomeMetricsCard
import com.google.android.material.color.MaterialColors
import dagger.hilt.android.AndroidEntryPoint
import java.util.Date
import com.dynatech2012.kamleonuserapp.views.showProfileImage
import com.dynatech2012.kamleonuserapp.utils.DebugLog


@AndroidEntryPoint
class HomeFragment : BaseFragment<ActivityHomeBinding>() {
    private val viewModel: MainViewModel by activityViewModels()
    override fun setBinding(): ActivityHomeBinding = ActivityHomeBinding.inflate(layoutInflater)

    override fun initView() {
        // The ring shrinks to fit between the message and the card; the drawn circle and the score follow its size.
        val ring = binding.homeProgressBar
        val scoreSize = binding.lastMeasure.textSize
        val unitSize = binding.tvHomeUnit.textSize
        ring.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            val size = minOf(ring.width, ring.height)
            if (size > 0 && ring.indicatorSize != size) ring.post {
                ring.indicatorSize = size
                val scale = minOf(1f, size / fullRingPx)
                binding.lastMeasure.setTextSize(TypedValue.COMPLEX_UNIT_PX, scoreSize * scale)
                binding.tvHomeUnit.setTextSize(TypedValue.COMPLEX_UNIT_PX, unitSize * scale)
            }
        }
        showStatus(HomeStatus.Loading)

        //selectTab(0)
        initObservers()
        viewModel.getUserData()
        DebugLog.d(TAG) { "got measures -2" }
        //viewModel.getUserMeasures()
        viewModel.getInvitations()
    }

    override fun initEvent() {
        DebugLog.d(TAG) { "settings clicked 0" }
        binding.cvHomeProfile.isClickable = true
        binding.cvHomeProfile.setOnClickListener {
            DebugLog.d(TAG) { "settings clicked 1" }
            val navHostFragment = requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_fragment_main) as NavHostFragment
            val navController = navHostFragment.navController
            //Log.d(TAG, "frgmts ${navHostFragment.childFragmentManager.fragments[0]}")
            DebugLog.d(TAG) { "settings clicked 2" }
            navController.navigate(R.id.action_tabFragment_to_settingFragment)
            //navController.popBackStack(R.id.tabFragment, true)
        }
    }
    private fun initObservers() {
        viewModel.userData.observe(this, this::onUserDataChanged)
        viewModel.userImageUrl.observe(this, this::onUserImageChanged)
        viewModel.lastMeasure.observe(this, this::onLastMeasureChanged)
        viewModel.homeStatus.observe(this) { status ->
            // A failed refresh must not replace a sample that is already on screen.
            if (viewModel.lastMeasure.value == null) showStatus(status)
        }
        viewModel.pendingInvitations.observe(this, this::onGetPendingInvitations)
    }

    private fun onUserDataChanged(userData: CustomUser) {
        DebugLog.d(TAG) { "on user data changed" }
        val initials = getString(R.string.user_name_initials, userData.name.substring(0, 1).uppercase(), userData.lastName.substring(0, 1).uppercase())
        binding.tvHomeProfileName.text = initials
    }

    private fun onUserImageChanged(url: String?) {
        binding.ivHomeProfile.showProfileImage(url, viewModel.imageLoader)
    }

    private fun onLastMeasureChanged(measure: MeasureData) {
        DebugLog.d(TAG) { "got measures 6 changed" }
        val date = Date(measure.analysisDate)
        //val format = SimpleDateFormat("d MMMM yyy", Locale.US)
        //val dateString = format.format(date)
        val dateString = date.formatTime.uppercase()
        binding.tvHomeLastSample.text = getString(R.string.home_last_sample_message, dateString)
        binding.cvHome.setContent { HomeMetricsCard(measure) }
        if (!measure.isPrecise) {
            binding.lastMeasure.text = getString(R.string.default_value)
            binding.tvHomeUnit.visibility = View.GONE
            binding.homeProgressBar.progress = 0

            binding.homeBg.setImageResource(R.drawable.bg_low_volume)
            binding.tvHomeMessage.text = getString(R.string.analytic_volume_low)
            val color = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnSurface, Color.BLACK)
            binding.tvHomeMessage.setTextColor(color)
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
        val color = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnSurfaceInverse, Color.WHITE)
        binding.tvHomeMessage.setTextColor(color)
    }

    private fun onGetPendingInvitations(invitations: ArrayList<Invitation>) {
        if (invitations.isNotEmpty())
            binding.ivHomeNotiBadge.visibility = View.VISIBLE
        else binding.ivHomeNotiBadge.visibility = View.GONE
    }

    /** No sample to show: a neutral grey screen that says why. */
    private fun showStatus(status: HomeStatus) {
        binding.homeBg.setImageResource(R.drawable.bg_no_measure)
        binding.tvHomeLastSample.text = getString(R.string.home_last_sample_message_default)
        binding.tvHomeMessage.text = getString(
            when (status) {
                HomeStatus.Loading -> R.string.home_status_loading
                HomeStatus.Empty -> R.string.home_status_empty
                HomeStatus.Unavailable -> R.string.home_status_unavailable
            }
        )
        binding.tvHomeMessage.setTextColor(MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnSurface, Color.BLACK))
        binding.lastMeasure.text = getString(R.string.default_value)
        binding.tvHomeUnit.visibility = View.GONE
        binding.homeProgressBar.progress = 0
        binding.cvHome.setContent { HomeMetricsCard(null) }
    }

    /** The ring's designed size (240dp), at which the score keeps its full text size. */
    private val fullRingPx get() = 240 * resources.displayMetrics.density

    companion object {
        val TAG: String = HomeFragment::class.java.simpleName
    }
}
