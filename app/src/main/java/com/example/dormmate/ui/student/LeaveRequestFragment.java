package com.example.dormmate.ui.student;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.dormmate.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LeaveRequestFragment extends Fragment {

    private EditText etFromDate, etToDate, etReason;
    private Button btnSubmit, btnViewQR;
    private TextView tvStatus, tvBack;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration leaveListener;
    private String approvedFromDate, approvedToDate, approvedDocId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_leave_request, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        etFromDate = view.findViewById(R.id.etFromDate);
        etToDate = view.findViewById(R.id.etToDate);
        etReason = view.findViewById(R.id.etReason);
        btnSubmit = view.findViewById(R.id.btnSubmitLeave);
        btnViewQR = view.findViewById(R.id.btnViewQR);
        tvStatus = view.findViewById(R.id.tvLeaveStatus);
        tvBack = view.findViewById(R.id.tvBack);

        tvBack.setOnClickListener(v -> requireActivity().onBackPressed());

        // DatePicker for From Date
        etFromDate.setOnClickListener(v -> showDatePicker(etFromDate));
        etToDate.setOnClickListener(v -> showDatePicker(etToDate));

        btnSubmit.setOnClickListener(v -> submitLeaveRequest());

        btnViewQR.setOnClickListener(v -> {
            QRPassFragment qrFragment = QRPassFragment.newInstance(
                    approvedDocId, approvedFromDate, approvedToDate);
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.studentContainer, qrFragment)
                    .addToBackStack(null)
                    .commit();
        });

        // Real-time listener for leave status
        listenToLeaveStatus();
    }

    private void showDatePicker(EditText target) {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(requireContext(), (picker, year, month, day) -> {
            String date = String.format(Locale.getDefault(), "%02d/%02d/%d", day, month + 1, year);
            target.setText(date);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void submitLeaveRequest() {
        String from = etFromDate.getText().toString().trim();
        String to = etToDate.getText().toString().trim();
        String reason = etReason.getText().toString().trim();

        if (from.isEmpty() || to.isEmpty() || reason.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (auth.getCurrentUser() == null)
            return;

        Map<String, Object> leaveData = new HashMap<>();
        leaveData.put("studentUid", auth.getCurrentUser().getUid());
        leaveData.put("email", auth.getCurrentUser().getEmail());
        leaveData.put("fromDate", from);
        leaveData.put("toDate", to);
        leaveData.put("reason", reason);
        leaveData.put("status", "Pending");
        leaveData.put("timestamp", com.google.firebase.Timestamp.now());

        db.collection("leave_requests").add(leaveData)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(getContext(), "Leave request submitted!", Toast.LENGTH_SHORT).show();
                    etReason.setText("");
                })
                .addOnFailureListener(
                        e -> Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void listenToLeaveStatus() {
        if (auth.getCurrentUser() == null)
            return;

        leaveListener = db.collection("leave_requests")
                .whereEqualTo("studentUid", auth.getCurrentUser().getUid())
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null || snapshots.isEmpty())
                        return;

                    com.google.firebase.firestore.DocumentSnapshot doc = snapshots.getDocuments().get(0);
                    String status = doc.getString("status");
                    String from = doc.getString("fromDate");
                    String to = doc.getString("toDate");

                    tvStatus.setText("Status: " + status + "\nFrom: " + from + " → To: " + to);

                    if ("Approved".equals(status)) {
                        approvedDocId = doc.getId();
                        approvedFromDate = from;
                        approvedToDate = to;
                        btnViewQR.setVisibility(View.VISIBLE);
                    } else {
                        btnViewQR.setVisibility(View.GONE);
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (leaveListener != null)
            leaveListener.remove();
    }
}
