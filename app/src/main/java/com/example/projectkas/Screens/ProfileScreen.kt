package com.example.projectkas.Screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.projectkas.Network.RetrofitInstance
import com.example.projectkas.Network.RetrofitInstance.api
import com.example.projectkas.Network.resizeAndCompress
import com.example.projectkas.Network.uriToMultipart
import com.example.projectkas.R
import retrofit2.Response
import okhttp3.ResponseBody
import com.example.projectkas.ViewModel.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnrememberedGetBackStackEntry")
@Composable
fun ProfileScreen(navController: NavController, rollNo: String?, studentName: String?, id : String?) {

    val parentEntry = remember(navController) { navController.getBackStackEntry("main") }
    val authViewModel: AuthViewModel = hiltViewModel(parentEntry)
    val currentUserEmail = authViewModel.auth.currentUser?.email ?: ""

    var isLoading by remember { mutableStateOf(false) }
    var isLoadingDelete by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf(studentName ?: "") }
    var roll by remember { mutableStateOf(rollNo ?: "") }
    var id by remember { mutableStateOf(id ?: "") }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val focusManager = LocalFocusManager.current

    var showPickerDialog by remember { mutableStateOf(false) }

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // image picker container
    var showReEnroll by remember { mutableStateOf(false) }
    var selectedUris by rememberSaveable { mutableStateOf<List<Uri>>(emptyList()) }
    var apiMessage by remember { mutableStateOf<String?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                // append new ones, avoid duplicates, max 3
                selectedUris = (selectedUris + uris).distinct().take(3)
            }
            // if uris is empty → do nothing (user cancelled)
        }
    )
    // --------- CAMERA LAUNCHER ---------
    var currentImageUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentImageUri != null) {
            if (selectedUris.size < 3) {
                selectedUris = selectedUris + currentImageUri!!
            }
        }
    }

    fun captureImage() {
        if (selectedUris.size < 3) {
            val file = File.createTempFile(
                "image_${System.currentTimeMillis()}",
                ".jpg",
                context.cacheDir
            )
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            currentImageUri = uri
            cameraLauncher.launch(uri)
        }
    }

    LaunchedEffect(key1 = id) {
        if (id.isBlank()) return@LaunchedEffect

        loading = true
        error = null
        bitmap = null

        try {
            // build multipart text part (same as your ViewModel approach)
            val req: RequestBody = id.toRequestBody("text/plain".toMediaTypeOrNull())

            // call network on coroutine. Retrofit's suspend functions are main-safe, but body.bytes()
            // is blocking -> do the decoding on IO
            val response: Response<ResponseBody> = api.getImageByUuidMultipart(req)

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    // decode bytes on IO dispatcher to avoid blocking main thread
                    val bmp: Bitmap? = withContext(Dispatchers.IO) {
                        val bytes = body.bytes() // careful: loads into memory
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }

                    if (bmp != null) {
                        bitmap = bmp
                    } else {
                        error = "Failed to decode image"
                    }
                } else {
                    error = "Empty response body"
                }
            } else {
                val errStr = response.errorBody()?.string()
                error = "Server error: ${response.code()} ${errStr ?: ""}"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            error = e.message ?: "Unknown error"
        } finally {
            loading = false
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(color = Color(24, 23, 23))
            .padding(top = 4.dp, start = 16.dp, end = 16.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(id = R.string.edit_profile),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(4.dp))
        Divider(
            color = Color(0xFF468A9A), // teal accent
            thickness = 2.dp,
            modifier = Modifier.width(180.dp)
        )
        Spacer(modifier = Modifier.height(25.dp))

        // Profile Picture Placeholder
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Color.Gray),
            contentAlignment = Alignment.Center
        ) {
            when {
                loading -> CircularProgressIndicator()
                bitmap != null -> Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "Profile Picture",
                    modifier = Modifier.size(120.dp)
                )
                else -> Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "Profile placeholder",
                    modifier = Modifier.size(80.dp)
                )
            }
        }
        TextButton(onClick = {

        showReEnroll = true

        }) {
            Text(stringResource(id = R.string.edit))
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Name and Roll No Fields
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(id = R.string.name)) },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(24, 23, 23),
                unfocusedContainerColor = Color(24, 23, 23),

                focusedTextColor = Color.White,
                unfocusedTextColor = Color.LightGray,

                focusedLabelColor = Color.White,
                unfocusedLabelColor = Color.Gray,

                focusedIndicatorColor = Color.White,
                unfocusedIndicatorColor = Color.Gray
            ),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus() // Unfocus on Done
                }
            )

        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = roll,
            onValueChange = { roll = it },
            label = { Text(stringResource(id = R.string.roll_no)) },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(24, 23, 23),
                unfocusedContainerColor = Color(24, 23, 23),

                focusedTextColor = Color.White,
                unfocusedTextColor = Color.LightGray,

                focusedLabelColor = Color.White,
                unfocusedLabelColor = Color.Gray,

                focusedIndicatorColor = Color.White,
                unfocusedIndicatorColor = Color.Gray
            ),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus() // Unfocus on Done
                }
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        Spacer(modifier = Modifier.weight(1f))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        isLoading = true
                        try {
                            val response = api.updateStudentMetadata(
                                studentId = id,
                                rollNo = roll.ifBlank { null },
                                name = name.ifBlank { null },
                                email = currentUserEmail
                            )
                            if (response.isSuccessful) {
                                val body = response.body()
                                Toast.makeText(context, "Updated: ${body?.message + body?.student}", Toast.LENGTH_SHORT).show()
                                Log.d("debug", body?.message + body?.student)
                                name = ""
                                roll = ""
                                id = ""
                                navController.popBackStack()
                            } else {
                                Toast.makeText(context, "Error: ${response.errorBody()?.string()}", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }finally {
                            isLoading = false
                        }
                    }

                },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3A3A3A), // Darker container
                    contentColor = Color.White
                )
            ) {

                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(id = R.string.saving),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }else{
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save",
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(stringResource(id = R.string.save))
                }

            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = {
                    showPickerDialog = true
                },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8B1A1A),
                    contentColor = Color.White
                )
            ) {

                if(isLoadingDelete){
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(id = R.string.deleting),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }else{
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(stringResource(id = R.string.delete))
                }
            }
        }
    }

    if (showPickerDialog) {
        AlertDialog(
            onDismissRequest = { showPickerDialog = false },

            title = { Text(stringResource(id = R.string.are_you_sure)) },
            text = { Text(stringResource(id = R.string.selected_student, studentName ?: "")) },
            confirmButton = {
                TextButton(onClick = {
                    showPickerDialog = false
                    coroutineScope.launch {
                        isLoadingDelete = true
                        try {
                            val response = api.deleteStudentById(
                                studentId = id,
                                email = currentUserEmail
                            )
                            if (response.isSuccessful) {
                                val body = response.body()
                                Toast.makeText(context, "${body?.message} Total students : ${body?.total_students}", Toast.LENGTH_SHORT).show()
                                Log.d("debug", body?.message + body?.total_students)
                                name = ""
                                roll = ""
                                id = ""
                                navController.popBackStack()
                            } else {
                                Toast.makeText(context, "Error: ${response.errorBody()?.string()}", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }finally {
                            isLoadingDelete = false
                        }
                    }

                }) { Text(stringResource(id = R.string.delete), color = Color.Red) }
            },

            dismissButton = {
                TextButton(onClick = {
                    showPickerDialog = false
                }) { Text(stringResource(id = R.string.cancel) ,color = Color.Green) }
            }
        )
    }

    if (showReEnroll) {
        BackHandler(enabled = true) {
            showReEnroll = false
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                // draw a faint scrim so user knows it's modal
                .background(Color.Black.copy(alpha = 0.3f))
                // intercept taps
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { showReEnroll = false })
                }
        ) {
            Column (modifier = Modifier
                .align(Alignment.Center)
                .clip(RoundedCornerShape(16.dp))
                .wrapContentSize()
                .padding(16.dp)
                .background(Color(0xFF2C2C2C), shape = RoundedCornerShape(16.dp))) {
                MultiImagePickerContainer(
                    selectedUris = selectedUris,
                    onUpload = { galleryLauncher.launch(arrayOf("image/*")) },
                    onCapture = { captureImage() },
                    onClear = { uri -> selectedUris = selectedUris - uri }
                )
                // Register Button
                Card(
                    modifier = Modifier
                        .height(55.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable(
                            enabled = selectedUris.size <= 3 && selectedUris.isNotEmpty() && !isLoading
                        ) {
                            coroutineScope.launch {
                                if (selectedUris.size > 3 || selectedUris.isEmpty()) {
                                    apiMessage = "Enter enroll no & select 3 images"
                                    return@launch
                                }

                                isLoading = true

                                try {
                                    val idPart = id.toRequestBody("text/plain".toMediaTypeOrNull())

                                    val imageParts = selectedUris.map { uri ->
                                        val bitmap = MediaStore.Images.Media.getBitmap(
                                            context.contentResolver,
                                            uri
                                        )
                                        val compressedFile =
                                            resizeAndCompress(bitmap, maxSize = 800, quality = 85)
                                        uriToMultipart(context, compressedFile.toUri(), "images")
                                    }


                                    val response = api.reenrollStudentEmbeddings(
                                        images = imageParts,
                                        studentId = idPart,
                                        email = currentUserEmail
                                    )

                                    if (response.isSuccessful) {
                                        response.body()?.let { body ->
                                            Toast.makeText(context, "${body.message} | ${body.total_students}", Toast.LENGTH_SHORT).show()
                                            showReEnroll = false
                                        }
                                    } else {
                                        response.errorBody()?.let { Log.e("ERROR", it.string()) }
                                        response.errorBody()?.let {
                                            Toast.makeText(
                                                context,
                                                it.string(),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }

                                } catch (e: Exception) {
                                    apiMessage = "Exception: ${e.localizedMessage}"
                                } finally {
                                    isLoading = false
                                    showReEnroll = false
                                }
                            }
                        },
                    shape = RoundedCornerShape(25.dp),
                    elevation = CardDefaults.cardElevation(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isLoading -> Color(0xFF3A3A3A) // dimmed while loading
                            selectedUris.size <= 3 && selectedUris.isNotEmpty() -> Color(0xFF468A9A) // teal active
                            else -> Color(0xFF541212) // red inactive
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(id = R.string.registering),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = "Register",
                                tint = Color.Black
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(id = R.string.register),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.Black
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

}