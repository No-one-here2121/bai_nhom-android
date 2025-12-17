package com.example.demo_thoi_tiet.Model;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
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

import com.example.demo_thoi_tiet.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class After_MoDau extends AppCompatActivity {

    private EditText edtUsernameDN, editPassLogUpDN;
    private Button btnDangNhap;
    private TextView Quen,DangKi;
    private FirebaseAuth mAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_after_mo_dau);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        DangKi = findViewById(R.id.btnDangKi);
        btnDangNhap = findViewById(R.id.btnDN);
        edtUsernameDN = findViewById(R.id.edtUsernameDN);
        editPassLogUpDN = findViewById(R.id.editPassLogUpDN);
        mAuth = FirebaseAuth.getInstance();
        DangKi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent IntentDangKi = new Intent(After_MoDau.this,Trang_dang_ki.class);
                startActivity(IntentDangKi);
            }
        });
        btnDangNhap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = edtUsernameDN.getText().toString().trim();
                String password = editPassLogUpDN.getText().toString().trim();
                if (email.isEmpty() || password.isEmpty()){
                    Toast.makeText(After_MoDau.this, "Vui long khong duoc de trong", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!isValidEmail(email)){
                    Toast.makeText(After_MoDau.this, "Vui long nhap dung dinh dang email", Toast.LENGTH_SHORT).show();
                    return;
                }
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(After_MoDau.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()){
                                    Log.d("DangNhap", "signInWithEmail:success");
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    Toast.makeText(After_MoDau.this, "Dang nhap thanh cong", Toast.LENGTH_SHORT).show();
                                    Intent intentManHinhChinh = new Intent(After_MoDau.this, ManHinhChinhLog.class);
                                    startActivity(intentManHinhChinh);
                                }else{
                                    Log.w("DangNhap", "signInWithEmail:failure", task.getException());
                                    Toast.makeText(After_MoDau.this, "Sai tai khoan hoac mat khau", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });
    }
    private boolean isValidEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
}