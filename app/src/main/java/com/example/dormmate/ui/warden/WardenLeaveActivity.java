package com.example.dormmate.ui.warden;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.example.dormmate.R;
import com.google.firebase.firestore.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WardenLeaveActivity extends AppCompatActivity {

    private RecyclerView rvLeaveRequests;
    private LeaveAdapter adapter;
    private final List<DocumentSnapshot> leaveList = new ArrayList<>();
    private FirebaseFirestore db;
    private ListenerRegistration leaveListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_warden_leave);

        db = FirebaseFirestore.getInstance();
        rvLeaveRequests = findViewById(R.id.rvLeaveRequests);
        rvLeaveRequests.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.tvLeaveBack).setOnClickListener(v -> finish());

        adapter = new LeaveAdapter();
        rvLeaveRequests.setAdapter(adapter);

        attachSwipeGesture();
        listenToLeaveRequests();
    }

    private void listenToLeaveRequests() {
        leaveListener = db.collection("leave_requests")
                .whereEqualTo("status", "Pending")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null)
                        return;
                    leaveList.clear();
                    leaveList.addAll(snapshots.getDocuments());
                    adapter.notifyDataSetChanged();
                });
    }

    private void approveLeave(int position) {
        DocumentSnapshot doc = leaveList.get(position);
        String studentUid = doc.getString("studentUid");

        Map<String, Object> update = new HashMap<>();
        update.put("status", "Approved");
        doc.getReference().update(update);

        // Notify student
        if (studentUid != null) {
            Map<String, Object> notif = new HashMap<>();
            notif.put("Leave Approved", "Your leave request from " +
                    doc.getString("fromDate") + " to " + doc.getString("toDate") +
                    " has been approved by the Warden");
            notif.put("leave", com.google.firebase.Timestamp.now());
            db.collection("users").document(studentUid)
                    .collection("notifications").add(notif);
        }
        Toast.makeText(this, "✅ Leave Approved", Toast.LENGTH_SHORT).show();
    }

    private void rejectLeave(int position) {
        DocumentSnapshot doc = leaveList.get(position);
        String studentUid = doc.getString("studentUid");

        Map<String, Object> update = new HashMap<>();
        update.put("status", "Rejected");
        doc.getReference().update(update);

        // Notify student
        if (studentUid != null) {
            Map<String, Object> notif = new HashMap<>();
            notif.put("Leave Rejected", "Your leave request has been rejected by the Warden");
            notif.put("leave", com.google.firebase.Timestamp.now());
            db.collection("users").document(studentUid)
                    .collection("notifications").add(notif);
        }
        Toast.makeText(this, "❌ Leave Rejected", Toast.LENGTH_SHORT).show();
    }

    /** Swipe RIGHT = Approve (green), LEFT = Reject (red) */
    private void attachSwipeGesture() {
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(@NonNull RecyclerView rv,
                    @NonNull RecyclerView.ViewHolder vh,
                    @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (direction == ItemTouchHelper.RIGHT) {
                    approveLeave(position);
                } else {
                    rejectLeave(position);
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                    int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                View itemView = viewHolder.itemView;
                Paint paint = new Paint();
                if (dX > 0) {
                    // Swipe right — green approve background
                    paint.setColor(0xFF388E3C);
                    RectF background = new RectF(itemView.getLeft(), itemView.getTop(),
                            itemView.getLeft() + dX, itemView.getBottom());
                    c.drawRoundRect(background, 16f, 16f, paint);
                    paint.setColor(Color.WHITE);
                    paint.setTextSize(42f);
                    c.drawText("✅", itemView.getLeft() + 30, itemView.getTop() + itemView.getHeight() / 2f + 15f,
                            paint);
                } else if (dX < 0) {
                    // Swipe left — red reject background
                    paint.setColor(0xFFD32F2F);
                    RectF background = new RectF(itemView.getRight() + dX, itemView.getTop(),
                            itemView.getRight(), itemView.getBottom());
                    c.drawRoundRect(background, 16f, 16f, paint);
                    paint.setColor(Color.WHITE);
                    paint.setTextSize(42f);
                    c.drawText("❌", itemView.getRight() - 80, itemView.getTop() + itemView.getHeight() / 2f + 15f,
                            paint);
                }
            }
        };
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(rvLeaveRequests);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (leaveListener != null)
            leaveListener.remove();
    }

    // ── Adapter ────────────────────────────────────────────────────────────────
    class LeaveAdapter extends RecyclerView.Adapter<LeaveAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_leave_card, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            DocumentSnapshot doc = leaveList.get(position);
            holder.tvEmail.setText(doc.getString("email") != null ? doc.getString("email") : "Student");
            holder.tvDates.setText("From: " + doc.getString("fromDate") + "  →  To: " + doc.getString("toDate"));
            holder.tvReason.setText("Reason: " + doc.getString("reason"));
            holder.tvStatus.setText(doc.getString("status"));
        }

        @Override
        public int getItemCount() {
            return leaveList.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvEmail, tvDates, tvReason, tvStatus;

            VH(@NonNull View view) {
                super(view);
                tvEmail = view.findViewById(R.id.tvLeaveEmail);
                tvDates = view.findViewById(R.id.tvLeaveDates);
                tvReason = view.findViewById(R.id.tvLeaveReason);
                tvStatus = view.findViewById(R.id.tvLeaveStatus);
            }
        }
    }
}
