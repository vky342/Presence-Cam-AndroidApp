package com.example.projectkas.Screens

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.projectkas.Network.ClassUi
import com.example.projectkas.Network.RecognizeResponse
import com.example.projectkas.Network.RecognizedStudent
import com.example.projectkas.Network.RetrofitInstance
import com.example.projectkas.Network.Student
import com.example.projectkas.Network.uriToMultipart
import com.example.projectkas.R
import com.example.projectkas.ViewModel.AttendanceViewModel
import com.example.projectkas.ViewModel.AuthViewModel
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnrememberedGetBackStackEntry", "StringFormatMatches")
@Composable
fun Home(
    navController: NavController,
    attendanceViewModel: AttendanceViewModel = hiltViewModel()
){

    val parentEntry = remember(navController) { navController.getBackStackEntry("main") }
    val authViewModel: AuthViewModel = hiltViewModel(parentEntry)

    val currentUserEmail = authViewModel.auth.currentUser?.email ?: ""

    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    val currentDateTime = dateFormat.format(Date())

    val context = LocalContext.current
    var selectedPhotoUris by rememberSaveable { mutableStateOf(listOf<Uri>()) }
    val recognizedList = attendanceViewModel.recognizedList

    val debugMode by authViewModel.debugMode.collectAsState()

    var selectedStudentToDelete: RecognizedStudent? by remember { mutableStateOf<RecognizedStudent?>(null) }

    var apiResponse = attendanceViewModel.apiResponse
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteClassDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    var classes by remember { mutableStateOf<List<ClassUi>>(emptyList()) }
    val selectedClass = attendanceViewModel.selectedClass
    var expanded by remember { mutableStateOf(false) }
    var isClassLoading by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    var latestImageUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 4),
        onResult = { uri ->
            selectedPhotoUris = selectedPhotoUris + uri
        }
    )


    fun createImageFileUri(context: Context): Uri {
        val file = File(context.cacheDir, "camera_photo_${System.currentTimeMillis()}.jpg")
        try {
            file.parentFile?.mkdirs()
            if (!file.exists()) file.createNewFile()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    }


    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                latestImageUri?.let { uri ->
                    selectedPhotoUris = selectedPhotoUris + uri
                }
            } else {
                Toast.makeText(context, context.getString(R.string.camera_cancelled_or_failed), Toast.LENGTH_SHORT).show()
            }
        }
    )

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        uri?.let {
            try {
                val pdfDocument = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
                val page = pdfDocument.startPage(pageInfo)

                val canvas = page.canvas
                val paint = Paint().apply {
                    textSize = 14f
                    isFakeBoldText = true
                }
                val normalPaint = Paint().apply {
                    textSize = 12f
                }

                var y = 60f
                val startX = 50f
                val col1X = 50f
                val col2X = 200f
                val tableWidth = 500f
                val rowHeight = 30f

                paint.isFakeBoldText = true

                canvas.drawText(context.getString(R.string.attendance_list), startX, y, paint)

                val dateX = pageInfo.pageWidth - paint.measureText(currentDateTime) - 50f
                canvas.drawText(currentDateTime, dateX, y, paint)

                y += 40f

                paint.textSize = 14f
                canvas.drawText(context.getString(R.string.roll_no), col1X, y, paint)
                canvas.drawText(context.getString(R.string.name), col2X, y, paint)

                canvas.drawLine(col1X, y + 5f, col1X + tableWidth, y + 5f, paint)
                y += rowHeight

                recognizedList.sortedBy { it.roll_no }.forEach { student ->
                    canvas.drawText(student.roll_no, col1X, y, normalPaint)
                    canvas.drawText(student.name ?: context.getString(R.string.unknown), col2X, y, normalPaint)
                    y += rowHeight
                }

                pdfDocument.finishPage(page)

                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }

                pdfDocument.close()
                Toast.makeText(context, context.getString(R.string.pdf_saved_successfully), Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.failed_with_error, e.message), Toast.LENGTH_SHORT).show()
            }
        }
    }


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


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Color(24, 23, 23)
            )
            .padding(top = 4.dp, start = 16.dp, end = 16.dp, bottom = 4.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = stringResource(id = R.string.presence_cam),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Divider(
            color = Color(0xFF468A9A),
            thickness = 2.dp,
            modifier = Modifier
                .width(180.dp)
                .align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(24.dp))

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
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                label = { Text("Select Class") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
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
                            attendanceViewModel.setClass(cls)
                            expanded = false
                            focusManager.clearFocus()
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        ImagePickerContainer(
            selectedPhotoUris = selectedPhotoUris,
            onAddUpload = { galleryLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            ) },
            onAddCapture = {
                val newUri = createImageFileUri(context)
                latestImageUri = newUri
                cameraLauncher.launch(newUri)
            },
            onRemove = { uri -> selectedPhotoUris = selectedPhotoUris - uri },
            enable = recognizedList.isEmpty()
        )


        Spacer(modifier = Modifier.height(20.dp))

        if (isLoading) {
            Card(
                modifier = Modifier
                    .height(55.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(25.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF3A3A3A))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(id = R.string.processing),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
            }
        } else {
            ActionCard(
                text = if (recognizedList.isEmpty() && apiResponse == null ) stringResource(id = R.string.check_attendance) else stringResource(id = R.string.reset),
                icon = if (recognizedList.isEmpty() && apiResponse == null) Icons.AutoMirrored.Filled.FactCheck else Icons.Default.Refresh,
                color = if (recognizedList.isEmpty() && apiResponse == null) Color(0xFF468A9A) else Color(0xFF541212),
                enabled = selectedPhotoUris.isNotEmpty() || recognizedList.isNotEmpty()
            ) {
                if (apiResponse == null && recognizedList.isEmpty() && selectedClass != null) {
                    coroutineScope.launch {
                        isLoading = true
                        try {
                            selectedPhotoUris.let { uris ->
                                val imageParts = uris.map { uri ->
                                    uriToMultipart(context, uri, "files")
                                }

                                Log.e("DBG", "classId string = ${selectedClass.id}")

                                val classIdBody =
                                    selectedClass!!.id.toRequestBody("text/plain".toMediaType())

                                val response = if (debugMode) {
                                    RetrofitInstance.api.debugRecognize(
                                        files = imageParts,
                                        email = currentUserEmail
                                    )
                                } else {
                                    Log.e(
                                        "ERR",
                                        "Home: ${imageParts} ${currentUserEmail} ${classIdBody}",
                                    )
                                    RetrofitInstance.api.recognize(
                                        files = imageParts,
                                        email = currentUserEmail,
                                        classId = classIdBody
                                    )
                                }

                                response?.let { res ->
                                    if (res.isSuccessful) {
                                        val body = res.body()
                                        attendanceViewModel.apiResponse = body
                                        Log.e("ERR", "Home: $apiResponse")
                                        attendanceViewModel.setRecognized(
                                            body?.recognized_students ?: emptyList()
                                        )
                                    } else {
                                        val code = res.code()
                                        val msg = res.message()
                                        val err = try {
                                            res.errorBody()?.string()
                                        } catch (_: Exception) {
                                            null
                                        }
                                        Log.e(
                                            "API",
                                            "Unsuccessful: code=$code, message=$msg, error=$err"
                                        )
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.api_error, code),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } ?: run {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.null_response_from_server),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                            }
                        } catch (e: Exception) {
                            Log.e("DEBUG", "Exception: ${e.message}")
                            Toast.makeText(
                                context,
                                context.getString(R.string.exception, e.message),
                                Toast.LENGTH_SHORT
                            ).show()
                        } finally {
                            isLoading = false
                        }
                    }
                }
                 else{
                    showResetDialog = true
                }
            }
        }

        Spacer(Modifier.height(30.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C))
        ) {
            Column(Modifier.padding(vertical = 12.dp, horizontal = 12.dp)) {
                Text(
                    stringResource(id = R.string.recognized_students),
                    modifier = Modifier
                        .padding(horizontal = 5.dp)
                        .align(Alignment.CenterHorizontally),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.height(8.dp))

                AttendanceTable(
                    recognizedList,
                    onDelete = {
                        selectedStudentToDelete = it
                        showDeleteClassDialog = true
                               },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(400.dp)
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SmallActionCard(
                        text = stringResource(id = R.string.add),
                        icon = Icons.Default.PersonAdd,
                        color = Color(0xFF468A9A),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (selectedClass != null){
                            showAddDialog = true
                        }
                        Toast.makeText(context, "Select Class Please", Toast.LENGTH_SHORT).show()


                    }

                    SmallActionCard(
                        text = stringResource(id = R.string.save),
                        icon = Icons.Default.PictureAsPdf,
                        color = Color(0xFF8A9A46),
                        modifier = Modifier.weight(1f)
                    ) { createDocumentLauncher.launch("attendance_list.pdf") }
                }
            }
        }

        if (debugMode) {
            Spacer(Modifier.height(20.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C))
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        stringResource(id = R.string.debug_returned_images), // Assumes this string exists
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(Modifier.height(8.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        apiResponse?.annotated_all?.let { base64 ->
                            DebugImageCard(stringResource(id = R.string.all_faces), base64) // Assumes this string exists
                        }
                        apiResponse?.annotated_unrecognized?.let { base64 ->
                            DebugImageCard(stringResource(id = R.string.unrecognized), base64) // Assumes this string exists
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    SmallActionCard(
                        text = stringResource(id = R.string.save_images),
                        icon = Icons.Default.Save,
                        color = Color(0xFF468A9A),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        apiResponse?.annotated_all?.let { saveBase64Image(context, it, "annotated_all.jpg") }
                        apiResponse?.annotated_unrecognized?.let { saveBase64Image(context, it, "annotated_unrecognized.jpg") }
                        Toast.makeText(context, context.getString(R.string.images_saved_to_storage), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    if (showDeleteClassDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteClassDialog = false
            },
            title = {
                Text("remove this student?", color = Color.White)
            },
            text = {
                Text(
                    text = "Are you sure you want to remove this student from attendance list?",
                    color = Color.LightGray
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedStudentToDelete?.let { attendanceViewModel.removeStudent(it) }
                        showDeleteClassDialog = false
                    }
                ) {
                    Text("Remove", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        selectedStudentToDelete = null
                        showDeleteClassDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            },
            containerColor = Color(0xFF2C2C2C)
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = {
                showResetDialog = false
            },
            title = {
                Text("Reset", color = Color.White)
            },
            text = {
                Text(
                    text = "Are you sure you want to reset? All the unsaved attendance will be lost along with the picture.",
                    color = Color.LightGray
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        apiResponse = null
                        attendanceViewModel.clear()
                        attendanceViewModel.resetClass()
                        selectedPhotoUris = emptyList()
                        attendanceViewModel.apiResponse = null
                        showResetDialog = false
                    }
                ) {
                    Text("Reset", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            },
            containerColor = Color(0xFF2C2C2C)
        )
    }

    if (showAddDialog) {
        var enrollInput by remember { mutableStateOf("") }
        var nameInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(stringResource(id = R.string.add_student), color = Color.White) }, // Assumes this string exists
            text = {
                Column {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text(stringResource(id = R.string.name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = enrollInput,
                        onValueChange = { enrollInput = it },
                        label = { Text(stringResource(id = R.string.roll_no)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                SmallActionCard(
                    text = stringResource(id = R.string.add),
                    icon = Icons.Default.Check,
                    color = Color(0xFF468A9A),
                    modifier = Modifier.width(100.dp)
                ) {
                    if (enrollInput.isNotBlank() && nameInput.isNotBlank()) {
                        attendanceViewModel.addStudent(
                            RecognizedStudent(roll_no = enrollInput, name = nameInput)
                        )
                    }
                    showAddDialog = false
                }
            },
            dismissButton = {
                SmallActionCard (
                    text = stringResource(id = R.string.cancel), // Assumes this string exists
                    icon = Icons.Default.Close,
                    color = Color(0xFF541212),
                    modifier = Modifier.width(100.dp)
                ) {
                    showAddDialog = false
                }
            },
            containerColor = Color(0xFF2C2C2C)
        )

    }
}

@Composable
fun AttendanceTable(
    recognizedList: List<RecognizedStudent>,
    modifier: Modifier = Modifier,
    onDelete: (RecognizedStudent) -> Unit
) {

    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 5.dp)
    ) {
        if (recognizedList.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF3A3A3A)
                ),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.no_faces_recognized),
                    color = Color.Gray,
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.CenterHorizontally),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            recognizedList.sortedBy { it.roll_no }.forEach { student ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3A3A3A))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Spacer(Modifier.width(10.dp))

                        Column(Modifier.weight(1f)) {
                            // Name
                            Text(
                                text = stringResource(id = R.string.student_name, student.name ?: stringResource(id = R.string.unknown)),
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White
                            )
                            // Roll no
                            Text(
                                text = stringResource(id = R.string.student_roll_no, student.roll_no),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.LightGray
                            )
                        }

                        Box {
                            IconButton(onClick = { expanded = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = stringResource(id = R.string.more_options),
                                    tint = Color.White
                                )
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Remove") },
                                    onClick = {
                                        onDelete(student)
                                        expanded = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = stringResource(id = R.string.delete)
                                        )
                                    }
                                )
                            }
                        }

                    }
                }
            }
        }
    }
}

@Composable
fun ActionCard(
    text: String,
    icon: ImageVector,
    color: Color,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(55.dp)
            .fillMaxWidth() // Added to ensure it fills width
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(25.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = if (enabled) color else Color.DarkGray)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = text, tint = Color.Black)
            Spacer(Modifier.width(8.dp))
            Text(text, color = Color.Black, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@SuppressLint("FrequentlyChangingValue")
@Composable
fun ImagePickerContainer(
    selectedPhotoUris: List<Uri>,
    onAddUpload: () -> Unit,
    onAddCapture: () -> Unit,
    onRemove: (Uri) -> Unit,
    enable : Boolean
) {

    val listState = rememberLazyListState()

    Column(
        modifier = Modifier.fillMaxWidth(0.9f)
    ) {
        if (selectedPhotoUris.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF2C2C2C))
                    .clickable(enable) { onAddUpload() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(id = R.string.tap_to_upload_or_capture), color = Color.Gray)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = onAddUpload) {
                            Icon(Icons.Default.PhotoLibrary, null, tint = Color.White)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(id = R.string.upload), color = Color.White)
                        }
                        OutlinedButton(onClick = onAddCapture) {
                            Icon(Icons.Default.PhotoCamera, null, tint = Color.White)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(id = R.string.capture), color = Color.White)
                        }
                    }
                }
            }
        }
        else if ( selectedPhotoUris.size < 4 ) {
            Box(modifier = Modifier.height(90.dp)) {
                LazyRow(
                    state = listState,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF2C2C2C))
                                .clickable(enable) {
                                    onAddCapture()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = stringResource(id = R.string.add), tint = Color.White)
                        }

                    }

                    items(selectedPhotoUris) { uri ->
                        Box {
                            Image(
                                painter = rememberAsyncImagePainter(uri),
                                contentDescription = stringResource(id = R.string.selected_image),
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { onRemove(uri) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(id = R.string.remove), tint = Color.White)
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(40.dp)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, Color(0x6DDDE0E0))
                            )
                        )
                )

            }
        }
        else {
            Box(modifier = Modifier.height(90.dp)) {

                LazyRow(
                    state = listState,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(selectedPhotoUris) { uri ->
                        Box {
                            Image(
                                painter = rememberAsyncImagePainter(uri),
                                contentDescription = stringResource(id = R.string.selected_image),
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { onRemove(uri) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(id = R.string.remove), tint = Color.White)
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(40.dp)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, Color(0x6DDDE0E0))
                            )
                        )
                )

            }
        }
    }
}

@Composable
fun SmallActionCard(
    text: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(40.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = Color.Black,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text,
                color = Color.Black,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun DebugImageCard(label: String, base64: String) {
    val bitmap = remember(base64) {
        decodeBase64Scaled(base64, 800, 800)
    }

    if (bitmap != null) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = label,
                modifier = Modifier
                    .size(150.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(4.dp))
            Text(label, color = Color.White, style = MaterialTheme.typography.bodySmall)
        }
    } else {
        Text(stringResource(id = R.string.failed_to_load_image, label), color = Color.Red)
    }
}

fun saveBase64Image(context: Context, base64: String, fileName: String) {
    try {
        val imageBytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, context.getString(R.string.failed_to_save_file, fileName), Toast.LENGTH_SHORT).show()
    }
}

fun decodeBase64Scaled(base64: String, reqWidth: Int, reqHeight: Int): Bitmap? {
    return try {
        val imageBytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)

        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false

        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val (height: Int, width: Int) = options.run { outHeight to outWidth }
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2

        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

