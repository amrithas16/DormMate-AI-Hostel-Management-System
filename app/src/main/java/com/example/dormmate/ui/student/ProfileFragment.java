package com.example.dormmate.ui.student;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.dormmate.R;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private EditText etName, etEmail, etAddress, etPassword, etPhone, etAge, etDOB, etCourse, etYearSem, etParentsDetails;
    private Button btnSave;
    private TextView tvBack, tvAvatar;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        etName = view.findViewById(R.id.etProfileName);
        etEmail = view.findViewById(R.id.etProfileEmail);
        etPhone = view.findViewById(R.id.etProfilePhone);
        etAge = view.findViewById(R.id.etProfileAge);
        etDOB = view.findViewById(R.id.etProfileDOB);
        etCourse = view.findViewById(R.id.etProfileCourse);
        etYearSem = view.findViewById(R.id.etProfileYearSem);
        etParentsDetails = view.findViewById(R.id.etProfileParentsDetails);
        etAddress = view.findViewById(R.id.etProfileAddress);
        etPassword = view.findViewById(R.id.etProfilePassword);
        btnSave = view.findViewById(R.id.btnSaveProfile);
        tvBack = view.findViewById(R.id.tvProfileBack);
        tvAvatar = view.findViewById(R.id.tvAvatar);

        tvBack.setOnClickListener(v -> requireActivity().onBackPressed());
        btnSave.setOnClickListener(v -> saveProfile());

        loadProfile();
    }

    private void loadProfile() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null)
            return;

        // Email always from FirebaseAuth (read-only)
        etEmail.setText(user.getEmail());

        // Load name, address from Firestore
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    String name = doc.getString("name");
                    String address = doc.getString("address");
                    String phone = doc.getString("phone");
                    String age = doc.getString("age");
                    String dob = doc.getString("dob");
                    String course = doc.getString("course");
                    String yearSem = doc.getString("yearSem");
                    String parentsDetails = doc.getString("parentsDetails");

                    if (name != null) {
                        etName.setText(name);
                        // Set first letter as avatar
                        tvAvatar.setText(String.valueOf(name.charAt(0)).toUpperCase());
                    } else if (user.getEmail() != null) {
                        tvAvatar.setText(String.valueOf(user.getEmail().charAt(0)).toUpperCase());
                    }
                    if (address != null) etAddress.setText(address);
                    if (phone != null) etPhone.setText(phone);
                    if (age != null) etAge.setText(age);
                    if (dob != null) etDOB.setText(dob);
                    if (course != null) etCourse.setText(course);
                    if (yearSem != null) etYearSem.setText(yearSem);
                    if (parentsDetails != null) etParentsDetails.setText(parentsDetails);
                });
    }

    private void saveProfile() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null)
            return;

        String name = etName.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String newPassword = etPassword.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(getContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update Firestore profile
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("address", address);
        updates.put("phone", etPhone.getText().toString().trim());
        updates.put("age", etAge.getText().toString().trim());
        updates.put("dob", etDOB.getText().toString().trim());
        updates.put("course", etCourse.getText().toString().trim());
        updates.put("yearSem", etYearSem.getText().toString().trim());
        updates.put("parentsDetails", etParentsDetails.getText().toString().trim());

        db.collection("users").document(user.getUid())
                .update(updates)
                .addOnSuccessListener(unused -> {
                    // Update avatar
                    tvAvatar.setText(String.valueOf(name.charAt(0)).toUpperCase());
                    Toast.makeText(getContext(), "Profile updated!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast
                        .makeText(getContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());

        // Update password if provided
        if (!TextUtils.isEmpty(newPassword)) {
            if (newPassword.length() < 6) {
                Toast.makeText(getContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }
            user.updatePassword(newPassword)
                    .addOnSuccessListener(unused -> {
                        etPassword.setText("");
                        Toast.makeText(getContext(), "Password changed!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Toast
                            .makeText(getContext(), "Password update failed: " + e.getMessage(), Toast.LENGTH_LONG)
                            .show());
        }
    }
}
