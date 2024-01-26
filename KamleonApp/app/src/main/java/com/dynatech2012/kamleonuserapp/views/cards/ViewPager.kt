package com.dynatech2012.kamleonuserapp.views.cards

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
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.database.MeasureData
import com.dynatech2012.kamleonuserapp.models.Recommendation
import com.dynatech2012.kamleonuserapp.models.RecommendationType
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ViewPager(recommendationType: RecommendationType, measure: MeasureData?, modifier: Modifier) {
    val pagerState = rememberPagerState(pageCount = {
        recommendationType.size
    })
    //val TAG: String = "ViewPager"
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
            CardViewHomeItemHome(recommendationType, measure, page)
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
                val color = Color.DarkGray
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .clip(CircleShape)
                        .alpha(if (pagerState.currentPage == iteration) 0.7f else 0.2f)
                        .background(color)
                        .size(10.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardViewHomeItemHome(recommType: RecommendationType, measure: MeasureData?, page: Int) {
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(true)
    val scope = rememberCoroutineScope()
    val recommendations = Recommendation.fromTipType(recommType)
    val recommendation = recommendations[page]
    recommendation.blocked = page == recommendations.count() - 1
    if (recommType == RecommendationType.HOME && page == 0 && measure != null) {
        recommendation.title = recommendation.getHydrationTitle(measure)
        recommendation.text = recommendation.getHydrationSubtitle(measure)
    }
    val systemUiController = rememberSystemUiController()
    val colorGray = colorResource(id = R.color.kamleon_dark_grey)
    LaunchedEffect(key1 = "changeStatusBar", block = {
        systemUiController.setSystemBarsColor(
            color = colorGray)
    })
    RecommendationCardView(
        recommendation = recommendation,
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
        if (page == recommendations.count() - 1) {
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
        else
            RecommendationDetailView(
                modifier = Modifier
                    .fillMaxSize(),
                recommendation = recommendation,
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

@Preview
@Composable
fun ViewPagerPrev() {
    ViewPager(RecommendationType.ELECTROLYTE, MeasureData(), Modifier
        .fillMaxSize()
        .background(colorResource(id = R.color.color_red))
    )
}