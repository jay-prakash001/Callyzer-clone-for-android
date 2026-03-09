package com.callyzer.app.util

import android.content.Context

import javax.inject.Singleton

@Singleton
class EmployeePrefs (
   private val   context: Context
) {

    private val prefs = context.getSharedPreferences("callyzer_prefs", Context.MODE_PRIVATE)
    var recordingFolderUri: String
        get()      = prefs.getString("recording_folder_uri", "") ?: ""
        set(value) = prefs.edit().putString("recording_folder_uri", value).apply()

    var employeeId: String
        get()      = prefs.getString("employee_id", "EMP-0001") ?: "EMP-0001"
        set(value) = prefs.edit().putString("employee_id", value).apply()

    var employeeName: String
        get()      = prefs.getString("employee_name", "Employee") ?: "Employee"
        set(value) = prefs.edit().putString("employee_name", value).apply()

    var fcmToken: String
        get()      = prefs.getString("fcm_token", "") ?: ""
        set(value) = prefs.edit().putString("fcm_token", value).apply()
}
