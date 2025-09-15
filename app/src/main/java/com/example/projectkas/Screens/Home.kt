package com.example.projectkas.Screens

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.projectkas.Network.RecognizeResponse
import com.example.projectkas.Network.RecognizedStudent
import com.example.projectkas.Network.RetrofitInstance
import com.example.projectkas.Network.uriToMultipart
import com.example.projectkas.ViewModel.AttendanceViewModel
import com.example.projectkas.ViewModel.AuthViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@SuppressLint("UnrememberedGetBackStackEntry")
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

    var apiResponse by remember { mutableStateOf<RecognizeResponse?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    // NEW: holds the URI that the camera should write the full-res photo to
    var latestImageUri by remember { mutableStateOf<Uri?>(null) }

    // Launcher for gallery
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                selectedPhotoUris = selectedPhotoUris + uri
            }
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
                Toast.makeText(context, "Camera cancelled or failed", Toast.LENGTH_SHORT).show()
            }
        }
    )

    // Launcher to create document
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
                val col1X = 50f     // Roll No column start
                val col2X = 200f    // Name column start
                val tableWidth = 500f
                val rowHeight = 30f

                // Title
                paint.isFakeBoldText = true

                // Title left
                canvas.drawText("Attendance List", startX, y, paint)

                // Date right
                val dateX = pageInfo.pageWidth - paint.measureText(currentDateTime) - 50f
                canvas.drawText(currentDateTime, dateX, y, paint)

                y += 40f

                // Header row
                paint.textSize = 14f
                canvas.drawText("Roll No", col1X, y, paint)
                canvas.drawText("Name", col2X, y, paint)

                // Horizontal line under header
                canvas.drawLine(col1X, y + 5f, col1X + tableWidth, y + 5f, paint)
                y += rowHeight

                // Data rows
                recognizedList.sortedBy { it.roll_no }.forEach { student ->
                    canvas.drawText(student.roll_no, col1X, y, normalPaint)
                    canvas.drawText(student.name ?: "Unknown", col2X, y, normalPaint)
                    y += rowHeight
                }

                pdfDocument.finishPage(page)

                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }

                pdfDocument.close()
                Toast.makeText(context, "PDF saved successfully!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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
            text = "Presence Cam",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Divider(
            color = Color(0xFF468A9A), // teal accent
            thickness = 2.dp,
            modifier = Modifier
                .width(180.dp)
                .align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(24.dp))

        ImagePickerContainer(
            selectedPhotoUris = selectedPhotoUris,
            onAddUpload = { galleryLauncher.launch(arrayOf("image/*")) },
            onAddCapture = {
                val newUri = createImageFileUri(context)
                latestImageUri = newUri
                cameraLauncher.launch(newUri)
            },
            onRemove = { uri -> selectedPhotoUris = selectedPhotoUris - uri }
        )


        Spacer(modifier = Modifier.height(20.dp))

        // Send / Reset
        if (isLoading) {
            // Show loading card instead of ActionCard
            Card(
                modifier = Modifier
                    .height(55.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(25.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF3A3A3A)) // dimmed while loading
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
                        "Processing...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
            }
        } else {
            // Use ActionCard normally
            ActionCard(
                text = if (recognizedList.isEmpty() && apiResponse == null) "Check Attendance" else "Reset",
                icon = if (recognizedList.isEmpty() && apiResponse == null) Icons.Default.FactCheck else Icons.Default.Refresh,
                color = if (recognizedList.isEmpty() && apiResponse == null) Color(0xFF468A9A) else Color(0xFF541212),
                enabled = selectedPhotoUris != null
            ) {
                if (apiResponse == null && recognizedList.isEmpty()) {
                    coroutineScope.launch {
                        isLoading = true
                        try {
                            selectedPhotoUris.let { uris ->
                                val imageParts = uris.map { uri ->
                                    uriToMultipart(context, uri, "files")  // 👈 use "files" for each
                                }

                                val response = if (debugMode) {
                                    RetrofitInstance.api.debugRecognize(
                                        files = imageParts,
                                        email = currentUserEmail
                                    )
                                } else {
                                    RetrofitInstance.api.recognize(
                                        files = imageParts,
                                        email = currentUserEmail
                                    )
                                }

                                response?.let { res ->
                                    if (res.isSuccessful) {
                                        val body = res.body()
                                        apiResponse = body
                                        attendanceViewModel.setRecognized(body?.recognized_students ?: emptyList())
                                    } else {
                                        val code = res.code()
                                        val msg = res.message()
                                        val err = try { res.errorBody()?.string() } catch (_: Exception) { null }
                                        Log.e("API", "Unsuccessful: code=$code, message=$msg, error=$err")
                                        Toast.makeText(context, "API Error ($code)", Toast.LENGTH_SHORT).show()
                                    }
                                } ?: run {
                                    Toast.makeText(context, "Null response from server", Toast.LENGTH_SHORT).show()
                                }

                            }
                        } catch (e: Exception) {
                            Log.e("DEBUG","Exception: ${e.message}")
                            Toast.makeText(context, "Exception: ${e.message}", Toast.LENGTH_SHORT).show()
                        } finally {
                            isLoading = false
                        }
                    }
                } else {
                    apiResponse = null
                    attendanceViewModel.clear()
                    selectedPhotoUris = emptyList()
                }
            }
        }

        Spacer(Modifier.height(30.dp))

        // Attendance table
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C))
        ) {
            Column(Modifier.padding(vertical = 12.dp, horizontal = 12.dp)) {
                // Header
                Text(
                    "Recognized Students",
                    modifier = Modifier.padding(horizontal = 5.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.height(8.dp))

                // Table
                AttendanceTable(
                    recognizedList,
                    onDelete = { attendanceViewModel.removeStudent(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(400.dp) // compact height
                )

                Spacer(Modifier.height(8.dp))

                // Integrated action row (smaller buttons)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SmallActionCard(
                        text = "Add",
                        icon = Icons.Default.PersonAdd,
                        color = Color(0xFF468A9A),
                        modifier = Modifier.weight(1f)
                    ) { showAddDialog = true }

                    SmallActionCard(
                        text = "Save",
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
                        "Debug Returned Images",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(Modifier.height(8.dp))

                    // Column instead of Row
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        apiResponse?.annotated_all?.let { base64 ->
                            DebugImageCard("All Faces", base64)
                        }
                        apiResponse?.annotated_unrecognized?.let { base64 ->
                            DebugImageCard("Unrecognized", base64)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Save button
                    SmallActionCard(
                        text = "Save Images",
                        icon = Icons.Default.Save,
                        color = Color(0xFF468A9A),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Save both if available
                        apiResponse?.annotated_all?.let { saveBase64Image(context, it, "annotated_all.jpg") }
                        apiResponse?.annotated_unrecognized?.let { saveBase64Image(context, it, "annotated_unrecognized.jpg") }
                        Toast.makeText(context, "Images saved to storage", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }


    }

    if (showAddDialog) {
        var enrollInput by remember { mutableStateOf("") }
        var nameInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Student", color = Color.White) },
            text = {
                Column {
                    OutlinedTextField(
                        value = enrollInput,
                        onValueChange = { enrollInput = it },
                        label = { Text("Roll No") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                SmallActionCard(
                    text = "Add",
                    icon = Icons.Default.Check,
                    color = Color(0xFF468A9A),
                    modifier = Modifier.width(100.dp) // compact
                ) {
                    if (enrollInput.isNotBlank() && nameInput.isNotBlank()) {
                        // ✅ Add to recognized students
                        attendanceViewModel.addStudent(
                            RecognizedStudent(roll_no = enrollInput, name = nameInput)
                        )
                    }
                    showAddDialog = false
                }
            },
            dismissButton = {
                SmallActionCard (
                    text = "Cancel",
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
    onDelete: (RecognizedStudent) -> Unit // 👈 callback
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 5.dp)
    ) {
        if (recognizedList.isEmpty()) {
            // Empty state
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF3A3A3A) // 👈 lighter grey than background
                ),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Text(
                    text = "No faces recognized",
                    color = Color.Gray,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            // Student list
            recognizedList.sortedBy { it.roll_no }.forEach { student ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3A3A3A))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        // Delete button as left end cap
                        Box(
                            modifier = Modifier
                                .height(40.dp)
                                .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)) // 👈 rounded only left side
                                .background(Color(0xFFE57373)) // softer red
                                .clickable { onDelete(student) }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Roll No: ${student.roll_no}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )
                            Text(
                                text = "Name: ${student.name ?: "Unknown"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )
                        }

                        // Profile icon
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Student",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )


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

@Composable
fun ImagePickerContainer(
    selectedPhotoUris: List<Uri>,
    onAddUpload: () -> Unit,
    onAddCapture: () -> Unit,
    onRemove: (Uri) -> Unit
) {
    var showAddMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState() // 👈 to check scroll

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (selectedPhotoUris.isEmpty()) {
            // Empty placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF2C2C2C))
                    .clickable { onAddUpload() },
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
                    Text("Tap to upload or capture", color = Color.Gray)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = onAddUpload) {
                            Icon(Icons.Default.PhotoLibrary, null, tint = Color.White)
                            Spacer(Modifier.width(6.dp))
                            Text("Upload", color = Color.White)
                        }
                        OutlinedButton(onClick = onAddCapture) {
                            Icon(Icons.Default.PhotoCamera, null, tint = Color.White)
                            Spacer(Modifier.width(6.dp))
                            Text("Capture", color = Color.White)
                        }
                    }
                }
            }
        } else {
            // Scrollable thumbnail strip with fade indicator
            Box(modifier = Modifier.height(90.dp)) {
                LazyRow(
                    state = listState,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Add more button
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF2C2C2C))
                                .clickable { showAddMenu = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                        }

                        if (showAddMenu) {
                            AlertDialog(
                                onDismissRequest = { showAddMenu = false },
                                title = {
                                    Text(
                                        "Add Image",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                },
                                text = {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().height(80.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {

                                        SmallActionCard(
                                            text = "Upload from Gallery",
                                            icon = Icons.Default.UploadFile,
                                            color = Color(0xFF468A9A),
                                            modifier = Modifier.weight(1f) // compact
                                        ) {
                                            showAddMenu = false
                                            onAddUpload()
                                        }

                                        SmallActionCard(
                                            text = "Capture with Camera",
                                            icon = Icons.Default.PhotoCamera,
                                            color = Color(0xFF8A9A46),
                                            modifier = Modifier.weight(1f)
                                        ) { showAddMenu = false
                                            onAddCapture() }

                                    }
                                },
                                confirmButton = {},
                                containerColor = Color(0xFF2C2C2C),
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }

                    // Selected images
                    items(selectedPhotoUris) { uri ->
                        Box {
                            Image(
                                painter = rememberAsyncImagePainter(uri),
                                contentDescription = "Selected image",
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
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White)
                            }
                        }
                    }
                }

                // Fade indicator on right edge if scrollable
                val showFade = listState.layoutInfo.totalItemsCount > 0 &&
                        (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                            ?: 0) < listState.layoutInfo.totalItemsCount - 1

                if (showFade) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(40.dp)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color.Transparent, Color(0xFF2C2C2C))
                                )
                            )
                    )
                }
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
            .height(40.dp) // smaller height than before
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
                modifier = Modifier.size(16.dp) // smaller icon
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text,
                color = Color.Black,
                style = MaterialTheme.typography.bodyMedium // smaller text
            )
        }
    }
}

@Composable
fun DebugImageCard(label: String, base64: String) {
    val bitmap = remember(base64) {
        decodeBase64Scaled(base64, 800, 800) // scale down to max 800x800
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
        Text("Failed to load $label", color = Color.Red)
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
        Toast.makeText(context, "Failed to save $fileName", Toast.LENGTH_SHORT).show()
    }
}

fun decodeBase64Scaled(base64: String, reqWidth: Int, reqHeight: Int): Bitmap? {
    val imageBytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)

    // First decode with inJustDecodeBounds=true to check dimensions
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)

    // Calculate scale factor
    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
    options.inJustDecodeBounds = false

    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)
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













