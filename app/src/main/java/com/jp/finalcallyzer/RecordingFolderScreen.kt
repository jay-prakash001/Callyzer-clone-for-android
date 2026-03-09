package com.jp.finalcallyzer.ui.recording

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.ExitToApp
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jp.finalcallyzer.AudioFile
import com.jp.finalcallyzer.AudioPlayerState
import com.jp.finalcallyzer.CallViewModel
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingFolderScreen(
    viewModel: CallViewModel = koinViewModel(),
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val state   by viewModel.state.collectAsStateWithLifecycle()
    val player  = remember { AudioPlayerState() }

    LaunchedEffect(player.isPlaying) {
        while (player.isPlaying) {
            if (!player.isSeeking) {
                player.currentMs = player.player?.currentPosition?.toFloat() ?: 0f
            }
            delay(500)
        }
    }

    DisposableEffect(Unit) {
        viewModel.init(context)
        onDispose { player.stop() }
    }

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) viewModel.onFolderSelected(context, uri)
    }

    Scaffold(
        containerColor = Color(0xFF080B12),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Recordings",
                            fontWeight = FontWeight.Bold,
                            color      = Color.White,
                            fontSize   = 18.sp,
                        )
                        if (state.folderUri.isNotBlank()) {
                            Text(
                                state.folderName,
                                fontSize = 11.sp,
                                color    = Color(0xFF00E5A0),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshFiles() }) {
                        Icon(Icons.Default.Refresh, null, tint = Color.White.copy(alpha = 0.7f))
                    }
                    IconButton(onClick = { folderPicker.launch(null) }) {
                        Icon(Icons.Default.MailOutline, null, tint = Color(0xFF00E5A0))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D1017))
            )
        }
    ) { padding ->

        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.folderUri.isBlank() ->
                    EmptyFolderState(onPickFolder = { folderPicker.launch(null) })

                state.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = Color(0xFF00E5A0))
                            Text("Scanning folder…", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
                        }
                    }
                }

                state.error != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Close, null, tint = Color(0xFFFF4060), modifier = Modifier.size(48.dp))
                            Text(state.error!!, color = Color(0xFFFF4060), fontSize = 13.sp)
                            TextButton(onClick = { folderPicker.launch(null) }) {
                                Text("Pick Another Folder", color = Color(0xFF00E5A0))
                            }
                        }
                    }
                }

                state.audioFiles.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Person, null, tint = Color.White.copy(alpha = 0.15f), modifier = Modifier.size(56.dp))
                            Text("No audio files found", color = Color.White.copy(alpha = 0.4f), fontSize = 14.sp)
                            Text("in ${state.folderName}", color = Color.White.copy(alpha = 0.25f), fontSize = 12.sp)
                            TextButton(onClick = { folderPicker.launch(null) }) {
                                Text("Change Folder", color = Color(0xFF00E5A0))
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        item {
                            Text(
                                "${state.audioFiles.size} recordings",
                                color    = Color.White.copy(alpha = 0.35f),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                            )
                        }
                        items(state.audioFiles, key = { it.uri.toString() }) { file ->
                            AudioFileCard(
                                file        = file,
                                playerState = player,
                                isUploading = file.uri.toString() in state.uploadingIds,
                                isUploaded  = file.uri.toString() in state.uploadedIds,
                                onPlay      = { player.play(context, file.uri, file.durationMs) },
                                onUpload    = { viewModel.uploadFile(context, file) },
                            )
                        }
                        item { Spacer(Modifier.height(32.dp)) }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  EMPTY STATE
// ═══════════════════════════════════════════════════════════════

@Composable
fun EmptyFolderState(onPickFolder: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(40.dp)
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.9f,
                targetValue  = 1.05f,
                animationSpec = infiniteRepeatable(
                    tween(1200, easing = FastOutSlowInEasing),
                    RepeatMode.Reverse
                ),
                label = "scale"
            )

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF00E5A0).copy(alpha = 0.2f), Color.Transparent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.PlayArrow, null,
                    tint     = Color(0xFF00E5A0),
                    modifier = Modifier.size(48.dp)
                )
            }

            Text("Select Recording Folder", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                "Choose the folder where your call recordings are saved. All audio files will be listed here.",
                color      = Color.White.copy(alpha = 0.45f),
                fontSize   = 13.sp,
                textAlign  = TextAlign.Center,
                lineHeight = 20.sp,
            )

            Button(
                onClick  = onPickFolder,
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5A0)),
                shape    = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Default.Email, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Browse Folder", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  AUDIO FILE CARD
// ═══════════════════════════════════════════════════════════════

@Composable
fun AudioFileCard(
    file: AudioFile,
    playerState: AudioPlayerState,
    isUploading: Boolean,
    isUploaded:  Boolean,
    onPlay:      () -> Unit,
    onUpload:    () -> Unit,
) {
    val isThisPlaying = playerState.playingUri == file.uri
    val isPlaying     = isThisPlaying && playerState.isPlaying

    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val bar1 by infiniteTransition.animateFloat(0.3f, 1.0f, infiniteRepeatable(tween(400), RepeatMode.Reverse), label = "b1")
    val bar2 by infiniteTransition.animateFloat(0.6f, 0.2f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "b2")
    val bar3 by infiniteTransition.animateFloat(0.2f, 0.9f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "b3")
    val bar4 by infiniteTransition.animateFloat(0.7f, 0.3f, infiniteRepeatable(tween(350), RepeatMode.Reverse), label = "b4")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(
            containerColor = if (isThisPlaying) Color(0xFF0D1F1A) else Color(0xFF0E1118)
        ),
        border = BorderStroke(
            1.dp,
            if (isThisPlaying) Color(0xFF00E5A0).copy(alpha = 0.3f)
            else               Color.White.copy(alpha = 0.05f)
        )
    ) {
        Column(Modifier.padding(16.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {

                // Waveform / icon
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isThisPlaying) Color(0xFF00E5A0).copy(alpha = 0.15f)
                            else               Color.White.copy(alpha = 0.05f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPlaying) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            modifier              = Modifier.height(24.dp)
                        ) {
                            listOf(bar1, bar2, bar3, bar4).forEach { frac ->
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .fillMaxHeight(frac)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color(0xFF00E5A0))
                                )
                            }
                        }
                    } else {
                        Icon(
                            Icons.Rounded.Star, null,
                            tint     = if (isThisPlaying) Color(0xFF00E5A0) else Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        file.name,
                        color      = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 13.sp,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(formatDateTime(file.dateTime), color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                        Text("•", color = Color.White.copy(alpha = 0.2f), fontSize = 11.sp)
                        Text(
                            formatDurationMs(file.durationMs),
                            color      = if (isThisPlaying) Color(0xFF00E5A0) else Color.White.copy(alpha = 0.4f),
                            fontSize   = 11.sp,
                            fontWeight = if (isThisPlaying) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Play button
                IconButton(
                    onClick  = onPlay,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (isThisPlaying) Color(0xFF00E5A0) else Color(0xFF1A2030)
                        )
                ) {
                    Icon(
                        if (isPlaying) Icons.Rounded.ExitToApp else Icons.Rounded.PlayArrow,
                        null,
                        tint     = if (isThisPlaying) Color.Black else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(6.dp))

                // Upload button
                IconButton(
                    onClick  = { if (!isUploaded && !isUploading) onUpload() },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isUploaded  -> Color(0xFF0077FF).copy(alpha = 0.2f)
                                isUploading -> Color(0xFFFFB300).copy(alpha = 0.2f)
                                else        -> Color(0xFF1A2030)
                            }
                        )
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            color       = Color(0xFFFFB300),
                            modifier    = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            if (isUploaded) Icons.Rounded.Refresh else Icons.Rounded.Close,
                            null,
                            tint     = if (isUploaded) Color(0xFF0077FF) else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Player expand
            AnimatedVisibility(
                visible = isThisPlaying,
                enter   = fadeIn() + expandVertically(),
                exit    = fadeOut() + shrinkVertically(),
            ) {
                Column(modifier = Modifier.padding(top = 14.dp)) {

                    HorizontalDivider(color = Color(0xFF00E5A0).copy(alpha = 0.1f), modifier = Modifier.padding(bottom = 12.dp))

                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(formatDurationMs(playerState.currentMs.toLong()), color = Color(0xFF00E5A0), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Text(formatDurationMs(file.durationMs), color = Color.White.copy(alpha = 0.35f), fontSize = 11.sp)
                    }

                    Slider(
                        value             = playerState.currentMs,
                        onValueChange     = { v ->
                            playerState.isSeeking = true
                            playerState.currentMs = v
                        },
                        onValueChangeFinished = {
                            playerState.seekTo(playerState.currentMs)
                            playerState.isSeeking = false
                        },
                        valueRange = 0f..playerState.totalMs.coerceAtLeast(1f),
                        modifier   = Modifier.fillMaxWidth(),
                        colors     = SliderDefaults.colors(
                            thumbColor         = Color(0xFF00E5A0),
                            activeTrackColor   = Color(0xFF00E5A0),
                            inactiveTrackColor = Color.White.copy(alpha = 0.1f),
                        )
                    )

                    Spacer(Modifier.height(4.dp))

                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = {
                            playerState.seekTo((playerState.currentMs - 10_000f).coerceAtLeast(0f))
                        }) {
                            Icon(Icons.Rounded.DateRange, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(24.dp))
                        }

                        Spacer(Modifier.width(12.dp))

                        FilledIconButton(
                            onClick  = onPlay,
                            modifier = Modifier.size(52.dp),
                            colors   = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF00E5A0))
                        ) {
                            Icon(
                                if (isPlaying) Icons.Rounded.PlayArrow else Icons.Rounded.PlayArrow,
                                null,
                                tint     = Color.Black,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        IconButton(onClick = {
                            playerState.seekTo((playerState.currentMs + 10_000f).coerceAtMost(playerState.totalMs))
                        }) {
                            Icon(Icons.Rounded.PlayArrow, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  HELPERS
// ═══════════════════════════════════════════════════════════════

private fun formatDurationMs(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSecs = ms / 1000
    return "%d:%02d".format(totalSecs / 60, totalSecs % 60)
}

private fun formatDateTime(ts: Long): String =
    SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(ts))