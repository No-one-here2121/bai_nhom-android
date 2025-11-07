package com.example.demo_thoi_tiet.Model;

import android.Manifest;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.WorkManager;

import com.example.demo_thoi_tiet.R;

import org.osmdroid.config.Configuration;
import org.osmdroid.views.MapView;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.views.overlay.TilesOverlay;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.MapTileProviderBasic;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;

import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {
    private static final String API_KEY = "";
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5";
    private static final double DEFAULT_LAT = 40.7128;
    private static final double DEFAULT_LNG = -74.0060;
    private static final double DEFAULT_ZOOM = 10.0;

    private MapView map;
    private Marker locationMarker;
    private OkHttpClient client = new OkHttpClient();
    private TextView currentWeatherText, forecastText, locationText;
    private EditText locationGetUser;
    private Button locationGet,locationAddLv;
    public static ArrayList<Info_thoi_tiet> LvViTri = new ArrayList<>();
    private String latestCity = "", latestTemp = "", latestFeels = "", latestHumidity = "",
            latestDesc = "", latestWind = "", latestVis = "", latestCloud = "";

    public static void fetchWeatherAndSaveForWidget(Context context) {
        android.location.LocationManager locationManager = (android.location.LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        Location location = null;
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            location = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER);
            if (location == null) {
                location = locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER);
            }
        }
        if (location == null) return;

        double lat = location.getLatitude();
        double lon = location.getLongitude();

        OkHttpClient client = new OkHttpClient();
        String url = BASE_URL + "/weather?lat=" + lat + "&lon=" + lon + "&appid=" + API_KEY + "&units=metric";
        Request request = new Request.Builder().url(url).build();

        try {
            Response response = client.newCall(request).execute();
            JSONObject data = new JSONObject(response.body().string());
            String icon = "🌡️"; // You can extract or just skip unless you want the emoji
            int temp = (int) Math.round(data.getJSONObject("main").getDouble("temp"));
            int feelsLike = (int) Math.round(data.getJSONObject("main").getDouble("feels_like"));
            int humidity = data.getJSONObject("main").getInt("humidity");
            int pressure = data.getJSONObject("main").getInt("pressure");
            String description = data.getJSONArray("weather").getJSONObject(0).getString("description");
            String cityName = data.optString("name", "--");

            SharedPreferences prefs = context.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("location", cityName);
            editor.putInt("temperature", temp);
            editor.putInt("feel", feelsLike);
            editor.putInt("humidity", humidity);
            editor.putInt("pressure", pressure);
            editor.putString("weatherDescription", description);
            editor.putFloat("rainPercent", 0);
            editor.putString("weatherIcon", icon);
            editor.apply();
        } catch (Exception e) {
            // Optionally log
        }
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_main);

        androidx.appcompat.widget.Toolbar myToolbar = findViewById(R.id.myToolbar);
        setSupportActionBar(myToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Demo thời tiết");
        }

        LvViTri = new ArrayList<Info_thoi_tiet>();

        PeriodicWorkRequest weatherRequest =
                new PeriodicWorkRequest.Builder(Update_thong_tin_widget.class, 1, TimeUnit.HOURS)
                        .build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("weather_update", ExistingPeriodicWorkPolicy.REPLACE, weatherRequest);

        PeriodicWorkRequest weatherListRequest =
                new PeriodicWorkRequest.Builder(Update_thong_tin_listview.class, 1, TimeUnit.HOURS)
                        .build();

        WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork("weather_list_update", androidx.work.ExistingPeriodicWorkPolicy.REPLACE, weatherListRequest);



        currentWeatherText = findViewById(R.id.currentWeather);
        forecastText = findViewById(R.id.forecast);
        locationText = findViewById(R.id.locationName);

        locationGetUser = findViewById(R.id.location_get_user);
        locationGet = findViewById(R.id.location_get);
        locationAddLv = findViewById(R.id.location_add_list);

        locationAddLv.setOnClickListener(v -> {
            String name = locationText.getText().toString().trim();
            if (name.equals("Now") || name.isEmpty()) {
                Toast.makeText(MainActivity.this, "Vui lòng nhập hoặc tìm vị trí để thêm", Toast.LENGTH_SHORT).show();
                return;
            }
            for (Info_thoi_tiet info : Lv_thoi_tiet.weatherList) {
                if (info.city.equalsIgnoreCase(name)) {
                    Toast.makeText(MainActivity.this, "Vị trí đã có trong danh sách hoặc không tồn tại", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            Info_thoi_tiet info = new Info_thoi_tiet(
                    latestCity, latestTemp, latestFeels, latestHumidity, latestDesc, latestWind, latestVis, latestCloud
            );

            // Add to static list for your table page:
            Lv_thoi_tiet.weatherList.add(info);

            Toast.makeText(MainActivity.this, name + " đã thêm vào danh sách!", Toast.LENGTH_SHORT).show();
        });




        map = findViewById(R.id.map);
        map.setMultiTouchControls(true);

        GeoPoint defaultPoint = new GeoPoint(DEFAULT_LAT, DEFAULT_LNG);
        map.getController().setZoom(DEFAULT_ZOOM);
        map.getController().setCenter(defaultPoint);
        addOrMoveMarker(defaultPoint, "New York City");

        addWeatherOverlay("precipitation_new");
        requestLocationPermissionAndFetch();

        MapEventsReceiver mReceiver = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                return false;
            }
            @Override
            public boolean longPressHelper(GeoPoint p) {
                updateLocation(p.getLatitude(), p.getLongitude());
                return true;
            }
        };
        MapEventsOverlay eventsOverlay = new MapEventsOverlay(mReceiver);
        map.getOverlays().add(eventsOverlay);

        fetchCurrentWeather(DEFAULT_LAT, DEFAULT_LNG);
        fetchForecast(DEFAULT_LAT, DEFAULT_LNG);

        // SEARCH BUTTON HANDLER
        locationGet.setOnClickListener(view -> {
            String query = locationGetUser.getText().toString().trim();
            if (query.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập vị trí!", Toast.LENGTH_SHORT).show();
                return;
            }
            searchLocation(query);
        });
    }

    private void addOrMoveMarker(GeoPoint point, String name) {
        if (locationMarker != null) {
            locationMarker.setPosition(point);
            locationMarker.setTitle(name);
        } else {
            locationMarker = new Marker(map);
            locationMarker.setPosition(point);
            locationMarker.setTitle(name);
            locationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            map.getOverlays().add(locationMarker);
        }
        locationText.setText(name);
    }

    private void updateLocation(double lat, double lon) {
        GeoPoint newPoint = new GeoPoint(lat, lon);
        addOrMoveMarker(newPoint, "Now");
        map.getController().setCenter(newPoint);
        fetchCurrentWeather(lat, lon);
        fetchForecast(lat, lon);
    }

    private void requestLocationPermissionAndFetch() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            fetchUserLocation();
        }
    }

    private void fetchUserLocation() {
        android.location.LocationManager locationManager = (android.location.LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] {
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    },
                    1
            );
            return;
        }
        Location location = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER);
        if (location == null) {
            location = locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER);
        }
        if (location != null) {
            updateLocation(location.getLatitude(), location.getLongitude());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchUserLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_option_menu,menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menuList) {
            Intent intentLV = new Intent(MainActivity.this, Lv_thoi_tiet.class);
            startActivity(intentLV);
        }
        return super.onOptionsItemSelected(item);
    }

    private void addWeatherOverlay(String layerName) {
        XYTileSource weatherTileSource = new XYTileSource(
                layerName,
                0, 18, 256, ".png",
                new String[]{"https://tile.openweathermap.org/map/" + layerName + "/"},
                "OpenWeatherMap") {
            @Override
            public String getTileURLString(long pMapTileIndex) {
                int z = org.osmdroid.util.MapTileIndex.getZoom(pMapTileIndex);
                int x = org.osmdroid.util.MapTileIndex.getX(pMapTileIndex);
                int y = org.osmdroid.util.MapTileIndex.getY(pMapTileIndex);
                return String.format("https://tile.openweathermap.org/map/%s/%d/%d/%d.png?appid=%s",
                        layerName, z, x, y, API_KEY);
            }
        };

        MapTileProviderBasic tileProvider = new MapTileProviderBasic(getApplicationContext());
        tileProvider.setTileSource(weatherTileSource);
        TilesOverlay weatherOverlay = new TilesOverlay(tileProvider, this);
        map.getOverlays().add(weatherOverlay);
    }

    // SEARCH LOCATION FUNCTION
    private void searchLocation(String query) {
        try {
            String encoded = URLEncoder.encode(query, "UTF-8");
            String url = "https://api.openweathermap.org/geo/1.0/direct?q=" + encoded + "&limit=1&appid=" + API_KEY;
            Request request = new Request.Builder().url(url).build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Không tìm thấy vị trí!", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        JSONArray data = new JSONArray(response.body().string());
                        if (data.length() > 0) {
                            JSONObject loc = data.getJSONObject(0);
                            double lat = loc.getDouble("lat");
                            double lon = loc.getDouble("lon");
                            String name = loc.getString("name");
                            runOnUiThread(() -> {
                                addOrMoveMarker(new GeoPoint(lat, lon), name);
                                map.getController().setCenter(new GeoPoint(lat, lon));
                                fetchCurrentWeather(lat, lon);
                                fetchForecast(lat, lon);
                            });
                        } else {
                            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Vị trí không tồn tại!", Toast.LENGTH_SHORT).show());
                        }
                    } catch (Exception e) {
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Lỗi tìm kiếm vị trí!", Toast.LENGTH_SHORT).show());
                    }
                }
            });
        } catch (Exception ex) {
            Toast.makeText(this, "Lỗi khi encode từ khóa tìm kiếm.", Toast.LENGTH_SHORT).show();
        }
    }

    // Weather icon mapping
    public String getWeatherIcon(String code) {
        switch (code) {
            case "01d": return "☀️";
            case "01n": return "🌙";
            case "02d": case "02n": case "03d": case "03n":
            case "04d": case "04n": return "☁️";
            case "09d": case "09n": return "🌧️";
            case "10d": case "10n": return "🌦️";
            case "11d": case "11n": return "⛈️";
            case "13d": case "13n": return "❄️";
            case "50d": case "50n": return "🌫️";
            default: return "🌡️";
        }
    }

    private void fetchCurrentWeather(double lat, double lon) {
        String url = BASE_URL + "/weather?lat=" + lat + "&lon=" + lon + "&appid=" + API_KEY + "&units=metric";
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) { runOnUiThread(() -> currentWeatherText.setText("Unable to load weather data.")); }
            @Override public void onResponse(Call call, Response response) throws IOException {
                try {
                    JSONObject data = new JSONObject(response.body().string());
                    String icon = getWeatherIcon(data.getJSONArray("weather").getJSONObject(0).getString("icon"));
                    int temp = (int) Math.round(data.getJSONObject("main").getDouble("temp"));
                    int feelsLike = (int) Math.round(data.getJSONObject("main").getDouble("feels_like"));
                    int humidity = data.getJSONObject("main").getInt("humidity");
                    int pressure = data.getJSONObject("main").getInt("pressure");
                    double windSpeed = data.getJSONObject("wind").getDouble("speed") * 3.6;
                    String description = data.getJSONArray("weather").getJSONObject(0).getString("description");
                    double visibility = data.optDouble("visibility", 0) / 1000;
                    int cloudiness = data.getJSONObject("clouds").getInt("all");

                    latestCity = data.optString("location", locationText.getText().toString());
                    latestTemp = temp + "°C";
                    latestFeels = feelsLike + "°C";
                    latestHumidity = humidity + "%";
                    latestDesc = description;
                    latestWind = ((int) windSpeed) + " km/h";
                    latestVis = String.format("%.1f", visibility) + " km";
                    latestCloud = cloudiness + "%";

                    String weatherInfo = icon + " " + temp + "°C\n" +
                            "Feels like: " + feelsLike + "°C\n" +
                            "Humidity: " + humidity + "%\n" +
                            "Wind: " + (int) windSpeed + " km/h\n" +
                            "Pressure: " + pressure + " hPa\n" +
                            "Visibility: " + String.format("%.1f", visibility) + " km\n" +
                            "Clouds: " + cloudiness + "%\n" +
                            description;

                    // Hien ket qua tu lan ti kiem cuoi cung:
                    //SharedPreferences prefs = getSharedPreferences("weather_prefs", MODE_PRIVATE);
                    //SharedPreferences.Editor editor = prefs.edit();
                    //editor.putString("location", locationText.getText().toString());
                    //editor.putInt("temperature", temp);
                    //editor.putInt("feel",feelsLike);
                    //editor.putInt("humidity", humidity);
                    //editor.putInt("pressure", pressure);
                    //editor.putString("weatherDescription", description);
                    //editor.putFloat("rainPercent", 0);
                    //editor.putString("weatherIcon", icon);
                    //editor.apply();

                    //Intent intent = new Intent(MainActivity.this, Widget_thoi_tiet.class);
                    //intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                    //int[] ids = AppWidgetManager.getInstance(MainActivity.this)
                            //.getAppWidgetIds(new ComponentName(MainActivity.this, Widget_thoi_tiet.class));
                    //intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                    //sendBroadcast(intent);

                    runOnUiThread(() -> currentWeatherText.setText(weatherInfo));
                } catch (Exception e) {
                    runOnUiThread(() -> currentWeatherText.setText("Error processing weather data."));
                }
            }
        });
    }

    private void fetchForecast(double lat, double lon) {
        String url = BASE_URL + "/forecast?lat=" + lat + "&lon=" + lon + "&appid=" + API_KEY + "&units=metric";
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) { runOnUiThread(() -> forecastText.setText("Unable to load forecast data.")); }
            @Override public void onResponse(Call call, Response response) throws IOException {
                try {
                    JSONObject data = new JSONObject(response.body().string());
                    JSONArray list = data.getJSONArray("list");
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < list.length(); i++) {
                        JSONObject item = list.getJSONObject(i);
                        long dt = item.getLong("dt") * 1000L;
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("EEE HH:mm");
                        String dateTime = sdf.format(new java.util.Date(dt));
                        String icon = getWeatherIcon(item.getJSONArray("weather").getJSONObject(0).getString("icon"));
                        int temp = (int) Math.round(item.getJSONObject("main").getDouble("temp"));
                        String desc = item.getJSONArray("weather").getJSONObject(0).getString("description");
                        sb.append(dateTime).append(": ").append(icon).append(" ").append(temp).append("°C ").append(desc).append("\n");
                    }
                    runOnUiThread(() -> forecastText.setText(sb.toString()));
                } catch (Exception e) {
                    runOnUiThread(() -> forecastText.setText("Error processing forecast data."));
                }
            }
        });
    }
}
