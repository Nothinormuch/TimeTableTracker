package com.timetabletracker.data.api.models

import com.google.gson.annotations.SerializedName

data class TimetableRequest(
    @SerializedName("ActivityCode") val activityCode: String = "studentdashboard",
    @SerializedName("TimeTableContextDetails") val timeTableContextDetails: TimetableContextDetails
)

data class TimetableContextDetails(
    @SerializedName("SlotStartDate") val slotStartDate: String,
    @SerializedName("SlotEndDate") val slotEndDate: String,
    @SerializedName("StudentCode") val studentCode: String
)
