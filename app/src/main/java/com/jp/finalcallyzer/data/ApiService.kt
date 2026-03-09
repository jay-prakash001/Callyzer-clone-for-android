package com.jp.finalcallyzer.data

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    @Multipart
    @POST("api/recordings/upload")
    suspend fun uploadRecording(
        @Part("callId")     callId: RequestBody,
        @Part("employeeId") employeeId: RequestBody,
        @Part recording:    MultipartBody.Part,
    ): Response<UploadRecordingResponse>

    @Multipart
    @POST("api/recordings/upload0")
    suspend fun uploadRecordingBackGround(
        @Part("duration") duration: RequestBody,
        @Part("dateTime") dateTime: RequestBody,
        @Part("contactNo") contactNo: RequestBody,
        @Part recording: MultipartBody.Part
    ): Response<UploadRecordingResponse>
}


data class UploadRecordingResponse(
    val success:      Boolean,
    val callId:       String,
    val recordingUrl: String,
)