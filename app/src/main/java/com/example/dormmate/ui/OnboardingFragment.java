package com.example.dormmate.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.example.dormmate.R;
import java.util.Arrays;
import java.util.List;

public class OnboardingFragment extends Fragment {

    private ViewPager2 viewPager;
    private Button btnNext;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_onboarding, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewPager = view.findViewById(R.id.viewPager);
        btnNext = view.findViewById(R.id.btnNext);

        List<OnboardingPage> pages = Arrays.asList(
                new OnboardingPage("Welcome to DormMate", "Your ultimate hostel companion for seamless living."),
                new OnboardingPage("Stay Connected", "Instant updates, complaints, and community chats."),
                new OnboardingPage("Easy Management", "Pay fees, request leave, and more with just a tap."));

        OnboardingAdapter adapter = new OnboardingAdapter(pages);
        viewPager.setAdapter(adapter);

        btnNext.setOnClickListener(v -> {
            if (viewPager.getCurrentItem() < pages.size() - 1) {
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
            } else {
                NavController navController = Navigation.findNavController(view);
                navController.navigate(R.id.action_onboardingFragment_to_roleSelectionFragment);
            }
        });
    }

    static class OnboardingPage {
        String title;
        String description;

        OnboardingPage(String title, String description) {
            this.title = title;
            this.description = description;
        }
    }

    static class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.ViewHolder> {
        private final List<OnboardingPage> pages;

        OnboardingAdapter(List<OnboardingPage> pages) {
            this.pages = pages;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_onboarding_page, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            OnboardingPage page = pages.get(position);
            holder.tvTitle.setText(page.title);
            holder.tvDescription.setText(page.description);
        }

        @Override
        public int getItemCount() {
            return pages.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle;
            TextView tvDescription;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvDescription = itemView.findViewById(R.id.tvDescription);
            }
        }
    }
}
