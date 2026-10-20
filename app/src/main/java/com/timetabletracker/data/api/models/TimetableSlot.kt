package com.timetabletracker.data.api.models

import com.google.gson.annotations.SerializedName
import java.io.Serializable

// NOTE: The timetable endpoint returns a plain JSON array — no wrapper object.
// ApiService returns Response<List<TimetableSlot>> directly.

data class TimetableSlot(
    @SerializedName("SlotDate") val slotDate: String = "",
    @SerializedName("SlotStartTime") val slotStartTime: String = "",
    @SerializedName("SlotEndTime") val slotEndTime: String = "",
    @SerializedName("ModuleList") val moduleList: List<ModuleInfo>? = null,
    @SerializedName("TeacherList") val teacherList: List<TeacherInfo>? = null,
    @SerializedName("FloorPlanDetails") val floorPlanDetails: FloorPlanDetails? = null,
    @SerializedName("DayofWeekDetails") val dayOfWeekDetails: DayOfWeekDetails? = null,
    @SerializedName("IsCancelledSlot") val isCancelledSlot: Boolean = false,
    @SerializedName("IsRescheduledSlot") val isRescheduledSlot: Boolean = false,
    @SerializedName("IsExtraSlot") val isExtraSlot: Boolean = false
) : Serializable

data class ModuleInfo(
    @SerializedName("ModuleName") val moduleName: String = "",
    @SerializedName("ModuleCode") val moduleCode: String = ""
) : Serializable

data class TeacherInfo(
    @SerializedName("Name") val name: String = ""
) : Serializable

data class FloorPlanDetails(
    @SerializedName("VenueName") val venueName: String = "",
    @SerializedName("VenueCategory") val venueCategory: String = "",
    @SerializedName("MeetingLink") val meetingLink: String = ""
) : Serializable

data class DayOfWeekDetails(
    @SerializedName("DayOfWeekName") val dayOfWeekName: String = ""
) : Serializable
