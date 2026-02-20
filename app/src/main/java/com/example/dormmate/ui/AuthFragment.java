package com.example.dormmate.ui;

import android.content.Intent;
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
import com.example.dormmate.ui.student.StudentDashboardActivity;
import com.example.dormmate.ui.warden.WardenDashboardActivity;
import com.example.dormmate.ui.warden.EmergencyAlertService;
import com.example.dormmate.ui.admin.AdminActivity;
import com.example.dormmate.utils.BiometricHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class AuthFragment extends Fragment {

    private EditText etEmail, etPassword;
    private Button btnAuth, btnBiometric;
    private TextView tvTitle;

    private BiometricHelper biometricHelper;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_auth, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        biometricHelper = new BiometricHelper(requireContext());

        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        btnAuth = view.findViewById(R.id.btnAuth);
        btnBiometric = view.findViewById(R.id.btnBiometric);
        tvTitle = view.findViewById(R.id.tvTitle);

        updateUI();

        btnAuth.setOnClickListener(v -> handleAuth());

        if (biometricHelper.isBiometricAvailable()) {
            btnBiometric.setVisibility(View.VISIBLE);
            btnBiometric.setOnClickListener(v -> biometricHelper.showBiometricPrompt(requireActivity(),
                    new BiometricHelper.BiometricCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(getContext(), "Biometric Login Success!", Toast.LENGTH_SHORT).show();
                            if (auth.getCurrentUser() != null) {
                                // Already signed in, just fetch role and go
                                String uid = auth.getCurrentUser().getUid();
                                db.collection("users").document(uid).get()
                                        .addOnSuccessListener(doc -> {
                                            String role = doc.getString("role");
                                            if (role != null) {
                                                navigateToDashboard(role);
                                            } else {
                                                Toast.makeText(getContext(), "Profile missing for cached user",
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            } else {
                                Toast.makeText(getContext(), "Please login with password once first",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                        }
                    }));
        } else {
            btnBiometric.setVisibility(View.GONE);
        }
    }

    private void updateUI() {
        tvTitle.setText("Login");
        btnAuth.setText("Login");
        if (biometricHelper.isBiometricAvailable())
            btnBiometric.setVisibility(View.VISIBLE);
        else
            btnBiometric.setVisibility(View.GONE);
    }

    private void handleAuth() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        loginUser(email, password);
    }

    private void loginUser(String email, String password) {
        if ("admin@dormmate.com".equals(email) && "admin123".equals(password)) {
            startActivity(new Intent(requireActivity(), AdminActivity.class));
            requireActivity().finish();
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();
                    db.collection("users").document(uid).get()
                            .addOnSuccessListener(doc -> {
                                if (doc.exists()) {
                                    String role = doc.getString("role");
                                    if (role != null) {
                                        navigateToDashboard(role);
                                    } else {
                                        Toast.makeText(getContext(), "Profile corrupted: role missing",
                                                Toast.LENGTH_LONG).show();
                                        auth.signOut();
                                    }
                                } else {
                                    Toast.makeText(getContext(), "Contact Admin: Profile Not Found", Toast.LENGTH_LONG)
                                            .show();
                                    auth.signOut();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Database error: " + e.getMessage(), Toast.LENGTH_SHORT)
                                        .show();
                                auth.signOut();
                            });
                })
                .addOnFailureListener(e -> {
                    if (e instanceof com.google.firebase.auth.FirebaseAuthInvalidUserException) {
                        String errorCode = ((com.google.firebase.auth.FirebaseAuthInvalidUserException) e)
                                .getErrorCode();
                        if ("ERROR_USER_NOT_FOUND".equals(errorCode)) {
                            // Check for invitation activation
                            activatePreApprovedAccount(email, password);
                        } else {
                            Toast.makeText(getContext(), "Auth Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "Login Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void activatePreApprovedAccount(String email, String password) {
        db.collection("pre_approved_students").document(email).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String savedPass = doc.getString("password");
                        if (password.equals(savedPass)) {
                            // Match found! Create the real account
                            auth.createUserWithEmailAndPassword(email, password)
                                    .addOnSuccessListener(authResult -> {
                                        String uid = authResult.getUser().getUid();
                                        Map<String, Object> studentData = doc.getData();
                                        if (studentData != null) {
                                            studentData.put("uid", uid);
                                            studentData.put("activatedAt", com.google.firebase.Timestamp.now());

                                            // 1. Create official user doc
                                            db.collection("users").document(uid).set(studentData)
                                                    .addOnSuccessListener(aVoid -> {
                                                        // 2. Delete invitation
                                                        db.collection("pre_approved_students").document(email).delete();
                                                        // 3. Go to Dashboard
                                                        navigateToDashboard("Student");
                                                    });
                                        }
                                    })
                                    .addOnFailureListener(err -> Toast.makeText(getContext(),
                                            "Activation failed: " + err.getMessage(), Toast.LENGTH_LONG).show());
                        } else {
                            Toast.makeText(getContext(), "Invalid Credentials", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "Contact Admin: Profile Not Found", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(
                        err -> Toast.makeText(getContext(), "Activation query failed", Toast.LENGTH_SHORT).show());
    }

    private void navigateToDashboard(String role) {
        if (getContext() == null)
            return;
        Intent intent;
        if ("Warden".equals(role)) {
            intent = new Intent(requireContext(), WardenDashboardActivity.class);
            Intent serviceIntent = new Intent(requireContext(), EmergencyAlertService.class);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                requireContext().startForegroundService(serviceIntent);
            } else {
                requireContext().startService(serviceIntent);
            }
        } else if ("Student".equals(role)) {
            intent = new Intent(requireContext(), StudentDashboardActivity.class);
        } else {
            Toast.makeText(getContext(), "Invalid Role", Toast.LENGTH_SHORT).show();
            return;
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}
