package com.example.dormmate_ai;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class StudentLoginActivity extends AppCompatActivity {

    Button btnLogin;
    TextView tvRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_login);

        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);

        btnLogin.setOnClickListener(v ->
                startActivity(new Intent(StudentLoginActivity.this, StudentDashboardActivity.class))
        );

        tvRegister.setOnClickListener(v ->
                startActivity(new Intent(StudentLoginActivity.this, StudentRegisterActivity.class))
        );
    }
}
