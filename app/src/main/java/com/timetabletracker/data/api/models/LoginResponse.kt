package com.timetabletracker.data.api.models

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("Item") val item: LoginItem?
)

data class LoginItem(
    @SerializedName("Identity") val identity: LoginIdentity?
)

data class LoginIdentity(
    @SerializedName("AccessToken") val accessToken: String?
)
