package com.timetabletracker.data.api.models

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("UserId") val userId: String,
    @SerializedName("Password") val password: String,
    @SerializedName("Captcha") val captcha: String = "-",
    @SerializedName("MobileAppPlatform") val mobileAppPlatform: String = "android"
)
