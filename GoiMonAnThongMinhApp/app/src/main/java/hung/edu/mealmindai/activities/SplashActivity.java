package hung.edu.mealmindai.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import hung.edu.mealmindai.MainActivity;
import hung.edu.mealmindai.R;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Hiển thị màn hình chờ trong 1.5 giây
        new Handler(Looper.getMainLooper()).postDelayed(this::checkLoginStatus, 1500);
    }

    private void checkLoginStatus() {
        try {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            
            if (currentUser != null) {
                // Đã đăng nhập -> Vào thẳng trang chủ
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
            } else {
                // Chưa đăng nhập -> Ra trang đăng nhập
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            }
            finish();
        } catch (Exception e) {
            android.widget.Toast.makeText(this, "Lỗi Splash: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
            // Nếu lỗi nặng quá thì chờ 3s rồi thử vào LoginActivity xem sao
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                finish();
            }, 3000);
        }
    }
}
