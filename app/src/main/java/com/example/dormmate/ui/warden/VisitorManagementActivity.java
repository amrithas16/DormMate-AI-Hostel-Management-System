package com.example.dormmate.ui.warden;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.dormmate.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class VisitorManagementActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private RecyclerView rvActive;
    private VisitorAdapter adapter; // Reusing standalone adapter
    private final List<DocumentSnapshot> activeVisitors = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visitor_management);

        db = FirebaseFirestore.getInstance();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.cardRegister).setOnClickListener(v -> {
            startActivity(new Intent(this, VisitorLogActivity.class));
        });

        findViewById(R.id.cardQuickScan).setOnClickListener(v -> {
            Intent intent = new Intent(this, FaceScanActivity.class);
            startActivityForResult(intent, 1001); // Quick Scan Mode
        });

        findViewById(R.id.cardActive).setOnClickListener(v -> {
            // Can open a filtered list activity if needed, or just focus on dashboard
            Toast.makeText(this, "Showing Active Visitors Below", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.cardLogs).setOnClickListener(v -> {
            Intent intent = new Intent(this, VisitorLogActivity.class);
            intent.putExtra("SHOW_HISTORY_ONLY", true);
            startActivity(intent);
        });

        // Dashboard List (Active Only)
        rvActive = findViewById(R.id.rvActiveDashboard);
        rvActive.setLayoutManager(new LinearLayoutManager(this));
        adapter = new VisitorAdapter(this, activeVisitors, db);
        rvActive.setAdapter(adapter);
        
        loadActiveVisitors();
        loadDashboardStats();
    }

    private void loadDashboardStats() {
        // 1. Active Count
        db.collection("visitor_logs")
                .whereEqualTo("status", "active")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;
                    ((android.widget.TextView)findViewById(R.id.tvActiveCount)).setText(String.valueOf(snapshots.size()));
                });

        // 2. Today's Total Count
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        com.google.firebase.Timestamp todayStart = new com.google.firebase.Timestamp(cal.getTime());

        db.collection("visitor_logs")
                .whereGreaterThanOrEqualTo("entry_time", todayStart)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;
                    ((android.widget.TextView)findViewById(R.id.tvTotalTodayCount)).setText(String.valueOf(snapshots.size()));
                });
    }

    private void loadActiveVisitors() {
        db.collection("visitor_logs")
                .whereEqualTo("status", "active")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;
                    activeVisitors.clear();
                    activeVisitors.addAll(snapshots.getDocuments());
                    adapter.notifyDataSetChanged();
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            float[] embedding = data.getFloatArrayExtra("face_embedding");
            if (embedding == null) return;

            // 1. Search for Active Visitors first (Check-out check)
            db.collection("visitor_logs")
                    .whereEqualTo("status", "active")
                    .get()
                    .addOnSuccessListener(snapshots -> {
                        for (DocumentSnapshot doc : snapshots) {
                            List<Double> storedObj = (List<Double>) doc.get("face_embedding");
                            if (storedObj != null) {
                                float[] storedEmbedding = new float[storedObj.size()];
                                for (int i = 0; i < storedObj.size(); i++) storedEmbedding[i] = storedObj.get(i).floatValue();

                                if (FaceNetHelper.cosineSimilarity(embedding, storedEmbedding) > 0.7f) {
                                    showExitConfirmation(doc);
                                    return;
                                }
                            }
                        }

                        // 2. Fallback: Search all Visitors (Returning Check-in)
                        Intent intent = new Intent(this, VisitorLogActivity.class);
                        intent.putExtra("PRE_SCANNED_EMBEDDING", embedding);
                        startActivity(intent);
                    });
        }
    }

    private void showExitConfirmation(DocumentSnapshot doc) {
        String name = doc.getString("name");
        new android.app.AlertDialog.Builder(this)
                .setTitle("Recognized: " + name)
                .setMessage("This visitor is currently ACTIVE. Would you like to mark their EXIT now?")
                .setPositiveButton("Mark Exit", (dialog, which) -> {
                    db.collection("visitor_logs").document(doc.getId())
                            .update("status", "completed", "exit_time", com.google.firebase.Timestamp.now())
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "✅ " + name + " marked as Exited", Toast.LENGTH_SHORT).show();
                                androidx.work.WorkManager.getInstance(this).cancelAllWorkByTag(doc.getId());
                                loadActiveVisitors();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
