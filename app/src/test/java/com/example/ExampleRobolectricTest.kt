package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
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
    assertEquals("Ігри на двох", appName)
  }

  @Test
  fun `verify four mini games are configured`() {
    val games = com.example.model.GameType.entries
    assertEquals(4, games.size)
    val bombActions = com.example.model.BombAction.entries
    assertEquals(4, bombActions.size)
  }
}
