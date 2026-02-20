package com.example.dormmate.ui.warden;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.dormmate.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class BroadcastActivity extends AppCompatActivity {

    private EditText etTitle, etMessage, etFloor;
    private RadioGroup rgTarget;
    private RadioButton rbAll, rbFloor;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_broadcast);

        db = FirebaseFirestore.getInstance();

        etTitle = findViewById(R.id.etBroadcastTitle);
        etMessage = findViewById(R.id.etBroadcastMessage);
        etFloor = findViewById(R.id.etFloorNumber);
        rgTarget = findViewById(R.id.rgBroadcastTarget);
        rbAll = findViewById(R.id.rbAll);
        rbFloor = findViewById(R.id.rbFloor);

        findViewById(R.id.tvBroadcastBack).setOnClickListener(v -> finish());

        // Show/hide floor input based on toggle
        rgTarget.setOnCheckedChangeListener((group, checkedId) -> {
            etFloor.setVisibility(checkedId == R.id.rbFloor ? View.VISIBLE : View.GONE);
        });

        findViewById(R.id.btnSendBroadcast).setOnClickListener(v -> sendBroadcast());
    }

    private void sendBroadcast() {
        String title = etTitle.getText().toString().trim();
        String message = etMessage.getText().toString().trim();

        if (title.isEmpty() || message.isEmpty()) {
            Toast.makeText(this, "Enter both title and message", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isAllStudents = rbAll.isChecked();
        String floorTarget = isAllStudents ? "all" : etFloor.getText().toString().trim();

        if (!isAllStudents && floorTarget.isEmpty()) {
            Toast.makeText(this, "Enter a floor number", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> announcement = new HashMap<>();
        announcement.put("title", title);
        announcement.put("message", message);
        announcement.put("target", isAllStudents ? "all" : "floor_" + floorTarget);
        announcement.put("floor", isAllStudents ? null : floorTarget);
        announcement.put("timestamp", Timestamp.now());

        db.collection("global_announcements").add(announcement)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "📢 Announcement Sent!", Toast.LENGTH_SHORT).show();
                    etTitle.setText("");
                    etMessage.setText("");
                    etFloor.setText("");
                })
                .addOnFailureListener(
                        e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
