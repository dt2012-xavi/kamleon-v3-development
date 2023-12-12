package com.dynatech2012.kamleonuserapp.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getDrawable
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import coil.load
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.base.BaseFragment
import com.dynatech2012.kamleonuserapp.camera.QRCodeFoundListener
import com.dynatech2012.kamleonuserapp.database.MeasureData
import com.dynatech2012.kamleonuserapp.databinding.ActivityHomeBinding
import com.dynatech2012.kamleonuserapp.extensions.formatTime
import com.dynatech2012.kamleonuserapp.models.RecommendationType
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel
import com.dynatech2012.kamleonuserapp.viewmodels.QrViewModel
import com.dynatech2012.kamleonuserapp.views.cards.ViewPager
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import java.util.Date
import java.util.concurrent.ExecutionException


@AndroidEntryPoint
class HomeFragment : BaseFragment<ActivityHomeBinding>() {
    private val viewModel: MainViewModel by activityViewModels()
    override fun setBinding(): ActivityHomeBinding = ActivityHomeBinding.inflate(layoutInflater)

    private val qrViewModel: QrViewModel by viewModels()
    private lateinit var previewView: PreviewView
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private var qrListener: QRCodeFoundListener? = null

    private val onDismissScanIntro = object : BottomFragmentDismissListener {
        override fun onDismissFragment() {
        }
    }

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
            navController.navigate(R.id.action_tabFragment_to_settingFragment)
        }
    }
    private fun initObservers() {
        viewModel.userImageDrawable.observe(this, this::onUserImageChanged)
        viewModel.lastMeasure.observe(this, this::onLastMeasureChanged)
    }

    private fun onUserImageChanged(drawable: Drawable?) {
        Log.d(TAG, "image changed")
        drawable?.let {
            binding.ivHomeProfile.load(drawable)
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
