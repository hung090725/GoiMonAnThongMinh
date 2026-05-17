package hung.edu.mealmindai.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import hung.edu.mealmindai.R;
import hung.edu.mealmindai.activities.AddRecipeActivity;

public class CommunityFragment extends Fragment {

    private MaterialButton buttonAddRecipe;
    private TextView textMyRecipeCount;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_community, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        buttonAddRecipe = view.findViewById(R.id.buttonAddRecipe);
        textMyRecipeCount = view.findViewById(R.id.textMyRecipeCount);

        buttonAddRecipe.setOnClickListener(v -> {
            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                Toast.makeText(requireContext(), "Vui lòng đăng nhập để đóng góp công thức", Toast.LENGTH_SHORT).show();
                return;
            }
            
            Intent intent = new Intent(requireContext(), AddRecipeActivity.class);
            startActivity(intent);
        });

        loadMyRecipeCount();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMyRecipeCount();
    }

    private void loadMyRecipeCount() {
        if (textMyRecipeCount == null || db == null) {
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            textMyRecipeCount.setText("Đăng nhập để theo dõi công thức đã gửi");
            return;
        }

        db.collection("recipes")
                .whereEqualTo("authorId", user.getUid())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded()) return;
                    int total = querySnapshot.size();
                    textMyRecipeCount.setText(total + " công thức đã gửi");
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    textMyRecipeCount.setText("Chưa tải được số công thức đã gửi");
                });
    }
}
