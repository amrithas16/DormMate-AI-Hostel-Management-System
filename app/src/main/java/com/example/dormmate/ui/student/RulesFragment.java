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
            "Students must return to the hostel by 10:00 PM unless prior permission is granted.",
            "No alcohol, drugs, or tobacco is permitted on hostel premises.",
            "Visitors are only allowed in the visiting area between 4 PM and 7 PM.",
            "Keep your room clean and tidy at all times.",
            "Noise must be kept to a minimum during study hours (8 PM – 10 PM).",
            "Ragging in any form is strictly prohibited and is a punishable offence.",
            "Students must report any maintenance issues to the warden immediately.",
            "Electricity and water must be used responsibly.",
            "Any damage to hostel property will lead to a fine.",
            "Students must attend the mandatory hostel meeting held every month.",
            "Gambling and card games are not permitted on campus.",
            "Students must carry their ID cards at all times within the hostel.");

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
