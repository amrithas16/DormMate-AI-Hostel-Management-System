package com.example.dormmate.ui.student;

import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.dormmate.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class PaymentGatewayActivity extends AppCompatActivity {

    private EditText etCardNumber, etExpiry, etCvv;
    private TextView tvCardNumber, tvAmountToPay, tvStatusMessage;
    private Button btnPay;
    private FrameLayout overlayProcessing;
    private ProgressBar pbProcessing;
    private ImageView ivSuccess;
    
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private long totalAmount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_gateway);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        totalAmount = getIntent().getLongExtra("TOTAL_AMOUNT", 0);

        initViews();
        setupListeners();

        tvAmountToPay.setText("Amount to Pay: ₹" + totalAmount);
    }

    private void initViews() {
        etCardNumber = findViewById(R.id.etCardNumber);
        etExpiry = findViewById(R.id.etExpiry);
        etCvv = findViewById(R.id.etCvv);
        tvCardNumber = findViewById(R.id.tvCardNumber);
        tvAmountToPay = findViewById(R.id.tvAmountToPay);
        tvStatusMessage = findViewById(R.id.tvStatusMessage);
        btnPay = findViewById(R.id.btnPay);
        overlayProcessing = findViewById(R.id.overlayProcessing);
        pbProcessing = findViewById(R.id.pbProcessing);
        ivSuccess = findViewById(R.id.ivSuccess);
        
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void setupListeners() {
        etCardNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String formatted = formatCardNumber(s.toString());
                tvCardNumber.setText(formatted.isEmpty() ? "**** **** **** ****" : formatted);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnPay.setOnClickListener(v -> processPayment());
    }

    private String formatCardNumber(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (i > 0 && i % 4 == 0) sb.append(" ");
            sb.append(s.charAt(i));
        }
        return sb.toString();
    }

    private void processPayment() {
        if (etCardNumber.getText().toString().length() < 16) {
            Toast.makeText(this, "Valid card number required", Toast.LENGTH_SHORT).show();
            return;
        }

        overlayProcessing.setVisibility(View.VISIBLE);
        pbProcessing.setVisibility(View.VISIBLE);
        ivSuccess.setVisibility(View.GONE);
        tvStatusMessage.setText("Verifying Card...");

        new Handler().postDelayed(() -> {
            tvStatusMessage.setText("Authorizing Transaction...");
            new Handler().postDelayed(() -> {
                updateFirebaseAndFinish();
            }, 1500);
        }, 1500);
    }

    private void updateFirebaseAndFinish() {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();

        Map<String, Object> historyItem = new HashMap<>();
        historyItem.put("amount", totalAmount);
        historyItem.put("timestamp", System.currentTimeMillis());
        historyItem.put("status", "Success");
        historyItem.put("type", "Fee Payment");

        db.collection("fees").document(uid).update("status", "Paid")
            .addOnSuccessListener(aVoid -> {
                // Add to history subcollection
                db.collection("fees").document(uid).collection("history").add(historyItem);
                
                showSuccess();
            })
            .addOnFailureListener(e -> {
                overlayProcessing.setVisibility(View.GONE);
                Toast.makeText(this, "Payment failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void showSuccess() {
        pbProcessing.setVisibility(View.GONE);
        ivSuccess.setVisibility(View.VISIBLE);
        tvStatusMessage.setText("Payment Successful!");
        tvStatusMessage.setTextColor(0xFF4CAF50);

        new Handler().postDelayed(() -> {
            finish();
        }, 2000);
    }
}
