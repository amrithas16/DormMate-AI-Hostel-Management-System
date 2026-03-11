# DormMate - Smart Hostel Management System

**DormMate** is a high-end hostel management application for Android designed to streamline the interaction between Students and Hostel Wardens. Built with a "Vibrant Glassmorphism" UI philosophy, it combines traditional management features with a cutting-edge **Gemini-powered AI Assistant** to provide instant answers to student queries.

---

🚀 ## Features

### 1. AI Assistant (Gemini powered)
- **Hostel Omni-Context**: The chatbot has real-time access to mess menus, hostel rules, broadcasts, and personal student data.
- **Natural Language Queries**: Students can ask "What's for breakfast tomorrow?", "Where is my room?", or "Any new notices?" and get instant data-backed answers.

### 2. Warden Dashboard (Management Level)
- **Student Management**: View and manage the complete directory of residents.
- **Broadcast & Alerts**: Send global announcements or trigger emergency alerts with real-time notifications.
- **Mess Management**: Update the weekly menu (Breakfast, Lunch, Dinner).
- **Rules Publishing**: Manage the digital hostel rulebook.
- **Leave Approvals**: Review and approve/reject student leave requests.
- **Complaint Handling**: Monitor and resolve issues raised by students.
- **Visitor Logs**: Maintain a digital record of all visitors.

### 3. Student Dashboard
- **Room Details**: Instant access to room number, floor, and wing details.
- **Digital Mess Menu**: Check the full week's menu at a glance.
- **Leave Requests**: Apply for leave Digitally and track approval status in real-time.
- **Fee Tracking**: Monitor pending room fees and payment status.
- **Complaints Portal**: Raise grievances and track resolution progress.
- **E-Pass (QR Code)**: Digital identity for hostel entry and exit.
- **Notifications**: Get instant updates from the Warden regarding broadcasts or leave status.

---

🛠️ ## Installation & Setup

Follow these steps to get the project running on your local machine.

### Prerequisites
- **Android Studio** (Hedgehog or higher recommended).
- **JDK 17** or higher.
- A **Firebase Project** for backend services.
- A **Google Gemini API Key**.

### Step 1: Clone the Repository
```bash
git clone <repository-url>
cd DormMate
```

### Step 2: Open in Android Studio
Launch Android Studio and select **Open**, then navigate to the cloned `DormMate` folder.

### Step 3: Firebase Configuration
1. Create a project in the [Firebase Console](https://console.firebase.google.com/).
2. Add an Android App with package name `com.example.dormmate`.
3. Download the `google-services.json` file and place it in the `app/` directory.
4. Enable **Firestore Database** and **Firebase Authentication** (Email/Password).

### Step 4: AI API Key Setup
Create a `local.properties` file in the project root (if it doesn't exist) and add your Gemini API Key:
```properties
GEMINI_API_KEY=your_api_key_here
```

### Step 5: Build & Run
Connect your Android device or start an emulator and click the **Run** button in Android Studio.

---

🔑 ## Usage Guide

- **Login**: Use the shared login screen to enter as a Warden or Student.
- **Warden**: Manage the hostel ecosystem (Menu, Rules, Broadcasts).
- **Student**: Use the sidebar to navigate between Fee status, Leaves, and the AI Assistant.

---

📝 ## Tech Stack

- **Backend**: Firebase Firestore (Real-time Database)
- **AI Engine**: Google Gemini (Direct SDK Integration)
- **Language**: Java 100% (Native Android)
- **UI Architecture**: XML Layouts with Material Design 3
- **Design Style**: Vibrant Glassmorphism (Aurora Gradients, Lottie Micro-animations)
