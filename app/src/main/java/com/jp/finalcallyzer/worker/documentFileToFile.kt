package com.jp.finalcallyzer.worker

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileOutputStream

fun documentFileToFile(context: Context, documentFile: DocumentFile): File {
    val inputStream = context.contentResolver.openInputStream(documentFile.uri)

    val tempFile = File(
        context.cacheDir,
        documentFile.name ?: "recording_${System.currentTimeMillis()}.mp3"
    )

    val outputStream = FileOutputStream(tempFile)

    inputStream?.copyTo(outputStream)

    inputStream?.close()
    outputStream.close()

    return tempFile
}