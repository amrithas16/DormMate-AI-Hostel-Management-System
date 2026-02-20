package com.example.dormmate.ui.warden;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.dormmate.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WardenComplaintsActivity extends AppCompatActivity {

    private RecyclerView rvComplaints;
    private ComplaintAdapter adapter;
    private final List<DocumentSnapshot> complaintList = new ArrayList<>();
    private FirebaseFirestore db;
    private ListenerRegistration complaintListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complaints);

        db = FirebaseFirestore.getInstance();
        rvComplaints = findViewById(R.id.rvComplaints);
        rvComplaints.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.tvComplaintsBack).setOnClickListener(v -> finish());

        adapter = new ComplaintAdapter();
        rvComplaints.setAdapter(adapter);

        listenToComplaints();
    }

    private void listenToComplaints() {
        complaintListener = db.collection("complaints")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Toast.makeText(this, "Query failed. Retrying without sort...", Toast.LENGTH_SHORT).show();
                        // Fallback in case index isn't created yet
                        db.collection("complaints").addSnapshotListener((snaps, err) -> {
                            if (err != null || snaps == null)
                                return;
                            complaintList.clear();
                            complaintList.addAll(snaps.getDocuments());
                            adapter.notifyDataSetChanged();
                        });
                        return;
                    }
                    if (snapshots == null)
                        return;
                    complaintList.clear();
                    complaintList.addAll(snapshots.getDocuments());
                    adapter.notifyDataSetChanged();
                });
    }

    private void resolveComplaint(int position, String resolutionNote) {
        DocumentSnapshot doc = complaintList.get(position);
        String studentUid = doc.getString("studentUid");

        Map<String, Object> update = new HashMap<>();
        update.put("status", "Resolved");
        update.put("resolution", resolutionNote);
        update.put("resolvedAt", Timestamp.now());
        doc.getReference().update(update).addOnFailureListener(
                e -> Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());

        // Notify student
        if (studentUid != null && !studentUid.isEmpty()) {
            Map<String, Object> notif = new HashMap<>();
            notif.put("Complaint Resolved", "Your complaint '" +
                    doc.getString("category") + "' has been resolved. Note: " + resolutionNote);
            notif.put("leave", Timestamp.now()); // Using "leave" field as expected by Student's NotificationsFragment
            db.collection("users").document(studentUid)
                    .collection("notifications").add(notif);
        }
        Toast.makeText(this, "✅ Complaint Resolved", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (complaintListener != null)
            complaintListener.remove();
    }

    // ── Adapter ────────────────────────────────────────────────────────────────
    class ComplaintAdapter extends RecyclerView.Adapter<ComplaintAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_complaint_warden, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            DocumentSnapshot doc = complaintList.get(position);
            String status = doc.getString("status");

            holder.tvCategory.setText(doc.getString("category") != null ? doc.getString("category") : "General");
            holder.tvEmail.setText(doc.getString("email") != null ? doc.getString("email") : "Student");
            holder.tvDesc.setText(doc.getString("description") != null ? doc.getString("description") : "");
            holder.tvStatus.setText(status != null ? status : "Open");

            if ("Resolved".equals(status)) {
                holder.tvStatus.setTextColor(0xFF4CAF50);
                holder.tvStatus.setBackgroundColor(0x334CAF50);
                holder.btnResolve.setEnabled(false);
                holder.btnResolve.setText("RESOLVED");
            } else {
                holder.tvStatus.setTextColor(0xFFFF5252);
                holder.tvStatus.setBackgroundColor(0x33FF5252);
                holder.btnResolve.setEnabled(true);
                holder.btnResolve.setText("MARK RESOLVED");
                holder.btnResolve.setOnClickListener(v -> {
                    String note = holder.etNote.getText().toString().trim();
                    resolveComplaint(position, note.isEmpty() ? "Issue resolved by Warden" : note);
                });
            }
        }

        @Override
        public int getItemCount() {
            return complaintList.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvCategory, tvEmail, tvDesc, tvStatus;
            EditText etNote;
            Button btnResolve;

            VH(@NonNull View view) {
                super(view);
                tvCategory = view.findViewById(R.id.tvComplaintCategory);
                tvEmail = view.findViewById(R.id.tvComplaintEmail);
                tvDesc = view.findViewById(R.id.tvComplaintDesc);
                tvStatus = view.findViewById(R.id.tvComplaintStatus);
                etNote = view.findViewById(R.id.etResolutionNote);
                btnResolve = view.findViewById(R.id.btnResolve);
            }
        }
    }
}
