package com.example.projectkas.Network

import android.content.Context
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.util.concurrent.TimeUnit


data class RecognizeResponse(
    val recognized_ids: List<String>,
    val annotated_all: String,
    val annotated_unrecognized: String
)

data class RegisterResponse(
    val message: String,
    val total_students: Int
)

interface ApiService {
    @Multipart
    @POST("recognize")
    suspend fun recognize(
        @Part image: MultipartBody.Part
    ): Response<RecognizeResponse>

    @Multipart
    @POST("register")
    suspend fun register(
        @Part images: List<MultipartBody.Part>,
        @Part("enroll_no") enrollNo: RequestBody
    ): Response<RegisterResponse>
}

fun uriToMultipart(context: Context, uri: Uri, paramName: String): MultipartBody.Part {
    val inputStream = context.contentResolver.openInputStream(uri)!!
    val fileBytes = inputStream.readBytes()

    val mimeType = context.contentResolver.getType(uri) ?: "image/*"
    val requestFile = fileBytes.toRequestBody(mimeType.toMediaTypeOrNull())

    val fileName = "upload_${System.currentTimeMillis()}.${mimeType.substringAfter("/")}"
    return MultipartBody.Part.createFormData(paramName, fileName, requestFile)
}

object RetrofitInstance {

    val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl("http://51.20.253.100:8000/")
            .client(okHttpClient)//
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}