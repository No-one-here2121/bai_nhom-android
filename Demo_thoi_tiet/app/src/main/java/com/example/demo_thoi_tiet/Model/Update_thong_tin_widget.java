package com.example.demo_thoi_tiet.Model;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class Update_thong_tin_widget extends Worker {
    public Update_thong_tin_widget(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        // Call your current location fetch + weather update function
        // This should update SharedPreferences as usual.

        // Trivial approach, launch a background service or intent that updates weather_prefs
        MainActivity.fetchWeatherAndSaveForWidget(getApplicationContext());

        // Trigger widget update broadcast
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(getApplicationContext(), Widget_thoi_tiet.class));
        Intent intent = new Intent(getApplicationContext(), Widget_thoi_tiet.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        getApplicationContext().sendBroadcast(intent);

        return Result.success();
    }
}
