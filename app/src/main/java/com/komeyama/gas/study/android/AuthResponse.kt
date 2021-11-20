package com.komeyama.gas.study.android

data class AuthResponse(
    var access_token: String,
    var expires_in: Int,
    var scope: String,
    var token_type: String,
    var refresh_token: String = "",
    var id_token: String
)