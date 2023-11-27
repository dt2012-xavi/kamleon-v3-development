package com.dynatech2012.kamleonuserapp.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.databinding.LayoutHomeListItemBinding
import com.dynatech2012.kamleonuserapp.views.cards.CardViewHomeItem
import com.dynatech2012.kamleonuserapp.views.cards.Tip

class HomeItemFragment : Fragment() {
    lateinit var binding: LayoutHomeListItemBinding
    var type: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        arguments?. let {
            type = it.getInt(ARG_TYPE)
            Log.d(TAG, "get args: page: $type")
        }
        binding = LayoutHomeListItemBinding.inflate(layoutInflater, container, false)
        binding.composeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                when (type) {
                    0 -> {
                        CardViewHomeItem(tip = Tip(getString(R.string.home_item1_type), false, getString(R.string.home_item1_title), getString(R.string.home_item1_description)),
                            modifier = Modifier
                        ) {
                            Log.d(TAG, "onClick: $type")
                        }
                    }
                    1 -> {
                        CardViewHomeItem(tip = Tip(getString(R.string.home_item2_type), false, getString(R.string.home_item2_title), getString(R.string.home_item2_description)),
                            modifier = Modifier
                        ) {

                        }
                    }
                }
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        /*
        when (type) {
            0 -> {
                binding.tvType.text = getString(R.string.home_item1_type)
                binding.tvTitle.text = getString(R.string.home_item1_title)
                binding.tvDesc.text = getString(R.string.home_item1_description)
            }
            1 -> {
                binding.tvType.text = getString(R.string.home_item2_type)
                binding.tvTitle.text = getString(R.string.home_item2_title)
                binding.tvDesc.text = getString(R.string.home_item2_description)
            }
        }
        */

    }

    companion object {
        private val TAG = this::class.simpleName
        fun newInstance(type: Int): HomeItemFragment {
            val args = Bundle()
            args.putInt(ARG_TYPE, type)
            Log.d(TAG, "put args: type: $type")
            val fragment = HomeItemFragment()
            fragment.arguments = args
            return fragment
        }

        //private lateinit var totalAdapter: TotalAdapter
        private const val ARG_TYPE = "type"
    }
}
