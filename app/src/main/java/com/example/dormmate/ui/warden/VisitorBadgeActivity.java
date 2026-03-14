package com.example.dormmate.ui.warden;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.dormmate.R;
import com.example.dormmate.ui.warden.FaceNetHelper;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class VisitorBadgeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visitor_badge);

        String name = getIntent().getStringExtra("visitorName");
        String phone = getIntent().getStringExtra("visitorPhone");
        String logId = getIntent().getStringExtra("visitorLogId");

        ((TextView) findViewById(R.id.tvBadgeName)).setText(name != null ? name.toUpperCase() : "VISITOR");

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnPrintBadge).setOnClickListener(v -> {
            // Placeholder for PDF or Share intent
            android.widget.Toast.makeText(this, "Badge Shared Successfully", android.widget.Toast.LENGTH_SHORT).show();
        });

        generateQRCode(logId != null ? logId : phone);
    }

    private void generateQRCode(String data) {
        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(data, BarcodeFormat.QR_CODE, 400, 400);
            ImageView ivQRCode = findViewById(R.id.ivQRCode);
            ivQRCode.setImageBitmap(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
