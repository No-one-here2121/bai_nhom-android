package com.example.demo_thoi_tiet.Model;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.demo_thoi_tiet.R;

public class Bao_thuc_Receiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String noiDung = intent.getStringExtra("NOI_DUNG");

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "KENH_THOI_TIET")
                .setSmallIcon(R.drawable.calendar)
                .setContentTitle("Đến giờ rồi!")
                .setContentText(noiDung)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat manager = NotificationManagerCompat.from(context);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }
}