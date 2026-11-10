package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("WatchAccurancy", appName)
  }

  @Test
  fun `test reminder manager settings persistence`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    de.uhrwerksarchiv.watchaccurancy.reminder.ReminderManager.setReminderEnabled(context, true, 21, 30)

    val isEnabled = de.uhrwerksarchiv.watchaccurancy.reminder.ReminderManager.isReminderEnabled(context)
    val time = de.uhrwerksarchiv.watchaccurancy.reminder.ReminderManager.getReminderTime(context)

    assertEquals(true, isEnabled)
    assertEquals(21, time.first)
    assertEquals(30, time.second)

    // Disable reminder
    de.uhrwerksarchiv.watchaccurancy.reminder.ReminderManager.setReminderEnabled(context, false)
    assertEquals(false, de.uhrwerksarchiv.watchaccurancy.reminder.ReminderManager.isReminderEnabled(context))
  }
}
