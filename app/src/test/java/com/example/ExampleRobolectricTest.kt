package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.GamePreferencesRepository
import com.example.ui.GameUiState
import com.example.ui.GameViewModel
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
  fun `read app name from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Nuncat Scapetime", appName)
  }

  @Test
  fun `initial game state points to real game url`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val viewModel = GameViewModel(
      preferencesRepository = GamePreferencesRepository(context),
      connectivityManager = null,
    )
    val state = viewModel.uiState.value
    assertEquals("https://nuncatscapetime-editionbeta.ai.studio/", state.targetUrl)
    assertEquals(GameUiState.GAME_URL, state.currentUrl)
    assertTrue(state.isInitialLoading)
  }
}
