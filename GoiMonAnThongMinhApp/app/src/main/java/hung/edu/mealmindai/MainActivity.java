package hung.edu.mealmindai;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import hung.edu.mealmindai.activities.AdminDashboardActivity;
import hung.edu.mealmindai.activities.LoginActivity;
import hung.edu.mealmindai.fragments.CommunityFragment;
import hung.edu.mealmindai.fragments.FavoriteFragment;
import hung.edu.mealmindai.fragments.HomeFragment;
import hung.edu.mealmindai.fragments.ProfileFragment;
import hung.edu.mealmindai.fragments.SearchFragment;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private boolean redirectingByRole = false;
    private boolean userUiReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            EdgeToEdge.enable(this);
            setContentView(R.layout.activity_main);

            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });

            bottomNavigationView = findViewById(R.id.bottomNavigationView);
            guardUserRoute(savedInstanceState == null);
        } catch (Exception e) {
            android.widget.Toast.makeText(this, "Lỗi Main: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void guardUserRoute(boolean shouldOpenHome) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            redirectingByRole = true;
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String role = documentSnapshot.getString("role");
                    if ("admin".equalsIgnoreCase(role)) {
                        redirectingByRole = true;
                        Intent intent = new Intent(this, AdminDashboardActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        setupUserUi(shouldOpenHome);
                    }
                })
                .addOnFailureListener(e -> setupUserUi(shouldOpenHome));
    }

    private void setupUserUi(boolean shouldOpenHome) {
        if (redirectingByRole || isFinishing() || userUiReady) {
            return;
        }

        userUiReady = true;
        bottomNavigationView.setLabelVisibilityMode(NavigationBarView.LABEL_VISIBILITY_LABELED);
        bottomNavigationView.setItemHorizontalTranslationEnabled(false);
        setupBottomNavigation();

        if (shouldOpenHome) {
            openFragment(new HomeFragment());
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                animateSelectedTab();
                openFragment(new HomeFragment());
                return true;
            } else if (itemId == R.id.nav_search) {
                animateSelectedTab();
                openFragment(new SearchFragment());
                return true;
            } else if (itemId == R.id.nav_favorite) {
                animateSelectedTab();
                openFragment(new FavoriteFragment());
                return true;
            } else if (itemId == R.id.nav_community) {
                animateSelectedTab();
                openFragment(new CommunityFragment());
                return true;
            } else if (itemId == R.id.nav_profile) {
                animateSelectedTab();
                openFragment(new ProfileFragment());
                return true;
            }

            return false;
        });
    }

    private void openFragment(Fragment fragment) {
        // Replace the visible page when the user changes bottom tabs.
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    private void animateSelectedTab() {
        bottomNavigationView.animate()
                .scaleX(1.015f)
                .scaleY(1.015f)
                .setDuration(90)
                .withEndAction(() -> bottomNavigationView.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(120)
                        .start())
                .start();
    }
}
