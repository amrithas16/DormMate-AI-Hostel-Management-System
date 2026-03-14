package com.example.dormmate.ui.warden;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.example.dormmate.R;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.concurrent.CountDownLatch;

public class VisitorTimerWorker extends Worker {

    private static final String CHANNEL_ID = "VisitorAlarmChannel";
    private FirebaseFirestore db;

    public VisitorTimerWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public Result doWork() {
        String visitorLogId = getInputData().getString("visitorLogId");
        String visitorName = getInputData().getString("visitorName");
        String visitorStudent = getInputData().getString("visitorStudent");

        if (visitorLogId == null) {
            return Result.failure();
        }

        // We need to wait for Firestore callback
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] isOverstaying = {false};

        db.collection("visitor_logs").document(visitorLogId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String status = documentSnapshot.getString("status");
                        if ("active".equals(status)) {
                            isOverstaying[0] = true;
                        }
                    }
                    latch.countDown();
                })
                .addOnFailureListener(e -> latch.countDown());

        try {
            latch.await();
        } catch (InterruptedException e) {
            return Result.retry();
        }

        if (isOverstaying[0]) {
            sendNotification(visitorName, visitorStudent);
        }

        return Result.success();
    }

    private void sendNotification(String visitorName, String visitorStudent) {
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Visitor Overstay Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(getApplicationContext(), VisitorLogActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(),
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent exitIntent = new Intent(getApplicationContext(), VisitorActionReceiver.class);
        exitIntent.setAction("MARK_EXIT");
        exitIntent.putExtra("visitorLogId", getInputData().getString("visitorLogId"));
        exitIntent.putExtra("notificationId", (int) System.currentTimeMillis()); // Just for cancellation
        
        PendingIntent exitPendingIntent = PendingIntent.getBroadcast(
                getApplicationContext(),
                1,
                exitIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("⚠ Visitor Overstay Alert")
                .setContentText(visitorName + " (visting " + visitorStudent + ") has stayed > 1 hour.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_exit_to_app, "Mark Exit", exitPendingIntent)
                .setAutoCancel(true);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
