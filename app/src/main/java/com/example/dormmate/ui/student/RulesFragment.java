package com.example.dormmate.ui.student;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.dormmate.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RulesFragment extends Fragment {

    private static final List<String> DEFAULT_RULES = Arrays.asList(
            "The hostel gate will open at 5:30 AM and close at 9:30 PM. All students must return to the hostel before the closing time unless special permission is granted by the hostel warden.",
            "All hostel residents must mark their daily attendance between 9:00 PM and 9:30 PM through a register or biometric system.",
            "Students must carry their college or hostel ID card at all times inside the hostel premises.",
            "Visitors are allowed only between 4:00 PM and 6:00 PM in the designated visitor area.",
            "Visitors must sign the visitor logbook and provide valid identification at the hostel security gate.",
            "No outsider or non-hostel resident is allowed to enter or stay in the hostel without permission from the hostel authorities.",
            "Students must stay only in their assigned rooms and are not allowed to change rooms without official approval.",
            "Ragging, harassment, bullying, or any form of intimidation is strictly prohibited and punishable under anti-ragging laws.",
            "Hostel meals will be served at the following times: Breakfast: 7:00 AM – 8:30 AM, Lunch: 12:30 PM – 2:00 PM, Dinner: 7:00 PM – 8:30 PM.",
            "Students must avoid wasting food and should take only the amount they can consume.",
            "Students must maintain cleanliness in the dining hall and mess area.",
            "Outside food delivery is allowed only during permitted hours and must be consumed in common areas.",
            "Students must switch off lights, fans, and electrical appliances when not in use to conserve electricity.",
            "Use of heaters, electric stoves, induction cookers, and other high-power appliances inside rooms is prohibited.",
            "Students must use water responsibly and avoid unnecessary wastage.",
            "Common bathrooms must be kept clean after use and water taps should be properly closed.",
            "Students must maintain silence during study hours from 7:00 PM to 9:00 PM.",
            "Noise, loud music, or disturbances are not allowed after 10:00 PM.",
            "Students must not damage hostel furniture, walls, electrical equipment, or other property.",
            "Students are responsible for maintaining clean and hygienic rooms at all times.",
            "Hostel authorities may conduct monthly room inspections to ensure cleanliness and rule compliance.",
            "Students planning to stay outside the hostel overnight must submit a leave request 24 hours in advance.",
            "In emergency situations, students must inform the warden or hostel office immediately before leaving.",
            "Students must report any illness or medical emergency to the hostel warden or hostel office.",
            "CCTV cameras are installed in hostel entrances, corridors, and common areas for safety monitoring.",
            "Students must immediately report any suspicious person or activity to hostel authorities.",
            "Hostel Wi-Fi is available from 6:00 AM to 11:00 PM and should be used responsibly.",
            "Students must follow all fire safety guidelines and must not block emergency exits.",
            "Students may submit complaints or suggestions through the hostel complaint register or hostel office.",
            "Violation of hostel rules may lead to warnings, fines, suspension, or cancellation of hostel accommodation.");

    private final List<String> rules = new ArrayList<>(DEFAULT_RULES);
    private RulesAdapter adapter;
    private ListenerRegistration rulesListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_rules, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView rvRules = view.findViewById(R.id.rvRules);
        rvRules.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new RulesAdapter(rules);
        rvRules.setAdapter(adapter);

        view.findViewById(R.id.tvRulesBack).setOnClickListener(v -> requireActivity().onBackPressed());

        // Real-time listener — Warden updates hostel_rules/rules, student sees it
        // instantly
        rulesListener = FirebaseFirestore.getInstance()
                .collection("hostel_rules").document("rules")
                .addSnapshotListener((doc, e) -> {
                    if (e != null || doc == null || !doc.exists())
                        return;
                    String content = doc.getString("content");
                    if (content != null && !content.isEmpty()) {
                        rules.clear();
                        for (String line : content.split("\n")) {
                            String trimmed = line.trim();
                            if (!trimmed.isEmpty())
                                rules.add(trimmed);
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (rulesListener != null)
            rulesListener.remove();
    }

    static class RulesAdapter extends RecyclerView.Adapter<RulesAdapter.ViewHolder> {
        private final List<String> rules;

        RulesAdapter(List<String> rules) {
            this.rules = rules;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_rule, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.tvNumber.setText(String.valueOf(position + 1));
            holder.tvText.setText(rules.get(position));
        }

        @Override
        public int getItemCount() {
            return rules.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvNumber, tvText;

            ViewHolder(@NonNull View view) {
                super(view);
                tvNumber = view.findViewById(R.id.tvRuleNumber);
                tvText = view.findViewById(R.id.tvRuleText);
            }
        }
    }
}
