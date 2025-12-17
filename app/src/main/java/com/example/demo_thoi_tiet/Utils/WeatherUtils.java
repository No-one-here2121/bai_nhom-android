package com.example.demo_thoi_tiet.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import com.example.demo_thoi_tiet.Adapter.ForecastAdapter;

public class WeatherUtils {
    public static final String API_KEY = "";
    public static final String BASE_URL = "https://api.openweathermap.org/data/2.5";
    //public static final String CACHE_NAME_WIDGET = "WidgetLocationCache";
    public static final String CACHE_NAME_HOME = "HomeLocationCache";
    //public static final String CACHE_NAME_DETAIL = "DetailLocationCache";

    private static OkHttpClient httpClient;

    public static synchronized OkHttpClient getHttpClient() {
        if (httpClient == null) {
            //chi tao 1 lan
            httpClient = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)//qua 15s khong ket noi -> loi
                    .readTimeout(15, TimeUnit.SECONDS)//qua 15s khong tai xong du lieu -> loi
                    .build();
        }
        return httpClient;
    }

    public static String formatTime(long timestamp, long timezoneOffset) {
        Date date = new Date((timestamp + timezoneOffset) * 1000L);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(date);
    }

    public static String getStringFromPrefs(Context context, String cacheName, String key, String defaultValue) {
        SharedPreferences prefs = context.getSharedPreferences(cacheName, Context.MODE_PRIVATE);
        return prefs.getString(key, defaultValue);
    }

    public static void saveStringToPrefs(Context context, String cacheName, String key, String value) {
        SharedPreferences prefs = context.getSharedPreferences(cacheName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, value);
        editor.apply();
    }

    //ham ket qua thoi tiet
    public interface WeatherCallback {
        void onSuccess(JSONObject result);
        void onFailure(Exception e);
    }

    //lay thong tin thoi tiet tu toa do
    public static void fetchCurrentWeatherByCoords(double lat, double lon, @NonNull WeatherCallback callback) {
        String url = BASE_URL + "/weather?lat=" + lat + "&lon=" + lon + "&appid=" + API_KEY + "&units=metric&lang=vi";

        Request request = new Request.Builder().url(url).build();

        getHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onFailure(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseData = response.body().string();//chuyen thanh JSONobject roi dua ve ham ket qua
                        JSONObject json = new JSONObject(responseData);
                        new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(json));
                    } catch (Exception e) {
                        new Handler(Looper.getMainLooper()).post(() -> callback.onFailure(e));
                    }
                } else {
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onFailure(new IOException("Lỗi API: " + response.code()))
                    );
                }
            }
        });
    }

    //ham ket qua du bao thoi tiet
    public interface ForecastCallback {
        void onSuccess(List<ForecastAdapter.ForecastItem> forecastList); // Bây giờ List đã được nhận diện
        void onFailure(Exception e);
    }

    //ham ket qua cho aqi
    public interface AQICallback {
        void onSuccess(int aqi);
        void onFailure(Exception e);
    }

    //ham du bao
    public static void fetchForecast(double lat, double lon, @NonNull ForecastCallback callback) {
        String url = BASE_URL + "/forecast?lat=" + lat + "&lon=" + lon + "&appid=" + API_KEY + "&units=metric&lang=vi";
        Request request = new Request.Builder().url(url).build();

        getHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onFailure(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        //phan tich mang
                        JSONObject data = new JSONObject(response.body().string());
                        JSONArray list = data.getJSONArray("list");//co 40 phan tu do day la du bao moi 3 gio trong 5 ngay

                        List<ForecastAdapter.ForecastItem> resultList = new ArrayList<>();

                        //loc
                        for (int i = 0; i < list.length(); i++) {
                            JSONObject item = list.getJSONObject(i);

                            //lay chuoi thoi gian
                            String dtTxt = item.getString("dt_txt");

                            //lay o 12h trua
                            if (dtTxt.contains("12:00")) {
                                long dt = item.getLong("dt") * 1000L;//chuyen thoi gian sang mili giay
                                String dayName = new SimpleDateFormat("EEE, dd/MM", new Locale("vi", "VN")).format(new Date(dt));//dinh dang thu ngay
                                int temp = (int) Math.round(item.getJSONObject("main").getDouble("temp"));//lay nhiet do
                                String iconCode = item.getJSONArray("weather").getJSONObject(0).getString("icon");//lay icon
                                resultList.add(new ForecastAdapter.ForecastItem(dayName, temp + "°C", iconCode));//dua vao danh sach ket qua
                            }
                        }

                        //tra ve ket qua
                        new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(resultList));

                    } catch (Exception e) {
                        new Handler(Looper.getMainLooper()).post(() -> callback.onFailure(e));
                    }
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onFailure(new IOException("Lỗi API: " + response.code())));
                }
            }
        });
    }

    //ham lay aqi
    public static void fetchAQI(double lat, double lon, @NonNull AQICallback callback) {
        String url = "https://api.openweathermap.org/data/2.5/air_pollution?lat=" + lat + "&lon=" + lon + "&appid=" + API_KEY;
        Request request = new Request.Builder().url(url).build();

        getHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onFailure(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject data = new JSONObject(response.body().string());
                        int aqi = data.getJSONArray("list").getJSONObject(0).getJSONObject("main").getInt("aqi");

                        //tra ve ham ket qua
                        new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(aqi));

                    } catch (Exception e) {
                        new Handler(Looper.getMainLooper()).post(() -> callback.onFailure(e));
                    }
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onFailure(new IOException("Lỗi API AQI")));
                }
            }
        });
    }
}