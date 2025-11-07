package com.example.demo_thoi_tiet.Model;

import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.demo_thoi_tiet.Adapter.Adapter_thoi_tiet;
import com.example.demo_thoi_tiet.R;

import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Lv_thoi_tiet extends AppCompatActivity {
    ListView lvWeather;
    Adapter_thoi_tiet weatherAdapter;
    public static ArrayList<Info_thoi_tiet> weatherList = new ArrayList<>();
    Button backToMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lv_thoi_tiet); // e.g. R.layout.activity_lv_thoi_tiet

        lvWeather = findViewById(R.id.lv_danh_sach); // this id must match your ListView in XML
        backToMenu = findViewById(R.id.back_to_menu);

        weatherAdapter = new Adapter_thoi_tiet(this, weatherList);
        lvWeather.setAdapter(weatherAdapter);

        backToMenu.setOnClickListener(v -> finish());
    }

    public static void refreshWeatherList(Context context) {
        ArrayList<Info_thoi_tiet> updatedList = new ArrayList<>();

        for (Info_thoi_tiet oldInfo : weatherList) {
            // Example: use city name and sync HTTP (OkHttp) as in your widget update
            try {
                String API_KEY = "your_api_key"; // Or import from MainActivity
                String url = "https://api.openweathermap.org/data/2.5/weather?q=" +
                        URLEncoder.encode(oldInfo.city, "UTF-8") +
                        "&appid=" + API_KEY + "&units=metric";
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();
                JSONObject data = new JSONObject(response.body().string());

                int temp = (int) Math.round(data.getJSONObject("main").getDouble("temp"));
                int feelsLike = (int) Math.round(data.getJSONObject("main").getDouble("feels_like"));
                int humidity = data.getJSONObject("main").getInt("humidity");
                String desc = data.getJSONArray("weather").getJSONObject(0).getString("description");
                double windSpeed = data.getJSONObject("wind").getDouble("speed") * 3.6;
                double visibility = data.optDouble("visibility", 0) / 1000;
                int cloudiness = data.getJSONObject("clouds").getInt("all");

                Info_thoi_tiet newInfo = new Info_thoi_tiet(
                        oldInfo.city,
                        "Nhiệt độ: " + temp + "°C",
                        "Cảm giác: " + feelsLike + "°C",
                        "Độ ẩm: " + humidity + "%",
                        desc,
                        "Gió: " + ((int) windSpeed) + " km/h",
                        "Tầm nhìn: " + String.format("%.1f", visibility) + " km",
                        "Mây: " + cloudiness + "%"
                );
                updatedList.add(newInfo);

            } catch (Exception e) {
                // Optionally add previous info if update fails:
                updatedList.add(oldInfo);
            }
        }

        weatherList.clear();
        weatherList.addAll(updatedList);
    }


}
