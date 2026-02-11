package com.example.projectkas.Screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.projectkas.Network.RetrofitInstance
import com.example.projectkas.Network.RetrofitInstance.getApi
import com.example.projectkas.Network.Student
import com.example.projectkas.R
import com.example.projectkas.Screen
import com.example.projectkas.ViewModel.AuthState
import com.example.projectkas.ViewModel.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun StudentsList(navController: NavController, classID: String?,authViewModel: AuthViewModel = hiltViewModel()){


    val currentUserEmail = authViewModel.auth.currentUser?.email ?: ""
    val authState = authViewModel.authState.observeAsState()

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var isLoadingDelete by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPickerDialog by remember { mutableStateOf(false) }
    var selectedStudent: Student by remember { mutableStateOf(Student("", "", "")) }

    // State for API call
    var query by rememberSaveable { mutableStateOf("") }
    var students by remember { mutableStateOf<List<Student>>(emptyList()) }

    // Fetch students when authenticated
    LaunchedEffect(authState.value) {
        when (authState.value) {
            is AuthState.Authenticated -> {
                isLoading = true
                try {
                    val response = RetrofitInstance.getApi().listStudents(classId = classID, currentUserEmail)
                    if (response.isSuccessful) {
                        students = response.body()?.students ?: emptyList()
                        Log.d("debug","" + students.toString())
                    } else {
                        errorMessage = "Error: ${response.errorBody()?.string()}"
                    }
                } catch (e: Exception) {
                    errorMessage = "Exception: ${e.localizedMessage}"
                } finally {
                    isLoading = false
                }
            }
            else -> Unit
        }
    }

    val filteredStudents by remember {
        derivedStateOf {
            val q = query.trim()
            if (q.isEmpty()) students.toList()
            else students.filter { st ->
                st.roll_no.contains(q, true) ||
                        st.name.contains(q, true)
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background)
            .padding(top = 4.dp, start = 16.dp, end = 16.dp, bottom = 4.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Students",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Divider(
            color = Color(0xFF468A9A), // teal accent
            thickness = 2.dp,
            modifier = Modifier.width(180.dp)
        )
        Spacer(modifier = Modifier.height(25.dp))

        SimpleSearchBar(
            query = query,
            onQueryChange = {
                query = it
            },
            placeholder = "Search student...",
            modifier = Modifier.align(Alignment.CenterHorizontally).fillMaxWidth(0.95f),
        )

        Spacer(modifier = Modifier.height(25.dp))

        // Student table
        if (isLoading) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.onBackground)
        } else if (errorMessage != null) {
            Log.e("ERROR", "" + errorMessage)
            Text("Server Error - unable to connect", color = MaterialTheme.colorScheme.error)
        } else {
            if (filteredStudents.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(id = R.string.registered_students) + " - " + filteredStudents.size,
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp)
                        )
                        Divider(
                            color = MaterialTheme.colorScheme.outline,
                            thickness = 1.dp
                        )

                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredStudents) { student ->
                                StudentRow(
                                    student = student,
                                    onEdit = {
                                        navController.navigate("${Screen.Profile.route}/${student.roll_no}/${student.name}/${student.id}/${classID}")
                                    },
                                    onDelete = {
                                        showPickerDialog = true
                                        selectedStudent = student
                                    }
                                )
                                Divider(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    thickness = 0.5.dp
                                )
                            }
                        }
                    }
                }
            } else {
                Text("No student found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (showPickerDialog) {
            AlertDialog(
                onDismissRequest = { showPickerDialog = false },

                title = { Text(stringResource(id = R.string.are_you_sure)) },
                text = { Text(stringResource(id = R.string.selected_student, selectedStudent.name)) },
                confirmButton = {
                    TextButton(enabled = selectedStudent != Student("","","") ,onClick = {
                        showPickerDialog = false
                        coroutineScope.launch {
                            isLoadingDelete = true
                            try {
                                val response = RetrofitInstance.getApi().deleteStudentById(
                                    studentId = selectedStudent.id,
                                    email = currentUserEmail,
                                    classID = classID
                                )
                                if (response.isSuccessful) {
                                    val body = response.body()
                                    Toast.makeText(context, "${body?.message} Total students : ${body?.total_students}", Toast.LENGTH_SHORT).show()
                                    Log.d("debug", body?.message + body?.total_students)
                                } else {
                                    Toast.makeText(context, "Error: ${response.errorBody()?.string()}", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }finally {
                                isLoadingDelete = false
                                selectedStudent = Student("","","")
                            }

                            try {
                                val response = RetrofitInstance.getApi().listStudents(classId = classID, currentUserEmail)
                                if (response.isSuccessful) {
                                    students = response.body()?.students ?: emptyList()
                                    Log.d("debug","" + students.toString())
                                } else {
                                    errorMessage = "Error: ${response.errorBody()?.string()}"
                                }
                            } catch (e: Exception) {
                                errorMessage = "Exception: ${e.localizedMessage}"
                            }
                        }

                    }) { Text(stringResource(id = R.string.delete), color = MaterialTheme.colorScheme.error) }
                },

                dismissButton = {
                    TextButton(onClick = {
                        showPickerDialog = false
                        selectedStudent = Student("","","")
                    }) { Text(stringResource(id = R.string.cancel) ,color = MaterialTheme.colorScheme.primary) }
                }
            )

        }
    }
}