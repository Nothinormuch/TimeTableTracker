package com.timetabletracker.data.api

import com.timetabletracker.data.api.models.LoginRequest
import com.timetabletracker.data.api.models.LoginResponse
import com.timetabletracker.data.api.models.StudentInfoResponse
import com.timetabletracker.data.api.models.TimetableRequest
import com.timetabletracker.data.api.models.TimetableSlot
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {

    @POST("sso/api/account/oauth2/token")
    suspend fun login(
        @Header("Content-Type") contentType: String = "application/json",
        @Header("x-applicationname") appName: String = "oneportal",
        @Header("x-requestfrom") requestFrom: String = "mobile",
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @GET("apigateway/connect-portal/api/studentloginbasicinfo")
    suspend fun getStudentInfo(
        @Header("Content-Type") contentType: String = "application/json",
        @Header("x-applicationname") appName: String = "connectportal",
        @Header("x-appsecret") appSecret: String,
        @Header("x-requestfrom") requestFrom: String = "web",
        @Header("Authorization") authorization: String
    ): Response<StudentInfoResponse>

    @POST("apigateway/api/timetable")
    suspend fun getTimetable(
        @Header("Content-Type") contentType: String = "application/json",
        @Header("x-applicationname") appName: String = "connectportal",
        @Header("x-appsecret") appSecret: String,
        @Header("x-requestfrom") requestFrom: String = "web",
        @Header("Authorization") authorization: String,
        @Header("x-studentUniqueId") studentUniqueId: String,
        @Body request: TimetableRequest
    ): Response<List<TimetableSlot>>
}
