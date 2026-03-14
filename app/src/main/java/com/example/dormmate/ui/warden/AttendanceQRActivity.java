package com.example.dormmate.ui.warden;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.dormmate.R;
import com.example.dormmate.utils.SecureQRHelper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class AttendanceQRActivity extends AppCompatActivity {

    private ImageView ivQR;
    private ProgressBar pbRefresh;
    private TextView tvRefreshLabel;
    private Handler handler = new Handler();
    private int secondsRemaining = 10;
    
    private Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (secondsRemaining <= 0) {
                updateQRCode();
                secondsRemaining = 10;
            } else {
                secondsRemaining--;
            }
            
            pbRefresh.setProgress(10 - secondsRemaining);
            tvRefreshLabel.setText("Refreshing in " + secondsRemaining + "s...");
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_qr);

        ivQR = findViewById(R.id.ivQR);
        pbRefresh = findViewById(R.id.pbRefresh);
        tvRefreshLabel = findViewById(R.id.tvRefreshLabel);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Initial generation
        updateQRCode();
        handler.post(refreshRunnable);
    }

    private void updateQRCode() {
        String token = SecureQRHelper.generateToken();
        if (token == null) {
            Toast.makeText(this, "Failed to generate token", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(token, BarcodeFormat.QR_CODE, 800, 800);
            ivQR.setImageBitmap(bitmap);
            
            // Subtle pulse animation to show refresh
            ivQR.setAlpha(0.5f);
            ivQR.animate().alpha(1.0f).setDuration(500).start();
            
        } catch (WriterException e) {
            e.printStackTrace();
            Toast.makeText(this, "QR generation error", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(refreshRunnable);
    }
}
