package com.example.dormmate.ui.warden;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.dormmate.R;
import com.google.firebase.firestore.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentListActivity extends AppCompatActivity {

    private RecyclerView rvStudents;
    private StudentAdapter adapter;
    private final List<DocumentSnapshot> allStudents = new ArrayList<>();
    private final List<DocumentSnapshot> filteredStudents = new ArrayList<>();
    private FirebaseFirestore db;
    private ListenerRegistration studentListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_list);

        db = FirebaseFirestore.getInstance();
        rvStudents = findViewById(R.id.rvStudents);
        rvStudents.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.tvStudentListBack).setOnClickListener(v -> finish());

        adapter = new StudentAdapter();
        rvStudents.setAdapter(adapter);

        // Real-time search
        EditText etSearch = findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterStudents(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        findViewById(R.id.btnAddStudent).setOnClickListener(v -> showAddStudentDialog());

        listenToStudents();
    }

    private void showAddStudentDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_student, null);
        final EditText etName = dialogView.findViewById(R.id.etNewStudentName);
        final EditText etEmail = dialogView.findViewById(R.id.etNewStudentEmail);
        final EditText etPass = dialogView.findViewById(R.id.etNewStudentPassword);
        final EditText etRoomNumber = dialogView.findViewById(R.id.etNewStudentRoom);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("ENROLL", (d, which) -> {
                    String name = etName.getText().toString().trim();
                    String email = etEmail.getText().toString().trim();
                    String password = etPass.getText().toString().trim();
                    String roomNumber = etRoomNumber.getText().toString().trim();

                    if (name.isEmpty() || email.isEmpty() || password.isEmpty() || roomNumber.isEmpty()) {
                        Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    createStudentAccount(name, email, password, roomNumber);
                })
                .setNegativeButton("Cancel", null)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
        dialog.show();
    }

    private void createStudentAccount(String name, String email, String password, String roomNumber) {
        // In the new system, we don't create an Auth account here.
        // Instead, we save to 'pre_approved_students' for the student to activate
        // later.
        Map<String, Object> invitation = new HashMap<>();
        invitation.put("name", name);
        invitation.put("email", email);
        invitation.put("password", password);
        invitation.put("roomNumber", roomNumber);
        invitation.put("role", "Student");
        invitation.put("createdAt", com.google.firebase.Timestamp.now());

        db.collection("pre_approved_students").document(email).set(invitation)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Invitation sent to " + email, Toast.LENGTH_LONG).show();
                    // Clear fields
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to send invitation: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private final Map<String, DocumentSnapshot> activatedMap = new HashMap<>();
    private final Map<String, DocumentSnapshot> invitationMap = new HashMap<>();

    private void listenToStudents() {
        // 1. Listen to Activated Students
        db.collection("users")
                .whereEqualTo("role", "Student")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Toast.makeText(this, "Profile Query Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snapshots != null) {
                        activatedMap.clear();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            String email = doc.getString("email");
                            if (email != null)
                                activatedMap.put(email, doc);
                        }
                        mergeAndDisplay();
                    }
                });

        // 2. Listen to Pending Invitations
        db.collection("pre_approved_students")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Toast.makeText(this, "Invitation Query Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snapshots != null) {
                        invitationMap.clear();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            String email = doc.getString("email");
                            if (email != null)
                                invitationMap.put(email, doc);
                        }
                        mergeAndDisplay();
                    }
                });
    }

    private void mergeAndDisplay() {
        allStudents.clear();

        // Add all activated first
        allStudents.addAll(activatedMap.values());

        // Add invitations that aren't activated yet
        for (String email : invitationMap.keySet()) {
            if (!activatedMap.containsKey(email)) {
                allStudents.add(invitationMap.get(email));
            }
        }

        filterStudents(
                findViewById(R.id.etSearch) != null ? ((EditText) findViewById(R.id.etSearch)).getText().toString()
                        : "");
    }

    private void filterStudents(String query) {
        filteredStudents.clear();
        if (query.isEmpty()) {
            filteredStudents.addAll(allStudents);
        } else {
            String q = query.toLowerCase();
            for (DocumentSnapshot doc : allStudents) {
                String name = doc.getString("name") != null ? doc.getString("name").toLowerCase() : "";
                String email = doc.getString("email") != null ? doc.getString("email").toLowerCase() : "";
                String room = doc.getString("roomNumber") != null ? doc.getString("roomNumber").toLowerCase() : "";
                if (room.isEmpty())
                    room = doc.getString("room") != null ? doc.getString("room").toLowerCase() : "";

                if (name.contains(q) || email.contains(q) || room.contains(q)) {
                    filteredStudents.add(doc);
                }
            }
        }
        runOnUiThread(() -> adapter.notifyDataSetChanged());
    }

    private void showRoomAllocationDialog(DocumentSnapshot studentDoc) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Room Details: " + studentDoc.getString("name"));

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        EditText etRoom = new EditText(this);
        etRoom.setHint("Room Number");
        String currentRoom = studentDoc.getString("roomNumber");
        if (currentRoom == null)
            currentRoom = studentDoc.getString("room");
        etRoom.setText(currentRoom);
        layout.addView(etRoom);

        builder.setView(layout);
        builder.setPositiveButton("SAVE", (dialog, which) -> {
            String newRoom = etRoom.getText().toString().trim();
            if (!newRoom.isEmpty()) {
                studentDoc.getReference().update("roomNumber", newRoom, "room", newRoom)
                        .addOnSuccessListener(unused -> Toast.makeText(this, "Updated", Toast.LENGTH_SHORT).show());
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (studentListener != null)
            studentListener.remove();
    }

    class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_student_card, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            DocumentSnapshot doc = filteredStudents.get(position);
            String name = doc.getString("name") != null ? doc.getString("name") : "Unknown";
            String email = doc.getString("email") != null ? doc.getString("email") : "";

            // Check if it's an invitation or activated
            boolean isInvitation = doc.getReference().getPath().contains("pre_approved_students");

            holder.tvName.setText(name + (isInvitation ? " (Invitation)" : ""));
            holder.tvEmail.setText(email);

            String room = doc.getString("roomNumber");
            if (room == null)
                room = doc.getString("room");
            holder.tvRoom.setText("Room " + (room != null ? room : "—"));

            holder.tvFloor.setText("Floor " + (doc.getString("floor") != null ? doc.getString("floor") : "—"));
            holder.tvAvatar.setText(!name.isEmpty() ? String.valueOf(name.charAt(0)).toUpperCase() : "?");
            holder.btnAllocate.setOnClickListener(v -> showRoomAllocationDialog(doc));
        }

        @Override
        public int getItemCount() {
            return filteredStudents.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvEmail, tvRoom, tvFloor, tvAvatar;
            Button btnAllocate;

            VH(@NonNull View view) {
                super(view);
                tvName = view.findViewById(R.id.tvStudentName);
                tvEmail = view.findViewById(R.id.tvStudentEmail);
                tvRoom = view.findViewById(R.id.tvStudentRoom);
                tvFloor = view.findViewById(R.id.tvStudentFloor);
                tvAvatar = view.findViewById(R.id.tvStudentAvatar);
                btnAllocate = view.findViewById(R.id.btnAllocateRoom);
            }
        }
    }
}
