package hung.edu.mealmindai;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import hung.edu.mealmindai.fragments.CommunityFragment;
import hung.edu.mealmindai.fragments.FavoriteFragment;
import hung.edu.mealmindai.fragments.HomeFragment;
import hung.edu.mealmindai.fragments.ProfileFragment;
import hung.edu.mealmindai.fragments.SearchFragment;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

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
            setupBottomNavigation();

            if (savedInstanceState == null) {
                openFragment(new HomeFragment());
                bottomNavigationView.setSelectedItemId(R.id.nav_home);
            }
        } catch (Exception e) {
            android.widget.Toast.makeText(this, "Lỗi Main: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                openFragment(new HomeFragment());
                return true;
            } else if (itemId == R.id.nav_search) {
                openFragment(new SearchFragment());
                return true;
            } else if (itemId == R.id.nav_favorite) {
                openFragment(new FavoriteFragment());
                return true;
            } else if (itemId == R.id.nav_community) {
                openFragment(new CommunityFragment());
                return true;
            } else if (itemId == R.id.nav_profile) {
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
}
