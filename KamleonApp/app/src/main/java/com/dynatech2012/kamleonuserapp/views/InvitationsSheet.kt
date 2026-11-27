package com.dynatech2012.kamleonuserapp.views

import com.dynatech2012.kamleonuserapp.models.InvitationRecord
import com.dynatech2012.kamleonuserapp.analytics.AnalyticsProps
import com.dynatech2012.kamleonuserapp.analytics.AnalyticsEvents
import com.dynatech2012.kamleonuserapp.analytics.Analytics
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.application.AppTheme
import com.dynatech2012.kamleonuserapp.constants.UrlConstants
import com.dynatech2012.kamleonuserapp.models.InvitationException
import com.dynatech2012.kamleonuserapp.models.InvitationItem
import com.dynatech2012.kamleonuserapp.models.InvitationRules
import com.dynatech2012.kamleonuserapp.models.RoleKind
import com.dynatech2012.kamleonuserapp.repositories.Response
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel
import com.dynatech2012.kamleonuserapp.views.settings.SettingsSheetSurface
import com.dynatech2012.kamleonuserapp.views.settings.openInAppBrowser
import com.dynatech2012.kamleonuserapp.views.settings.showExpanded
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** What the invitations sheet shows. */
sealed interface InvitationsState {
    data object Loading : InvitationsState
    data object Failed : InvitationsState
    data class Loaded(val items: List<InvitationItem>) : InvitationsState
}

/**
 * The bell's sheet: the user's pending invitations (accepted with the privacy policy, declined, or
 * dismissed once expired) and the ones accepted in the last days. Each card waits for its own answer;
 * closing the sheet marks the unseen ones as read.
 */
/** The panel and the function it belongs to, in the web's analytics taxonomy. */
private const val NOTIFICATIONS = "notifications"

class InvitationsSheet : BottomSheetDialogFragment() {
    private val viewModel: MainViewModel by activityViewModels()
    private var state by mutableStateOf<InvitationsState>(InvitationsState.Loading)
    /** Invitations whose answer is being sent. */
    private var busy by mutableStateOf(emptySet<String>())
    /** Invitations whose privacy policy box is ticked. */
    private var agreed by mutableStateOf(emptySet<String>())
    /** Whether all previous invitations are listed, not only the recently accepted ones. */
    private var showAll by mutableStateOf(false)
    private val snackbar = SnackbarHostState()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppTheme(useDarkTheme = false) {
                    InvitationsContent(
                        state = state,
                        busy = busy,
                        agreed = agreed,
                        now = System.currentTimeMillis() / 1000,
                        snackbar = snackbar,
                        showAll = showAll,
                        onShowAll = { showAll = true },
                        onAgree = { id, on -> agreed = if (on) agreed + id else agreed - id },
                        onOpenPolicy = ::openPolicy,
                        onAccept = { answer(it, viewModel::acceptInvitation, R.string.invitation_accepted_msg, R.string.invitation_accept_failed, AnalyticsEvents.INVITATION_ACCEPTED) },
                        onDecline = { answer(it, viewModel::declineInvitation, R.string.invitation_declined, R.string.invitation_decline_failed, AnalyticsEvents.INVITATION_DECLINED) },
                        onDismiss = { answer(it, viewModel::dismissInvitation, null, R.string.invitation_dismiss_failed, null) },
                        onRetry = { load(showSpinner = true) }
                    )
                }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) Analytics.drawerOpened(NOTIFICATIONS, NOTIFICATIONS)
        load(showSpinner = true)
    }

    override fun onStart() {
        super.onStart()
        showExpanded(draggable = true)
    }

    override fun onDismiss(dialog: DialogInterface) {
        // Seen now: the pending ones lose their dot.
        (state as? InvitationsState.Loaded)?.items
            ?.filter { it.unread }
            ?.map { it.record.id }
            ?.let(viewModel::markInvitationsRead)
        Analytics.drawerClosed()
        super.onDismiss(dialog)
    }

    /** Reads the invitations again; [then] runs once the list on screen is the new one. */
    private fun load(showSpinner: Boolean, then: () -> Unit = {}) {
        if (showSpinner) state = InvitationsState.Loading
        viewLifecycleOwner.lifecycleScope.launch {
            when (val response = viewModel.loadInvitations()) {
                is Response.Success -> state = InvitationsState.Loaded(response.data)
                else -> if (showSpinner) state = InvitationsState.Failed
            }
            then()
        }
    }

    /** Sends one answer for [item] and tells how it went; an invitation no longer valid is dropped. */
    private fun answer(
        item: InvitationItem,
        call: suspend (String) -> Response<Unit>,
        @StringRes success: Int?,
        @StringRes failure: Int,
        /** Invitation Accepted or Declined, sent when it worked; null for none. */
        event: String?
    ) {
        val id = item.record.id
        if (id in busy) return
        busy = busy + id
        viewLifecycleOwner.lifecycleScope.launch {
            val response = call(id)
            // When the invitation goes away, its button keeps spinning until the new list replaces
            // it; otherwise it would come back for the length of the reload.
            val done = { busy = busy - id }
            when {
                response is Response.Success -> {
                    Analytics.drawerAction()
                    event?.let { trackAnswer(it, item.record) }
                    success?.let { show(it) }
                    load(showSpinner = false, then = done)
                }
                (response as? Response.Failure)?.exception?.invitationError()?.noLongerValid == true -> {
                    show(R.string.invitation_no_longer_valid)
                    load(showSpinner = false, then = done)
                }
                else -> {
                    done()
                    show(failure)
                }
            }
        }
    }

    private fun trackAnswer(event: String, invitation: InvitationRecord) {
        Analytics.track(
            event,
            AnalyticsProps.invitationAnsweredProps(
                invitationID = invitation.id,
                role = invitation.userRole,
                organizationID = invitation.organizationID,
                centerID = invitation.centerID,
                teamID = invitation.teamID,
                dateSentSeconds = invitation.dateSent,
                nowMs = System.currentTimeMillis(),
            ) + mapOf("feature" to NOTIFICATIONS)
        )
    }

    private fun show(@StringRes message: Int) {
        viewLifecycleOwner.lifecycleScope.launch { snackbar.showSnackbar(getString(message)) }
    }

    private fun openPolicy(item: InvitationItem) {
        val url = if (InvitationRules.isAdminRole(item.record.userRole)) UrlConstants.URL_POLICY_ADMIN else UrlConstants.URL_POLICY
        if (!requireContext().openInAppBrowser(url)) show(R.string.error_no_browser)
    }

    companion object {
        const val TAG = "invitations_sheet"
    }
}

/** The [InvitationException] behind [this] failure, if any (a transaction may wrap it). */
private fun Throwable.invitationError(): InvitationException? =
    generateSequence(this) { it.cause }.filterIsInstance<InvitationException>().firstOrNull()

@Composable
fun InvitationsContent(
    state: InvitationsState,
    busy: Set<String>,
    agreed: Set<String>,
    now: Long,
    snackbar: SnackbarHostState,
    showAll: Boolean,
    onShowAll: () -> Unit,
    onAgree: (String, Boolean) -> Unit,
    onOpenPolicy: (InvitationItem) -> Unit,
    onAccept: (InvitationItem) -> Unit,
    onDecline: (InvitationItem) -> Unit,
    onDismiss: (InvitationItem) -> Unit,
    onRetry: () -> Unit
) {
    Box {
        SettingsSheetSurface(title = stringResource(R.string.invitations_title)) {
            when (state) {
                InvitationsState.Loading -> Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colorResource(R.color.kamleon_blue), modifier = Modifier.size(32.dp))
                }
                InvitationsState.Failed -> {
                    Message(stringResource(R.string.invitations_error))
                    Spacer(Modifier.height(24.dp))
                    KamleonRoundButton(text = stringResource(R.string.retry), onClick = onRetry, enabled = true, testTag = "invitations_retry")
                }
                is InvitationsState.Loaded -> if (state.items.isEmpty()) {
                    Message(stringResource(R.string.invitations_empty))
                } else {
                    val (previous, pending) = state.items.partition { it.isPrevious }
                    // Before "See all", only the ones accepted in the last days.
                    val shownPrevious = if (showAll) previous else previous.filter { it.recent }
                    Column(
                        Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth()
                            .nestedScroll(rememberNestedScrollInteropConnection())
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        pending.forEach { item ->
                            InvitationCard(
                                item = item,
                                now = now,
                                busy = item.record.id in busy,
                                agreed = item.record.id in agreed,
                                onAgree = { onAgree(item.record.id, it) },
                                onOpenPolicy = { onOpenPolicy(item) },
                                onAccept = { onAccept(item) },
                                onDecline = { onDecline(item) },
                                onDismiss = { onDismiss(item) }
                            )
                        }
                        if (previous.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.invitations_previous),
                                style = kamleonBodySmall().copy(fontWeight = FontWeight.Bold),
                                color = colorResource(R.color.kamleon_dark_grey),
                                modifier = Modifier.padding(top = if (pending.isEmpty()) 0.dp else 12.dp)
                            )
                            shownPrevious.forEach { InvitationCard(it, now, false, false, {}, {}, {}, {}, {}) }
                            if (shownPrevious.size < previous.size) {
                                KamleonTextLink(
                                    text = stringResource(R.string.invitations_see_all),
                                    onClick = onShowAll,
                                    style = kamleonBodySmall(),
                                    color = colorResource(R.color.kamleon_blue),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                                    modifier = Modifier.align(Alignment.CenterHorizontally).testTag("invitations_see_all")
                                )
                            }
                        }
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }
}

@Composable
private fun Message(text: String) {
    Text(
        text = text,
        style = kamleonBodySmall(),
        color = colorResource(R.color.kamleon_secondary_grey_60),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp).testTag("invitations_message")
    )
}

@Composable
private fun InvitationCard(
    item: InvitationItem,
    now: Long,
    busy: Boolean,
    agreed: Boolean,
    onAgree: (Boolean) -> Unit,
    onOpenPolicy: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onDismiss: () -> Unit
) {
    val grey = colorResource(R.color.kamleon_secondary_grey_60)
    val dark = colorResource(R.color.kamleon_dark_grey)
    val teal = colorResource(R.color.kamleon_blue)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colorResource(R.color.white),
        border = BorderStroke(1.dp, colorResource(R.color.kamleon_outline)),
        modifier = Modifier.fillMaxWidth().testTag("invitation_card")
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                if (item.unread) {
                    Box(Modifier.padding(top = 6.dp, end = 8.dp).size(8.dp).background(teal, CircleShape))
                }
                Text(
                    text = invitationText(item),
                    style = kamleonBodySmall().copy(fontWeight = FontWeight.Bold),
                    color = dark,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                item.record.dateSent?.let {
                    Text(text = daysAgo(it, now), style = kamleonCaption(), color = grey)
                }
            }
            item.context?.let {
                Text(text = it, style = kamleonBodySmall(), color = grey, modifier = Modifier.padding(top = 2.dp))
            }
            when (item.state) {
                InvitationItem.State.ACCEPTED -> Pill(stringResource(R.string.invitation_accepted), Modifier.padding(top = 10.dp))
                InvitationItem.State.DECLINED -> Pill(stringResource(R.string.invitation_declined_state), Modifier.padding(top = 10.dp))
                InvitationItem.State.DISMISSED -> Pill(stringResource(R.string.invitation_expired), Modifier.padding(top = 10.dp))
                InvitationItem.State.EXPIRED -> {
                    Pill(stringResource(R.string.invitation_expired), Modifier.padding(top = 10.dp))
                    Spacer(Modifier.height(12.dp))
                    KamleonRoundButton(
                        text = stringResource(R.string.invitation_dismiss),
                        onClick = onDismiss,
                        enabled = !busy,
                        loading = busy,
                        testTag = "invitation_dismiss"
                    )
                }
                InvitationItem.State.PENDING -> {
                    InvitationRules.expiresAt(item.record)?.let {
                        Text(text = expiresIn(it, now), style = kamleonCaption(), color = grey, modifier = Modifier.padding(top = 6.dp))
                    }
                    PrivacyBox(agreed = agreed, enabled = !busy, onAgree = onAgree, onOpenPolicy = onOpenPolicy)
                    KamleonRoundButton(
                        text = stringResource(if (busy) R.string.invitation_loading else R.string.invitation_accept),
                        onClick = onAccept,
                        enabled = agreed && !busy,
                        testTag = "invitation_accept"
                    )
                    Spacer(Modifier.height(4.dp))
                    KamleonTextLink(
                        text = stringResource(R.string.invitation_decline),
                        onClick = { if (!busy) onDecline() },
                        style = kamleonBodySmall(),
                        color = if (busy) grey else teal,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                        modifier = Modifier.align(Alignment.CenterHorizontally).testTag("invitation_decline")
                    )
                }
            }
        }
    }
}

@Composable
private fun PrivacyBox(agreed: Boolean, enabled: Boolean, onAgree: (Boolean) -> Unit, onOpenPolicy: () -> Unit) {
    val link = stringResource(R.string.invitation_privacy_link)
    val full = stringResource(R.string.invitation_privacy, link)
    val start = full.indexOf(link)
    val dark = colorResource(R.color.kamleon_dark_grey)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Checkbox(
            checked = agreed,
            onCheckedChange = onAgree,
            enabled = enabled,
            colors = CheckboxDefaults.colors(checkedColor = colorResource(R.color.kamleon_blue)),
            modifier = Modifier.testTag("invitation_privacy")
        )
        Text(
            text = buildAnnotatedString {
                append(full.substring(0, start.coerceAtLeast(0)))
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)) { append(link) }
                if (start >= 0) append(full.substring(start + link.length))
            },
            style = kamleonBodySmall(),
            color = dark,
            modifier = Modifier.clickable(onClick = onOpenPolicy).padding(vertical = 8.dp)
        )
    }
}

@Composable
private fun Pill(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = kamleonCaption(),
        color = colorResource(R.color.kamleon_dark_grey),
        modifier = modifier
            .background(colorResource(R.color.kamleon_dark_grey_05), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

/** "Someone invited you to join Primer equipo as Team user", or "You joined …" once accepted. */
@Composable
private fun invitationText(item: InvitationItem): String {
    val role = RoleKind.of(item.record.userRole)?.let { stringResource(it.label) } ?: item.record.userRole
    val entity = item.entityName?.takeIf { it.isNotBlank() }
    return if (item.state == InvitationItem.State.ACCEPTED) {
        if (entity != null) stringResource(R.string.invitation_accepted_text, entity, role)
        else stringResource(R.string.invitation_accepted_text_no_entity, role)
    } else {
        val sender = item.senderName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.invitation_someone)
        if (entity != null) stringResource(R.string.invitation_text, sender, entity, role)
        else stringResource(R.string.invitation_text_no_entity, sender, role)
    }
}

/** Today, Yesterday or "N days ago" in the first week; after that the date, as Settings writes it ("24 Nov 2025"). */
@Composable
private fun daysAgo(then: Long, now: Long): String {
    val zone = ZoneId.systemDefault()
    val days = InvitationRules.daysBetween(then, now, zone).toInt()
    return when {
        days <= 0 -> stringResource(R.string.invitation_today)
        days == 1 -> stringResource(R.string.invitation_yesterday)
        days < DAYS_AGO_LIMIT -> pluralStringResource(R.plurals.invitation_days_ago, days, days)
        else -> Instant.ofEpochSecond(then).atZone(zone).toLocalDate()
            .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }
}

/** From a week on, a date reads better than a count of days. */
private const val DAYS_AGO_LIMIT = 7

@Composable
private fun expiresIn(expiresAt: Long, now: Long): String {
    val days = InvitationRules.daysBetween(now, expiresAt, ZoneId.systemDefault()).toInt()
    return if (days <= 0) stringResource(R.string.invitation_expires_today)
    else pluralStringResource(R.plurals.invitation_expires_in, days, days)
}
