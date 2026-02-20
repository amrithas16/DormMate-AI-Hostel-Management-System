package com.example.dormmate.ui.warden;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.dormmate.R;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class WardenRulesActivity extends AppCompatActivity {

    private EditText etRulesContent;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_warden_rules);

        db = FirebaseFirestore.getInstance();
        etRulesContent = findViewById(R.id.etRulesContent);

        findViewById(R.id.tvRulesWardenBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSaveRules).setOnClickListener(v -> publishRules());

        loadRules();
    }

    private void loadRules() {
        db.collection("hostel_rules").document("rules").get()
                .addOnSuccessListener(doc -> {
                    String content = doc.getString("content");
                    if (content != null)
                        etRulesContent.setText(content);
                });
    }

    private void publishRules() {
        String content = etRulesContent.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "Rules cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("content", content);

        db.collection("hostel_rules").document("rules")
                .set(data)
                .addOnSuccessListener(unused -> Toast
                        .makeText(this, "✅ Rules Published — Students see it instantly!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(
                        e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
