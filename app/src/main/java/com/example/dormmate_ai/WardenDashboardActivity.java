package com.example.dormmate_ai;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class StudentDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        findViewById(R.id.btnProfile)
                .setOnClickListener(v -> open(StudentProfileActivity.class));

        findViewById(R.id.btnRoom)
                .setOnClickListener(v -> open(StudentRoomActivity.class));

        findViewById(R.id.btnLeave)
                .setOnClickListener(v -> open(StudentLeaveActivity.class));

        findViewById(R.id.btnComplaint)
                .setOnClickListener(v -> open(StudentComplaintActivity.class));

        findViewById(R.id.btnRules)
                .setOnClickListener(v -> open(StudentRulesActivity.class));

        findViewById(R.id.btnEmergency)
                .setOnClickListener(v -> open(StudentEmergencyActivity.class));

        findViewById(R.id.btnLogout)
                .setOnClickListener(v -> finish());
    }

    private void open(Class<?> cls) {
        startActivity(new Intent(this, cls));
    }
}
