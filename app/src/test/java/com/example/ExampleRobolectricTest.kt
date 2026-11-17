package com.example

import android.content.Context
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import com.example.core.common.AppResult
import com.example.core.common.getOrNull
import com.example.core.common.map
import com.example.core.common.safeSuspendCall
import com.example.core.designsystem.SidebarDockSide
import com.example.core.designsystem.bookSpreadPair
import com.example.core.designsystem.sidebarDockSideFor
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Qalam", appName)
    }

    @Test
    fun `directional helpers mirror sidebar and book spreads for RTL`() {
        assertEquals(SidebarDockSide.LEFT, sidebarDockSideFor(LayoutDirection.Ltr))
        assertEquals(SidebarDockSide.RIGHT, sidebarDockSideFor(LayoutDirection.Rtl))

        assertEquals(1 to 2, bookSpreadPair(1, 2, LayoutDirection.Ltr))
        assertEquals(2 to 1, bookSpreadPair(1, 2, LayoutDirection.Rtl))
    }

    @Test
    fun `safeSuspendCall maps success and error cleanly`() = runTest {
        val ok = safeSuspendCall { 21 }.map { it * 2 }
        assertTrue(ok is AppResult.Success)
        assertEquals(42, ok.getOrNull())

        val fail = safeSuspendCall<Int> { throw IllegalStateException("boom") }
        assertTrue(fail is AppResult.Error)
    }
}
