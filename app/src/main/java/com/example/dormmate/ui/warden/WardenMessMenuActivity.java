package com.example.dormmate.ui.warden;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.dormmate.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class WardenMessMenuActivity extends AppCompatActivity {

    private String[] daysOfWeek = { "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday" };
    private Spinner spinnerDay;
    private EditText etBreakfast, etLunch, etDinner, etSpecial;
    private FirebaseFirestore db;
    private String currentDocId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_warden_mess_menu);

        db = FirebaseFirestore.getInstance();

        spinnerDay = findViewById(R.id.spinnerDay);
        etBreakfast = findViewById(R.id.etBreakfast);
        etLunch = findViewById(R.id.etLunch);
        etDinner = findViewById(R.id.etDinner);
        etSpecial = findViewById(R.id.etSpecial);

        findViewById(R.id.tvWardenMessBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Initialize Spinner with standard bright text so it's readable in dark mode
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, daysOfWeek);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDay.setAdapter(adapter);

        spinnerDay.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedDay = daysOfWeek[position];
                fetchMenuForDay(selectedDay);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        findViewById(R.id.btnSaveMenu).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveMenuData();
            }
        });
    }

    private void fetchMenuForDay(String day) {
        // Clear inputs first
        etBreakfast.setText("");
        etLunch.setText("");
        etDinner.setText("");
        etSpecial.setText("");
        currentDocId = null;

        db.collection("mess_menu")
                .whereEqualTo("Day", day)
                .get()
                .addOnSuccessListener(new OnSuccessListener<com.google.firebase.firestore.QuerySnapshot>() {
                    @Override
                    public void onSuccess(com.google.firebase.firestore.QuerySnapshot snapshots) {
                        if (!snapshots.isEmpty()) {
                            DocumentSnapshot doc = snapshots.getDocuments().get(0);
                            currentDocId = doc.getId();

                            etBreakfast.setText(doc.getString("Breakfast") != null ? doc.getString("Breakfast") : "");
                            etLunch.setText(doc.getString("Lunch") != null ? doc.getString("Lunch") : "");
                            etDinner.setText(doc.getString("Dinner") != null ? doc.getString("Dinner") : "");
                            etSpecial.setText(doc.getString("Special") != null ? doc.getString("Special") : "");
                        } else {
                            // Document wasn't found using exact capitalization. Attempting lowercase
                            // fallback search.
                            db.collection("mess_menu")
                                    .whereEqualTo("day", day)
                                    .get()
                                    .addOnSuccessListener(
                                            new OnSuccessListener<com.google.firebase.firestore.QuerySnapshot>() {
                                                @Override
                                                public void onSuccess(
                                                        com.google.firebase.firestore.QuerySnapshot lowSnaps) {
                                                    if (!lowSnaps.isEmpty()) {
                                                        DocumentSnapshot lowDoc = lowSnaps.getDocuments().get(0);
                                                        currentDocId = lowDoc.getId();
                                                        // Map standard values reading standard keys since fallback
                                                        // triggered
                                                        String b = lowDoc.getString("Breakfast") != null
                                                                ? lowDoc.getString("Breakfast")
                                                                : lowDoc.getString("breakfast");
                                                        String l = lowDoc.getString("Lunch") != null
                                                                ? lowDoc.getString("Lunch")
                                                                : lowDoc.getString("lunch");
                                                        String d = lowDoc.getString("Dinner") != null
                                                                ? lowDoc.getString("Dinner")
                                                                : lowDoc.getString("dinner");
                                                        String s = lowDoc.getString("Special") != null
                                                                ? lowDoc.getString("Special")
                                                                : lowDoc.getString("special");

                                                        etBreakfast.setText(b != null ? b : "");
                                                        etLunch.setText(l != null ? l : "");
                                                        etDinner.setText(d != null ? d : "");
                                                        etSpecial.setText(s != null ? s : "");
                                                    }
                                                }
                                            });
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(WardenMessMenuActivity.this, "Failed to load: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveMenuData() {
        String selectedDay = spinnerDay.getSelectedItem().toString();
        String breakfast = etBreakfast.getText().toString().trim();
        String lunch = etLunch.getText().toString().trim();
        String dinner = etDinner.getText().toString().trim();
        String special = etSpecial.getText().toString().trim();

        if (breakfast.isEmpty() || lunch.isEmpty() || dinner.isEmpty()) {
            Toast.makeText(this, "Breakfast, Lunch, and Dinner are mandatory fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> menuData = new HashMap<>();
        menuData.put("Day", selectedDay);
        menuData.put("Breakfast", breakfast);
        menuData.put("Lunch", lunch);
        menuData.put("Dinner", dinner);
        if (!special.isEmpty()) {
            menuData.put("Special", special);
        }

        if (currentDocId != null) {
            // Update existing record
            db.collection("mess_menu").document(currentDocId).update(menuData)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(WardenMessMenuActivity.this, selectedDay + " Menu Updated Successfully",
                                    Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(WardenMessMenuActivity.this, "Update Failed: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        } else {
            // Create brand new document
            db.collection("mess_menu").add(menuData)
                    .addOnSuccessListener(new OnSuccessListener<com.google.firebase.firestore.DocumentReference>() {
                        @Override
                        public void onSuccess(com.google.firebase.firestore.DocumentReference documentReference) {
                            currentDocId = documentReference.getId(); // lock document to prevent duplicate creations
                            Toast.makeText(WardenMessMenuActivity.this, selectedDay + " Menu Created Successfully",
                                    Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(WardenMessMenuActivity.this, "Creation Failed: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }
}
