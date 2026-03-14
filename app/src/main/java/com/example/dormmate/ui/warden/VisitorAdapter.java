package com.example.dormmate.ui.warden;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.dormmate.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class VisitorAdapter extends RecyclerView.Adapter<VisitorAdapter.VH> {
    
    private final Context context;
    private final List<DocumentSnapshot> visitors;
    private final FirebaseFirestore db;

    public VisitorAdapter(Context context, List<DocumentSnapshot> visitors, FirebaseFirestore db) {
        this.context = context;
        this.visitors = visitors;
        this.db = db;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_visitor, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        DocumentSnapshot doc = visitors.get(position);
        holder.tvName.setText(doc.getString("name"));
        holder.tvPhone.setText(doc.getString("phone") != null ? doc.getString("phone") : "");
        holder.tvVisiting.setText("Visiting: " + (doc.getString("visitingStudent") != null ? doc.getString("visitingStudent") : "—"));
        
        Timestamp entryTs = doc.getTimestamp("entry_time");
        Timestamp exitTs = doc.getTimestamp("exit_time");
        Timestamp expectedTs = doc.getTimestamp("expected_exit_time");
        
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        
        if (entryTs != null) {
            String timeText = "In: " + sdf.format(entryTs.toDate());
            if (exitTs != null) {
                timeText += " | Out: " + sdf.format(exitTs.toDate());
            } else if (expectedTs != null) {
                timeText += " | Exp: " + sdf.format(expectedTs.toDate());
            }
            holder.tvTime.setText(timeText);

            String status = doc.getString("status");
            
            // Show Overstay Alert if active and current time > expected
            if ("active".equals(status) && expectedTs != null && System.currentTimeMillis() > expectedTs.toDate().getTime()) {
                holder.tvOverstayAlert.setVisibility(View.VISIBLE);
            } else {
                holder.tvOverstayAlert.setVisibility(View.GONE);
            }

            if ("completed".equals(status)) {
                holder.btnExit.setVisibility(View.GONE);
                holder.tvTime.setTextColor(0xFF4CAF50);
            } else {
                holder.btnExit.setVisibility(View.VISIBLE);
                holder.btnExit.setOnClickListener(v -> markExit(doc.getId()));
                holder.tvTime.setTextColor(0xFF7B7B9D);
            }
        }
    }

    private void markExit(String logId) {
        db.collection("visitor_logs").document(logId)
                .update("status", "completed", "exit_time", Timestamp.now())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Visitor Marked as Exited", Toast.LENGTH_SHORT).show();
                    androidx.work.WorkManager.getInstance(context).cancelAllWorkByTag(logId);
                })
                .addOnFailureListener(e -> Toast.makeText(context, "Error marking exit", Toast.LENGTH_SHORT).show());
    }

    @Override
    public int getItemCount() {
        return visitors.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvPhone, tvVisiting, tvTime, tvOverstayAlert;
        Button btnExit;

        VH(@NonNull View view) {
            super(view);
            tvName = view.findViewById(R.id.tvVisitorName);
            tvPhone = view.findViewById(R.id.tvVisitorPhone);
            tvVisiting = view.findViewById(R.id.tvVisitingStudent);
            tvTime = view.findViewById(R.id.tvVisitTime);
            tvOverstayAlert = view.findViewById(R.id.tvOverstayAlert);
            btnExit = view.findViewById(R.id.btnExit);
        }
    }
}
