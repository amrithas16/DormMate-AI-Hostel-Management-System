package com.example.dormmate.ui.student;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.dormmate.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NotificationsFragment extends Fragment {

    private RecyclerView rvNotifications;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration notifListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        rvNotifications = view.findViewById(R.id.rvNotifications);
        rvNotifications.setLayoutManager(new LinearLayoutManager(getContext()));

        view.findViewById(R.id.tvNotifBack).setOnClickListener(v -> requireActivity().onBackPressed());

        listenToNotifications();
    }

    private void listenToNotifications() {
        if (auth.getCurrentUser() == null)
            return;
        String uid = auth.getCurrentUser().getUid();

        // Correct path: users/{uid}/notifications (subcollection)
        notifListener = db.collection("users").document(uid)
                .collection("notifications")
                .orderBy("leave", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null)
                        return;

                    // Each document may have dynamic field names as titles
                    List<NotifItem> items = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Map<String, Object> data = doc.getData();
                        for (Map.Entry<String, Object> entry : data.entrySet()) {
                            String key = entry.getKey();
                            // Skip Timestamp fields (they're metadata, not notifications)
                            if (entry.getValue() instanceof com.google.firebase.Timestamp)
                                continue;
                            items.add(new NotifItem(key, String.valueOf(entry.getValue())));
                        }
                    }
                    rvNotifications.setAdapter(new NotifAdapter(items));
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (notifListener != null)
            notifListener.remove();
    }

    // Simple model: field key = title, field value = message
    static class NotifItem {
        String title, message;

        NotifItem(String title, String message) {
            this.title = title;
            this.message = message;
        }
    }

    // Notifications Adapter
    static class NotifAdapter extends RecyclerView.Adapter<NotifAdapter.ViewHolder> {
        private final List<NotifItem> items;

        NotifAdapter(List<NotifItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_notification, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            NotifItem item = items.get(position);
            holder.tvTitle.setText(item.title); // e.g. "Leave Approved"
            holder.tvMessage.setText(item.message); // e.g. "Your leave request..."
            holder.tvTime.setText(""); // timestamp skipped (it's a Firestore Timestamp)
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvMessage, tvTime;

            ViewHolder(@NonNull View view) {
                super(view);
                tvTitle = view.findViewById(R.id.tvNotifTitle);
                tvMessage = view.findViewById(R.id.tvNotifMessage);
                tvTime = view.findViewById(R.id.tvNotifTime);
            }
        }
    }
}
