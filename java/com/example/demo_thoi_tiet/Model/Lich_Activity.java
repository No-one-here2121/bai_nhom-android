package com.example.demo_thoi_tiet.Model;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.demo_thoi_tiet.R;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class Lich_Activity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private Cong_viec_Adapter adapter;
    private List<Cong_viec> listCongViec;
    private Luu_tru_du_lieu db;
    private String weatherReceived = "Clear";
    private String tempReceived = "--";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lich);

        // GET WEATHER DATA
        if (getIntent().hasExtra("KEY_THOI_TIET")) {
            weatherReceived = getIntent().getStringExtra("KEY_THOI_TIET");
        }

        if (getIntent().hasExtra("KEY_NHIET_DO")) {
            tempReceived = getIntent().getStringExtra("KEY_NHIET_DO");
        }

        checkPermission();


        taoKenhThongBao();

        db = new Luu_tru_du_lieu(this);
        listCongViec = new ArrayList<>();

        recyclerView = findViewById(R.id.recycler_view_cong_viec);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadDuLieu();

        View fab = findViewById(R.id.fab_them);
        fab.setOnClickListener(v -> hienDialogThem());
    }

    private void hienDialogThem() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_them_cong_viec, null);

        EditText edtNoiDung = view.findViewById(R.id.nhap_noi_dung);
        TimePicker timePicker = view.findViewById(R.id.chon_gio);
        ImageButton btnGoiY = view.findViewById(R.id.nut_goi_y);
        timePicker.setIs24HourView(true);


        btnGoiY.setOnClickListener(v -> {

            String goiY = Goi_y_hoat_dong.layGoiY(weatherReceived);
            edtNoiDung.setText(goiY);
        });

        builder.setView(view);
        builder.setPositiveButton("Lưu", (dialog, which) -> {
            String noiDung = edtNoiDung.getText().toString();
            int gio = timePicker.getHour();
            int phut = timePicker.getMinute();
            String timeStr = String.format("%02d:%02d", gio, phut);

            Cong_viec cv = new Cong_viec(noiDung, timeStr, tempReceived, "");
            db.themCongViec(cv);


            datBaoThuc(cv, gio, phut);

            loadDuLieu();
            Toast.makeText(this, "Đã đặt báo thức lúc " + timeStr, Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void datBaoThuc(Cong_viec cv, int gio, int phut) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, gio);
        calendar.set(Calendar.MINUTE, phut);
        calendar.set(Calendar.SECOND, 0);

        // time set for tomorrow
        if (calendar.before(Calendar.getInstance())) {
            calendar.add(Calendar.DATE, 1);
        }

        Intent intent = new Intent(this, Bao_thuc_Receiver.class);
        intent.putExtra("NOI_DUNG", cv.getNoiDung());

        // Unique ID
        int uniqueId = (int) System.currentTimeMillis();

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, uniqueId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager != null) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            }
        }
    }

    private void taoKenhThongBao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Lịch Thời Tiết";
            String description = "Thông báo nhắc nhở";
            int importance = NotificationManager.IMPORTANCE_HIGH; // Must be HIGH
            NotificationChannel channel = new NotificationChannel("KENH_THOI_TIET", name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }


    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private void loadDuLieu() {
        listCongViec = db.layDanhSach();
        adapter = new Cong_viec_Adapter(this, listCongViec, db);
        recyclerView.setAdapter(adapter);
    }
}