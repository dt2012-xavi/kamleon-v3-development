package com.kamleonapp

import com.dynatech2012.kamleonuserapp.utils.NotificationAsk
import com.dynatech2012.kamleonuserapp.utils.notificationAskFor
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationAskTest {
    private fun ask(
        sdkInt: Int = 34,
        alreadyAsked: Boolean = false,
        granted: Boolean = false,
        fromScanLink: Boolean = false
    ) = notificationAskFor(sdkInt, alreadyAsked, granted, fromScanLink)

    @Test fun neverAsksTwice() {
        listOf(false, true).forEach { granted ->
            listOf(false, true).forEach { fromScanLink ->
                assertEquals(NotificationAsk.Skip, ask(alreadyAsked = true, granted = granted, fromScanLink = fromScanLink))
            }
        }
    }

    @Test fun androidBefore13NeedsNoPermissionSoItOnlyMarksIt() {
        assertEquals(NotificationAsk.MarkAsked, ask(sdkInt = 32))
        assertEquals(NotificationAsk.MarkAsked, ask(sdkInt = 32, fromScanLink = true))
    }

    @Test fun alreadyGrantedOnlyMarksIt() {
        assertEquals(NotificationAsk.MarkAsked, ask(granted = true))
        assertEquals(NotificationAsk.MarkAsked, ask(granted = true, fromScanLink = true))
    }

    @Test fun firstRequestIsExplainedBeforeTheSystemDialog() {
        assertEquals(NotificationAsk.ExplainFirst, ask(sdkInt = 33))
        assertEquals(NotificationAsk.ExplainFirst, ask(sdkInt = 36))
    }

    @Test fun aScanLinkDoesNotStackASecondSheetOverTheScanResult() {
        // It is asked the next time Main opens: the flag stays unset.
        assertEquals(NotificationAsk.Skip, ask(fromScanLink = true))
    }
}
