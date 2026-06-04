package hung.edu.mealmindai.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;

import java.text.DecimalFormat;

import hung.edu.mealmindai.R;
import hung.edu.mealmindai.activities.AiSuggestionHistoryActivity;
import hung.edu.mealmindai.activities.EditHealthProfileActivity;
import hung.edu.mealmindai.activities.LoginActivity;
import hung.edu.mealmindai.activities.MySubmittedRecipesActivity;
import hung.edu.mealmindai.activities.NotificationActivity;
import hung.edu.mealmindai.models.User;
import hung.edu.mealmindai.repositories.UserRepository;

public class ProfileFragment extends Fragment {

    private TextView textAvatar, textName, textEmail;
    private TextView textHealthGoal, textHeight, textWeight;
    private TextView textMonthlyBudget, textMealBudget, textAvailableTime;
    private View layoutContent;
    private ProgressBar progressBar;
    private Button buttonEdit, buttonLogout, buttonOpenNotifications, buttonOpenAiHistory, buttonOpenMyRecipes;

    private UserRepository userRepository;
    private final DecimalFormat currencyFormat = new DecimalFormat("#,###");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        userRepository = new UserRepository();
        initViews(view);
        setupClickListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserProfile();
    }

    private void initViews(View view) {
        textAvatar = view.findViewById(R.id.textProfileAvatar);
        textName = view.findViewById(R.id.textProfileName);
        textEmail = view.findViewById(R.id.textProfileEmail);
        textHealthGoal = view.findViewById(R.id.textHealthGoal);
        textHeight = view.findViewById(R.id.textHeight);
        textWeight = view.findViewById(R.id.textWeight);
        textMonthlyBudget = view.findViewById(R.id.textMonthlyBudget);
        textMealBudget = view.findViewById(R.id.textMealBudget);
        textAvailableTime = view.findViewById(R.id.textAvailableTime);
        layoutContent = view.findViewById(R.id.layoutProfileContent);
        progressBar = view.findViewById(R.id.progressProfile);
        buttonEdit = view.findViewById(R.id.buttonEditProfile);
        buttonLogout = view.findViewById(R.id.buttonLogout);
        buttonOpenNotifications = view.findViewById(R.id.buttonOpenNotifications);
        buttonOpenAiHistory = view.findViewById(R.id.buttonOpenAiHistory);
        buttonOpenMyRecipes = view.findViewById(R.id.buttonOpenMyRecipes);
    }

    private void setupClickListeners() {
        buttonEdit.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), EditHealthProfileActivity.class));
        });

        buttonOpenNotifications.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), NotificationActivity.class)));

        buttonOpenAiHistory.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AiSuggestionHistoryActivity.class)));

        buttonOpenMyRecipes.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), MySubmittedRecipesActivity.class)));

        buttonLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private void loadUserProfile() {
        setLoading(true);
        userRepository.getCurrentUserProfile(new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                if (!isAdded()) return;
                setLoading(false);
                displayUser(user);
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                setLoading(false);
                Toast.makeText(requireContext(), "Lỗi tải hồ sơ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        layoutContent.setVisibility(isLoading ? View.INVISIBLE : View.VISIBLE);
    }

    private void displayUser(User user) {
        if (user == null) return;

        String name = user.getFullName();
        textName.setText(name != null && !name.isEmpty() ? name : "Người dùng");
        textEmail.setText(user.getEmail());

        // Avatar initials
        if (name != null && !name.isEmpty()) {
            textAvatar.setText(name.substring(0, 1).toUpperCase());
        } else {
            textAvatar.setText("U");
        }

        // Stats
        textHealthGoal.setText(user.getHealthGoal() != null ? user.getHealthGoal() : "Chưa thiết lập");
        textHeight.setText(formatValue(user.getHeight(), "cm"));
        textWeight.setText(formatValue(user.getWeight(), "kg"));
        
        textMonthlyBudget.setText(formatCurrency(user.getMonthlyFoodBudget()));
        textMealBudget.setText(formatCurrency(user.getMealBudget()));
        
        int time = user.getAvailableTime() != null ? user.getAvailableTime() : 0;
        textAvailableTime.setText(time + " phút");
    }

    private String formatValue(Double val, String unit) {
        if (val == null) return "0 " + unit;
        if (val <= 0) return "0 " + unit;
        return val.intValue() + " " + unit;
    }

    private String formatCurrency(Double amount) {
        if (amount == null) return "0đ";
        if (amount <= 0) return "0đ";
        return currencyFormat.format(amount).replace(",", ".") + "đ";
    }
}
