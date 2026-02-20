package com.example.dormmate.ui.student;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.dormmate.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MessMenuFragment extends Fragment {

    private RecyclerView rvMessMenu;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mess_menu, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();

        rvMessMenu = view.findViewById(R.id.rvMessMenu);
        rvMessMenu.setLayoutManager(new LinearLayoutManager(getContext()));

        view.findViewById(R.id.tvMessBack).setOnClickListener(v -> requireActivity().onBackPressed());

        loadMessMenu();
    }

    private void loadMessMenu() {
        db.collection("mess_menu").get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Map<String, Object>> menuList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        menuList.add(doc.getData());
                    }
                    if (menuList.isEmpty()) {
                        // Use sample data if Firestore empty
                        menuList = getSampleMenu();
                    }
                    MessMenuAdapter adapter = new MessMenuAdapter(menuList);
                    rvMessMenu.setAdapter(adapter);
                })
                .addOnFailureListener(e -> {
                    // Fallback to sample data
                    MessMenuAdapter adapter = new MessMenuAdapter(getSampleMenu());
                    rvMessMenu.setAdapter(adapter);
                });
    }

    private List<Map<String, Object>> getSampleMenu() {
        List<Map<String, Object>> list = new ArrayList<>();
        String[] days = { "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday" };
        for (String day : days) {
            java.util.HashMap<String, Object> m = new java.util.HashMap<>();
            m.put("day", day);
            m.put("breakfast", "Poha, Tea/Coffee");
            m.put("lunch", "Dal, Rice, Sabzi, Roti");
            m.put("dinner", "Paneer/Egg, Rice, Roti, Salad");
            list.add(m);
        }
        return list;
    }

    // Mess Menu Adapter
    static class MessMenuAdapter extends RecyclerView.Adapter<MessMenuAdapter.ViewHolder> {
        private final List<Map<String, Object>> menuItems;

        MessMenuAdapter(List<Map<String, Object>> menuItems) {
            this.menuItems = menuItems;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_mess_day, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map<String, Object> item = menuItems.get(position);
            holder.tvDay.setText(String.valueOf(getField(item, "Day", "day", "Day")));
            holder.tvBreakfast.setText("🌅 Breakfast: " + getField(item, "Breakfast", "breakfast", "—"));
            holder.tvLunch.setText("☀️ Lunch: " + getField(item, "Lunch", "lunch", "—"));
            holder.tvDinner.setText("🌙 Dinner: " + getField(item, "Dinner", "dinner", "—"));
            holder.ratingBar.setOnRatingBarChangeListener((bar, rating, fromUser) -> {
                if (fromUser) {
                    Toast.makeText(bar.getContext(),
                            "Rated " + (int) rating + "★ for " + item.get("day"),
                            Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return menuItems.size();
        }

        // Reads field with Capitalized key first, falls back to lowercase key
        private static String getField(Map<String, Object> map, String capitalizedKey, String lowerKey,
                String defaultVal) {
            if (map.containsKey(capitalizedKey) && map.get(capitalizedKey) != null)
                return String.valueOf(map.get(capitalizedKey));
            if (map.containsKey(lowerKey) && map.get(lowerKey) != null)
                return String.valueOf(map.get(lowerKey));
            return defaultVal;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvDay, tvBreakfast, tvLunch, tvDinner;
            RatingBar ratingBar;

            ViewHolder(@NonNull View view) {
                super(view);
                tvDay = view.findViewById(R.id.tvDay);
                tvBreakfast = view.findViewById(R.id.tvBreakfast);
                tvLunch = view.findViewById(R.id.tvLunch);
                tvDinner = view.findViewById(R.id.tvDinner);
                ratingBar = view.findViewById(R.id.ratingBar);
            }
        }
    }
}
