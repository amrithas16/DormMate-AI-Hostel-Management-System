package com.example.dormmate.ui.warden;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.media.RingtoneManager;
import android.media.Ringtone;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.dormmate.R;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class EmergencyAlertActivity extends AppCompatActivity {

    private Ringtone ringtone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Show over lock screen
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_emergency_alert);

        String name = getIntent().getStringExtra("name");
        String email = getIntent().getStringExtra("email");
        String room = getIntent().getStringExtra("room");
        String docId = getIntent().getStringExtra("docId");

        TextView tvStudent = findViewById(R.id.tvEmergencyStudent);
        TextView tvEmail = findViewById(R.id.tvEmergencyEmail);
        TextView tvRoom = findViewById(R.id.tvEmergencyRoom);
        Button btnDismiss = findViewById(R.id.btnDismissAlert);
        View flashOverlay = findViewById(R.id.flashOverlay);

        tvStudent.setText("Student: " + (name != null ? name : "Unknown"));
        tvEmail.setText(email != null ? email : "");
        tvRoom.setText("📍 Room: " + (room != null ? room : "—"));

        // Flashing red animation
        ObjectAnimator flashAnim = ObjectAnimator.ofFloat(flashOverlay, "alpha", 0f, 0.6f);
        flashAnim.setDuration(500);
        flashAnim.setRepeatMode(ValueAnimator.REVERSE);
        flashAnim.setRepeatCount(ValueAnimator.INFINITE);
        flashAnim.start();

        // Play alarm sound
        Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        ringtone = RingtoneManager.getRingtone(this, alarmUri);
        if (ringtone != null)
            ringtone.play();

        // Dismiss button — marks emergency as resolved
        btnDismiss.setOnClickListener(v -> {
            if (ringtone != null && ringtone.isPlaying())
                ringtone.stop();
            flashAnim.cancel();

            if (docId != null) {
                Map<String, Object> update = new HashMap<>();
                update.put("resolved", true);
                FirebaseFirestore.getInstance()
                        .collection("emergency").document(docId)
                        .update(update);
            }
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ringtone != null && ringtone.isPlaying())
            ringtone.stop();
    }
}
