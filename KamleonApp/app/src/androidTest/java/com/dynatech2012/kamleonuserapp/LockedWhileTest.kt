package com.dynatech2012.kamleonuserapp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.dynatech2012.kamleonuserapp.views.lockedWhile
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LockedWhileTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun tapsReachTheContentOnlyWhenNotLocked() {
        var locked by mutableStateOf(true)
        var taps = 0
        rule.setContent {
            Box(Modifier.lockedWhile(locked)) {
                Box(Modifier.size(100.dp).clickable { taps++ }.testTag("content"))
            }
        }

        rule.onNodeWithTag("content").performClick()
        rule.runOnIdle { assertEquals(0, taps) }

        locked = false
        rule.onNodeWithTag("content").performClick()
        rule.runOnIdle { assertEquals(1, taps) }
    }
}
