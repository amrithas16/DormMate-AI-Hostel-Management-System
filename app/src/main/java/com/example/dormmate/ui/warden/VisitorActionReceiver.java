package com.example.dormmate.ui.warden;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import androidx.work.WorkManager;

public class VisitorActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String logId = intent.getStringExtra("visitorLogId");
        int notificationId = intent.getIntExtra("notificationId", 0);

        if ("MARK_EXIT".equals(action) && logId != null) {
            FirebaseFirestore.getInstance().collection("visitor_logs").document(logId)
                    .update("status", "completed", "exit_time", Timestamp.now())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, "Visitor marked as exited via notification", Toast.LENGTH_SHORT).show();
                        
                        // Cancel the notification
                        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                        manager.cancel(notificationId);
                        
                        // Cancel any other related work
                        WorkManager.getInstance(context).cancelAllWorkByTag(logId);
                    });
        }
    }
}
