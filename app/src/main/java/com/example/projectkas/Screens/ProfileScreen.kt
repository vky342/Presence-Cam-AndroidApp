package com.example.projectkas.Screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.projectkas.Network.RetrofitInstance.api
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
            text = "Edit Profile",
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

        /* Handle Edit Picture */

        }) {
            Text("Edit")
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Name and Roll No Fields
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
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
            label = { Text("Roll No") },
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
                        "Saving....",
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
                    Text("Save")
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
                        "Deleting....",
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
                    Text("Delete")
                }
            }
        }
    }

    if (showPickerDialog) {
        AlertDialog(
            onDismissRequest = { showPickerDialog = false },

            title = { Text("Are you sure?") },
            text = { Text("Selected Student : $studentName") },
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

                }) { Text("Delete", color = Color.Red) }
            },

            dismissButton = {
                TextButton(onClick = {
                    showPickerDialog = false
                }) { Text("Cancel" ,color = Color.Green) }
            }
        )
    }
}

