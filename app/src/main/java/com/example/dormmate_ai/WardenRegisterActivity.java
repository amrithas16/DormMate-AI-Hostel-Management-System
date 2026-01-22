package com.example.dormmate_ai;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class WardenRegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_warden_register);

        Button btnRegister = findViewById(R.id.btnWardenRegister);

        btnRegister.setOnClickListener(v ->
                Toast.makeText(this, "Warden Registered (Prototype)", Toast.LENGTH_SHORT).show()
        );
    }
}
