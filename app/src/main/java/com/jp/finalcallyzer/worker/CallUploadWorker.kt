package com.jp.finalcallyzer.worker

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jp.finalcallyzer.data.ApiRepoImpl
import com.jp.finalcallyzer.data.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CallUploadWorker(
    context: Context,
    params: WorkerParameters,
    private val apiRepo: ApiRepoImpl
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {

            val contactNo = inputData.getString("contactNo") ?: return@withContext Result.failure()
            val folderPath = inputData.getString("filePath") ?: return@withContext Result.failure()
            val duration = inputData.getString("duration") ?: "0"
            val dateTime = inputData.getString("dateTime") ?: ""

            println("Worker File ... $contactNo $folderPath, $duration $dateTime")

            val latestFile: DocumentFile = getLatestRecording(applicationContext, folderPath)
                ?: return@withContext Result.retry()

            val file = documentFileToFile(applicationContext, latestFile)


            println("Worker File $file")
            apiRepo.uploadRecording(
                duration = duration,
                dateTime = dateTime,
                contactNo = contactNo,
                file = file
            )


            Result.success()

        } catch (e: Exception) {

            println("Worker error: ${e.message}")

            Result.retry()
        }
    }
}