package com.walkietalkie.app.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

data class Group(
    val id: String,
    val code: String,
    val name: String
)

data class TokenResponse(
    val livekitUrl: String,
    val token: String,
    val room: String,
    val groupName: String
)

interface ApiService {
    @POST("groups")
    suspend fun createGroup(@Body body: Map<String, String>): Group

    @POST("groups/join")
    suspend fun joinGroup(@Body body: Map<String, String>): Group

    @POST("groups/{code}/token")
    suspend fun getToken(
        @Path("code") code: String,
        @Query("nickname") nickname: String
    ): TokenResponse
}