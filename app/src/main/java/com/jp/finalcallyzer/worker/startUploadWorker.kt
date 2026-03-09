package com.jp.finalcallyzer.worker

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

fun startUploadWorker(
    context: Context,
    duration: Int,
    dateTime: String,
    contactNo: String,
    filePath: String
) {


    val data = workDataOf(
        "duration" to duration.toString(),
        "dateTime" to dateTime,
        "contactNo" to contactNo,
        "filePath" to filePath
    )

    println("startUploadWorker() $duration, $dateTime, $contactNo, $filePath")

    val request = OneTimeWorkRequestBuilder<CallUploadWorker>()
        .setInputData(data)
        .setInitialDelay(10, TimeUnit.SECONDS) // wait for recording file
        .build()

    WorkManager.getInstance(context)
        .enqueueUniqueWork(
            "upload_recording",
            ExistingWorkPolicy.REPLACE,
            request
        )
}