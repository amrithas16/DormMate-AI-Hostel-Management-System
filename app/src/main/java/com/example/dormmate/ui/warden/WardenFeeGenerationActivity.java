package com.example.dormmate.ui.warden;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.dormmate.R;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class WardenFeeGenerationActivity extends AppCompatActivity {

    private EditText etStudentEmail, etBillingMonth, etDueDate, etRoomRent, etMessFee, etMaintenance, etOther;
    private TextView tvCalculatedTotal;
    private Button btnGenerateBill;
    private ImageView btnBack;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_warden_fee_generation);

        db = FirebaseFirestore.getInstance();

        etStudentEmail = findViewById(R.id.etStudentEmail);
        etBillingMonth = findViewById(R.id.etBillingMonth);
        etDueDate = findViewById(R.id.etDueDate);
        etRoomRent = findViewById(R.id.etRoomRent);
        etMessFee = findViewById(R.id.etMessFee);
        etMaintenance = findViewById(R.id.etMaintenance);
        etOther = findViewById(R.id.etOther);
        tvCalculatedTotal = findViewById(R.id.tvCalculatedTotal);
        btnGenerateBill = findViewById(R.id.btnGenerateBill);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> onBackPressed());

        TextWatcher feeWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateTotal();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        };

        etRoomRent.addTextChangedListener(feeWatcher);
        etMessFee.addTextChangedListener(feeWatcher);
        etMaintenance.addTextChangedListener(feeWatcher);
        etOther.addTextChangedListener(feeWatcher);

        btnGenerateBill.setOnClickListener(v -> processFeeGeneration());
    }

    private void calculateTotal() {
        long total = getLongValue(etRoomRent) + getLongValue(etMessFee) + getLongValue(etMaintenance) + getLongValue(etOther);
        tvCalculatedTotal.setText("₹" + total);
    }

    private long getLongValue(EditText et) {
        String s = et.getText().toString().trim();
        if (TextUtils.isEmpty(s)) return 0;
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void processFeeGeneration() {
        String email = etStudentEmail.getText().toString().trim();
        String month = etBillingMonth.getText().toString().trim();
        String dueDate = etDueDate.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(month) || TextUtils.isEmpty(dueDate)) {
            Toast.makeText(this, "Please fill in email, month, and due date", Toast.LENGTH_SHORT).show();
            return;
        }

        long roomFee = getLongValue(etRoomRent);
        long messFee = getLongValue(etMessFee);
        long maintenance = getLongValue(etMaintenance);
        long other = getLongValue(etOther);
        long totalFee = roomFee + messFee + maintenance + other;

        if (totalFee == 0) {
            Toast.makeText(this, "Total fee cannot be 0", Toast.LENGTH_SHORT).show();
            return;
        }

        btnGenerateBill.setEnabled(false);
        btnGenerateBill.setText("Processing...");

        // Lookup student by email
        db.collection("users")
                .whereEqualTo("email", email)
                .whereEqualTo("role", "Student")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(WardenFeeGenerationActivity.this, "Student not found with this email", Toast.LENGTH_LONG).show();
                        resetButton();
                        return;
                    }
                    
                    String uid = queryDocumentSnapshots.getDocuments().get(0).getId();
                    createFeeDocument(uid, month, dueDate, roomFee, messFee, maintenance, other);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(WardenFeeGenerationActivity.this, "Error looking up student: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    resetButton();
                });
    }

    private void createFeeDocument(String studentUid, String month, String dueDate, long roomFee, long messFee, long maintenance, long other) {
        Map<String, Object> feeData = new HashMap<>();
        feeData.put("status", "Pending");
        feeData.put("month", month);
        feeData.put("dueDate", dueDate);
        feeData.put("roomFee", roomFee);
        feeData.put("messFee", messFee);
        feeData.put("maintenance", maintenance);
        feeData.put("other", other);
        feeData.put("timestamp", System.currentTimeMillis());

        // Saving under fees/{studentUid}
        db.collection("fees").document(studentUid)
                .set(feeData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(WardenFeeGenerationActivity.this, "Fee Bill Generated Successfully!", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(WardenFeeGenerationActivity.this, "Failed to generate bill: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    resetButton();
                });
    }

    private void resetButton() {
        btnGenerateBill.setEnabled(true);
        btnGenerateBill.setText("Generate Fee Bill");
    }
}
