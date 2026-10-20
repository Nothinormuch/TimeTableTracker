package com.timetabletracker.data.api.models

import com.google.gson.annotations.SerializedName

data class StudentInfoResponse(
    @SerializedName("Items") val items: List<StudentInfoItem>?
)

data class StudentInfoItem(
    @SerializedName("StudentId") val studentId: String?
)
