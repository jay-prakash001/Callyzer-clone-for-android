package com.jp.finalcallyzer.worker

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File
import androidx.core.net.toUri

fun getLatestRecording(path: String): File? {

    val dir = File(path)

    val files = dir.listFiles() ?: return null

    return files
        .filter { it.isFile }
        .maxByOrNull { it.lastModified() }
}

fun getLatestRecording(context: Context, folderUriString: String): DocumentFile? {

    val folderUri = folderUriString.toUri()

    val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return null

    val files = folder.listFiles()

    if (files.isEmpty()) return null

    return files
        .filter { it.isFile }
        .maxByOrNull { it.lastModified() }
}