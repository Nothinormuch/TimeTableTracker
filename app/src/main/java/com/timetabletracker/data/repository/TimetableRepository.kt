package com.timetabletracker.data.repository

import android.util.Log
import com.timetabletracker.data.api.ApiClient
import com.timetabletracker.data.api.models.LoginRequest
import com.timetabletracker.data.api.models.TimetableContextDetails
import com.timetabletracker.data.api.models.TimetableRequest
import com.timetabletracker.data.api.models.TimetableSlot
import com.timetabletracker.data.local.PreferencesManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Sealed result type for repository calls.
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val cause: Throwable? = null) : Result<Nothing>()
}

/**
 * Central repository for all timetable-related network operations.
 * Handles token caching, silent re-authentication, and date range calculation.
 */
class TimetableRepository(private val prefsManager: PreferencesManager) {

    companion object {
        private const val TAG = "TimetableRepository"
        private const val APP_SECRET = ApiClient.APP_SECRET
    }

    private val api = ApiClient.apiService

    /**
     * Returns a valid Bearer token string, re-authenticating if needed.
     */
    suspend fun getValidToken(): Result<String> {
        if (prefsManager.isTokenValid()) {
            return Result.Success("Bearer ${prefsManager.accessToken}")
        }
        return authenticate()
    }

    /**
     * Performs the 2-step login flow: password auth → token storage.
     */
    suspend fun authenticate(): Result<String> {
        val userId = prefsManager.userId
        val password = prefsManager.password

        if (userId.isBlank() || password.isBlank()) {
            return Result.Error("No credentials saved. Please log in.")
        }

        return try {
            val response = api.login(
                request = LoginRequest(userId = userId, password = password)
            )
            if (response.isSuccessful) {
                val token = response.body()?.item?.identity?.accessToken
                if (token != null) {
                    prefsManager.saveToken(token)
                    Log.d(TAG, "Authentication successful")
                    Result.Success("Bearer $token")
                } else {
                    Result.Error("Login succeeded but no token was returned.")
                }
            } else {
                Result.Error("Login failed: HTTP ${response.code()} — ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Authentication exception", e)
            Result.Error("Network error during login: ${e.message}", e)
        }
    }

    /**
     * Fetches the StudentId from the portal, caching it for future use.
     */
    suspend fun getStudentId(bearerToken: String): Result<String> {
        val cachedId = prefsManager.studentId
        if (cachedId.isNotBlank()) {
            return Result.Success(cachedId)
        }

        return try {
            val response = api.getStudentInfo(
                appSecret = APP_SECRET,
                authorization = bearerToken
            )
            if (response.isSuccessful) {
                val studentId = response.body()?.items?.firstOrNull()?.studentId
                if (!studentId.isNullOrBlank()) {
                    prefsManager.studentId = studentId
                    Log.d(TAG, "StudentId fetched: $studentId")
                    Result.Success(studentId)
                } else {
                    Result.Error("StudentId not found in response.")
                }
            } else {
                Result.Error("Failed to get student info: HTTP ${response.code()} — ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "GetStudentId exception", e)
            Result.Error("Network error fetching student info: ${e.message}", e)
        }
    }

    /**
     * Fetches this week's timetable slots.
     * Date range: today → end of current week (Sunday).
     */
    suspend fun fetchTimetable(bearerToken: String, studentId: String): Result<List<TimetableSlot>> {
        val (startDate, endDate) = getWeekDateRange()
        val request = TimetableRequest(
            timeTableContextDetails = TimetableContextDetails(
                slotStartDate = startDate,
                slotEndDate = endDate,
                studentCode = studentId
            )
        )

        return try {
            val response = api.getTimetable(
                appSecret = APP_SECRET,
                authorization = bearerToken,
                studentUniqueId = studentId,
                request = request
            )
            if (response.isSuccessful) {
                val slots = response.body() ?: emptyList()
                Log.d(TAG, "Fetched ${slots.size} timetable slots ($startDate → $endDate)")
                Result.Success(slots)
            } else {
                Result.Error("Timetable fetch failed: HTTP ${response.code()} — ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "FetchTimetable exception", e)
            Result.Error("Network error fetching timetable: ${e.message}", e)
        }
    }

    /**
     * Full pipeline: authenticate → get StudentId → fetch timetable.
     */
    suspend fun fetchTimetableFull(): Result<List<TimetableSlot>> {
        val tokenResult = getValidToken()
        if (tokenResult is Result.Error) return tokenResult

        val token = (tokenResult as Result.Success).data

        val studentIdResult = getStudentId(token)
        if (studentIdResult is Result.Error) return studentIdResult

        val studentId = (studentIdResult as Result.Success).data
        return fetchTimetable(token, studentId)
    }

    /**
     * Computes the date range based on the current day of the week.
     * Sunday is considered the first day of the week (Day 1).
     *
     * Date range rules:
     * - Sunday: Sunday to next week's Saturday (6 days)
     * - Monday: Monday to Saturday (5 days)
     * - Tuesday: Tuesday to Saturday (4 days)
     * - Wednesday: Wednesday to Saturday (3 days)
     * - Thursday: Thursday to Saturday (2 days)
     * - Friday: Friday to Saturday (1 day)
     * - Saturday: Saturday only (0 days extension)
     *
     * Format: DD-Mon-YYYY (e.g., 20-Oct-2026)
     */
    private fun getWeekDateRange(): Pair<String, String> {
        val formatter = SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH)
        val today = Calendar.getInstance()
        val todayDayOfWeek = today.get(Calendar.DAY_OF_WEEK)

        // In Android Calendar, Sunday is 1, Monday is 2, ..., Saturday is 7
        val daysToSaturday = when (todayDayOfWeek) {
            1 -> 6  // Sunday: need to go to Saturday (6 days ahead)
            2 -> 5  // Monday: need to go to Saturday (5 days ahead)
            3 -> 4  // Tuesday: need to go to Saturday (4 days ahead)
            4 -> 3  // Wednesday: need to go to Saturday (3 days ahead)
            5 -> 2  // Thursday: need to go to Saturday (2 days ahead)
            6 -> 1  // Friday: need to go to Saturday (1 day ahead)
            7 -> 0  // Saturday: already on Saturday
            else -> 0
        }

        val end = today.clone() as Calendar
        end.add(Calendar.DAY_OF_MONTH, daysToSaturday)

        return Pair(formatter.format(today.time), formatter.format(end.time))
    }
}
