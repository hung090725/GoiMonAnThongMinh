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
import com.google.firebase.firestore.FirebaseFirestore;

import hung.edu.mealmindai.MainActivity;
import hung.edu.mealmindai.R;

/**
 * Handles email and password sign in for MealMind users.
 */
public class LoginActivity extends AppCompatActivity {
    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private MaterialButton loginButton;
    private TextView registerLinkTextView;
    private ProgressBar progressBar;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        bindViews();
        loginButton.setOnClickListener(v -> loginUser());
        registerLinkTextView.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
    }

    private void bindViews() {
        emailLayout = findViewById(R.id.layoutEmail);
        passwordLayout = findViewById(R.id.layoutPassword);
        emailEditText = findViewById(R.id.editTextEmail);
        passwordEditText = findViewById(R.id.editTextPassword);
        loginButton = findViewById(R.id.buttonLogin);
        registerLinkTextView = findViewById(R.id.textRegisterLink);
        progressBar = findViewById(R.id.progressLogin);
    }

    private void loginUser() {
        String email = getInputText(emailEditText).trim();
        String password = getInputText(passwordEditText);

        if (!validateInput(email, password)) {
            return;
        }

        setLoading(true);
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user == null) {
                            setLoading(false);
                            Toast.makeText(this, "Không tìm thấy tài khoản sau khi đăng nhập.", Toast.LENGTH_LONG).show();
                            return;
                        }
                        loadUserRole(user.getUid());
                    } else {
                        setLoading(false);
                        Toast.makeText(this, getFirebaseError("Đăng nhập thất bại", task.getException()), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean validateInput(String email, String password) {
        boolean isValid = true;
        emailLayout.setError(null);
        passwordLayout.setError(null);

        if (TextUtils.isEmpty(email)) {
            emailLayout.setError("Email không được để trống");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Email không đúng định dạng");
            isValid = false;
        }

        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError("Mật khẩu không được để trống");
            isValid = false;
        }

        return isValid;
    }

    private void loadUserRole(String uid) {
        firestore.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    setLoading(false);
                    String role = null;
                    if (documentSnapshot.exists()) {
                        role = documentSnapshot.getString("role");
                    }

                    if ("admin".equalsIgnoreCase(role)) {
                        openScreen(AdminDashboardActivity.class);
                    } else {
                        openScreen(MainActivity.class);
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, getFirebaseError("Không thể đọc thông tin người dùng", e), Toast.LENGTH_LONG).show();
                });
    }

    private void openScreen(Class<?> targetActivity) {
        Intent intent = new Intent(this, targetActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    public void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!isLoading);
        registerLinkTextView.setEnabled(!isLoading);
        emailEditText.setEnabled(!isLoading);
        passwordEditText.setEnabled(!isLoading);
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
