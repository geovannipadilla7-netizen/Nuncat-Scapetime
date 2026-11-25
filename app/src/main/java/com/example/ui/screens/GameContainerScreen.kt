package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.GameUiState
import com.example.ui.GameViewModel
import com.example.ui.WebError
import com.example.ui.components.WebGameController
import com.example.ui.components.WebGameView
import com.example.ui.theme.AshMuted
import com.example.ui.theme.BloodOutline
import com.example.ui.theme.CatacombElevated
import com.example.ui.theme.CatacombSurface
import com.example.ui.theme.CatacombSurfaceVariant
import com.example.ui.theme.CrimsonDeep
import com.example.ui.theme.CrimsonGlow
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.EmeraldOnline
import com.example.ui.theme.MoonlightText
import com.example.ui.theme.ObsidianBg

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameContainerScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val webController = remember { WebGameController() }

    var customHtml5View by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    val closeCustomView = {
        customViewCallback?.onCustomViewHidden()
        customHtml5View = null
        customViewCallback = null
    }

    BackHandler(enabled = true) {
        when {
            customHtml5View != null -> closeCustomView()
            uiState.isMenuExpanded -> viewModel.setMenuExpanded(false)
            uiState.canGoBack -> webController.goBack()
            else -> viewModel.setShowExitDialog(true)
        }
    }

    val openInBrowser: (String) -> Unit = { urlToOpen ->
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlToOpen)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            // No browser available
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg),
    ) {
        val webContainerModifier = if (uiState.preferences.immersiveMode) {
            Modifier.fillMaxSize()
        } else {
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        }

        WebGameView(
            initialUrl = uiState.targetUrl,
            controller = webController,
            onPageStarted = viewModel::onPageStarted,
            onPageFinished = viewModel::onPageFinished,
            onProgressChanged = viewModel::onProgressChanged,
            onTitleReceived = viewModel::onTitleReceived,
            onNavigationStateChanged = viewModel::onNavigationStateChanged,
            onMainFrameError = viewModel::onMainFrameError,
            onShowCustomView = { view, callback ->
                customHtml5View = view
                customViewCallback = callback
            },
            onHideCustomView = {
                customHtml5View = null
                customViewCallback = null
            },
            modifier = webContainerModifier,
        )

        // Subtle top progress bar when navigating inside the game after initial boot
        if (!uiState.isInitialLoading && uiState.isPageLoading && uiState.progress in 1..99) {
            LinearProgressIndicator(
                progress = { uiState.progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (uiState.preferences.immersiveMode) Modifier
                        else Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
                    )
                    .height(2.5.dp),
                color = CrimsonGlow,
                trackColor = Color.Transparent,
            )
        }

        // Connection / Load error state
        AnimatedVisibility(
            visible = uiState.errorState != null,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(200)),
        ) {
            uiState.errorState?.let { error ->
                GameErrorOverlay(
                    error = error,
                    isOnline = uiState.isOnline,
                    onRetry = {
                        viewModel.clearErrorForRetry()
                        webController.reload(uiState.targetUrl)
                    },
                    onOpenExternal = { openInBrowser(uiState.targetUrl) },
                )
            }
        }

        // Atmospheric Gothic Boot / Loading Portal
        AnimatedVisibility(
            visible = uiState.isInitialLoading && uiState.errorState == null,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(450)),
        ) {
            InitialGamePortalOverlay(
                progress = uiState.progress,
                onSkipToGame = viewModel::dismissInitialOverlay,
            )
        }

        // HTML5 Fullscreen video/canvas container
        customHtml5View?.let { customView ->
            AndroidView(
                factory = { ctx ->
                    FrameLayout(ctx).apply {
                        setBackgroundColor(android.graphics.Color.BLACK)
                        (customView.parent as? ViewGroup)?.removeView(customView)
                        addView(
                            customView,
                            FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            ),
                        )
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Floating quick game control handle (respects notches/cutouts)
        if (!uiState.isInitialLoading && customHtml5View == null) {
            FloatingGameControlButton(
                isLoading = uiState.isPageLoading,
                isOnline = uiState.isOnline,
                onClick = { viewModel.setMenuExpanded(true) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(top = 10.dp, end = 12.dp),
            )
        }

        // Quick Game Menu Bottom Sheet
        if (uiState.isMenuExpanded) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { viewModel.setMenuExpanded(false) },
                sheetState = sheetState,
                containerColor = CatacombSurface,
                contentColor = MoonlightText,
                scrimColor = ObsidianBg.copy(alpha = 0.72f),
            ) {
                GameControlMenuSheet(
                    uiState = uiState,
                    onBack = {
                        webController.goBack()
                        viewModel.setMenuExpanded(false)
                    },
                    onForward = {
                        webController.goForward()
                        viewModel.setMenuExpanded(false)
                    },
                    onReload = {
                        viewModel.clearErrorForRetry()
                        webController.reload(uiState.targetUrl)
                        viewModel.setMenuExpanded(false)
                    },
                    onHome = {
                        viewModel.clearErrorForRetry()
                        webController.loadHome(uiState.targetUrl)
                        viewModel.setMenuExpanded(false)
                    },
                    onOpenExternal = {
                        openInBrowser(uiState.currentUrl)
                        viewModel.setMenuExpanded(false)
                    },
                    onToggleImmersive = viewModel::toggleImmersiveMode,
                    onToggleKeepScreenOn = viewModel::toggleKeepScreenOn,
                )
            }
        }

        // Exit Confirmation Dialog
        if (uiState.showExitDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.setShowExitDialog(false) },
                containerColor = CatacombSurface,
                titleContentColor = MoonlightText,
                textContentColor = AshMuted,
                title = {
                    Text(
                        text = stringResource(R.string.exit_dialog_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.exit_dialog_message),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.setShowExitDialog(false)
                            (context as? Activity)?.finish()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CrimsonPrimary,
                            contentColor = MoonlightText,
                        ),
                        modifier = Modifier.testTag("exit_confirm_button"),
                    ) {
                        Text(stringResource(R.string.exit_dialog_confirm))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.setShowExitDialog(false) },
                        modifier = Modifier.testTag("exit_cancel_button"),
                    ) {
                        Text(
                            text = stringResource(R.string.exit_dialog_cancel),
                            color = MoonlightText,
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun FloatingGameControlButton(
    isLoading: Boolean,
    isOnline: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .testTag("menu_fab_button"),
        shape = CircleShape,
        color = CatacombSurface.copy(alpha = 0.78f),
        border = BorderStroke(
            width = 1.dp,
            color = if (isOnline) BloodOutline else CrimsonGlow,
        ),
        shadowElevation = 6.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = stringResource(R.string.menu_button_cd),
                tint = MoonlightText,
                modifier = Modifier.size(20.dp),
            )
            if (isLoading || !isOnline) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(9.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (!isOnline) CrimsonGlow else CrimsonPrimary),
                )
            }
        }
    }
}

@Composable
private fun InitialGamePortalOverlay(
    progress: Int,
    onSkipToGame: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "portal_pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow_alpha",
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        CrimsonDeep.copy(alpha = glowAlpha * 0.45f),
                        ObsidianBg,
                    ),
                ),
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        val isLandscapeCompact = maxWidth > maxHeight && maxHeight < 520.dp

        if (isLandscapeCompact) {
            Row(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
            ) {
                Box(
                    modifier = Modifier
                        .size(188.dp)
                        .shadow(
                            elevation = 24.dp,
                            shape = RoundedCornerShape(28.dp),
                            ambientColor = CrimsonGlow,
                            spotColor = CrimsonPrimary,
                        )
                        .clip(RoundedCornerShape(28.dp))
                        .border(
                            width = 1.5.dp,
                            color = CrimsonPrimary.copy(alpha = glowAlpha),
                            shape = RoundedCornerShape(28.dp),
                        ),
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.nuncat_cover),
                        contentDescription = stringResource(R.string.cover_art_cd),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(R.string.app_name).uppercase(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MoonlightText,
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        color = CrimsonDeep.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, BloodOutline),
                    ) {
                        Text(
                            text = stringResource(R.string.game_subtitle).uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = CrimsonGlow,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                        )
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    LinearProgressIndicator(
                        progress = { (progress.coerceAtLeast(8)) / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50)),
                        color = CrimsonPrimary,
                        trackColor = CatacombSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.loading_portal),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AshMuted,
                        )
                        Text(
                            text = stringResource(R.string.loading_progress_percent, progress),
                            style = MaterialTheme.typography.labelLarge,
                            color = CrimsonGlow,
                        )
                    }

                    if (progress >= 35) {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = onSkipToGame,
                            border = BorderStroke(1.dp, BloodOutline),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("enter_game_button"),
                        ) {
                            Text(
                                text = stringResource(R.string.show_game_now),
                                color = MoonlightText,
                            )
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .shadow(
                            elevation = 24.dp,
                            shape = RoundedCornerShape(32.dp),
                            ambientColor = CrimsonGlow,
                            spotColor = CrimsonPrimary,
                        )
                        .clip(RoundedCornerShape(32.dp))
                        .border(
                            width = 1.5.dp,
                            color = CrimsonPrimary.copy(alpha = glowAlpha),
                            shape = RoundedCornerShape(32.dp),
                        ),
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.nuncat_cover),
                        contentDescription = stringResource(R.string.cover_art_cd),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = stringResource(R.string.app_name).uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MoonlightText,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    color = CrimsonDeep.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, BloodOutline),
                ) {
                    Text(
                        text = stringResource(R.string.game_subtitle).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = CrimsonGlow,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                LinearProgressIndicator(
                    progress = { (progress.coerceAtLeast(8)) / 100f },
                    modifier = Modifier
                        .fillMaxWidth(0.78f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(50)),
                    color = CrimsonPrimary,
                    trackColor = CatacombSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.loading_portal),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AshMuted,
                    )
                    Text(
                        text = stringResource(R.string.loading_progress_percent, progress),
                        style = MaterialTheme.typography.labelLarge,
                        color = CrimsonGlow,
                    )
                }

                if (progress >= 35) {
                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedButton(
                        onClick = onSkipToGame,
                        border = BorderStroke(1.dp, BloodOutline),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("enter_game_button"),
                    ) {
                        Text(
                            text = stringResource(R.string.show_game_now),
                            color = MoonlightText,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GameErrorOverlay(
    error: WebError,
    isOnline: Boolean,
    onRetry: () -> Unit,
    onOpenExternal: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            color = CatacombSurface,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, BloodOutline),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(CrimsonDeep.copy(alpha = 0.35f))
                        .border(1.dp, BloodOutline, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = CrimsonGlow,
                        modifier = Modifier.size(34.dp),
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = stringResource(R.string.error_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MoonlightText,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(10.dp))

                val message = if (!isOnline) {
                    stringResource(R.string.error_offline_message)
                } else {
                    stringResource(
                        R.string.error_generic_message,
                        error.description.ifBlank { error.code.toString() },
                    )
                }

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AshMuted,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("retry_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CrimsonPrimary,
                        contentColor = MoonlightText,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.action_retry),
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onOpenExternal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("open_browser_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, CatacombElevated),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = AshMuted,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.action_open_browser),
                        color = MoonlightText,
                    )
                }
            }
        }
    }
}

@Composable
private fun GameControlMenuSheet(
    uiState: GameUiState,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onHome: () -> Unit,
    onOpenExternal: () -> Unit,
    onToggleImmersive: (Boolean) -> Unit,
    onToggleKeepScreenOn: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
    ) {
        // Header card with game artwork & live status
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(id = R.drawable.nuncat_cover),
                contentDescription = stringResource(R.string.cover_art_cd),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, BloodOutline, RoundedCornerShape(14.dp)),
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    color = MoonlightText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = uiState.currentUrl,
                    style = MaterialTheme.typography.labelMedium,
                    color = AshMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = if (uiState.isOnline) {
                    EmeraldOnline.copy(alpha = 0.14f)
                } else {
                    CrimsonGlow.copy(alpha = 0.16f)
                },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (uiState.isOnline) EmeraldOnline else CrimsonGlow),
                    )
                    Text(
                        text = stringResource(
                            if (uiState.isOnline) R.string.status_online else R.string.status_offline,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (uiState.isOnline) EmeraldOnline else CrimsonGlow,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Navigation & quick actions row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FilledTonalIconButton(
                onClick = onBack,
                enabled = uiState.canGoBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("back_button"),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = CatacombSurfaceVariant,
                    contentColor = MoonlightText,
                ),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                )
            }

            FilledTonalIconButton(
                onClick = onForward,
                enabled = uiState.canGoForward,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("forward_button"),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = CatacombSurfaceVariant,
                    contentColor = MoonlightText,
                ),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.action_forward),
                )
            }

            Button(
                onClick = onReload,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("reload_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CrimsonPrimary,
                    contentColor = MoonlightText,
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.action_reload),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            FilledTonalIconButton(
                onClick = onHome,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("home_button"),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = CatacombSurfaceVariant,
                    contentColor = MoonlightText,
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = stringResource(R.string.action_home),
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
        HorizontalDivider(color = CatacombElevated)
        Spacer(modifier = Modifier.height(14.dp))

        // Immersive mode switch
        MenuSwitchRow(
            icon = Icons.Default.Fullscreen,
            title = stringResource(R.string.setting_immersive),
            subtitle = stringResource(R.string.setting_immersive_desc),
            checked = uiState.preferences.immersiveMode,
            onCheckedChange = onToggleImmersive,
            testTag = "immersive_switch",
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Keep screen on switch
        MenuSwitchRow(
            icon = Icons.Default.LightMode,
            title = stringResource(R.string.setting_keep_screen_on),
            subtitle = stringResource(R.string.setting_keep_screen_on_desc),
            checked = uiState.preferences.keepScreenOn,
            onCheckedChange = onToggleKeepScreenOn,
            testTag = "keep_screen_switch",
        )

        Spacer(modifier = Modifier.height(18.dp))

        OutlinedButton(
            onClick = onOpenExternal,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("menu_open_browser_button"),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, BloodOutline),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = CrimsonGlow,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.action_open_browser),
                color = MoonlightText,
            )
        }
    }
}

@Composable
private fun MenuSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = CatacombSurfaceVariant,
            modifier = Modifier.size(40.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CrimsonGlow,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MoonlightText,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = AshMuted,
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = MoonlightText,
                checkedTrackColor = CrimsonPrimary,
                uncheckedThumbColor = AshMuted,
                uncheckedTrackColor = CatacombSurfaceVariant,
                uncheckedBorderColor = CatacombElevated,
            ),
        )
    }
}
