package hung.edu.mealmindai.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import hung.edu.mealmindai.MainActivity;
import hung.edu.mealmindai.R;

/**
 * Creates a Firebase account and stores the user profile in Firestore.
 */
public class RegisterActivity extends AppCompatActivity {
    private TextInputLayout fullNameLayout;
    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputLayout confirmPasswordLayout;
    private TextInputEditText fullNameEditText;
    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private TextInputEditText confirmPasswordEditText;
    private MaterialButton registerButton;
    private TextView loginLinkTextView;
    private ProgressBar progressBar;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        bindViews();
        registerButton.setOnClickListener(v -> registerUser());
        loginLinkTextView.setOnClickListener(v -> finish());
    }

    private void bindViews() {
        fullNameLayout = findViewById(R.id.layoutFullName);
        emailLayout = findViewById(R.id.layoutEmail);
        passwordLayout = findViewById(R.id.layoutPassword);
        confirmPasswordLayout = findViewById(R.id.layoutConfirmPassword);
        fullNameEditText = findViewById(R.id.editTextFullName);
        emailEditText = findViewById(R.id.editTextEmail);
        passwordEditText = findViewById(R.id.editTextPassword);
        confirmPasswordEditText = findViewById(R.id.editTextConfirmPassword);
        registerButton = findViewById(R.id.buttonRegister);
        loginLinkTextView = findViewById(R.id.textLoginLink);
        progressBar = findViewById(R.id.progressRegister);
    }

    private void registerUser() {
        String fullName = getInputText(fullNameEditText).trim();
        String email = getInputText(emailEditText).trim();
        String password = getInputText(passwordEditText);
        String confirmPassword = getInputText(confirmPasswordEditText);

        if (!validateInput(fullName, email, password, confirmPassword)) {
            return;
        }

        setLoading(true);
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user == null) {
                            setLoading(false);
                            Toast.makeText(this, "Không tìm thấy tài khoản sau khi đăng ký.", Toast.LENGTH_LONG).show();
                            return;
                        }
                        saveUserProfile(user.getUid(), fullName, email);
                    } else {
                        setLoading(false);
                        Toast.makeText(this, getFirebaseError("Đăng ký thất bại", task.getException()), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean validateInput(String fullName, String email, String password, String confirmPassword) {
        boolean isValid = true;
        fullNameLayout.setError(null);
        emailLayout.setError(null);
        passwordLayout.setError(null);
        confirmPasswordLayout.setError(null);

        if (TextUtils.isEmpty(fullName)) {
            fullNameLayout.setError("Họ tên không được để trống");
            isValid = false;
        }

        if (TextUtils.isEmpty(email)) {
            emailLayout.setError("Email không được để trống");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Email không đúng định dạng");
            isValid = false;
        }

        if (password.length() < 6) {
            passwordLayout.setError("Mật khẩu phải có ít nhất 6 ký tự");
            isValid = false;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordLayout.setError("Mật khẩu xác nhận không khớp");
            isValid = false;
        }

        return isValid;
    }

    private void saveUserProfile(String uid, String fullName, String email) {
        Map<String, Object> user = new HashMap<>();
        user.put("uid", uid);
        user.put("fullName", fullName);
        user.put("email", email);
        user.put("avatarUrl", "");
        user.put("height", 0);
        user.put("weight", 0);
        user.put("healthGoal", "");
        user.put("monthlyFoodBudget", 0);
        user.put("role", "user");
        user.put("createdAt", FieldValue.serverTimestamp());

        firestore.collection("users").document(uid).set(user)
                .addOnSuccessListener(unused -> {
                    setLoading(false);
                    Toast.makeText(this, "Đăng ký thành công", Toast.LENGTH_SHORT).show();
                    openMainActivity();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, getFirebaseError("Không thể lưu thông tin người dùng", e), Toast.LENGTH_LONG).show();
                });
    }

    private void openMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    public void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        registerButton.setEnabled(!isLoading);
        loginLinkTextView.setEnabled(!isLoading);
        fullNameEditText.setEnabled(!isLoading);
        emailEditText.setEnabled(!isLoading);
        passwordEditText.setEnabled(!isLoading);
        confirmPasswordEditText.setEnabled(!isLoading);
    }

    private String getInputText(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString();
    }

    private String getFirebaseError(String prefix, Exception exception) {
        if (exception == null || TextUtils.isEmpty(exception.getMessage())) {
            return prefix + ". Vui lòng thử lại.";
        }
        return prefix + ": " + exception.getMessage();
    }
}
