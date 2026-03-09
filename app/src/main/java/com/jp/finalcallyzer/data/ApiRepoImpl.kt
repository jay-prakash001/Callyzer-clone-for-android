package com.jp.finalcallyzer.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.File

class ApiRepoImpl(private val apiService: ApiService) {


    suspend fun uploadRecording(
        duration: String,
        dateTime: String,
        contactNo: String,
        file: File
    ): Response<UploadRecordingResponse> {

        val durationBody =
            duration.toRequestBody("text/plain".toMediaType())

        val dateTimeBody =
            dateTime.toRequestBody("text/plain".toMediaType())

        val contactBody =
            contactNo.toRequestBody("text/plain".toMediaType())

        val requestFile =
            file.asRequestBody("audio/*".toMediaTypeOrNull())

        val recordingPart = MultipartBody.Part.createFormData(
            "recording",
            file.name,
            requestFile
        )

        return apiService.uploadRecordingBackGround(
            durationBody,
            dateTimeBody,
            contactBody,
            recordingPart
        )
    }

}