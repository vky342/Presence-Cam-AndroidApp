package com.example.projectkas.Screens

import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.projectkas.Network.ClassUi
import com.example.projectkas.Network.RetrofitInstance
import com.example.projectkas.Network.resizeAndCompress
import com.example.projectkas.Network.uriToMultipart
import com.example.projectkas.R
import com.example.projectkas.ViewModel.AuthViewModel
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import kotlin.collections.plus


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Register(navController: NavController, authViewModel: AuthViewModel = hiltViewModel()){

    val currentUserEmail = authViewModel.auth.currentUser?.email ?: ""

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var Rollno by remember { mutableStateOf("") }
    var StudentName by remember { mutableStateOf("") }
    var selectedUris by rememberSaveable { mutableStateOf<List<Uri>>(emptyList()) }
    var apiMessage by remember { mutableStateOf<String?>(null) }

    var isLoading by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current


    // CLass Block

    var classes by remember { mutableStateOf<List<ClassUi>>(emptyList()) }
    var selectedClass by remember { mutableStateOf<ClassUi?>(null) }
    var expanded by remember { mutableStateOf(false) }

    var showCreateClassDialog by remember { mutableStateOf(false) }
    var newClassName by remember { mutableStateOf("") }
    var isClassLoading by remember { mutableStateOf(false) }

    LaunchedEffect(currentUserEmail) {
        try {
            isClassLoading = true
            val res = RetrofitInstance.api.getClasses(
                userEmail = currentUserEmail
            )
            classes = res.classes
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to load classes", Toast.LENGTH_SHORT).show()
        } finally {
            isClassLoading = false
        }
    }


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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color(24, 23, 23))
            .padding(top = 4.dp, start = 16.dp, end = 16.dp, bottom = 4.dp)
            .verticalScroll(rememberScrollState())
        ,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.student_registration),
            textAlign = TextAlign.Center,
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Divider(
            color = Color(0xFF468A9A), // teal accent
            thickness = 2.dp,
            modifier = Modifier.width(180.dp)
        )

        Spacer(modifier = Modifier.height(25.dp))

        // NEW CLASS BLOCK

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedClass?.name ?: "",
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                label = { Text("Select Class") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = {
                    focusManager.moveFocus(FocusDirection.Down)
                }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(24, 23, 23),
                    unfocusedContainerColor = Color(24, 23, 23),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.LightGray,
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.Gray,
                    focusedIndicatorColor = Color.White,
                    unfocusedIndicatorColor = Color.Gray
                )
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                    focusManager.clearFocus()
                }
            ) {
                classes.forEach { cls ->
                    DropdownMenuItem(
                        text = { Text(cls.name) },
                        onClick = {
                            selectedClass = cls
                            expanded = false
                            focusManager.moveFocus(focusDirection = FocusDirection.Down)
                        }
                    )
                }

                Divider()

                DropdownMenuItem(
                    text = { Text("➕ Create new class") },
                    onClick = {
                        expanded = false
                        showCreateClassDialog = true
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(25.dp))

        OutlinedTextField(
            value = Rollno,
            onValueChange = { Rollno = it },
            label = { Text(stringResource(id = R.string.roll_no)) },
            textStyle = TextStyle(fontSize = 18.sp, color = Color.White),
            singleLine = true,
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
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = {
                focusManager.moveFocus(FocusDirection.Down)
            })
        )

        OutlinedTextField(
            value = StudentName,
            onValueChange = { StudentName = it },
            label = { Text(stringResource(id = R.string.name)) },            textStyle = TextStyle(fontSize = 18.sp, color = Color.White),
            singleLine = true,
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
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
            })
        )

        Spacer(modifier = Modifier.height(20.dp))

        MultiImagePickerContainer(
            selectedUris = selectedUris,
            onUpload = { galleryLauncher.launch(arrayOf("image/*")) },
            onCapture = { captureImage() },
            onClear = { uri -> selectedUris = selectedUris - uri }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Register Button
        Card(
            modifier = Modifier
                .height(55.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable(
                    enabled = selectedUris.isNotEmpty() && Rollno.isNotBlank() && StudentName.isNotBlank() && !isLoading
                ) {
                    coroutineScope.launch {
                        if (
                            Rollno.isBlank() ||
                            StudentName.isBlank() ||
                            selectedUris.isEmpty() ||
                            selectedUris.size > 3 ||
                            selectedClass == null
                        ) {
                            apiMessage = context.getString(R.string.enroll_no_and_3_images)
                            return@launch
                        }

                        isLoading = true

                        try {
                            val enrollPart = Rollno.toRequestBody("text/plain".toMediaTypeOrNull())
                            val studentPart = StudentName.toRequestBody("text/plain".toMediaTypeOrNull())
                            val classIdPart =
                                selectedClass!!.id.toRequestBody("text/plain".toMediaTypeOrNull())


                            val imageParts = selectedUris.map { uri ->
                                val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                                val compressedFile = resizeAndCompress(bitmap, maxSize = 800, quality = 85)
                                uriToMultipart(context, compressedFile.toUri(), "images")
                            }


                                val response = RetrofitInstance.api.register(
                                imageParts,
                                    Rollno = enrollPart,
                                    studentName = studentPart,
                                    email = currentUserEmail,
                                    classId = classIdPart
                            )

                            if (response.isSuccessful) {
                                response.body()?.let { body ->
                                    apiMessage =
                                        "${body.message} | ${context.getString(R.string.total_students_message, body.total_students)}"
                                    // Reset form
                                    Rollno = ""
                                    StudentName = ""
                                    selectedUris = emptyList()
                                }
                            } else {
                                response.errorBody()?.let { Log.e("ERROR",it.string()) }
                                apiMessage = context.getString(R.string.api_error, response.errorBody()?.string() ?: "")
                            }

                        } catch (e: Exception) {
                            apiMessage = context.getString(R.string.exception, e.localizedMessage)
                        } finally {
                            isLoading = false
                        }
                    }
                },
            shape = RoundedCornerShape(25.dp),
            elevation = CardDefaults.cardElevation(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    isLoading -> Color(0xFF3A3A3A) // dimmed while loading
                    selectedUris.isNotEmpty() && Rollno.isNotBlank() && StudentName.isNotBlank() -> Color(0xFF468A9A) // teal active
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

        // Observe apiMessage changes
        apiMessage?.let { message ->
            LaunchedEffect(message) {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }

        // ---------------- CREATE CLASS DIALOG ----------------

        if (showCreateClassDialog) {
            AlertDialog(
                onDismissRequest = { showCreateClassDialog = false },
                title = { Text("Create Class") },
                text = {
                    OutlinedTextField(
                        value = newClassName,
                        onValueChange = { newClassName = it },
                        label = { Text("Class name") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (newClassName.isBlank()) {
                                Toast.makeText(context, "Class name cannot be empty", Toast.LENGTH_SHORT).show()
                                return@TextButton
                            }

                            coroutineScope.launch {
                                try {
                                    val created = RetrofitInstance.api.createClass(
                                        userEmail = currentUserEmail,
                                        name = newClassName.trim()
                                    )

                                    classes = classes + created
                                    selectedClass = created
                                    newClassName = ""
                                    showCreateClassDialog = false
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "Class already exists or failed",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    ) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateClassDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

    }

}


@Composable
fun MultiImagePickerContainer(
    selectedUris: List<Uri>,
    onUpload: () -> Unit,
    onCapture: () -> Unit,
    onClear: (Uri) -> Unit
) {
    var showPickerDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF2C2C2C))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (selectedUris.isEmpty()) {
            // Empty state → upload/capture options
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "Placeholder",
                    tint = Color.Gray,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(stringResource(id = R.string.add_photos), color = Color.Gray)

                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onUpload) {
                        Icon(Icons.Default.PhotoLibrary, null, tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(id = R.string.upload), color = Color.White)
                    }
                    OutlinedButton(onClick = onCapture) {
                        Icon(Icons.Default.PhotoCamera, null, tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(id = R.string.capture), color = Color.White)
                    }
                }
            }
        } else {
            // Thumbnails of selected images
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                selectedUris.forEach { uri ->
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(uri),
                            contentDescription = "Selected Image",
                            modifier = Modifier.matchParentSize(),
                            contentScale = ContentScale.Fit // 👈 keeps full photo visible
                        )
                        IconButton(
                            onClick = { onClear(uri) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                .size(20.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(id = R.string.remove), tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
                // Empty slots placeholders (if < 3)
                // Inside the Row where you show placeholders
                repeat(3 - selectedUris.size) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.DarkGray.copy(alpha = 0.4f))
                            .clickable { showPickerDialog = true }, // 👈 show dialog
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = "Add", tint = Color.Gray, modifier = Modifier.size(28.dp))
                            Text(stringResource(id = R.string.add), color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }


            }

            // Progress indicator → e.g. "2/3 selected"
            Text(
                text = stringResource(id = R.string.selected_of_3, selectedUris.size),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )

        }

        Spacer(modifier = Modifier.height(3.dp))

        // Header
        Text(
            text = stringResource(id = R.string.note),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFFDA5555) // themed red
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Body
        Text(
            text = stringResource(id = R.string.register_note),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
            color = Color(0xFFB0B0B0) // softer gray for readability
        )
    }

    // dialog
    if (showPickerDialog) {
        AlertDialog(
            onDismissRequest = { showPickerDialog = false },

            title = { Text(stringResource(id = R.string.add_image)) },
            text = { Text(stringResource(id = R.string.choose_a_method)) },
            confirmButton = {
                TextButton(onClick = {
                    showPickerDialog = false
                    onCapture()
                }) { Text(stringResource(id = R.string.capture)) }
            },

            dismissButton = {
                TextButton(onClick = {
                    showPickerDialog = false
                    onUpload()
                }) { Text(stringResource(id = R.string.upload)) }
            }
        )
    }
}
