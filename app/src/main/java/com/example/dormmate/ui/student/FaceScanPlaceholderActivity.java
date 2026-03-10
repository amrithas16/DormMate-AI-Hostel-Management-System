package com.example.dormmate.ui.student;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.dormmate.R;

public class FaceScanPlaceholderActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_scan_placeholder);

        Button btnScanFace = findViewById(R.id.btnScanFace);
        Button btnBackFaceScan = findViewById(R.id.btnBackFaceScan);

        btnScanFace.setOnClickListener(
                v -> Toast.makeText(this, "Face Scan ML feature is pending implementation.", Toast.LENGTH_LONG).show());
        btnBackFaceScan.setOnClickListener(v -> finish());
    }
}
