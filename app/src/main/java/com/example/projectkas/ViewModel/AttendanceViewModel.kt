package com.example.projectkas.ViewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.projectkas.Network.ClassUi
import com.example.projectkas.Network.RecognizeResponse
import com.example.projectkas.Network.RecognizedStudent
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject

@HiltViewModel
class AttendanceViewModel @Inject constructor() : ViewModel() {

    // holds the recognized students
    var recognizedList by mutableStateOf<List<RecognizedStudent>>(emptyList())

    var selectedClass by  mutableStateOf<ClassUi?>(null)

    var apiResponse by mutableStateOf<RecognizeResponse?>(null)

    fun setClass(classui : ClassUi){
        selectedClass = classui
    }

    fun resetClass(){
        selectedClass = null
    }

    // update whole list
    fun setRecognized(students: List<RecognizedStudent>) {
        recognizedList = students
    }

    // add single student
    fun addStudent(student: RecognizedStudent) {
        recognizedList = recognizedList + student
    }

    // remove single student
    fun removeStudent(student: RecognizedStudent) {
        recognizedList = recognizedList - student
    }

    // clear/reset
    fun clear() {
        recognizedList = emptyList()
    }
}
