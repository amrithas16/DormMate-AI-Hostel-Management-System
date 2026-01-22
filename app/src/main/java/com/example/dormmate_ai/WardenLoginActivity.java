package com.example.dormmate_ai;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class WardenLoginActivity extends AppCompatActivity {

    Button btnLogin, btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_warden_login);

        btnLogin = findViewById(R.id.btnWardenLogin);
        btnRegister = findViewById(R.id.btnWardenRegister);

        btnLogin.setOnClickListener(v ->
                startActivity(new Intent(WardenLoginActivity.this, WardenDashboardActivity.class))
        );

        btnRegister.setOnClickListener(v ->
                startActivity(new Intent(WardenLoginActivity.this, WardenRegisterActivity.class))
        );
    }
}
