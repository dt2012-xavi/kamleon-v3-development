package com.dynatech2012.kamleonuserapp.views.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import coil.load
import coil.transform.CircleCropTransformation
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.application.AppTheme
import com.dynatech2012.kamleonuserapp.models.RoleCard
import com.dynatech2012.kamleonuserapp.models.RoleGroup
import com.dynatech2012.kamleonuserapp.repositories.Response
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel
import com.dynatech2012.kamleonuserapp.views.KamleonRoundButton
import com.dynatech2012.kamleonuserapp.views.kamleonBodySmall
import com.dynatech2012.kamleonuserapp.views.kamleonCaption
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

/** What the roles sheet shows. */
sealed interface RolesState {
    data object Loading : RolesState
    data object Failed : RolesState
    data class Loaded(val groups: List<RoleGroup>) : RolesState
}

/** The Settings sheet that lists the user's roles, grouped by organization, as the web shows them. */
class UserRolesSheet : BottomSheetDialogFragment() {
    private val viewModel: MainViewModel by activityViewModels()
    private var state by mutableStateOf<RolesState>(RolesState.Loading)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppTheme(useDarkTheme = false) {
                    UserRolesContent(state = state, imageLoader = viewModel.imageLoader, onRetry = ::load)
                }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        load()
    }

    override fun onStart() {
        super.onStart()
        showExpanded(draggable = true)
    }

    private fun load() {
        state = RolesState.Loading
        viewLifecycleOwner.lifecycleScope.launch {
            state = when (val response = viewModel.loadUserRoles()) {
                is Response.Success -> RolesState.Loaded(response.data)
                else -> RolesState.Failed
            }
        }
    }
}

/** The roles sheet: a spinner while loading, the error with Retry, or the groups of role cards. */
@Composable
fun UserRolesContent(state: RolesState, imageLoader: ImageLoader?, onRetry: () -> Unit) {
    SettingsSheetSurface(title = stringResource(R.string.roles_title)) {
        when (state) {
            RolesState.Loading -> Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    color = colorResource(R.color.kamleon_blue),
                    modifier = Modifier.size(32.dp).testTag("roles_loading")
                )
            }
            RolesState.Failed -> {
                SheetMessage(stringResource(R.string.roles_error))
                Spacer(Modifier.height(24.dp))
                KamleonRoundButton(
                    text = stringResource(R.string.retry),
                    onClick = onRetry,
                    enabled = true,
                    testTag = "roles_retry"
                )
            }
            is RolesState.Loaded -> if (state.groups.isEmpty()) {
                SheetMessage(stringResource(R.string.roles_empty))
            } else {
                Column(
                    Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        .nestedScroll(rememberNestedScrollInteropConnection())
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    state.groups.forEachIndexed { index, group ->
                        if (index > 0) Spacer(Modifier.height(8.dp))
                        GroupHeader(group, imageLoader)
                        group.cards.forEach { RoleCardView(it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetMessage(text: String) {
    Text(
        text = text,
        style = kamleonBodySmall(),
        color = colorResource(R.color.kamleon_secondary_grey_60),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp).testTag("roles_message")
    )
}

/** The organization's logo (or its initial; Kamleon's own logo for Kamleon) and its name. */
@Composable
private fun GroupHeader(group: RoleGroup, imageLoader: ImageLoader?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().testTag("roles_group")
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .border(1.dp, colorResource(R.color.kamleon_outline), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            when {
                group.isKamleon -> Image(
                    painter = painterResource(R.drawable.img_logo),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.padding(5.dp).fillMaxSize()
                )
                else -> {
                    Text(
                        text = group.name.trim().take(1).uppercase(),
                        style = kamleonCaption(),
                        color = colorResource(R.color.kamleon_secondary_grey_60)
                    )
                    val url = group.imageUrl?.trim()?.takeIf { it.isNotEmpty() }
                    if (url != null && imageLoader != null) {
                        // Shown over the initial once it has loaded; a failed load leaves the initial.
                        AndroidView(
                            factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.CENTER_CROP } },
                            update = { it.load(url, imageLoader) { transformations(CircleCropTransformation()) } },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = group.name,
            style = kamleonBodySmall().copy(fontWeight = FontWeight.Bold),
            color = colorResource(R.color.kamleon_dark_grey)
        )
    }
}

/** One role: its name, what it is held on and whether it is active; team roles add their details. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoleCardView(card: RoleCard) {
    val outline = colorResource(R.color.kamleon_outline)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colorResource(R.color.white),
        border = BorderStroke(1.dp, outline),
        modifier = Modifier.fillMaxWidth().testTag("roles_card")
    ) {
        Column {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(card.kind.label),
                        style = kamleonBodySmall().copy(fontWeight = FontWeight.Bold),
                        color = colorResource(R.color.kamleon_dark_grey)
                    )
                    Text(
                        text = card.entityName,
                        style = kamleonBodySmall(),
                        color = colorResource(R.color.kamleon_secondary_grey_60)
                    )
                }
                Spacer(Modifier.width(8.dp))
                StatusPill(card.active)
            }
            if (card.centerName != null) {
                HorizontalDivider(color = outline)
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row {
                        Detail(stringResource(R.string.roles_center), card.centerName.ifBlank { DASH }, Modifier.weight(1f))
                        card.teamUser?.let {
                            Detail(stringResource(R.string.roles_roster_number), it.rosterNumber ?: DASH, Modifier.weight(1f))
                        }
                    }
                    card.teamUser?.let {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.roles_permissions),
                            style = kamleonCaption(),
                            color = colorResource(R.color.kamleon_secondary_grey_60)
                        )
                        Spacer(Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Chip(stringResource(R.string.roles_screen, stringResource(statusText(it.screenOn))))
                            Chip(stringResource(R.string.roles_pin, stringResource(statusText(it.pinOn))))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(active: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(colorResource(R.color.kamleon_dark_grey_05), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Box(
            Modifier
                .size(7.dp)
                .background(colorResource(if (active) R.color.kamleon_blue else R.color.kamleon_secondary_grey_50), CircleShape)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(statusText(active)),
            style = kamleonCaption(),
            color = colorResource(R.color.kamleon_dark_grey)
        )
    }
}

@Composable
private fun Detail(label: String, value: String, modifier: Modifier) {
    Column(modifier.padding(end = 8.dp)) {
        Text(text = label, style = kamleonCaption(), color = colorResource(R.color.kamleon_secondary_grey_60))
        Text(text = value, style = kamleonBodySmall(), color = colorResource(R.color.kamleon_dark_grey))
    }
}

@Composable
private fun Chip(text: String) {
    Text(
        text = text,
        style = kamleonCaption(),
        color = colorResource(R.color.kamleon_dark_grey),
        modifier = Modifier
            .background(colorResource(R.color.kamleon_dark_grey_05), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

private const val DASH = "—"

/** "Active" or "Inactive": a role's status and what a team user can do on the screen read the same. */
@StringRes
private fun statusText(active: Boolean): Int = if (active) R.string.roles_active else R.string.roles_inactive
