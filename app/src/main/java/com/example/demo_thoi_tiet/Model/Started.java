package com.example.demo_thoi_tiet.Model;

import android.content.Intent;
import android.content.SharedPreferences; // Import thêm cái này
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.demo_thoi_tiet.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class  Started extends AppCompatActivity {

    private Button btnMoDau;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        SharedPreferences settings = getSharedPreferences("AppPrefs", MODE_PRIVATE);

        boolean isFirstRun = settings.getBoolean("isFirstRun", true);

        if (!isFirstRun) {
            navigateNextScreen();
            return;
        }

        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("isFirstRun", false);
        editor.apply();

        setContentView(R.layout.activity_started);

        btnMoDau = findViewById(R.id.btnStart);

        btnMoDau.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateNextScreen();
            }
        });
    }

    private void navigateNextScreen() {
        FirebaseUser user = mAuth.getCurrentUser();
        Intent intent;
        if (user != null) {
            intent = new Intent(Started.this, ManHinhChinhLog.class);
        } else {
            intent = new Intent(Started.this, ManHinhChinhNonLog.class);
        }
        startActivity(intent);
        finish();
    }
}