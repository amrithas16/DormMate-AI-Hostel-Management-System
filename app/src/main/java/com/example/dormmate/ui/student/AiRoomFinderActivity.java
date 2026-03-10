package com.example.dormmate.ui.student;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.dormmate.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class AiRoomFinderActivity extends AppCompatActivity {

    private EditText etSearchName;
    private LinearLayout cardResult;
    private TextView tvResultName, tvResultRoom, tvResultFloor, tvResultWing, tvNoResult;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_room_finder);

        db = FirebaseFirestore.getInstance();

        etSearchName = findViewById(R.id.etSearchName);
        cardResult = findViewById(R.id.cardResult);
        tvResultName = findViewById(R.id.tvResultName);
        tvResultRoom = findViewById(R.id.tvResultRoom);
        tvResultFloor = findViewById(R.id.tvResultFloor);
        tvResultWing = findViewById(R.id.tvResultWing);
        tvNoResult = findViewById(R.id.tvNoResult);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        etSearchName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchStudent(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void searchStudent(String query) {
        if (query.isEmpty()) {
            cardResult.setVisibility(View.GONE);
            tvNoResult.setVisibility(View.GONE);
            return;
        }

        db.collection("users")
                .whereEqualTo("role", "Student")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean found = false;
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String name = doc.getString("name");
                        if (name != null && name.toLowerCase().contains(query.toLowerCase())) {
                            displayResult(doc);
                            found = true;
                            break; // Show first match
                        }
                    }
                    if (!found) {
                        cardResult.setVisibility(View.GONE);
                        tvNoResult.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void displayResult(DocumentSnapshot doc) {
        tvNoResult.setVisibility(View.GONE);
        cardResult.setVisibility(View.VISIBLE);

        tvResultName.setText(doc.getString("name"));
        String room = doc.getString("room");
        String floor = doc.getString("floor");
        String wing = doc.getString("wing");

        tvResultRoom.setText("Room: " + (room != null ? room : "Not Assigned"));
        tvResultFloor.setText("Floor: " + (floor != null ? floor : "—"));
        tvResultWing.setText("Wing: " + (wing != null ? wing : "—"));
    }
}
