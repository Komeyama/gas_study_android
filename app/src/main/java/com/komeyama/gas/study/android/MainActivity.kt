package com.komeyama.gas.study.android

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.komeyama.gas.study.android.databinding.ActivityMainBinding
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var sharedPreference: SharedPreferences
    private var tokenStore = TokenStore()
    private val SAVED_TIME = "save time"
    private val EXPIRES_IN = 3590 * 1000 //[ms]

    private var googleSignInClient: GoogleSignInClient? = null
    private var retrofit = Retrofit.Builder()
    private var authApiService: Api? = null
    private var googleAppScriptService: Api? = null
    private var accessToken = ""
    private var refreshToken = ""
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private var client = OkHttpClient.Builder().addInterceptor { chain ->
        val newRequest: Request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $accessToken")
            .build()
        chain.proceed(newRequest)
    }.build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.plant(Timber.DebugTree())

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        initGoogleSignIn()
        initAuthApiService()
        initToken()
    }

    override fun onStart() {
        super.onStart()

        binding.signIn.setOnClickListener {
            signInWithGoogle()
        }

        binding.requestMessage.setOnClickListener {
            checkExpiredAndCreateOfToken()
            lifecycleScope.launch {
                initGoogleAppsScript()
                val response = execGoogleAppScript()
                Timber.d("response: $response")
                binding.message.text = response?.message.toString()
            }
        }
    }

    private fun initToken() {
        tokenStore.prepareKeyStore()
        sharedPreference = getSharedPreferences("token_store", Context.MODE_PRIVATE)
        val encryptToken = sharedPreference.getString(TokenStore.TOKEN_AREAS, "")
        Timber.d("store enc token: $encryptToken")
        if (encryptToken != "") {
            accessToken =
                tokenStore.decryptString(TokenStore.TOKEN_AREAS, encryptToken.toString()).toString()
            binding.accessTokenValue.text = accessToken
            Timber.d("store token: $accessToken")
        }

        val encryptRefreshToken = sharedPreference.getString(TokenStore.REFRESH_TOKEN_AREAS, "")
        Timber.d("store enc refresh token: $encryptRefreshToken")
        if (encryptRefreshToken != "") {
            refreshToken =
                tokenStore.decryptString(
                    TokenStore.REFRESH_TOKEN_AREAS,
                    encryptRefreshToken.toString()
                )
                    .toString()
            binding.refreshTokenValue.text = refreshToken
            Timber.d("store refresh token: $refreshToken")
        }

        checkExpiredAndCreateOfToken()
    }

    private fun saveToken(name: String, value: String) {
        val decryptValue = tokenStore.encryptString(name, value)
        sharedPreference.edit().putString(name, decryptValue).apply()
    }

    private fun saveTime() {
        val currentTime = System.currentTimeMillis()
        sharedPreference.edit().putString(SAVED_TIME, currentTime.toString()).apply()
    }

    private fun initGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(
                Scope("https://www.googleapis.com/auth/spreadsheets"),
                Scope("https://www.googleapis.com/auth/drive.readonly")
            )
            .requestServerAuthCode(
                BuildConfig.clientID,
                true
            )
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun initAuthApiService() {
        authApiService = retrofit
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .baseUrl("https://accounts.google.com/")
            .build()
            .create(Api::class.java)
    }

    private fun signInWithGoogle() {
        googleSignInClient?.apply {
            val signInIntent = this.signInIntent
            launcher.launch(signInIntent)
        }
    }

    private suspend fun getAccessToken(authCode: String): AuthResponse? {
        return try {
            authApiService?.getAccessTokenWithServerAuthCode(
                code = authCode
            )
        } catch (e: Exception) {
            Timber.d("access token get err: $e")
            null
        }
    }

    private suspend fun getAccessTokenWithRefreshToken(refreshToken: String): AuthResponse? {
        return try {
            authApiService?.getAccessTokenWithRefreshToken(
                refreshToken = refreshToken
            )
        } catch (e: Exception) {
            Timber.d("access token get err(with refresh token): $e")
            null
        }
    }

    private fun checkExpiredAndCreateOfToken() {
        val currentTime = System.currentTimeMillis()
        val savedTime = sharedPreference.getString(SAVED_TIME, "0")
        val diff = currentTime - (savedTime?.toLong() ?: 0L)
        Timber.d("current time: $currentTime, saved time: $savedTime, diff time: $diff")
        if (diff > EXPIRES_IN) {
            lifecycleScope.launch {
                Timber.d("create new access token!, refresh token: $refreshToken")
                val response = getAccessTokenWithRefreshToken(refreshToken)
                Timber.d("res: $response")
                if (response?.access_token == null) return@launch
                accessToken = response.access_token

                saveToken(TokenStore.TOKEN_AREAS, accessToken)
                binding.accessTokenValue.text = accessToken
                Timber.d("new access token: $accessToken")

                saveTime()
            }
        }
    }

    private fun initGoogleAppsScript() {
        googleAppScriptService = retrofit
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .baseUrl("https://script.google.com/macros/s/")
            .build()
            .create(Api::class.java)
    }

    private suspend fun execGoogleAppScript(): MessageResponse? {
        return try {
            googleAppScriptService?.requestMessage()
        } catch (e: Exception) {
            Timber.d("exec google app script err: $e")
            null
        }
    }

    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { lunchResult ->
        if (lunchResult.resultCode != RESULT_OK) {
            Timber.d("start activity for result error: $lunchResult")
            return@registerForActivityResult
        }
        lunchResult.data?.apply {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(this)
            if (result != null && result.isSuccess) {
                lifecycleScope.launch {
                    val account: GoogleSignInAccount = result.signInAccount as GoogleSignInAccount
                    if (account.serverAuthCode == null) return@launch
                    val serverAuthCode = account.serverAuthCode!!
                    Timber.d("serverAuthCode: $serverAuthCode")

                    val response = getAccessToken(serverAuthCode)
                    if (response?.access_token == null) return@launch
                    accessToken = response.access_token
                    refreshToken = response.refresh_token

                    saveToken(TokenStore.TOKEN_AREAS, accessToken)
                    saveToken(TokenStore.REFRESH_TOKEN_AREAS, refreshToken)

                    binding.accessTokenValue.text = accessToken
                    binding.refreshTokenValue.text = refreshToken
                    Timber.d("access token: $accessToken ,refresh token: $refreshToken")

                    saveTime()
                }
            }
        }
    }
}