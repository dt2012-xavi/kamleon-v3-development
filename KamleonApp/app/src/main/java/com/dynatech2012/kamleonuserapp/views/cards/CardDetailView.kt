package com.dynatech2012.kamleonuserapp.views.cards

import android.graphics.drawable.GradientDrawable
import android.text.style.ForegroundColorSpan
import android.view.View
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.LocalMinimumTouchTargetEnforcement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.paint
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.views.AppTheme
import com.dynatech2012.kamleonuserapp.views.md_theme_dark_onBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailView(modifier: Modifier, onClick: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }
    LaunchedEffect(key1 = "delay", block = {
        delay(1000)
        showBottomSheet = true
    })
    Box(
        modifier = modifier
            .fillMaxSize()
    )
    {
        Column(
            modifier = modifier
                .fillMaxWidth()
        ) {
            Row(

            )
            {
                Image(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(0.dp, 0.dp, 20.dp, 20.dp)),
                    painter = painterResource(
                        id = R.drawable.tip1
                    ),
                    contentDescription = "",
                    contentScale = ContentScale.Crop
                )

            }
            Text(text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed euismod, diam quis aliquam ultri")
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
                        .padding(10.dp)
                        .size(40.dp)
                        .background(colorResource(id = R.color.white30), CircleShape)
                        .clip(CircleShape)                       // clip to the circle shape
                        .border(2.dp, colorResource(id = R.color.color_fa), CircleShape)
                        .clickable { onClick() },

                    painter = painterResource(
                        id = R.drawable.close_24
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

        if (showBottomSheet) {
            ModalBottomSheet(
                modifier = Modifier
                    .height(260.dp),
                onDismissRequest = {
                    showBottomSheet = false
                },
                sheetState = sheetState,
                //shape = BottomSheetDefaults.ExpandedShape,
                dragHandle = {},
            ) {
                // Sheet content
                Column(
                    modifier = Modifier
                    .fillMaxSize()
                    .background(color = colorResource(id = R.color.color_fa))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        modifier = Modifier
                            .size(40.dp),
                        painter = painterResource(
                            id = R.drawable.tip1
                        ),
                        contentDescription = "",
                    )
                    Text(text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed euismod, diam quis aliquam ultri")

            }
            }
        }
    }
}