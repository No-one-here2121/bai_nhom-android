package com.example.demo_thoi_tiet.Model;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;

public class Widget_thoi_tiet extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Intent intent = new Intent(context, Update_thong_tin_widget.class);
        context.startService(intent);
    }
}