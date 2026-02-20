package com.example.dormmate.ui.warden;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.example.dormmate.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class EmergencyAlertService extends Service {

    private static final String CHANNEL_ID = "emergency_channel";
    private ListenerRegistration emergencyListener;
    private FirebaseFirestore db;

    @Override
    public void onCreate() {
        super.onCreate();
        db = FirebaseFirestore.getInstance();
        createNotificationChannel();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, buildForegroundNotification(),
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(1, buildForegroundNotification());
        }
        listenForEmergencies();
    }

    private void listenForEmergencies() {
        emergencyListener = db.collection("emergency")
                .whereEqualTo("resolved", false)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null)
                        return;
                    for (QueryDocumentSnapshot doc : snapshots) {
                        if (doc.getMetadata().hasPendingWrites())
                            continue; // Skip local writes

                        String studentName = doc.getString("name") != null ? doc.getString("name") : "Unknown Student";
                        String studentEmail = doc.getString("email") != null ? doc.getString("email") : "";
                        String room = doc.getString("room") != null ? doc.getString("room") : "Unknown";
                        String docId = doc.getId();

                        // Launch full-screen emergency alert activity
                        Intent alertIntent = new Intent(this, EmergencyAlertActivity.class);
                        alertIntent.putExtra("name", studentName);
                        alertIntent.putExtra("email", studentEmail);
                        alertIntent.putExtra("room", room);
                        alertIntent.putExtra("docId", docId);
                        alertIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(alertIntent);

                        // Also fire a high-priority notification
                        showEmergencyNotification(studentName, room);
                        break; // Handle one at a time
                    }
                });
    }

    private void showEmergencyNotification(String name, String room) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notif = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("🚨 EMERGENCY SOS!")
                .setContentText(name + " — Room " + room)
                .setSmallIcon(R.drawable.ic_sos)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .build();
        nm.notify(999, notif);
    }

    private Notification buildForegroundNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("DormMate Warden")
                .setContentText("Monitoring emergency alerts...")
                .setSmallIcon(R.drawable.ic_sos)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Emergency Alerts", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("DormMate SOS emergency alerts");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null)
                nm.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (emergencyListener != null)
            emergencyListener.remove();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
