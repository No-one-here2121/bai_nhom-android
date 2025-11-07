package com.example.demo_thoi_tiet.Model;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class Update_thong_tin_listview extends Worker {
    public Update_thong_tin_listview(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Lv_thoi_tiet.refreshWeatherList(getApplicationContext());
        return Result.success();
    }
}
