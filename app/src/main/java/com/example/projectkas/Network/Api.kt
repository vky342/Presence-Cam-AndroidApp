package com.example.projectkas.Network

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class RecognizedStudent(
    val roll_no: String,
    val name: String?
)

data class RecognizeResponse(
    val recognized_students: List<RecognizedStudent>,
    val annotated_all: String? = null,          // base64 image (only in debug)
    val annotated_unrecognized: String? = null  // base64 image (only in debug)
)

data class DebugRecognizeResponse(
    val recognized_students: List<RecognizedStudent>,
    val annotated_all: String?,              // base64 image
    val annotated_unrecognized: String?      // base64 image
)

data class RegisterResponse(
    val message: String,
    val total_students: Int,
    val student: Student? // optional: if you want to return the registered student too
)

data class Student(
    val roll_no: String,
    val name: String
)

data class SignupResponse(
    val message: String
)

data class HealthResponse(
    val message: String
)

data class StudentListResponse(
    val message: String,
    val students: List<Student>
)

interface ApiService {

    @Multipart
    @POST("recognize")
    suspend fun recognize(
        @Part files: List<MultipartBody.Part>,
        @Header("userEmail") email: String
    ): Response<RecognizeResponse>

    @Multipart
    @POST("debugRecognize")
    suspend fun debugRecognize(
        @Part files: List<MultipartBody.Part>,
        @Header("userEmail") email: String
    ): Response<RecognizeResponse>?

    @Multipart
    @POST("register")
    suspend fun register(
        @Part images: List<MultipartBody.Part>,
        @Part("Rollno") Rollno: RequestBody,
        @Part("studentName") studentName: RequestBody, // 👈 new param
        @Header("userEmail") email: String
    ): Response<RegisterResponse>

    @Multipart
    @POST("signup")
    suspend fun signup(
        @Part("email") email: RequestBody
    ): Response<SignupResponse>

    @GET("students")
    suspend fun listStudents(
        @Header("userEmail") email: String
    ): Response<StudentListResponse>

    @GET("/")
    suspend fun healthCheck(): Response<List<HealthResponse>>
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
            //.baseUrl("http://13.202.78.35:80/api/")
            .baseUrl("http://13.204.92.135/api/")
            .client(okHttpClient)//
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

fun resizeAndCompress(
    bitmap: Bitmap,
    maxSize: Int = 800, // max width or height in pixels
    quality: Int = 85   // JPEG quality (0–100)
): File {
    val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
    val width: Int
    val height: Int

    if (ratio > 1) {
        // Landscape
        width = maxSize
        height = (maxSize / ratio).toInt()
    } else {
        // Portrait
        height = maxSize
        width = (maxSize * ratio).toInt()
    }

    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)

    val file = File.createTempFile("compressed_", ".jpg")
    FileOutputStream(file).use { out ->
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
    }
    return file
}
