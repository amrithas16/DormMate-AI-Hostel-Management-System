package com.example.dormmate_ai;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {

    Button btnStudent, btnWarden;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        btnStudent = findViewById(R.id.btnStudent);
        btnWarden = findViewById(R.id.btnWarden);

        btnStudent.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, StudentLoginActivity.class))
        );

        btnWarden.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, WardenLoginActivity.class))
        );
    }
}
