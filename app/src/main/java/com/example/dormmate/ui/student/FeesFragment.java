package com.example.dormmate.ui.student;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.dormmate.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class FeesFragment extends Fragment {

    private TextView tvFeeStatus, tvFeeStatusIcon, tvDueDate, tvTotalFee;
    private RecyclerView rvHistory;
    private final java.util.List<com.google.firebase.firestore.DocumentSnapshot> historyList = new java.util.ArrayList<>();
    private TransactionAdapter historyAdapter;
    private FirebaseFirestore db;
    private com.google.firebase.auth.FirebaseAuth auth;
    private long currentTotalAmount = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_fees, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        tvTotalFee = view.findViewById(R.id.tvTotalFee);
        tvFeeStatus = view.findViewById(R.id.tvFeeStatus);
        tvFeeStatusIcon = view.findViewById(R.id.tvFeeStatusIcon);
        tvDueDate = view.findViewById(R.id.tvDueDate);
        rvHistory = view.findViewById(R.id.rvTransactionHistory);

        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        historyAdapter = new TransactionAdapter();
        rvHistory.setAdapter(historyAdapter);

        view.findViewById(R.id.tvFeesBack).setOnClickListener(v -> requireActivity().onBackPressed());
        view.findViewById(R.id.btnPayFees).setOnClickListener(v -> launchPaymentGateway());

        // Set fee row labels
        setFeeRowLabel(view, R.id.rowRoomFee, "Room Rent");
        setFeeRowLabel(view, R.id.rowMessFee, "Mess Charges");
        setFeeRowLabel(view, R.id.rowMaintenance, "Maintenance");
        setFeeRowLabel(view, R.id.rowOther, "Other Charges");

        loadFees(view);
        loadTransactionHistory();
    }

    private void launchPaymentGateway() {
        if (currentTotalAmount <= 0) {
            Toast.makeText(getContext(), "No pending fees to pay.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(getContext(), PaymentGatewayActivity.class);
        intent.putExtra("TOTAL_AMOUNT", currentTotalAmount);
        startActivity(intent);
    }

    private void showPaymentDialog() {
        if (getContext() == null)
            return;
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(getContext())
                .setTitle("Secure Payment")
                .setMessage("Initializing secure payment gateway...\nPlease wait.")
                .setCancelable(false)
                .show();

        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (dialog.isShowing()) {
                dialog.dismiss();
                
                // Update Firebase
                if (auth.getCurrentUser() != null) {
                    db.collection("fees").document(auth.getCurrentUser().getUid())
                            .update("status", "Paid")
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "Payment Successful! (Mock)", Toast.LENGTH_LONG).show();
                                loadFees(getView()); // Refresh UI
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Payment failed to update: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                }
            }
        }, 2000);
    }

    private void loadTransactionHistory() {
        if (auth.getCurrentUser() == null) return;
        db.collection("fees").document(auth.getCurrentUser().getUid())
            .collection("history")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener((snapshots, e) -> {
                if (e != null || snapshots == null) return;
                historyList.clear();
                historyList.addAll(snapshots.getDocuments());
                historyAdapter.notifyDataSetChanged();
            });
    }

    private void setFeeRowLabel(View root, int rowId, String label) {
        View row = root.findViewById(rowId);
        if (row != null) {
            TextView lbl = row.findViewById(R.id.tvFeeLabel);
            if (lbl != null)
                lbl.setText(label);
        }
    }

    private void setFeeRowAmount(View root, int rowId, String amount) {
        View row = root.findViewById(rowId);
        if (row != null) {
            TextView amt = row.findViewById(R.id.tvFeeAmount);
            if (amt != null)
                amt.setText(amount);
        }
    }

    private void loadFees(View view) {
        if (auth.getCurrentUser() == null)
            return;
        String uid = auth.getCurrentUser().getUid();

        db.collection("fees").document(uid).addSnapshotListener((doc, e) -> {
            if (doc != null && doc.exists()) {
                String status = doc.getString("status") != null ? doc.getString("status") : "Pending";
                String dueDate = doc.getString("dueDate") != null ? doc.getString("dueDate") : "";
                long roomFee = doc.getLong("roomFee") != null ? doc.getLong("roomFee") : 0;
                long messFee = doc.getLong("messFee") != null ? doc.getLong("messFee") : 0;
                long maintenance = doc.getLong("maintenance") != null ? doc.getLong("maintenance") : 0;
                long other = doc.getLong("other") != null ? doc.getLong("other") : 0;
                currentTotalAmount = roomFee + messFee + maintenance + other;

                tvFeeStatus.setText(status);
                tvDueDate.setText("Due: " + dueDate);
                tvTotalFee.setText("₹" + currentTotalAmount);

                setFeeRowAmount(view, R.id.rowRoomFee, "₹" + roomFee);
                setFeeRowAmount(view, R.id.rowMessFee, "₹" + messFee);
                setFeeRowAmount(view, R.id.rowMaintenance, "₹" + maintenance);
                setFeeRowAmount(view, R.id.rowOther, "₹" + other);

                if ("Paid".equals(status)) {
                    tvFeeStatusIcon.setText("✅");
                    tvFeeStatus.setTextColor(0xFF4CAF50);
                    view.findViewById(R.id.btnPayFees).setVisibility(View.GONE);
                } else {
                    view.findViewById(R.id.btnPayFees).setVisibility(View.VISIBLE);
                    if ("Overdue".equals(status)) {
                        tvFeeStatusIcon.setText("🚨");
                        tvFeeStatus.setTextColor(0xFFE53935);
                    } else {
                        tvFeeStatusIcon.setText("⏳");
                        tvFeeStatus.setTextColor(0xFFFFB300);
                    }
                }
            } else {
                showSampleFees(view);
            }
        });
    }

    private void showSampleFees(View view) {
        tvFeeStatus.setText("Pending");
        tvFeeStatusIcon.setText("⏳");
        tvFeeStatus.setTextColor(0xFFFFB300);
        tvDueDate.setText("Due: 28 Feb 2026");
        tvTotalFee.setText("₹15,000");
        setFeeRowAmount(view, R.id.rowRoomFee, "₹8,000");
        setFeeRowAmount(view, R.id.rowMessFee, "₹5,000");
        setFeeRowAmount(view, R.id.rowMaintenance, "₹1,500");
        setFeeRowAmount(view, R.id.rowOther, "₹500");
        currentTotalAmount = 15000;
    }

    class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.VH> {
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_transaction, p, false));
        }
        @Override public void onBindViewHolder(@NonNull VH h, int p) {
            com.google.firebase.firestore.DocumentSnapshot d = historyList.get(p);
            h.tvType.setText(d.getString("type") != null ? d.getString("type") : "Payment");
            
            Long amt = d.getLong("amount");
            h.tvAmount.setText("₹" + (amt != null ? amt : 0));
            
            h.tvStatus.setText(d.getString("status") != null ? d.getString("status") : "Success");
            
            Object tsObj = d.get("timestamp");
            if (tsObj != null) {
                long ts = 0;
                if (tsObj instanceof Long) {
                    ts = (Long) tsObj;
                } else if (tsObj instanceof com.google.firebase.Timestamp) {
                    ts = ((com.google.firebase.Timestamp) tsObj).toDate().getTime();
                }
                
                if (ts > 0) {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault());
                    h.tvDate.setText(sdf.format(new java.util.Date(ts)));
                } else {
                    h.tvDate.setText("Recently");
                }
            } else {
                h.tvDate.setText("Recently");
            }
        }
        @Override public int getItemCount() { return historyList.size(); }
        class VH extends RecyclerView.ViewHolder {
            TextView tvType, tvDate, tvAmount, tvStatus;
            VH(View v) { super(v);
                tvType = v.findViewById(R.id.tvTransactionType);
                tvDate = v.findViewById(R.id.tvTransactionDate);
                tvAmount = v.findViewById(R.id.tvTransactionAmount);
                tvStatus = v.findViewById(R.id.tvTransactionStatus);
            }
        }
    }
}
