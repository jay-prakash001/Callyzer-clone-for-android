package com.jp.finalcallyzer.di

import android.content.Context
import androidx.work.WorkerParameters
import com.callyzer.app.util.EmployeePrefs
import com.jp.finalcallyzer.CallViewModel
import com.jp.finalcallyzer.data.ApiRepoImpl
import com.jp.finalcallyzer.data.ApiService
import com.jp.finalcallyzer.worker.CallUploadWorker
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.workmanager.dsl.worker
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

val appModule = module {
    single<ApiService> {

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttp = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(40, TimeUnit.SECONDS)
            .readTimeout(40, TimeUnit.SECONDS)
            .writeTimeout(40, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl("http://192.168.29.132:3000/")
//            .baseUrl("http://10.0.2.2:3000/")
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    single {
        ApiRepoImpl(get())
    }

    single { EmployeePrefs(androidContext()) }
    viewModel<CallViewModel> {
        CallViewModel(apiService = get(), androidContext())

    }

    worker {
        CallUploadWorker(context = androidContext(), params = get(), get())
    }

}