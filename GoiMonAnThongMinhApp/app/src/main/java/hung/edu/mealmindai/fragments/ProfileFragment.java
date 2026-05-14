package hung.edu.mealmindai.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import hung.edu.mealmindai.R;
import hung.edu.mealmindai.activities.LoginActivity;

public class ProfileFragment extends Fragment {

    private TextView textProfileName, textProfileEmail, textLogout;
    private FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        auth = FirebaseAuth.getInstance();
        initViews(view);
        displayUserInfo();
        setupLogout();
    }

    private void initViews(View view) {
        textProfileName = view.findViewById(R.id.textProfileName);
        textProfileEmail = view.findViewById(R.id.textProfileEmail);
        textLogout = view.findViewById(R.id.textLogout);
    }

    private void displayUserInfo() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            textProfileEmail.setText(user.getEmail());
            // Nếu có DisplayName thì hiện, không thì hiện phần đầu của email
            if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                textProfileName.setText(user.getDisplayName());
            } else {
                String email = user.getEmail();
                if (email != null && email.contains("@")) {
                    textProfileName.setText(email.split("@")[0]);
                }
            }
        }
    }

    private void setupLogout() {
        textLogout.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}
