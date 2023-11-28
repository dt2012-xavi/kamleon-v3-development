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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.fragments.HomeFragment
import com.dynatech2012.kamleonuserapp.fragments.HomeItemFragment
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ViewPager(tipType: TipType, modifier: Modifier, onClick: () -> Unit, ) {
    val pagerState = rememberPagerState(pageCount = {
        tipType.size
    })
    val TAG: String = "ViewPager"
    Column(
        modifier = modifier
            //.wrapContentHeight()
            //.fillMaxSize()

    ) {

        HorizontalPager(
            state = pagerState,
            modifier = Modifier,
                //.weight(1f),
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
                HYDRATION -> 2
                ELECTROLYTE -> 2
                VOLUME -> 2
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardViewHomeItemHome(page: Int) {
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(true)
    val scope = rememberCoroutineScope()
    val tips = listOf(
        Tip(
            stringResource(R.string.home_item1_type),
            clickable = false,
            locked = false,
            title = stringResource(R.string.home_item1_title),
            description = stringResource(R.string.home_item1_description)
        ),
        Tip(
            stringResource(R.string.home_item1_type),
            clickable = true,
            locked = true,
            title = stringResource(R.string.home_item1_title),
            description = stringResource(R.string.home_item1_description)
        )
    )
    val tip = tips[page]
    when (page) {
        0 -> {
            CardViewHomeItem(
                tip = tip,
                modifier = Modifier,
                showBottomSheet = showBottomSheet,
                sheetState = sheetState,
                onClick = { },
                onDismiss = {
                    scope.launch {
                        sheetState.hide()
                    }
                        .invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                showBottomSheet = false
                            }
                        }
                }
                ) { }
        }
        1 -> {
            CardViewHomeItem(
                tip = tip,
                modifier = Modifier,
                showBottomSheet = showBottomSheet,
                sheetState = sheetState,
                onClick = {
                    showBottomSheet = true
                },
                onDismiss = {
                    scope.launch {
                        sheetState.hide()
                    }
                        .invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                showBottomSheet = false
                            }
                        }
                }
            )
            {
                PremiumView(
                    modifier = Modifier
                        .fillMaxSize(),
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                        }
                            .invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    showBottomSheet = false
                                }
                            }
                    }
                )
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardViewHomeItemHydration(page: Int) {
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(true)
    val scope = rememberCoroutineScope()
    val tips = listOf(
        Tip(
            stringResource(R.string.home_item1_type),
            clickable = true,
            locked = false,
            title = stringResource(R.string.home_item1_title),
            description = stringResource(R.string.home_item1_description)
        ),
        Tip(
            stringResource(R.string.home_item1_type),
            clickable = true,
            locked = true,
            title = stringResource(R.string.home_item1_title),
            description = stringResource(R.string.home_item1_description)
        )
    )
    val tip = tips[page]
    when (page) {
        0 -> {
            CardViewHomeItem(
                tip = tip,
                modifier = Modifier,
                showBottomSheet = showBottomSheet,
                sheetState = sheetState,
                onClick = {
                    showBottomSheet = true
                },
                onDismiss = {
                    scope.launch {
                        sheetState.hide()
                    }
                        .invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                showBottomSheet = false
                            }
                        }
                }
            ){
                CardDetailView(
                    modifier = Modifier
                        .fillMaxSize(),
                    tip = tip,
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                        }
                            .invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    showBottomSheet = false
                                }
                            }
                    }
                )
            }
        }
        1 -> {
            CardViewHomeItem(
                tip = tip,
                modifier = Modifier,
                showBottomSheet = showBottomSheet,
                sheetState = sheetState,
                onClick = {
                    showBottomSheet = true
                },
                onDismiss = {
                    scope.launch {
                        sheetState.hide()
                    }
                        .invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                showBottomSheet = false
                            }
                        }
                }
            )
            {
                PremiumView(
                    modifier = Modifier
                        .fillMaxSize(),
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                        }
                            .invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    showBottomSheet = false
                                }
                            }
                    }
                )
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardViewHomeItemElect(page: Int) {
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(true)
    val scope = rememberCoroutineScope()
    val tips = listOf(
        Tip(
            stringResource(R.string.home_item1_type),
            clickable = true,
            locked = false,
            title = stringResource(R.string.home_item1_title),
            description = stringResource(R.string.home_item1_description)
        ),
        Tip(
            stringResource(R.string.home_item1_type),
            clickable = true,
            locked = true,
            title = stringResource(R.string.home_item1_title),
            description = stringResource(R.string.home_item1_description)
        )
    )
    val tip = tips[page]
    when (page) {
        0 -> {
            CardViewHomeItem(
                tip = tip,
                modifier = Modifier,
                showBottomSheet = showBottomSheet,
                sheetState = sheetState,
                onClick = {
                    showBottomSheet = true
                },
                onDismiss = {
                    scope.launch {
                        sheetState.hide()
                    }
                        .invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                showBottomSheet = false
                            }
                        }
                }
            ) {
                CardDetailView(
                    modifier = Modifier
                        .fillMaxSize(),
                    tip = tip,
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                        }
                            .invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    showBottomSheet = false
                                }
                            }
                    }
                )
            }
        }
        1 -> {
            CardViewHomeItem(
                tip = tip,
                modifier = Modifier,
                showBottomSheet = showBottomSheet,
                sheetState = sheetState,
                onClick = {
                    showBottomSheet = true
                },
                onDismiss = {
                    scope.launch {
                        sheetState.hide()
                    }
                        .invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                showBottomSheet = false
                            }
                        }
                }
            ) {

            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardViewHomeItemVol(page: Int) {
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(true)
    val scope = rememberCoroutineScope()
    val tips = listOf(
        Tip(
            stringResource(R.string.home_item1_type),
            clickable = true,
            locked = false,
            title = stringResource(R.string.home_item1_title),
            description = stringResource(R.string.home_item1_description)
        ),
        Tip(
            stringResource(R.string.home_item1_type),
            clickable = true,
            locked = true,
            title = stringResource(R.string.home_item1_title),
            description = stringResource(R.string.home_item1_description)
        )
    )
    val tip = tips[page]
    when (page) {
        0 -> {
            CardViewHomeItem(
                tip = tip,
                modifier = Modifier,
                showBottomSheet = showBottomSheet,
                sheetState = sheetState,
                onClick = {
                    showBottomSheet = true
                },
                onDismiss = {
                    scope.launch {
                        sheetState.hide()
                    }
                        .invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                showBottomSheet = false
                            }
                        }
                }
            ) {
                CardDetailView(
                    modifier = Modifier
                        .fillMaxSize(),
                    tip = tip,
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                        }
                            .invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    showBottomSheet = false
                                }
                            }
                    }
                )
            }
        }
        1 -> {
            CardViewHomeItem(
                tip = tip,
                modifier = Modifier,
                showBottomSheet = showBottomSheet,
                sheetState = sheetState,
                onClick = {
                    showBottomSheet = true
                },
                onDismiss = {
                    scope.launch {
                        sheetState.hide()
                    }
                        .invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                showBottomSheet = false
                            }
                        }
                }
            ) {
                PremiumView(
                    modifier = Modifier
                        .fillMaxSize(),
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                        }
                            .invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    showBottomSheet = false
                                }
                            }
                    }
                )
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