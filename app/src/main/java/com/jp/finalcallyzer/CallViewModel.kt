package com.jp.finalcallyzer

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.runtime.*
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callyzer.app.util.EmployeePrefs
import com.jp.finalcallyzer.data.ApiService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

// ═══════════════════════════════════════════════════════════════
//  DATA MODEL
// ═══════════════════════════════════════════════════════════════

data class AudioFile(
    val uri: Uri,
    val name: String,
    val dateTime: Long,
    val durationMs: Long,
    val sizeBytes: Long,
)

// ═══════════════════════════════════════════════════════════════
//  VIEW MODEL
// ═══════════════════════════════════════════════════════════════

data class RecordingUiState(
    val folderUri: String = "",
    val folderName: String = "No folder selected",
    val audioFiles: List<AudioFile> = emptyList(),
    val isLoading: Boolean = false,
    val uploadingIds: Set<String> = emptySet(),
    val uploadedIds: Set<String> = emptySet(),
    val error: String? = null,
)

class CallViewModel(private val apiService: ApiService, private val context: Context) : ViewModel(),
    KoinComponent {

    private val prefs: EmployeePrefs by inject()

    private val _state = MutableStateFlow(RecordingUiState())
    val state: StateFlow<RecordingUiState> = _state.asStateFlow()


    fun init(context: Context) {

        val saved = prefs.recordingFolderUri
        if (saved.isNotBlank()) {
            _state.update { it.copy(folderUri = saved) }
            loadFiles(context, Uri.parse(saved))
        }
    }

    fun onFolderSelected(context: Context, uri: Uri) {
        // Persist permission
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        prefs.recordingFolderUri = uri.toString()

        val folder = DocumentFile.fromTreeUri(context, uri)
        val name = folder?.name ?: uri.lastPathSegment ?: "Selected Folder"

        _state.update { it.copy(folderUri = uri.toString(), folderName = name) }
        loadFiles(context, uri)
    }

    fun refreshFiles() {
        val ctx = context ?: return
        val uri = _state.value.folderUri.takeIf { it.isNotBlank() } ?: return
        loadFiles(ctx, Uri.parse(uri))
    }

    private fun loadFiles(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val folder = DocumentFile.fromTreeUri(context, uri)
                if (folder == null || !folder.exists()) {
                    _state.update { it.copy(isLoading = false, error = "Folder not accessible") }
                    return@launch
                }

                _state.update { it.copy(folderName = folder.name ?: "Recording Folder") }

                val files = folder.listFiles()
                    .filter { it.isFile && isAudioFile(it.name ?: "") }
                    .mapNotNull { doc ->
                        val dur = getAudioDuration(context, doc.uri)
                        AudioFile(
                            uri = doc.uri,
                            name = doc.name ?: "Unknown",
                            dateTime = doc.lastModified(),
                            durationMs = dur,
                            sizeBytes = doc.length(),
                        )
                    }
                    .sortedByDescending { it.dateTime }

                _state.update { it.copy(audioFiles = files, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun uploadFile0(context: Context, file: AudioFile) {
        val id = file.uri.toString()
        _state.update { it.copy(uploadingIds = it.uploadingIds + id) }
        viewModelScope.launch {
            delay(2000) // TODO: replace with real API call
            _state.update {
                it.copy(
                    uploadingIds = it.uploadingIds - id,
                    uploadedIds = it.uploadedIds + id,
                )
            }
        }
    }


    fun uploadFile(context: Context, file: AudioFile){}
//    {
//        val id = file.uri.toString()
//        if (id in _state.value.uploadingIds || id in _state.value.uploadedIds) return
//
//        _state.update { it.copy(uploadingIds = it.uploadingIds + id) }
//
//        viewModelScope.launch(Dispatchers.IO) {
//            try {
//                // 1. Read bytes from Uri
//                val inputStream = context.contentResolver.openInputStream(file.uri)
//                    ?: throw Exception("Cannot open file")
//                val bytes = inputStream.readBytes()
//                inputStream.close()
//
//                // 2. Build multipart parts
//                val callId = "CALL-${file.dateTime}"
//
//                val callIdPart = callId
//                    .toRequestBody("text/plain".toMediaType())
//
//                val employeeIdPart = prefs.employeeId
//                    .toRequestBody("text/plain".toMediaType())
//
//                val filePart = MultipartBody.Part.createFormData(
//                    name = "recording",
//                    filename = file.name,
//                    body = bytes.toRequestBody("audio/*".toMediaType()),
//                )
//
//                // 3. Call API
//                val response = apiService.uploadRecording(
//                    callId = callIdPart,
//                    employeeId = employeeIdPart,
//                    recording = filePart,
//                )
//
//                if (response.isSuccessful) {
//                    val url = response.body()?.recordingUrl
//                    println("Uploaded: $url")
//                    _state.update {
//                        it.copy(
//                            uploadingIds = it.uploadingIds - id,
//                            uploadedIds = it.uploadedIds + id,
//                        )
//                    }
//                } else {
//                    val error = response.errorBody()?.string() ?: "Unknown error"
//                    println("Upload failed: $error")
//                    _state.update {
//                        it.copy(
//                            uploadingIds = it.uploadingIds - id,
//                            error = "Upload failed: ${response.code()}",
//                        )
//                    }
//                }
//
//            } catch (e: Exception) {
//                e.printStackTrace()
//                _state.update {
//                    it.copy(
//                        uploadingIds = it.uploadingIds - id,
//                        error = e.message ?: "Upload error",
//                    )
//                }
//            }
//        }
//    }

    private fun isAudioFile(name: String): Boolean {
        val ext = name.substringAfterLast(".").lowercase()
        return ext in listOf("mp3", "aac", "m4a", "wav", "amr", "3gp", "ogg", "opus")
    }

    private fun getAudioDuration(context: Context, uri: Uri): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            retriever.release()
            dur
        } catch (e: Exception) {
            0L
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  MEDIA PLAYER STATE
// ═══════════════════════════════════════════════════════════════

class AudioPlayerState {
    var player: MediaPlayer? = null
    var playingUri: Uri? by mutableStateOf(null)
    var isPlaying: Boolean by mutableStateOf(false)
    var currentMs: Float by mutableStateOf(0f)
    var totalMs: Float by mutableStateOf(0f)
    var isSeeking: Boolean by mutableStateOf(false)

    fun play(context: Context, uri: Uri, durationMs: Long) {
        if (playingUri == uri) {
            // Toggle pause/play
            if (isPlaying) {
                player?.pause(); isPlaying = false
            } else {
                player?.start(); isPlaying = true
            }
            return
        }
        // New file
        stop()
        player = MediaPlayer().apply {
            setDataSource(context, uri)
            prepare()
            start()
            setOnCompletionListener { this@AudioPlayerState.isPlaying = false; currentMs = 0f }
        }
        playingUri = uri
        isPlaying = true
        totalMs = durationMs.toFloat().coerceAtLeast(1f)
    }

    fun seekTo(ms: Float) {
        player?.seekTo(ms.toInt())
        currentMs = ms
    }

    fun stop() {
        player?.stop()
        player?.release()
        player = null
        isPlaying = false
        playingUri = null
        currentMs = 0f
        totalMs = 0f
    }
}
