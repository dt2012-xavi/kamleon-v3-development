package com.dynatech2012.kamleonuserapp.views.cards

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.models.Recommendation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationDetailView(modifier: Modifier, recommendation: Recommendation, onClick: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    val sheetStatePremium = rememberModalBottomSheetState(true)
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }
    var showBottomSheetPremium by remember { mutableStateOf(false) }
    var key1 by remember { mutableStateOf(true) }
    var key2 by remember { mutableStateOf(false) }
    LaunchedEffect(key1 = key1) {
        Log.d("CardDetailView", "showModal: ${recommendation.showModal}")
        if (key1 && recommendation.showModal) {
            delay(5000)
            showBottomSheet = true
            key1 = false
        }
    }
    LaunchedEffect(key1 = key2) {
        if (key2) {
            delay(500)
            if (key2) showBottomSheet = true
            key2 = false
        }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.color_fa))
    )
    {
        Column(
            modifier = modifier
                .fillMaxWidth()
        ) {
            Row(

            )
            {
                if (recommendation.image != null)
                    Image(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.5f),
                            //.clip(RoundedCornerShape(0.dp, 0.dp, 20.dp, 20.dp)),
                        painter = painterResource(
                            id = recommendation.image
                        ),
                        contentDescription = "",
                        contentScale = ContentScale.Crop
                    )

            }
            Column(
                modifier = Modifier
                    .padding(horizontal = 30.dp),
            ) {
                Text(
                    modifier = Modifier
                        .padding(top = 14.dp),
                    text = stringResource(id = recommendation.title),
                    fontSize = dimensionResource(R.dimen.ts_20).value.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(id = R.color.kamleon_dark_grey)
                )
                if (recommendation.kind != null) {
                    Text(
                        modifier = Modifier
                            .padding(bottom = 30.dp),
                        text = stringResource(id = recommendation.kind),
                        fontSize = dimensionResource(R.dimen.ts_16).value.sp,
                        color = colorResource(id = R.color.kamleon_secondary_grey_60)
                    )
                }
                Text(
                    text = if (recommendation.text != null) stringResource(id = recommendation.text!!) else "",
                    fontSize = dimensionResource(R.dimen.ts_16).value.sp,
                    color = colorResource(id = R.color.kamleon_dark_grey)
                )
            }
            Spacer(
                modifier = Modifier
                    .weight(1f)
            )
        }

        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(

            )
            {
                Spacer(
                    modifier = Modifier
                        .weight(1f)
                )
                Image(
                    modifier = Modifier
                        .padding(horizontal = 6.dp, vertical = 32.dp)
                        .size(36.dp)
                        .coloredShadow(
                            color = colorResource(id = R.color.kamleon_dark_grey),
                            alpha = 0.1f,
                            offsetX = 2.dp,
                            offsetY = 2.dp,
                            borderRadius = 18.dp,
                            shadowRadius = 2.dp,
                        )
                        .background(colorResource(id = R.color.color_fa), CircleShape)
                        ///.background(colorResource(id = R.color.white30), CircleShape)
                        .clip(CircleShape)                       // clip to the circle shape
                        //.border(2.dp, colorResource(id = R.color.color_fa), CircleShape)
                        .clickable
                        {
                            key2 = false
                            onClick()
                        },

                    painter = painterResource(
                        id = R.drawable.ic_close_24
                    ),
                    contentDescription = "",
                    colorFilter = ColorFilter.tint(color = colorResource(id = R.color.kamleon_dark_grey))
                )

            }
            Spacer(
                modifier = Modifier
                    .weight(1f)
            )
        }

        if (showBottomSheet && recommendation.showModal) {
            ModalBottomSheet(
                modifier = Modifier,
                    //.wrapContentHeight(),
                    //.fillMaxSize(),
                onDismissRequest = {
                    showBottomSheet = false
                    key2 = true
                },
                sheetState = sheetState,
                shape = RoundedCornerShape(12.dp),
                dragHandle = {},
                windowInsets = WindowInsets(0, 0, 0, 0),
                )
            {
                // Sheet content
                PremiumModalView(
                    modifier = Modifier,
                    //.wrapContentHeight(),
                ) {
                    showBottomSheetPremium = true
                }
            }
        }

        if (showBottomSheetPremium) {
            ModalBottomSheet(
                modifier = Modifier
                    .fillMaxSize(),
                onDismissRequest = {
                    showBottomSheetPremium = false
                },
                sheetState = sheetStatePremium,
                shape = BottomSheetDefaults.ExpandedShape,
                dragHandle = {},
                containerColor = colorResource(id = R.color.kamleon_dark_grey),
                windowInsets = WindowInsets(0, 0, 0, 0),
                ) {
                // Sheet content
                Box(Modifier.safeDrawingPadding()) {
                    PremiumView(
                        modifier = Modifier
                            .fillMaxSize(),
                        onClick = {
                            scope.launch {
                                sheetStatePremium.hide()
                            }
                                .invokeOnCompletion {
                                    if (!sheetStatePremium.isVisible) {
                                        showBottomSheetPremium = false
                                    }
                                }
                        }
                    )
                }
            }
        }
    }
}