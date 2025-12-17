package com.example.demo_thoi_tiet.Model;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.demo_thoi_tiet.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvName, tvEmail;
    private Button btnLogout, btnBack;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvName = findViewById(R.id.tvName);

        tvEmail = findViewById(R.id.tvProfileEmail);
        btnLogout = findViewById(R.id.btnLogout);
        btnBack = findViewById(R.id.btnBack);

        loadUserInfo();

        // Xử lý nút Đăng xuất
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();

            WeatherDatabaseHelper localDb = new WeatherDatabaseHelper(ProfileActivity.this);
            try {
                localDb.deleteAllLocations();
            } catch (Exception e) {
                e.printStackTrace();
            }


            Toast.makeText(this, "Đã đăng xuất!", Toast.LENGTH_SHORT).show();



            Intent intent = new Intent(ProfileActivity.this, ManHinhChinhNonLog.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Xóa lịch sử back
            startActivity(intent);
            finish();
        });


        btnBack.setOnClickListener(v -> finish());
    }

    private void loadUserInfo() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // Hiển thị Email
            tvEmail.setText(user.getEmail());

            if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                tvName.setText(user.getDisplayName());
            } else {

                db.collection("users").document(user.getUid()).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String name = documentSnapshot.getString("username"); // Hoặc trường tên bạn lưu
                                if (name != null) tvName.setText(name);
                                else tvName.setText("Người dùng");
                            }
                        })
                        .addOnFailureListener(e -> tvName.setText("Người dùng"));
            }
        } else {
            tvName.setText("Chưa đăng nhập");
            tvEmail.setText("");
        }
    }
}