package com.example.demo_thoi_tiet.Model;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.widget.RemoteViews;
import androidx.annotation.Nullable;
import com.example.demo_thoi_tiet.R;

public class Update_thong_tin_widget extends Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateWidget();
        return super.onStartCommand(intent, flags, startId);
    }

    private void updateWidget() {
        SharedPreferences sharedPreferences = getSharedPreferences("WidgetData", MODE_PRIVATE);

        String cityName = sharedPreferences.getString("city", "Đang cập nhật...");
        String temp = sharedPreferences.getString("temp", "--");
        String status = sharedPreferences.getString("status", "Chạm để tải");
        String humidity = sharedPreferences.getString("humidity", "--%");
        String feelsLike = sharedPreferences.getString("feels_like", "--°C");

        int weatherId = sharedPreferences.getInt("weatherId", 800);

        RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget_thoi_tiet);

        views.setTextViewText(R.id.Location_widget, cityName);
        views.setTextViewText(R.id.Location_degree, temp + "°C");
        views.setTextViewText(R.id.Location_des, status);
        views.setTextViewText(R.id.Location_feel_like, feelsLike);
        views.setTextViewText(R.id.Loaction_rain_percent, humidity);

        updateWidgetStyle(views, weatherId);

        try {
            Intent intentActivity = new Intent(getApplicationContext(), Class.forName("com.example.demo_thoi_tiet.Model.ManHinhChinhLog"));
            intentActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intentActivity, PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.backgr, pendingIntent);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        AppWidgetManager manager = AppWidgetManager.getInstance(getApplicationContext());
        ComponentName widgetVar = new ComponentName(getApplicationContext(), Widget_thoi_tiet.class);
        manager.updateAppWidget(widgetVar, views);
    }


    private void updateWidgetStyle(RemoteViews views, int weatherId) {
        int iconRes;
        int bgColor;
        int textColor;


        if (weatherId >= 200 && weatherId <= 232) {

            iconRes = R.drawable.rain;
            bgColor = android.graphics.Color.parseColor("#40666A");
            textColor = android.graphics.Color.parseColor("#C9E8E0");
        } else if ((weatherId >= 300 && weatherId <= 321) || (weatherId >= 500 && weatherId <= 531)) {
            //  RAIN
            iconRes = R.drawable.rain;
            bgColor = android.graphics.Color.parseColor("#40666A");
            textColor = android.graphics.Color.parseColor("#C9E8E0");
        } else if (weatherId >= 600 && weatherId <= 622) {
            //  SNOW
            iconRes = R.drawable.snow;
            bgColor = android.graphics.Color.parseColor("#99B8CC");
            textColor = android.graphics.Color.parseColor("#E4F1F9");
        } else if (weatherId >= 701 && weatherId <= 781) {
            //  ATMOSPHERE
            iconRes = R.drawable.cloud_black;
            bgColor = android.graphics.Color.parseColor("#91B4C6");
            textColor = android.graphics.Color.parseColor("#CAD7DF");
        } else if (weatherId == 800) {
            // CLEAR
            iconRes = R.drawable.sunny;
            bgColor = android.graphics.Color.parseColor("#FAE2BD");
            textColor = android.graphics.Color.parseColor("#EFAA82");
        } else if (weatherId >= 801 && weatherId <= 804) {
            // CLOUDS
            iconRes = R.drawable.white_cloud; // or cloud_black
            bgColor = android.graphics.Color.parseColor("#91B4C6");
            textColor = android.graphics.Color.parseColor("#CAD7DF");
        } else {
            //DEFAULT
            iconRes = R.drawable.sunny;
            bgColor = android.graphics.Color.parseColor("#FAE2BD");
            textColor = android.graphics.Color.parseColor("#EFAA82");
        }

        //  Icon
        views.setImageViewResource(R.id.widget_icon, iconRes);


        views.setInt(R.id.widget_background, "setColorFilter", bgColor);

        // Text Color
        views.setTextColor(R.id.Location_widget, textColor);
        views.setTextColor(R.id.Location_degree, textColor);
        views.setTextColor(R.id.Location_des, textColor);
        views.setTextColor(R.id.Location_feel_like, textColor);
        views.setTextColor(R.id.Loaction_rain_percent, textColor);
    }
}