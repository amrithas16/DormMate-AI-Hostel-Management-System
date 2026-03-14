package com.example.dormmate.ui.warden;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.dormmate.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.*;
import java.text.SimpleDateFormat;
import java.util.*;
import android.content.Intent;
import android.util.Log;
import com.example.dormmate.ui.warden.FaceNetHelper;

public class VisitorLogActivity extends AppCompatActivity {

    private RecyclerView rvVisitors;
    private VisitorAdapter adapter;
    private final List<DocumentSnapshot> visitors = new ArrayList<>();
    private FirebaseFirestore db;
    private ListenerRegistration visitorListener;

    private static final int REQUEST_FACE_SCAN = 1001;
    private float[] currentEmbedding = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visitor_log);

        db = FirebaseFirestore.getInstance();
        rvVisitors = findViewById(R.id.rvVisitors);
        rvVisitors.setLayoutManager(new LinearLayoutManager(this));
        rvVisitors.setNestedScrollingEnabled(false);

        findViewById(R.id.tvVisitorBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnRegisterVisitor).setOnClickListener(v -> registerVisitor());
        
        findViewById(R.id.btnCaptureFace).setOnClickListener(v -> {
            Intent intent = new Intent(this, FaceScanActivity.class);
            startActivityForResult(intent, REQUEST_FACE_SCAN);
        });

        // Handle Dashboard Intents
        if (getIntent().hasExtra("SHOW_HISTORY_ONLY")) {
            findViewById(R.id.btnRegisterVisitor).setVisibility(android.view.View.GONE);
            findViewById(R.id.btnCaptureFace).setVisibility(android.view.View.GONE);
            ((TextView)findViewById(R.id.tvVisitorBack)).setText("← Logs Dashboard");
        }

        if (getIntent().hasExtra("PRE_SCANNED_EMBEDDING")) {
            float[] preScanned = getIntent().getFloatArrayExtra("PRE_SCANNED_EMBEDDING");
            if (preScanned != null) {
                currentEmbedding = preScanned;
                checkIfReturningVisitor(preScanned);
            }
        }

        adapter = new VisitorAdapter(this, visitors, db);
        rvVisitors.setAdapter(adapter);
        listenVisitors();
    }

    private void registerVisitor() {
        String name = ((EditText) findViewById(R.id.etVisitorName)).getText().toString().trim();
        String phone = ((EditText) findViewById(R.id.etVisitorPhone)).getText().toString().trim();
        String visiting = ((EditText) findViewById(R.id.etVisitingStudent)).getText().toString().trim();
        String purpose = ((EditText) findViewById(R.id.etVisitPurpose)).getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty() || visiting.isEmpty()) {
            Toast.makeText(this, "Please fill Name, Phone, and Visiting Student", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> visitor = new HashMap<>();
        visitor.put("name", name);
        visitor.put("phone", phone);
        visitor.put("visitingStudent", visiting);
        visitor.put("purpose", purpose);
        Timestamp entryTime = Timestamp.now();
        Calendar cal = Calendar.getInstance();
        cal.setTime(entryTime.toDate());
        cal.add(Calendar.HOUR, 1);
        Timestamp expectedExit = new Timestamp(cal.getTime());

        visitor.put("entry_time", entryTime);
        visitor.put("expected_exit_time", expectedExit);
        visitor.put("status", "active");

        if (currentEmbedding != null) {
            // Convert float[] to List<Double> for Firestore storage
            List<Double> embeddingList = new ArrayList<>();
            for (float f : currentEmbedding) {
                embeddingList.add((double) f);
            }
            visitor.put("face_embedding", embeddingList);
        }

        db.collection("visitor_logs").add(visitor)
                .addOnSuccessListener(ref -> {
                    // Update/Create Visitor Profile
                    Map<String, Object> profile = new HashMap<>();
                    profile.put("name", name);
                    profile.put("phone", phone);
                    if (currentEmbedding != null) {
                        List<Double> embeddingList = new ArrayList<>();
                        for (float f : currentEmbedding) embeddingList.add((double) f);
                        profile.put("face_embedding", embeddingList);
                    }
                    db.collection("visitors").document(phone).set(profile);

                    Toast.makeText(this, "✅ Visitor Registered", Toast.LENGTH_SHORT).show();
                    
                    // 1. Schedule 1 Hour Background Alarm
                    androidx.work.Data inputData = new androidx.work.Data.Builder()
                            .putString("visitorLogId", ref.getId())
                            .putString("visitorName", name)
                            .putString("visitorStudent", visiting)
                            .build();

                    androidx.work.OneTimeWorkRequest timerWork = new androidx.work.OneTimeWorkRequest.Builder(com.example.dormmate.ui.warden.VisitorTimerWorker.class)
                            .setInitialDelay(1, java.util.concurrent.TimeUnit.HOURS)
                            .setInputData(inputData)
                            .addTag(ref.getId())
                            .build();

                    androidx.work.WorkManager.getInstance(this).enqueue(timerWork);

                    // 2. Launch Visitor Badge
                    Intent badgeIntent = new Intent(this, VisitorBadgeActivity.class);
                    badgeIntent.putExtra("visitorName", name);
                    badgeIntent.putExtra("visitorPhone", phone);
                    badgeIntent.putExtra("visitorLogId", ref.getId());
                    startActivity(badgeIntent);
                    
                    clearInputs();
                    currentEmbedding = null;
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_FACE_SCAN && resultCode == RESULT_OK && data != null) {
            float[] embedding = data.getFloatArrayExtra("face_embedding");
            if (embedding != null) {
                currentEmbedding = embedding;
                Toast.makeText(this, "Face Captured successfully!", Toast.LENGTH_SHORT).show();
                checkIfReturningVisitor(embedding);
            }
        }
    }

    private void checkIfReturningVisitor(float[] newEmbedding) {
        // Fetch last 50 distinct visitors (or keep an offline cache in a real app)
        db.collection("visitor_logs")
                .orderBy("entry_time", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(snapshots -> {
                    for (DocumentSnapshot doc : snapshots) {
                        List<Double> storedObj = (List<Double>) doc.get("face_embedding");
                        if (storedObj != null) {
                            float[] storedEmbedding = new float[storedObj.size()];
                            for (int i = 0; i < storedObj.size(); i++) {
                                storedEmbedding[i] = storedObj.get(i).floatValue();
                            }
                            
                            float similarity = FaceNetHelper.cosineSimilarity(newEmbedding, storedEmbedding);
                            if (similarity > 0.7f) {
                                // Match found!
                                Toast.makeText(this, "Returning Visitor Recognized!", Toast.LENGTH_LONG).show();
                                ((EditText) findViewById(R.id.etVisitorName)).setText(doc.getString("name"));
                                ((EditText) findViewById(R.id.etVisitorPhone)).setText(doc.getString("phone"));
                                ((EditText) findViewById(R.id.etVisitingStudent)).setText(doc.getString("visitingStudent"));
                                ((EditText) findViewById(R.id.etVisitPurpose)).setText(doc.getString("purpose"));
                                return;
                            }
                        }
                    }
                });
    }

    private void clearInputs() {
        ((EditText) findViewById(R.id.etVisitorName)).setText("");
        ((EditText) findViewById(R.id.etVisitorPhone)).setText("");
        ((EditText) findViewById(R.id.etVisitingStudent)).setText("");
        ((EditText) findViewById(R.id.etVisitPurpose)).setText("");
    }

    private void listenVisitors() {
        visitorListener = db.collection("visitor_logs")
                .orderBy("entry_time", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null)
                        return;
                    visitors.clear();
                    visitors.addAll(snapshots.getDocuments());
                    adapter.notifyDataSetChanged();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (visitorListener != null)
            visitorListener.remove();
    }

}
