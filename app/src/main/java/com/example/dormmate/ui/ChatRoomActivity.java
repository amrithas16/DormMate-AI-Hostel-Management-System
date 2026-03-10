package com.example.dormmate.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.dormmate.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatRoomActivity extends AppCompatActivity {

    private RecyclerView rvMessages;
    private EditText etInput;
    private ChatMessageAdapter adapter;
    private List<DocumentSnapshot> messageList = new ArrayList<>();
    private FirebaseFirestore db;
    private String currentUserId;
    private String receiverId;
    private ListenerRegistration chatListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);

        db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
        }

        receiverId = getIntent().getStringExtra("receiverId");
        String receiverName = getIntent().getStringExtra("receiverName");

        TextView tvName = findViewById(R.id.tvChatRoomName);
        tvName.setText(receiverName != null ? receiverName : "Chat");

        findViewById(R.id.tvChatRoomBack).setOnClickListener(v -> finish());

        rvMessages = findViewById(R.id.rvChatRoomMessages);
        etInput = findViewById(R.id.etChatRoomInput);
        Button btnSend = findViewById(R.id.btnSendChatRoomMessage);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        adapter = new ChatMessageAdapter();
        rvMessages.setAdapter(adapter);

        btnSend.setOnClickListener(v -> sendMessage());

        listenForMessages();
    }

    private String getChatRoomId() {
        if (currentUserId == null || receiverId == null)
            return null;
        return currentUserId.compareTo(receiverId) < 0
                ? currentUserId + "_" + receiverId
                : receiverId + "_" + currentUserId;
    }

    private void listenForMessages() {
        String roomId = getChatRoomId();
        if (roomId == null)
            return;

        chatListener = db.collection("chats").document(roomId).collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null)
                        return;

                    messageList.clear();
                    messageList.addAll(snapshots.getDocuments());
                    adapter.notifyDataSetChanged();

                    if (!messageList.isEmpty()) {
                        rvMessages.scrollToPosition(messageList.size() - 1);
                    }
                });
    }

    private void sendMessage() {
        String text = etInput.getText().toString().trim();
        if (text.isEmpty() || currentUserId == null || receiverId == null)
            return;

        String roomId = getChatRoomId();
        if (roomId == null)
            return;

        Map<String, Object> message = new HashMap<>();
        message.put("senderId", currentUserId);
        message.put("receiverId", receiverId);
        message.put("messageText", text);
        message.put("timestamp", Timestamp.now());

        db.collection("chats").document(roomId).collection("messages").add(message)
                .addOnSuccessListener(docRef -> etInput.setText(""))
                .addOnFailureListener(
                        e -> Toast.makeText(this, "Failed to send: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatListener != null) {
            chatListener.remove();
        }
    }

    private class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DocumentSnapshot doc = messageList.get(position);
            String text = doc.getString("messageText");
            String senderId = doc.getString("senderId");

            boolean isSent = currentUserId != null && currentUserId.equals(senderId);

            if (isSent) {
                holder.layoutSent.setVisibility(View.VISIBLE);
                holder.layoutReceived.setVisibility(View.GONE);
                holder.tvSentMessage.setText(text);
            } else {
                holder.layoutSent.setVisibility(View.GONE);
                holder.layoutReceived.setVisibility(View.VISIBLE);
                holder.tvReceivedMessage.setText(text);
            }
        }

        @Override
        public int getItemCount() {
            return messageList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            View layoutSent, layoutReceived;
            TextView tvSentMessage, tvReceivedMessage;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                layoutSent = itemView.findViewById(R.id.layoutSent);
                layoutReceived = itemView.findViewById(R.id.layoutReceived);
                tvSentMessage = itemView.findViewById(R.id.tvSentMessage);
                tvReceivedMessage = itemView.findViewById(R.id.tvReceivedMessage);
            }
        }
    }
}
