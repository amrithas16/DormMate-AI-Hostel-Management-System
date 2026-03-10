package com.example.dormmate.ui;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class HostelContextBuilder {

    public interface OnContextBuiltListener {
        void onContextBuilt(String context);

        void onError(Exception e);
    }

    public static void buildLiveContext(String currentUserId, OnContextBuiltListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Tasks for Global Data
        Task<DocumentSnapshot> rulesTask = db.collection("system").document("hostel_rules").get();
        // Assuming mess menu is stored with document id as 'today' or we fetch the
        // latest
        Task<QuerySnapshot> messTask = db.collection("mess_menu").limit(1).get();
        Task<QuerySnapshot> broadcastTask = db.collection("global_announcements")
                .orderBy("timestamp", Query.Direction.DESCENDING).limit(1).get();

        // Tasks for Personalized Data
        Task<DocumentSnapshot> profileTask = db.collection("users").document(currentUserId).get();
        Task<DocumentSnapshot> feesTask = db.collection("fees").document(currentUserId).get();
        Task<QuerySnapshot> leavesTask = db.collection("leave_requests")
                .whereEqualTo("userId", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get();

        // Combine all tasks
        Task<List<Object>> allTasks = Tasks.whenAllSuccess(rulesTask, messTask, broadcastTask, profileTask, feesTask,
                leavesTask);

        allTasks.addOnSuccessListener(results -> {
            try {
                StringBuilder contextBuilder = new StringBuilder();
                contextBuilder.append("--- HOSTEL OMNI-CONTEXT ---\n\n");

                // Process Profile
                DocumentSnapshot profileDoc = (DocumentSnapshot) results.get(3);
                if (profileDoc.exists()) {
                    contextBuilder.append("[User Profile]\n");
                    contextBuilder.append("Name: ").append(profileDoc.getString("name")).append("\n");
                    contextBuilder.append("Room: ").append(profileDoc.getString("room")).append("\n");
                    contextBuilder.append("Floor: ").append(profileDoc.getLong("floor")).append("\n");
                    contextBuilder.append("Wing: ").append(profileDoc.getLong("wing")).append("\n\n");
                }

                // Process Fees
                DocumentSnapshot feesDoc = (DocumentSnapshot) results.get(4);
                if (feesDoc.exists()) {
                    contextBuilder.append("[Pending Fees]\n");
                    String status = feesDoc.getString("status");
                    long amount = feesDoc.getLong("roomFee") != null ? feesDoc.getLong("roomFee") : 0;
                    contextBuilder.append("Status: ").append(status).append("\n");
                    contextBuilder.append("Amount Due: ₹").append(amount).append("\n\n");
                } else {
                    contextBuilder.append("[Pending Fees]\nNo fees data available.\n\n");
                }

                // Process Leaves
                QuerySnapshot leavesSnap = (QuerySnapshot) results.get(5);
                if (!leavesSnap.isEmpty()) {
                    DocumentSnapshot leaveDoc = leavesSnap.getDocuments().get(0);
                    contextBuilder.append("[Latest Leave Request]\n");
                    contextBuilder.append("Status: ").append(leaveDoc.getString("status")).append("\n");
                    contextBuilder.append("Reason: ").append(leaveDoc.getString("reason")).append("\n\n");
                } else {
                    contextBuilder.append("[Latest Leave Request]\nNo recent leaves.\n\n");
                }

                // Process Rules
                DocumentSnapshot rulesDoc = (DocumentSnapshot) results.get(0);
                if (rulesDoc.exists() && rulesDoc.getString("content") != null) {
                    contextBuilder.append("[Hostel Rules]\n").append(rulesDoc.getString("content")).append("\n\n");
                }

                // Process Mess Menu
                QuerySnapshot messSnap = (QuerySnapshot) results.get(1);
                if (!messSnap.isEmpty()) {
                    DocumentSnapshot messDoc = messSnap.getDocuments().get(0);
                    contextBuilder.append("[Today's Mess Menu]\n");
                    contextBuilder.append("Breakfast: ").append(messDoc.getString("breakfast")).append("\n");
                    contextBuilder.append("Lunch: ").append(messDoc.getString("lunch")).append("\n");
                    contextBuilder.append("Dinner: ").append(messDoc.getString("dinner")).append("\n\n");
                }

                // Process Broadcast
                QuerySnapshot broadcastSnap = (QuerySnapshot) results.get(2);
                if (!broadcastSnap.isEmpty()) {
                    DocumentSnapshot broadcastDoc = broadcastSnap.getDocuments().get(0);
                    contextBuilder.append("[Latest Broadcast]\n");
                    contextBuilder.append(broadcastDoc.getString("message")).append("\n\n");
                }

                listener.onContextBuilt(contextBuilder.toString());

            } catch (Exception e) {
                listener.onError(e);
            }
        }).addOnFailureListener(listener::onError);
    }
}
