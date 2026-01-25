# Presence Cam 📸  
### AI-Based Smart Attendance System

Presence Cam is a smart attendance application designed to replace manual roll calls in classrooms with a fast, accurate, and automated solution. Professors can capture a single image of the class, and the system identifies students using AI-powered face recognition to instantly mark attendance.

---

## 🏗 System Architecture

Presence Cam follows a **client–server architecture** with a **monolithic FastAPI backend**, optimized for simplicity, speed, and maintainability during early-stage development.

### Why Monolithic FastAPI?
The backend is intentionally built as a **monolith** to:
- Reduce development overhead
- Enable faster iteration and debugging
- Keep deployment simple
- Avoid premature microservice complexity

This approach is ideal for MVPs and academic-to-production transitions.

---

### Architecture Overview

**Android App**
- Captures or uploads classroom images
- Sends requests to backend APIs
- Displays recognized students and attendance results

**FastAPI Backend (Monolithic)**
- Handles image preprocessing
- Runs face recognition inference
- Manages student registration and recognition
- Stores and retrieves face embeddings

**Database**
- Face embeddings stored in `embeddings.npz`
- Acts as the identity reference for recognition

---

## 🔌 API Endpoints

- `POST /register`  
  Registers a new student by generating and storing face embeddings.

- `POST /recognize`  
  Processes a classroom image and identifies students using stored embeddings.

Both endpoints are part of the same FastAPI application, sharing preprocessing and model logic for efficiency.

---

## 🧠 Face Recognition Model

The backend uses **InsightFace**, a state-of-the-art face recognition framework.

### Model Details
- Variant: `buffalo_l`
- Backbone: ResNet-based CNN
- Embedding Size: **512-dimensional**
- Loss Function: **ArcFace**

### Recognition Flow
1. Image preprocessing with L2 normalization
2. Face embedding extraction
3. Cosine similarity comparison with stored embeddings
4. Threshold-based identity matching to reduce false positives

This pipeline enables accurate and fast real-time recognition suitable for classroom environments.

---

## 🛠 Tech Stack

### Android
- Jetpack Compose
- MVVM Architecture
- Hilt (Dependency Injection)
- REST API integration

### Backend
- FastAPI (Monolithic Architecture)
- InsightFace
- Python
- NumPy (Embedding storage)

---

## 🚀 Key Features

- One-click classroom attendance
- Proxy-resistant system
- Real-time face recognition
- Minimal and distraction-free UI
- Simple and scalable backend design
- PDF attendance export

---

## 🔮 Future Improvements

- Modularization of backend into services if scaling demands
- Database migration from file-based storage to cloud DB
- Liveness detection for spoof prevention
- Attendance analytics dashboard
