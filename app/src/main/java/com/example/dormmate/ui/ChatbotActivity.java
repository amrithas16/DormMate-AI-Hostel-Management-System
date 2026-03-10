package com.example.dormmate.ui;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.dormmate.R;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class ChatbotActivity extends AppCompatActivity {

    private RecyclerView rvChatMessages;
    private EditText etChatInput;
    private Button btnSendChat;
    private ChatAdapter adapter;
    private List<ChatMessage> messageList = new ArrayList<>();
    private ChatbotManager chatbotManager;
    private View layoutSyncOverlay;

    private static class ChatMessage {
        String text;
        boolean isSent;

        ChatMessage(String text, boolean isSent) {
            this.text = text;
            this.isSent = isSent;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        rvChatMessages = findViewById(R.id.rvChatMessages);
        etChatInput = findViewById(R.id.etChatInput);
        btnSendChat = findViewById(R.id.btnSendChat);
        layoutSyncOverlay = findViewById(R.id.layoutSyncOverlay);

        findViewById(R.id.tvChatbotBack).setOnClickListener(v -> finish());

        adapter = new ChatAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvChatMessages.setLayoutManager(layoutManager);
        rvChatMessages.setAdapter(adapter);

        btnSendChat.setEnabled(false);
        etChatInput.setEnabled(false);

        btnSendChat.setOnClickListener(v -> {
            String text = etChatInput.getText().toString().trim();
            if (!text.isEmpty()) {
                sendMessage(text);
                etChatInput.setText("");
            }
        });

        initChatbotContext();
    }

    private void initChatbotContext() {
        String currentUserId = "mock_user_id"; // Default fallback
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        HostelContextBuilder.buildLiveContext(currentUserId, new HostelContextBuilder.OnContextBuiltListener() {
            @Override
            public void onContextBuilt(String context) {
                runOnUiThread(() -> {
                    layoutSyncOverlay.setVisibility(View.GONE);
                    chatbotManager = new ChatbotManager(context);
                    btnSendChat.setEnabled(true);
                    etChatInput.setEnabled(true);

                    messageList.add(new ChatMessage(
                            "Hello! I am your AI DormMate assistant. I've synced your latest hostel data. How can I help you today?",
                            false));
                    adapter.notifyItemInserted(0);
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    layoutSyncOverlay.setVisibility(View.GONE);
                    messageList.add(new ChatMessage(
                            "Error syncing live hostel data: " + e.getMessage() + "\nRunning in basic mode.", false));
                    adapter.notifyItemInserted(0);
                    chatbotManager = new ChatbotManager("No context available due to error.");
                    btnSendChat.setEnabled(true);
                    etChatInput.setEnabled(true);
                });
            }
        });
    }

    private void sendMessage(String text) {
        // Add sent message
        messageList.add(new ChatMessage(text, true));
        adapter.notifyItemInserted(messageList.size() - 1);
        rvChatMessages.scrollToPosition(messageList.size() - 1);

        // Show typing indicator
        int typingIndex = messageList.size();
        messageList.add(new ChatMessage("typing...", false));
        adapter.notifyItemInserted(typingIndex);
        rvChatMessages.scrollToPosition(typingIndex);

        if (chatbotManager != null) {
            chatbotManager.sendMessage(text, new FutureCallback<GenerateContentResponse>() {
                @Override
                public void onSuccess(GenerateContentResponse result) {
                    runOnUiThread(() -> {
                        messageList.remove(typingIndex);
                        adapter.notifyItemRemoved(typingIndex);

                        messageList.add(new ChatMessage(result.getText(), false));
                        adapter.notifyItemInserted(messageList.size() - 1);
                        rvChatMessages.scrollToPosition(messageList.size() - 1);
                    });
                }

                @Override
                public void onFailure(Throwable t) {
                    runOnUiThread(() -> {
                        messageList.remove(typingIndex);
                        adapter.notifyItemRemoved(typingIndex);

                        messageList.add(
                                new ChatMessage("Error: Could not reach AI backend. (" + t.getMessage() + ")", false));
                        adapter.notifyItemInserted(messageList.size() - 1);
                        rvChatMessages.scrollToPosition(messageList.size() - 1);
                    });
                }
            });
        }
    }

    private class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ChatMessage message = messageList.get(position);
            if (message.isSent) {
                holder.layoutSent.setVisibility(View.VISIBLE);
                holder.layoutReceived.setVisibility(View.GONE);
                holder.tvSentMessage.setText(message.text);
            } else {
                holder.layoutSent.setVisibility(View.GONE);
                holder.layoutReceived.setVisibility(View.VISIBLE);
                holder.tvReceivedMessage.setText(message.text);
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
