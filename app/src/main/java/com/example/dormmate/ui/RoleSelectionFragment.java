package com.example.dormmate.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.example.dormmate.R;

public class RoleSelectionFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_role_selection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        CardView cardStudent = view.findViewById(R.id.cardStudent);
        CardView cardWarden = view.findViewById(R.id.cardWarden);
        NavController navController = Navigation.findNavController(view);

        cardStudent.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("selected_role", "Student");
            navController.navigate(R.id.action_roleSelectionFragment_to_authFragment, bundle);
        });

        cardWarden.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("selected_role", "Warden");
            navController.navigate(R.id.action_roleSelectionFragment_to_authFragment, bundle);
        });
    }
}
