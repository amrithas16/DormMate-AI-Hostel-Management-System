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
        final EditText etPhone = dialogView.findViewById(R.id.etNewStudentPhone);
        final EditText etAge = dialogView.findViewById(R.id.etNewStudentAge);
        final EditText etDOB = dialogView.findViewById(R.id.etNewStudentDOB);
        final EditText etCourse = dialogView.findViewById(R.id.etNewStudentCourse);
        final EditText etYearSem = dialogView.findViewById(R.id.etNewStudentYearSem);
        final EditText etParentsDetails = dialogView.findViewById(R.id.etNewStudentParentsDetails);
        final EditText etPass = dialogView.findViewById(R.id.etNewStudentPassword);
        final TextView tvStatusMessage = dialogView.findViewById(R.id.tvStatusMessage);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("ENROLL", null) // Set to null to prevent auto-dismissal
                .setNegativeButton("Cancel", null)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        dialog.setOnShowListener(new android.content.DialogInterface.OnShowListener() {
            @Override
            public void onShow(android.content.DialogInterface dialogInterface) {
                Button button = ((androidx.appcompat.app.AlertDialog) dialog)
                        .getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String name = etName.getText().toString().trim();
                        String email = etEmail.getText().toString().trim();
                        String phone = etPhone.getText().toString().trim();
                        String age = etAge.getText().toString().trim();
                        String dob = etDOB.getText().toString().trim();
                        String course = etCourse.getText().toString().trim();
                        String yearSem = etYearSem.getText().toString().trim();
                        String parentsDetails = etParentsDetails.getText().toString().trim();
                        String password = etPass.getText().toString().trim();

                        boolean isValid = true;

                        if (name.isEmpty()) {
                            etName.setError("Name is required");
                            isValid = false;
                        }

                        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                            etEmail.setError("Valid email is required");
                            isValid = false;
                        }

                        if (!isValid) {
                            return;
                        }

                        createStudentAccount(name, email, password, phone, age, dob, course, yearSem, parentsDetails,
                                etName, etEmail, etPass, etPhone, etAge, etDOB, etCourse, etYearSem, etParentsDetails,
                                tvStatusMessage);
                    }
                });
            }
        });

        dialog.show();
    }

    private void createStudentAccount(String name, String email, String password, String phone, String age, String dob,
            String course, String yearSem, String parentsDetails, EditText etName, EditText etEmail,
            EditText etPass, EditText etPhone, EditText etAge, EditText etDOB, EditText etCourse,
            EditText etYearSem, EditText etParentsDetails, TextView tvStatusMessage) {
        Map<String, Object> newStudent = new HashMap<>();
        newStudent.put("name", name);
        newStudent.put("email", email);
        newStudent.put("password", password);
        newStudent.put("phone", phone);
        newStudent.put("age", age);
        newStudent.put("dob", dob);
        newStudent.put("course", course);
        newStudent.put("yearSem", yearSem);
        newStudent.put("parentsDetails", parentsDetails);
        newStudent.put("role", "Student");
        newStudent.put("createdAt", com.google.firebase.Timestamp.now());

        db.collection("pre_approved_students").document(email).set(newStudent)
                .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(StudentListActivity.this, "Student added to pre-approval list",
                                Toast.LENGTH_LONG).show();
                        // Clear fields and reset
                        etName.setText("");
                        etEmail.setText("");
                        etPass.setText("123123");
                        etPhone.setText("");
                        etAge.setText("");
                        etDOB.setText("");
                        etCourse.setText("");
                        etYearSem.setText("");
                        etParentsDetails.setText("");
                        tvStatusMessage.setText("User acc created. Student needs to login via this new cred.");
                        tvStatusMessage.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(StudentListActivity.this, "Failed to add student: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private final Map<String, DocumentSnapshot> activatedMap = new HashMap<>();
    private final Map<String, DocumentSnapshot> invitationMap = new HashMap<>();

    private void listenToStudents() {
        // 1. Listen to Activated Students
        db.collection("users")
                .whereEqualTo("role", "Student")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@androidx.annotation.Nullable QuerySnapshot snapshots,
                            @androidx.annotation.Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Toast.makeText(StudentListActivity.this, "Profile Query Failed: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
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
                    }
                });

        // 2. Listen to Pending Invitations
        db.collection("pre_approved_students")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@androidx.annotation.Nullable QuerySnapshot snapshots,
                            @androidx.annotation.Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Toast.makeText(StudentListActivity.this, "Invitation Query Failed: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
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

                if (name.contains(q) || email.contains(q)) {
                    filteredStudents.add(doc);
                }
            }
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void showStudentOptionsDialog(DocumentSnapshot doc) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_student_options, null);
        TextView tvStudentNameDialogMsg = dialogView.findViewById(R.id.tvStudentNameDialogMsg);
        Button btnAllocateRoom = dialogView.findViewById(R.id.btnOptionAllocateRoom);
        Button btnUpdateDetails = dialogView.findViewById(R.id.btnOptionUpdateDetails);
        Button btnDeleteStudent = dialogView.findViewById(R.id.btnOptionDeleteStudent);

        tvStudentNameDialogMsg.setText("Actions for " + doc.getString("name"));

        AlertDialog optionsDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (optionsDialog.getWindow() != null) {
            optionsDialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        btnAllocateRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                optionsDialog.dismiss();
                showAllocateRoomDialog(doc);
            }
        });

        btnUpdateDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                optionsDialog.dismiss();
                showUpdateDetailsDialog(doc);
            }
        });

        btnDeleteStudent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                optionsDialog.dismiss();
                showDeleteConfirmationDialog(doc);
            }
        });

        optionsDialog.show();
    }

    private void showDeleteConfirmationDialog(DocumentSnapshot doc) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Student")
                .setMessage("Are you sure you want to completely remove " + doc.getString("name") + " from DormMate?")
                .setPositiveButton("Delete", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        doc.getReference().delete()
                                .addOnSuccessListener(aVoid -> Toast.makeText(StudentListActivity.this,
                                        "Student deleted successfully", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast
                                        .makeText(StudentListActivity.this,
                                                "Failed to delete student: " + e.getMessage(), Toast.LENGTH_LONG)
                                        .show());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showUpdateDetailsDialog(DocumentSnapshot doc) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_student, null);

        // Hide password/status fields as this is an update dialog
        dialogView.findViewById(R.id.etNewStudentPassword).setVisibility(View.GONE);
        dialogView.findViewById(R.id.tvStatusMessage).setVisibility(View.GONE);

        // We reuse dialog_add_student, but update the Text
        View titleView = ((ViewGroup) dialogView).getChildAt(0);
        if (titleView instanceof TextView) {
            ((TextView) titleView).setText("Update Student Details");
        }

        final EditText etName = dialogView.findViewById(R.id.etNewStudentName);
        final EditText etEmail = dialogView.findViewById(R.id.etNewStudentEmail);
        final EditText etPhone = dialogView.findViewById(R.id.etNewStudentPhone);
        final EditText etAge = dialogView.findViewById(R.id.etNewStudentAge);
        final EditText etDOB = dialogView.findViewById(R.id.etNewStudentDOB);
        final EditText etCourse = dialogView.findViewById(R.id.etNewStudentCourse);
        final EditText etYearSem = dialogView.findViewById(R.id.etNewStudentYearSem);
        final EditText etParentsDetails = dialogView.findViewById(R.id.etNewStudentParentsDetails);

        // Pre-fill
        etName.setText(doc.getString("name") != null ? doc.getString("name") : "");
        etEmail.setText(doc.getString("email") != null ? doc.getString("email") : "");
        etEmail.setEnabled(false); // Email shouldn't be edited as it's often the auth/document key

        if (doc.getString("phone") != null)
            etPhone.setText(doc.getString("phone"));
        if (doc.getString("age") != null)
            etAge.setText(doc.getString("age"));
        if (doc.getString("dob") != null)
            etDOB.setText(doc.getString("dob"));
        if (doc.getString("course") != null)
            etCourse.setText(doc.getString("course"));
        if (doc.getString("yearSem") != null)
            etYearSem.setText(doc.getString("yearSem"));
        if (doc.getString("parentsDetails") != null)
            etParentsDetails.setText(doc.getString("parentsDetails"));

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("UPDATE", null)
                .setNegativeButton("Cancel", null)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        dialog.setOnShowListener(new android.content.DialogInterface.OnShowListener() {
            @Override
            public void onShow(android.content.DialogInterface dialogInterface) {
                Button button = ((androidx.appcompat.app.AlertDialog) dialog)
                        .getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String name = etName.getText().toString().trim();
                        if (name.isEmpty()) {
                            etName.setError("Name is required");
                            return;
                        }

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("name", name);
                        updates.put("phone", etPhone.getText().toString().trim());
                        updates.put("age", etAge.getText().toString().trim());
                        updates.put("dob", etDOB.getText().toString().trim());
                        updates.put("course", etCourse.getText().toString().trim());
                        updates.put("yearSem", etYearSem.getText().toString().trim());
                        updates.put("parentsDetails", etParentsDetails.getText().toString().trim());

                        doc.getReference().update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(StudentListActivity.this, "Details Updated", Toast.LENGTH_SHORT)
                                            .show();
                                    dialog.dismiss();
                                })
                                .addOnFailureListener(e -> Toast.makeText(StudentListActivity.this,
                                        "Update Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    }
                });
            }
        });

        dialog.show();
    }

    private void showAllocateRoomDialog(DocumentSnapshot doc) {
        // ... existing implementation
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
            String course = doc.getString("course");
            String yearSem = doc.getString("yearSem");

            // Check if it's an invitation or activated
            boolean isInvitation = doc.getReference().getPath().contains("pre_approved_students");

            holder.tvName.setText(name + (isInvitation ? " (Invitation)" : ""));
            holder.tvEmail.setText(email);
            holder.tvAvatar.setText(!name.isEmpty() ? String.valueOf(name.charAt(0)).toUpperCase() : "?");

            if (course != null && !course.isEmpty()) {
                holder.tvCourse.setVisibility(View.VISIBLE);
                holder.tvCourse.setText(course + (yearSem != null && !yearSem.isEmpty() ? " - " + yearSem : ""));
            } else {
                holder.tvCourse.setVisibility(View.GONE);
            }

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showStudentOptionsDialog(doc);
                }
            });
        }

        @Override
        public int getItemCount() {
            return filteredStudents.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvEmail, tvAvatar, tvCourse;

            VH(@NonNull View view) {
                super(view);
                tvName = view.findViewById(R.id.tvStudentName);
                tvEmail = view.findViewById(R.id.tvStudentEmail);
                tvAvatar = view.findViewById(R.id.tvStudentAvatar);
                tvCourse = view.findViewById(R.id.tvStudentCourse);
            }
        }
    }
}
