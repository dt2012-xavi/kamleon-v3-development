package com.dynatech2012.kamleonuserapp.views.cards

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.fragments.HomeFragment

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ViewPager(tipType: TipType, modifier: Modifier, onClick: () -> Unit, ) {
    val pagerState = rememberPagerState(pageCount = {
        tipType.size
    })
    val TAG: String = "ViewPager"
    Column(
        modifier = modifier
            .fillMaxSize()

    ) {

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f),
                //.fillMaxSize()
                //.padding(40.dp)
            pageSpacing = 8.dp,
            contentPadding = PaddingValues(
                horizontal = 36.dp,
                vertical = 8.dp
            ),
        ) { page ->
            // Our page content
            when (tipType) {
                TipType.HOME -> {
                    CardViewHomeItemHome(page)
                }
                TipType.HYDRATION -> {
                    CardViewHomeItemHydration(page)
                }
                TipType.ELECTROLYTE -> {
                    CardViewHomeItemElect(page)
                }
                TipType.VOLUME -> {
                    CardViewHomeItemVol(page)
                }
            }
        }
        Row(
            Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                //.align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pagerState.pageCount) { iteration ->
                val color =
                    if (pagerState.currentPage == iteration) Color.DarkGray else Color.LightGray
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(10.dp)
                )
            }
        }
    }
}

enum class TipType(val title: String) {
    HOME("Home"),
    HYDRATION("Hydration"),
    ELECTROLYTE("Electrolytes"),
    VOLUME("Volume");

    val size: Int
        get() {
            return when (this@TipType) {
                HOME -> 2
                HYDRATION -> 3
                ELECTROLYTE -> 3
                VOLUME -> 3
        }
    }

    companion object {
        fun from(id: Int): TipType {
            return when (id) {
                0 -> HOME
                1 -> HYDRATION
                2 -> ELECTROLYTE
                3 -> VOLUME
                else -> HYDRATION
            }
        }
    }
}

@Composable
fun CardViewHomeItemHome(page: Int) {
    when (page) {
        0 -> {
            CardViewHomeItem(
                tip = Tip("Title home 1", false, "Subtitle1", "Description1"),
                modifier = Modifier
            ) {

            }
        }
        1 -> {
            CardViewHomeItem(
                tip = Tip("Title home 2", false, "Subtitle2", "Description2"),
                modifier = Modifier
            ) {

            }
        }
    }
}
@Composable
fun CardViewHomeItemHydration(page: Int) {
    when (page) {
        0 -> {
            CardViewHomeItem(
                tip = Tip("Title hyd 1", false, "Subtitle1", "Description1"),
                modifier = Modifier
            ) {

            }
        }
        1 -> {
            CardViewHomeItem(
                tip = Tip("Title hyd 2", false, "Subtitle2", "Description2"),
                modifier = Modifier
            ) {

            }
        }
    }
}

@Composable
fun CardViewHomeItemElect(page: Int) {
    when (page) {
        0 -> {
            CardViewHomeItem(
                tip = Tip("Title elec 1", false, "Subtitle1", "Description1"),
                modifier = Modifier
            ) {

            }
        }
        1 -> {
            CardViewHomeItem(
                tip = Tip("Title elec 2", false, "Subtitle2", "Description2"),
                modifier = Modifier
            ) {

            }
        }
    }
}

@Composable
fun CardViewHomeItemVol(page: Int) {
    when (page) {
        0 -> {
            CardViewHomeItem(
                tip = Tip("Title vol 1", false, "Subtitle1", "Description1"),
                modifier = Modifier
            ) {

            }
        }
        1 -> {
            CardViewHomeItem(
                tip = Tip("Title vol 2", false, "Subtitle2", "Description2"),
                modifier = Modifier
            ) {

            }
        }
    }
}

@Preview
@Composable
fun ViewPagerPrev() {
    ViewPager(TipType.ELECTROLYTE, Modifier
        .fillMaxSize()
        .background(colorResource(id = R.color.color_red))
    ) {
        
    }

}