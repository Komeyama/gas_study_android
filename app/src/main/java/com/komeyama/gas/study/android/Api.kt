package com.komeyama.gas.study.android

import retrofit2.http.*

interface Api {

    enum class GrantType(val value: String) {
        CODE("authorization_code"),
        REFRESH_TOKEN("refresh_token")
    }

    @POST("o/oauth2/token")
    @FormUrlEncoded
    suspend fun getAccessTokenWithServerAuthCode(
        @Field("client_id") clientId: String = BuildConfig.clientID,
        @Field("client_secret") clientSecret: String = BuildConfig.clientSecret,
        @Field("code") code: String,
        @Field("grant_type") grantType: String = GrantType.CODE.value,
        @Field("redirect_uri") redirectUri: String = BuildConfig.redirectURL,
    ): AuthResponse

    @POST("o/oauth2/token")
    @FormUrlEncoded
    suspend fun getAccessTokenWithRefreshToken(
        @Field("client_id") clientId: String = BuildConfig.clientID,
        @Field("client_secret") clientSecret: String = BuildConfig.clientSecret,
        @Field("refresh_token") code: String,
        @Field("grant_type") grantType: String = GrantType.REFRESH_TOKEN.value,
        @Field("redirect_uri") redirectUri: String = BuildConfig.redirectURL,
    ): AuthResponse

    @GET("{macroID}/exec")
    suspend fun requestMessage(
        @Path("macroID") macroID: String = BuildConfig.macroID
    ): MessageResponse

}