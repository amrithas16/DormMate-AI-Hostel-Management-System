package com.example.dormmate.ui.student;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import com.example.dormmate.R;
import com.example.dormmate.utils.SecureQRHelper;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class StudentAttendanceActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FrameLayout overlaySuccess;
    private TextView tvSuccessTime;

    // ZXing Scanner Launcher
    private final ActivityResultLauncher<ScanOptions> qrCodeLauncher = registerForActivityResult(new ScanContract(), result -> {
        if (result.getContents() == null) {
            Toast.makeText(this, "Scan canceled", Toast.LENGTH_SHORT).show();
        } else {
            processScannedToken(result.getContents());
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_attendance);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        overlaySuccess = findViewById(R.id.overlaySuccess);
        tvSuccessTime = findViewById(R.id.tvSuccessTime);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnScan).setOnClickListener(v -> startScanning());
    }

    private void startScanning() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Scan Warden's QR Code");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        options.setCaptureActivity(com.journeyapps.barcodescanner.CaptureActivity.class); // Standard ZXing activity
        qrCodeLauncher.launch(options);
    }

    private void processScannedToken(String token) {
        String hostelId = SecureQRHelper.verifyAndDecrypt(token);

        if (hostelId != null) {
            markAttendanceInFirebase(hostelId);
        } else {
            showError("Invalid or Expired Token. Please scan again.");
        }
    }

    private void markAttendanceInFirebase(String hostelId) {
        if (auth.getCurrentUser() == null) return;

        String studentUid = auth.getCurrentUser().getUid();
        String studentEmail = auth.getCurrentUser().getEmail();
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String time = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());

        Map<String, Object> attendanceData = new HashMap<>();
        attendanceData.put("uid", studentUid);
        attendanceData.put("email", studentEmail);
        attendanceData.put("timestamp", com.google.firebase.Timestamp.now());
        attendanceData.put("hostelId", hostelId);
        attendanceData.put("status", "Present");

        // attendance_logs/{date}/students/{studentUid}
        db.collection("attendance_logs")
                .document(date)
                .collection("students")
                .document(studentUid)
                .set(attendanceData)
                .addOnSuccessListener(aVoid -> showSuccess(time))
                .addOnFailureListener(e -> showError("Database error: " + e.getMessage()));
    }

    private void showSuccess(String time) {
        // Trigger System Notification
        com.example.dormmate.utils.NotificationHelper.showAttendanceNotification(
                this, 
                "Check-in Successful", 
                "Your attendance has been marked present at " + time
        );

        tvSuccessTime.setText("Logged at: " + time);
        overlaySuccess.setVisibility(View.VISIBLE);
        
        // Hide overlay after 3 seconds and close activity
        new android.os.Handler().postDelayed(() -> {
            finish();
        }, 3000);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
