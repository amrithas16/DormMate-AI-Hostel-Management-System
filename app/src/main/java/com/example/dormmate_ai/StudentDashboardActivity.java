package com.example.dormmate_ai;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class WardenDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_warden_dashboard);

        open(R.id.btnLeaveReq, WardenLeaveActivity.class);
        open(R.id.btnComplaints, WardenComplaintActivity.class);
        open(R.id.btnAnnouncement, WardenAnnouncementActivity.class);
        open(R.id.btnLogs, WardenEntryLogActivity.class);
        open(R.id.btnEmergency, WardenEmergencyActivity.class);

        findViewById(R.id.btnLogout).setOnClickListener(v -> finish());
    }

    private void open(int btnId, Class<?> cls) {
        findViewById(btnId).setOnClickListener(
                v -> startActivity(new Intent(this, cls))
        );
    }
}
