package hung.edu.mealmindai.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import hung.edu.mealmindai.R;
import hung.edu.mealmindai.models.User;
import hung.edu.mealmindai.repositories.UserRepository;

public class EditHealthProfileActivity extends AppCompatActivity {

    private TextInputEditText editHeight, editWeight, editMonthlyBudget, editMealBudget, editAvailableTime;
    private Spinner spinnerHealthGoal;
    private MaterialButton buttonSave;
    private ProgressBar progressBar;
    
    private UserRepository userRepository;
    private final String[] healthGoals = {"Giảm cân", "Tăng cân", "Ăn cân bằng", "Ăn tiết kiệm", "Ăn nhanh"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_health_profile);

        userRepository = new UserRepository();
        initViews();
        setupToolbar();
        setupSpinner();
        loadCurrentData();

        buttonSave.setOnClickListener(v -> saveProfile());
    }

    private void initViews() {
        editHeight = findViewById(R.id.editHeight);
        editWeight = findViewById(R.id.editWeight);
        editMonthlyBudget = findViewById(R.id.editMonthlyBudget);
        editMealBudget = findViewById(R.id.editMealBudget);
        editAvailableTime = findViewById(R.id.editAvailableTime);
        spinnerHealthGoal = findViewById(R.id.spinnerHealthGoal);
        buttonSave = findViewById(R.id.buttonSaveProfile);
        progressBar = findViewById(R.id.progressEdit);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbarEdit);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_spinner_item, healthGoals);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerHealthGoal.setAdapter(adapter);
    }

    private void loadCurrentData() {
        setLoading(true);
        userRepository.getCurrentUserProfile(new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                setLoading(false);
                if (user != null) {
                    if (user.getHeight() != null) editHeight.setText(String.valueOf(user.getHeight()));
                    if (user.getWeight() != null) editWeight.setText(String.valueOf(user.getWeight()));
                    if (user.getMonthlyFoodBudget() != null) editMonthlyBudget.setText(String.valueOf(user.getMonthlyFoodBudget().intValue()));
                    if (user.getMealBudget() != null) editMealBudget.setText(String.valueOf(user.getMealBudget().intValue()));
                    if (user.getAvailableTime() != null) editAvailableTime.setText(String.valueOf(user.getAvailableTime()));
                    
                    // Set spinner selection
                    if (user.getHealthGoal() != null) {
                        for (int i = 0; i < healthGoals.length; i++) {
                            if (healthGoals[i].equals(user.getHealthGoal())) {
                                spinnerHealthGoal.setSelection(i);
                                break;
                            }
                        }
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                setLoading(false);
                Toast.makeText(EditHealthProfileActivity.this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveProfile() {
        String heightStr = editHeight.getText().toString().trim();
        String weightStr = editWeight.getText().toString().trim();
        String monthlyStr = editMonthlyBudget.getText().toString().trim();
        String mealStr = editMealBudget.getText().toString().trim();
        String timeStr = editAvailableTime.getText().toString().trim();
        String goal = spinnerHealthGoal.getSelectedItem().toString();

        if (TextUtils.isEmpty(heightStr) || TextUtils.isEmpty(weightStr)) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ chiều cao, cân nặng", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Double height = Double.parseDouble(heightStr);
            Double weight = Double.parseDouble(weightStr);
            Double monthly = TextUtils.isEmpty(monthlyStr) ? 0.0 : Double.parseDouble(monthlyStr);
            Double meal = TextUtils.isEmpty(mealStr) ? 0.0 : Double.parseDouble(mealStr);
            Integer time = TextUtils.isEmpty(timeStr) ? 0 : Integer.parseInt(timeStr);

            if (height < 0 || weight < 0 || monthly < 0 || meal < 0 || time < 0) {
                Toast.makeText(this, "Giá trị không được âm", Toast.LENGTH_SHORT).show();
                return;
            }

            setLoading(true);
            userRepository.updateHealthProfile(height, weight, goal, monthly, meal, time, new UserRepository.ActionCallback() {
                @Override
                public void onSuccess() {
                    setLoading(false);
                    Toast.makeText(EditHealthProfileActivity.this, "Cập nhật hồ sơ thành công", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(Exception e) {
                    setLoading(false);
                    Toast.makeText(EditHealthProfileActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Dữ liệu nhập vào không hợp lệ", Toast.LENGTH_SHORT).show();
        }
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        buttonSave.setEnabled(!isLoading);
    }
}
