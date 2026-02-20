package com.example.dormmate.ui.student;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.dormmate.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComplaintFragment extends Fragment {

    private Spinner spinnerCategory;
    private EditText etComplaintDesc;
    private Button btnSubmitComplaint;
    private TextView tvBack;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private final List<String> categories = Arrays.asList(
            "Select Category", "Maintenance", "Food", "Cleanliness",
            "Noise", "Security", "Internet", "Electricity", "Water", "Other");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_complaint, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        spinnerCategory = view.findViewById(R.id.spinnerCategory);
        etComplaintDesc = view.findViewById(R.id.etComplaintDesc);
        btnSubmitComplaint = view.findViewById(R.id.btnSubmitComplaint);
        tvBack = view.findViewById(R.id.tvComplaintBack);

        tvBack.setOnClickListener(v -> requireActivity().onBackPressed());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        btnSubmitComplaint.setOnClickListener(v -> submitComplaint());
    }

    private void submitComplaint() {
        String category = spinnerCategory.getSelectedItem().toString();
        String description = etComplaintDesc.getText().toString().trim();

        if (category.equals("Select Category")) {
            Toast.makeText(getContext(), "Please select a category", Toast.LENGTH_SHORT).show();
            return;
        }
        if (description.isEmpty()) {
            Toast.makeText(getContext(), "Please describe your complaint", Toast.LENGTH_SHORT).show();
            return;
        }

        if (auth.getCurrentUser() == null)
            return;

        Map<String, Object> complaint = new HashMap<>();
        complaint.put("studentUid", auth.getCurrentUser().getUid());
        complaint.put("email", auth.getCurrentUser().getEmail());
        complaint.put("category", category);
        complaint.put("description", description);
        complaint.put("status", "Open");
        complaint.put("timestamp", Timestamp.now());

        db.collection("complaints").add(complaint)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(getContext(), "Complaint submitted successfully!", Toast.LENGTH_SHORT).show();
                    etComplaintDesc.setText("");
                    spinnerCategory.setSelection(0);
                })
                .addOnFailureListener(
                        e -> Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
