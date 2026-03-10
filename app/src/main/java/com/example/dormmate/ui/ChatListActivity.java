package com.example.dormmate.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.dormmate.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class ChatListActivity extends AppCompatActivity {

    private RecyclerView rvChatUsers;
    private UserAdapter adapter;
    private List<DocumentSnapshot> userList = new ArrayList<>();
    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
        }

        findViewById(R.id.tvChatListBack).setOnClickListener(v -> finish());

        rvChatUsers = findViewById(R.id.rvChatUsers);
        rvChatUsers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UserAdapter();
        rvChatUsers.setAdapter(adapter);

        loadUsers();
    }

    private void loadUsers() {
        if (currentUserId == null)
            return;

        db.collection("users").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    userList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        if (!doc.getId().equals(currentUserId)) {
                            userList.add(doc);
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast
                        .makeText(this, "Failed to load users: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_user, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DocumentSnapshot doc = userList.get(position);
            String name = doc.getString("name");
            String role = doc.getString("role");
            String room = doc.getString("room");

            holder.tvUserName.setText(name != null ? name : "Unknown");

            String subtitle = role != null ? role : "Student";
            if ("Student".equals(role) && room != null && !room.isEmpty()) {
                subtitle += " • Room " + room;
            }
            holder.tvUserRole.setText(subtitle);

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(ChatListActivity.this, ChatRoomActivity.class);
                intent.putExtra("receiverId", doc.getId());
                intent.putExtra("receiverName", name);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return userList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvUserName, tvUserRole;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvUserName = itemView.findViewById(R.id.tvUserName);
                tvUserRole = itemView.findViewById(R.id.tvUserRole);
            }
        }
    }
}
