package com.example.dormmate_ai;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class DummyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dummy);

        String title = getIntent().getStringExtra("title");
        ((TextView) findViewById(R.id.tvTitle)).setText(title);
    }
}
