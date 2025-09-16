package com.example.projectkas.Screens



//Line 66: TextButton(onClick = { /* Handle Edit Picture */ })
//
//You need to add the logic here to allow the user to select or capture a new profile picture for the student.
//
//Line 131: onClick = { /* Handle Save */ }
//
//This is inside the "Save" button. You'll need to add the code to save any changes made to the student's name. This will likely involve making an API call to your backend.
//
//Line 147: onClick = { /* Handle Delete */ }
//
//This is inside the "Delete" button. You'll need to implement the logic to delete the student's record, which will also require an API call.



import android.annotation.SuppressLint
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.projectkas.Network.RetrofitInstance
import com.example.projectkas.Network.RetrofitInstance.api
import com.example.projectkas.Network.Student
import com.example.projectkas.R
import com.example.projectkas.Screen
import com.example.projectkas.ViewModel.AuthState
import com.example.projectkas.ViewModel.AuthViewModel
import kotlinx.coroutines.launch

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
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground), // Using a placeholder
                contentDescription = "Profile Picture",
                modifier = Modifier.size(80.dp)
            )
        }
        TextButton(onClick = { /* Handle Edit Picture */ }) {
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

