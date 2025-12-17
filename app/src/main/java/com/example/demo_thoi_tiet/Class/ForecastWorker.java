//chay ngam,ke ca khi khong mo app
package com.example.demo_thoi_tiet.Class;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.demo_thoi_tiet.Model.ManHinhChinhLog;
import com.example.demo_thoi_tiet.R;

import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ForecastWorker extends Worker {

    private static final String API_KEY = "3fb694b002ed5b62526c93906f111a4e";
    public static final String KEY_IS_APP_LAUNCH = "is_app_launch";

    public ForecastWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();

        //lay toa do da luu
        SharedPreferences prefs = context.getSharedPreferences("UserLocationPrefs", Context.MODE_PRIVATE);
        String latStr = prefs.getString("last_latitude", null);
        String lonStr = prefs.getString("last_longitude", null);

        //khong co -> that bai -> bao that bai va ngung
        if (latStr == null || lonStr == null) return Result.failure();

        //chay vi?
        //TRUE:vua mo app
        //FALSE:chua mo app -> thong bao thong tin thoi tiet vi tri hien tai sau moi a gio
        boolean isAppLaunch = getInputData().getBoolean(KEY_IS_APP_LAUNCH, false);

        OkHttpClient client = new OkHttpClient();

        try {
            //lay thoi tiet hien tai (Current Weather)
            String urlWeather = "https://api.openweathermap.org/data/2.5/weather?lat=" + latStr + "&lon=" + lonStr + "&appid=" + API_KEY + "&units=metric";
            Request reqWeather = new Request.Builder().url(urlWeather).build();
            Response resWeather = client.newCall(reqWeather).execute(); // Chạy đồng bộ

            if (!resWeather.isSuccessful() || resWeather.body() == null) return Result.retry();

            JSONObject jsonWeather = new JSONObject(resWeather.body().string());
            int temp = (int) Math.round(jsonWeather.getJSONObject("main").getDouble("temp"));
            String desc = jsonWeather.getJSONArray("weather").getJSONObject(0).getString("description");
            String cityName = jsonWeather.getString("name");

            // lay aqi
            String urlAQI = "https://api.openweathermap.org/data/2.5/air_pollution?lat=" + latStr + "&lon=" + lonStr + "&appid=" + API_KEY;
            Request reqAQI = new Request.Builder().url(urlAQI).build();
            Response resAQI = client.newCall(reqAQI).execute(); // chạy đồng bộ

            int aqiVal = 0;
            if (resAQI.isSuccessful() && resAQI.body() != null) {
                JSONObject jsonAQI = new JSONObject(resAQI.body().string());
                aqiVal = jsonAQI.getJSONArray("list").getJSONObject(0).getJSONObject("main").getInt("aqi");
            }

            // xu li thong bao

            // mo app se thong bao
            if (isAppLaunch) {
                String aqiStatus = getAQIStatus(aqiVal);
                String title = "Thời tiết tại " + cityName;
                String content = "";
                if (aqiVal<3){
                    content = "Nhiệt độ: " + temp + "°C. " + desc.substring(0,1).toUpperCase()+desc.substring(1) + ". AQI: " + aqiVal + " (" + aqiStatus + "). Môi trường trong lành bạn có thể yên tâm ra ngoài hoạt động!";
                } else if (aqiVal == 3) {
                    content = "Nhiệt độ: " + temp + "°C. " + desc.substring(0,1).toUpperCase()+desc.substring(1) + ". AQI: " + aqiVal + " (" + aqiStatus + "). Ô nhiễm không khí nên chú ý đeo khẩu trang khi đi ra ngoài nhé!";
                }else {
                    content = "Nhiệt độ: " + temp + "°C. " + desc.substring(0,1).toUpperCase()+desc.substring(1) + ". AQI: " + aqiVal + " (" + aqiStatus + "). Ô nhiễm nghiêm trọng không nên ra ngoài để đảm bảo sức khỏe của bản thân!!!!) ";
                }
                showNotification(title, content, 100);
            }
            // báo khi aqi > 3 xấu
            else {
                if (aqiVal > 3) {
                    String title = "⚠️ Cảnh báo không khí xấu!";
                    String content = "AQI tại " + cityName + " đang ở mức " + aqiVal + ". Hạn chế ra ngoài!!!";
                    showNotification(title, content, 200);
                }
            }

            return Result.success();

        } catch (Exception e) {
            e.printStackTrace();
            return Result.retry();
        }
    }

    // Hàm phụ trợ lấy tên trạng thái AQI
    private String getAQIStatus(int aqi) {
        switch (aqi) {
            case 1: return "Tốt";
            case 2: return "Trung bình";
            case 3: return "Kém";
            case 4: return "Xấu";
            case 5: return "Rất xấu";
            default: return "--";
        }
    }

    //gui thong bao
    private void showNotification(String title, String message, int notifyId) {
        Context context = getApplicationContext();
        String CHANNEL_ID = "weather_aqi_channel";
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Cảnh báo thời tiết & AQI", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(context, ManHinhChinhLog.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Nhớ đổi icon của bạn
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message)) // Để hiện text dài nếu cần
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            manager.notify(notifyId, builder.build());
        }
    }
}