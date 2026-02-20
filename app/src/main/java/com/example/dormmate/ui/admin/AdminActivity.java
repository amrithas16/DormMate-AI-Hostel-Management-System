package com.example.dormmate.ui.admin;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.dormmate.R;
import com.example.dormmate.ui.AuthFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class AdminActivity extends AppCompatActivity {

    private EditText etWardenName, etWardenEmail, etWardenPassword, etWhitelistEmail;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        etWardenName = findViewById(R.id.etWardenName);
        etWardenEmail = findViewById(R.id.etWardenEmail);
        etWardenPassword = findViewById(R.id.etWardenPassword);
        etWhitelistEmail = findViewById(R.id.etWhitelistEmail);

        findViewById(R.id.btnCreateWarden).setOnClickListener(v -> createWarden());
        findViewById(R.id.btnWhitelist).setOnClickListener(v -> whitelistEmail());
        findViewById(R.id.tvAdminBack).setOnClickListener(v -> finish());
    }

    private void createWarden() {
        String name = etWardenName.getText().toString().trim();
        String email = etWardenEmail.getText().toString().trim();
        String password = etWardenPassword.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Use a secondary Firebase app instance called "Secondary" to keep the Admin
        // logged in
        com.google.firebase.FirebaseOptions options = com.google.firebase.FirebaseApp.getInstance().getOptions();
        com.google.firebase.FirebaseApp secondaryApp;
        try {
            secondaryApp = com.google.firebase.FirebaseApp.initializeApp(this, options, "Secondary");
        } catch (Exception e) {
            secondaryApp = com.google.firebase.FirebaseApp.getInstance("Secondary");
        }

        com.google.firebase.auth.FirebaseAuth secondaryAuth = com.google.firebase.auth.FirebaseAuth
                .getInstance(secondaryApp);
        com.google.firebase.firestore.FirebaseFirestore secondaryDb = com.google.firebase.firestore.FirebaseFirestore
                .getInstance(secondaryApp);

        secondaryAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().getUser() != null) {
                        String uid = task.getResult().getUser().getUid();
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("name", name);
                        userData.put("email", email);
                        userData.put("role", "Warden");
                        userData.put("uid", uid);
                        userData.put("createdAt", com.google.firebase.Timestamp.now());

                        secondaryDb.collection("users").document(uid).set(userData)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Warden Created Successfully!", Toast.LENGTH_LONG).show();
                                    secondaryAuth.signOut();
                                    etWardenName.setText("");
                                    etWardenEmail.setText("");
                                    etWardenPassword.setText("");
                                })
                                .addOnFailureListener(e -> Toast
                                        .makeText(this, "DB Profile error: " + e.getMessage(), Toast.LENGTH_SHORT)
                                        .show());
                    } else {
                        String msg = task.getException() != null ? task.getException().getMessage() : "Auth Failed";
                        Toast.makeText(this, "Error: " + msg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void whitelistEmail() {
        String email = etWhitelistEmail.getText().toString().trim().toLowerCase();
        if (email.isEmpty())
            return;

        Map<String, Object> data = new HashMap<>();
        data.put("email", email);
        data.put("authorized", true);
        data.put("timestamp", com.google.firebase.Timestamp.now());

        db.collection("authorized_students").document(email).set(data)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Email Whitelisted: " + email, Toast.LENGTH_SHORT).show();
                    etWhitelistEmail.setText("");
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
