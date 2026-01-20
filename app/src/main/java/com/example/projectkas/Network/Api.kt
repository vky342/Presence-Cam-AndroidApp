package com.example.projectkas.Network

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
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
import retrofit2.http.*

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
    val student: Student?
)

data class ClassUi(
    val id: String,
    val name: String,
    val created_at: String
)

data class ClassesResponse(
    val total: Int,
    val classes: List<ClassUi>
)

data class StudentSummary(
    val id: String,
    val roll_no: String?,
    val name: String?
)

data class Student(
    val id : String,
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

data class GenericDeleteResponse(
    val message: String,
    val total_students: Int? = null
)

data class UpdateStudentResponse(
    val message: String,
    val student: StudentMetadata
)

data class StudentMetadata(
    val id: String,
    val roll_no: String?,
    val name: String?,
    val created_at: String? = null,
    val updated_at: String? = null
)

data class ReEnrollResponse(
    val message: String,
    val student: StudentSummary,
    val total_students: Int
)

interface ApiService {

    // Root health-check
    @GET("/")
    suspend fun healthCheck(): Response<List<HealthResponse>>

    // signup expects form data: email
    @FormUrlEncoded
    @POST("signup")
    suspend fun signup(
        @Field("email") email: RequestBody
    ): Response<SignupResponse>

    // register: multipart with 1..3 image files + Rollno + studentName form fields, header userEmail
    @Multipart
    @POST("register")
    suspend fun register(
        @Part images: List<MultipartBody.Part>,
        @Part("Rollno") Rollno: RequestBody,
        @Part("classId") classId: RequestBody,
        @Part("studentName") studentName: RequestBody,
        @Header("userEmail") email: String
    ): Response<RegisterResponse>

    // recognize: multipart images + header
    @Multipart
    @POST("recognize")
    suspend fun recognize(
        @Part files: List<MultipartBody.Part>,
        @Header("userEmail") email: String
    ): Response<RecognizeResponse>

    // debugRecognize: same as recognize but returns annotated images too
    @Multipart
    @POST("debugRecognize")
    suspend fun debugRecognize(
        @Part files: List<MultipartBody.Part>,
        @Header("userEmail") email: String
    ): Response<RecognizeResponse>?

    // list students (GET) needs header userEmail
    @GET("students")
    suspend fun listStudents(
        @Query("classId") classId: String?,
        @Header("userEmail") email: String
    ): Response<StudentListResponse>

    // Preferred delete-by-UUID endpoint: /studentDelete expects student_id in form data
    @FormUrlEncoded
    @HTTP(method = "DELETE", path = "studentDelete", hasBody = true)
    suspend fun deleteStudentById(
        @Field("student_id") studentId: String,
        @Field("classId") classID: String?,
        @Header("userEmail") email: String
    ): Response<GenericDeleteResponse>

    // Update metadata by UUID (PUT /studentsUpdate) - form data (fields optional on server)
    @FormUrlEncoded
    @PUT("studentsUpdate")
    suspend fun updateStudentMetadata(
        @Field("student_id") studentId: String,
        @Field("classId") classID: String?,
        @Field("roll_no") rollNo: String?, // send null / omit? Retrofit will send "null" as text if you pass null, so pass empty string or overload if needed
        @Field("name") name: String?,
        @Header("userEmail") email: String
    ): Response<UpdateStudentResponse>

    // Re-enroll: replace/update embeddings for an existing student (multipart 1..3 images + student_id form field)
    @Multipart
    @PUT("students/re-enroll")
    suspend fun reenrollStudentEmbeddings(
        @Part images: List<MultipartBody.Part>,
        @Part("student_id") studentId: RequestBody,
        @Header("userEmail") email: String
    ): Response<ReEnrollResponse>

    @Multipart
    @POST("images/get")
    suspend fun getImageByUuidMultipart(
        @Part("uuid") uuidPart: RequestBody
    ): Response<ResponseBody>

    // -------------------- GET /classes --------------------
    @GET("classes")
    suspend fun getClasses(
        @Header("User-Email") userEmail: String
    ): ClassesResponse


    // -------------------- POST /classes --------------------
    @FormUrlEncoded
    @POST("classes")
    suspend fun createClass(
        @Header("User-Email") userEmail: String,
        @Field("name") name: String
    ): ClassUi

    @FormUrlEncoded
    @PUT("classes")
    suspend fun updateClass(
        @Field("classId") classId: String,
        @Field("name") name: String,
        @Header("User-Email") email: String
    ): Response<ClassUi>

    @FormUrlEncoded
    @HTTP(method = "DELETE", path = "classes", hasBody = true)
    suspend fun deleteClass(
        @Field("classId") classId: String,
        @Header("User-Email") email: String
    ): Response<GenericDeleteResponse>

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
            .baseUrl("http://10.0.2.2:8000/")
            //.baseUrl("http://3.110.79.123:80/api/")
            .client(okHttpClient)//
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

fun resizeAndCompress(
    bitmap: Bitmap,
    maxSize: Int = 800,
    quality: Int = 85
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
