package com.example.demo_thoi_tiet.Model;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.demo_thoi_tiet.Class.TaiKhoan;
import com.example.demo_thoi_tiet.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;

public class Trang_dang_ki extends AppCompatActivity {

    private Button MakeAcc;
    private TextView HaveAcc;
    EditText edtTaiKhoanDK, edtMatKhauDK, edtNhapLaiMK;
    ArrayList<TaiKhoan> listTaiKhoan;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_trang_dang_ki);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        HaveAcc = findViewById(R.id.tvHaveAcc);
        MakeAcc = findViewById(R.id.btnTaoTaiKhoan);
        edtTaiKhoanDK = findViewById(R.id.edtUsernameDK);
        edtMatKhauDK = findViewById(R.id.edtPassLogUpDK);
        edtNhapLaiMK = findViewById(R.id.edtResurePass);
        listTaiKhoan = new ArrayList<>();
        mAuth = FirebaseAuth.getInstance();
        MakeAcc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (edtMatKhauDK.getText().toString().equals(edtNhapLaiMK.getText().toString())){
                    String email = edtTaiKhoanDK.getText().toString().trim();
                    String password = edtMatKhauDK.getText().toString().trim();
                    mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()){
                                Log.d("MainActivity", "createUserWithEmail:success");
                                FirebaseUser user = mAuth.getCurrentUser();
                                Toast.makeText(getApplicationContext(), user.getEmail(), Toast.LENGTH_LONG).show();
                                Intent intentDN = new Intent(Trang_dang_ki.this, After_MoDau.class);
                                startActivity(intentDN);
                            }else {
                                Log.w("MainActivity", "createUserWithEmail:failure", task.getException());
                                Toast.makeText(getApplicationContext(), task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }else {
                    Toast.makeText(Trang_dang_ki.this, "Mat khau khong khop, vui long nhap lai", Toast.LENGTH_SHORT).show();
                }

            }
        });
        HaveAcc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentHaveAcc = new Intent(Trang_dang_ki.this, After_MoDau.class);
                startActivity(intentHaveAcc);
            }
        });
    }
}