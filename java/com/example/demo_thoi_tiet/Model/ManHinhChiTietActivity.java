//nhu ManHinhChinhLog nhung cho Listview
package com.example.demo_thoi_tiet.Model;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.demo_thoi_tiet.Adapter.ForecastAdapter;
import com.example.demo_thoi_tiet.R;
import com.example.demo_thoi_tiet.Utils.WeatherUtils;
import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.TilesOverlay;
import org.osmdroid.tileprovider.MapTileProviderBasic;
import org.osmdroid.tileprovider.tilesource.XYTileSource;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ManHinhChiTietActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private final OkHttpClient client = WeatherUtils.getHttpClient();
    private Call currentCall;

    // --- BIẾN GIAO DIỆN ---
    private ConstraintLayout mainLayout;
    private android.widget.FrameLayout frameLayout;
    private TextView locationText, conditionText, temperatureText, minText, maxText, dayText, dateText, todayText;
    private TextView textConditionValue, textHumidityValue, textWindValue, textSunriseValue, textSunsetValue, textSeaValue, aqiLayoutText;
    private ImageView sunIcon;
    private TextView searchView;
    private ImageButton imageButton2;

    // Map & List
    private MapView miniMap;
    private Marker locationMarker;
    private RecyclerView rvDailyForecast;

    private ImageButton btnMap, btnList, btnProfile;

    // Helpers
    private ProgressDialog loadingDialog;
    private TextToSpeech tts;
    private Handler timeHandler;
    private Runnable timeRunnable;
    private long currentTimezoneOffset = 0;

    // Data Adapter
    private ArrayList<ForecastAdapter.ForecastItem> forecastList = new ArrayList<>();
    private ForecastAdapter forecastAdapter;

    // Dữ liệu toàn cục
    private double currentLat = 0;
    private double currentLon = 0;
    private int aqiRate = 0;
    private FirebaseAuth mAuth;

    private android.widget.ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().setCacheMapTileCount((short)12);
        Configuration.getInstance().setTileFileSystemCacheMaxBytes(100L * 1024 * 1024);
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.man_hinh_chi_tiet);

        progressBar = findViewById(R.id.progressBar);
        mAuth = FirebaseAuth.getInstance();
        timeHandler = new Handler(Looper.getMainLooper());

        checkPermissions();

        initViews();
        setupMiniMap();
        setupTTS();
        setupNavigation();

        loadDataFromCache();

        //lay ten vi tri tu trang listview
        String receivedCityName = getIntent().getStringExtra("CITY_NAME");
        if (receivedCityName != null && !receivedCityName.isEmpty() && !receivedCityName.equals("CurrentLocation")) {//neu lay duoc ten vi tri
            //cho viec loading
            if(progressBar != null) progressBar.setVisibility(View.VISIBLE);
            if(locationText != null) locationText.setText(receivedCityName);
            //goi api voi ten vi tri
            getCoordinatesAndFetchWeather(receivedCityName);
        } else {
            //khong co ten -> vi tri hien tai
            //du kien ban dau la tu tao 1 muc trong lv la vi tri cua toi nhung da co man hinh chinh
            checkPermissionAndFetchLocation();
        }
    }

    // --- WORKER & PERMISSIONS ---x

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 999);
            }
        }
    }

    private void checkPermissionAndFetchLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            fetchUserLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchUserLocation();
            } else {
                Toast.makeText(this, "Cần quyền vị trí", Toast.LENGTH_SHORT).show();
                getCoordinatesAndFetchWeather("Hanoi");
            }
        }
    }

    // --- CACHE SYSTEM ---

    private void cacheUIData(String temp, String city, String desc, String iconCode) {
        SharedPreferences prefs = getSharedPreferences("UICacheLog", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("cached_temp", temp);
        editor.putString("cached_city", city);
        editor.putString("cached_desc", desc);
        editor.putString("cached_icon", iconCode);
        editor.apply();
    }

    private void loadDataFromCache() {
        SharedPreferences prefs = getSharedPreferences("UICacheLog", MODE_PRIVATE);
        String temp = prefs.getString("cached_temp", "--");
        String city = prefs.getString("cached_city", "Loading...");
        String desc = prefs.getString("cached_desc", "");

        if (locationText != null) locationText.setText(city);
        if (temperatureText != null) temperatureText.setText(temp);
        if (conditionText != null) conditionText.setText(desc.toUpperCase());
    }

    // --- UI INIT ---

    private void initViews() {
        try {
            btnProfile = findViewById(R.id.btnProfile);
            frameLayout = findViewById(R.id.frameLayout);

            mainLayout = findViewById(R.id.main);
            locationText = findViewById(R.id.locationText);
            conditionText = findViewById(R.id.conditionText);
            todayText = findViewById(R.id.todayText);
            temperatureText = findViewById(R.id.temperatureText);
            minText = findViewById(R.id.minText);
            maxText = findViewById(R.id.maxText);
            dayText = findViewById(R.id.dayText);
            dateText = findViewById(R.id.dateText);
            sunIcon = findViewById(R.id.sunIcon);
            aqiLayoutText = findViewById(R.id.aqiLayoutText);
            imageButton2 = findViewById(R.id.imageButton2);
            searchView = findViewById(R.id.searchView);

            textConditionValue = findViewById(R.id.textConditionValue);
            textHumidityValue = findViewById(R.id.textHumidityValue);
            textWindValue = findViewById(R.id.textWindValue);
            textSunriseValue = findViewById(R.id.textSunriseValue);
            textSunsetValue = findViewById(R.id.textSunsetValue);
            textSeaValue = findViewById(R.id.textSeaValue);

            miniMap = findViewById(R.id.miniMap);
            rvDailyForecast = findViewById(R.id.rvDailyForecast);
            btnMap = findViewById(R.id.btnMap);
            btnList = findViewById(R.id.btnList);

            if (rvDailyForecast != null) {
                rvDailyForecast.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
                forecastAdapter = new ForecastAdapter(forecastList);
                rvDailyForecast.setAdapter(forecastAdapter);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void setupMiniMap() {
        if (miniMap != null) {
            miniMap.setTileSource(TileSourceFactory.MAPNIK);
            miniMap.setMultiTouchControls(true);
            miniMap.getController().setZoom(10.0);
            addWeatherOverlay("precipitation_new");
        }
    }

    private void setupNavigation() {
        if (btnMap != null) {
            btnMap.setOnClickListener(v -> {
                Intent intent = new Intent(ManHinhChiTietActivity.this, MapActivity.class);
                String city = (locationText != null) ? locationText.getText().toString() : "";
                intent.putExtra("CITY_NAME", city);
                intent.putExtra("LAT", currentLat);
                intent.putExtra("LON", currentLon);
                startActivity(intent);
            });
        }
        if (btnList != null) {
            btnList.setOnClickListener(v -> startActivity(new Intent(ManHinhChiTietActivity.this, Lv_thoi_tiet.class)));
        }

        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> startActivity(new Intent(ManHinhChiTietActivity.this, ProfileActivity.class)));
        }

        if (imageButton2 != null) {
            imageButton2.setOnClickListener(v -> speakWeatherInfo());
        }

        View btnMoLich = findViewById(R.id.btn_mo_lich);
        if (btnMoLich != null) {
            btnMoLich.setOnClickListener(v -> startActivity(new Intent(this, Lich_Activity.class)));
        }
    }


    private void fetchUserLocation() {
        if(progressBar != null) progressBar.setVisibility(View.VISIBLE);
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location == null) location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            if (location != null) {
                fetchWeatherDetails(location.getLatitude(), location.getLongitude());
                fetchForecast(location.getLatitude(), location.getLongitude());
                fetchAQI(location.getLatitude(), location.getLongitude());
            } else {
                getCoordinatesAndFetchWeather("Hanoi");
            }
        } catch (SecurityException e) { e.printStackTrace(); }
    }

    private void getCoordinatesAndFetchWeather(String cityName) {
        String nameNonCity = cityName.replace("City", "");
        try {
            String encodedCity = URLEncoder.encode(nameNonCity, "UTF-8");
            String url = "https://api.openweathermap.org/geo/1.0/direct?q=" + encodedCity + "&limit=1&appid=" + WeatherUtils.API_KEY;
            Request request = new Request.Builder().url(url).build();

            client.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    closeLoading(null);
                }
                @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            JSONArray data = new JSONArray(response.body().string());
                            if (data.length() > 0) {
                                JSONObject loc = data.getJSONObject(0);
                                double lat = loc.getDouble("lat");
                                double lon = loc.getDouble("lon");
                                if (!isFinishing()) {
                                    fetchWeatherDetails(lat, lon);
                                    fetchForecast(lat, lon);
                                    fetchAQI(lat, lon);
                                }
                            } else {
                                closeLoading("Không tìm thấy địa điểm");
                            }
                        } catch (Exception e) { e.printStackTrace(); }
                    }
                }
            });
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void fetchWeatherDetails(double lat, double lon) {
        WeatherUtils.fetchCurrentWeatherByCoords(lat, lon, new WeatherUtils.WeatherCallback() {
            @Override
            public void onSuccess(JSONObject data) {
                try {
                    JSONObject main = data.getJSONObject("main");
                    JSONObject wind = data.getJSONObject("wind");
                    JSONObject sys = data.getJSONObject("sys");
                    JSONObject weatherObj = data.getJSONArray("weather").getJSONObject(0);

                    // --- SỬA 1: Lấy ID thời tiết (Số) ---
                    int weatherId = weatherObj.getInt("id");

                    String cityName = data.getString("name");
                    int temp = (int) Math.round(main.getDouble("temp"));
                    double minTemp = main.getDouble("temp_min");
                    double maxTemp = main.getDouble("temp_max");
                    int humidity = main.getInt("humidity");
                    int pressure = main.getInt("pressure");
                    double windSpeed = wind.getDouble("speed");
                    String conditionDesc = weatherObj.getString("description");
                    String iconCode = weatherObj.getString("icon");
                    long timezoneOffset = data.getLong("timezone");
                    long sunrise = sys.getLong("sunrise");
                    long sunset = sys.getLong("sunset");

                    if (!isFinishing()) {
                        startRealTimeClock(timezoneOffset);
                        addOrMoveMarker(new GeoPoint(lat, lon), cityName);
                        cacheUIData(temp + "°C", cityName, conditionDesc, iconCode);


                        if(locationText != null) locationText.setText(cityName);
                        if(conditionText != null) conditionText.setText(conditionDesc.toUpperCase());
                        if(temperatureText != null) temperatureText.setText(temp + "℃");
                        if(minText != null) minText.setText("Min: " + String.format("%.1f", minTemp) + "℃");
                        if(maxText != null) maxText.setText("Max: " + String.format("%.1f", maxTemp) + "℃");
                        if(textConditionValue != null) textConditionValue.setText(conditionDesc);


                        //  Truyền ID
                        updateInterfaceByWeather(weatherId);

                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Exception e) {
                if(loadingDialog != null) loadingDialog.dismiss();
                Toast.makeText(ManHinhChiTietActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchForecast(double lat, double lon) {
        WeatherUtils.fetchForecast(lat, lon, new WeatherUtils.ForecastCallback() {
            @Override
            public void onSuccess(List<ForecastAdapter.ForecastItem> list) {
                if (!isFinishing()) {
                    forecastList.clear();
                    forecastList.addAll(list);
                    if (forecastAdapter != null) {
                        forecastAdapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onFailure(Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void fetchAQI(double lat, double lon) {
        WeatherUtils.fetchAQI(lat, lon, new WeatherUtils.AQICallback() {
            String aqiStatus = "";
            @Override
            public void onSuccess(int aqi) {
                if (!isFinishing()) {
                    aqiRate = aqi;
                    switch (aqi){
                        case 1:
                            aqiStatus = "Tốt";
                            break;
                        case 2:
                            aqiStatus = "Trung bình";
                            break;
                        case 3:
                            aqiStatus = "Kém";
                            break;
                        case 4:
                            aqiStatus = "Xấu";
                            break;
                        case 5:
                            aqiStatus = "Rất xấu";
                            break;
                    }
                    if (aqiLayoutText != null) {
                        aqiLayoutText.setText("AQI Rate: " + aqi + " (" + aqiStatus + ")");
                    }
                }
            }

            @Override
            public void onFailure(Exception e) {
                e.printStackTrace();
            }
        });
    }

    // --- UTILS ---

    private void addOrMoveMarker(GeoPoint point, String name) {
        if (miniMap == null) return;
        miniMap.getController().animateTo(point);
        miniMap.getController().setZoom(13.0);
        if (locationMarker != null) {
            locationMarker.setPosition(point);
            locationMarker.setTitle(name);
        } else {
            locationMarker = new Marker(miniMap);
            locationMarker.setPosition(point);
            locationMarker.setTitle(name);
            locationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            miniMap.getOverlays().add(locationMarker);
        }
        miniMap.invalidate();
    }

    private void addWeatherOverlay(String layerName) {
        if (miniMap == null) return;
        XYTileSource weatherTileSource = new XYTileSource(
                layerName, 0, 18, 256, ".png",
                new String[]{"https://tile.openweathermap.org/map/" + layerName + "/"},
                "OpenWeatherMap") {
            @Override
            public String getTileURLString(long pMapTileIndex) {
                int z = org.osmdroid.util.MapTileIndex.getZoom(pMapTileIndex);
                int x = org.osmdroid.util.MapTileIndex.getX(pMapTileIndex);
                int y = org.osmdroid.util.MapTileIndex.getY(pMapTileIndex);
                return String.format("https://tile.openweathermap.org/map/%s/%d/%d/%d.png?appid=%s",
                        layerName, z, x, y, WeatherUtils.API_KEY);
            }
        };
        MapTileProviderBasic tileProvider = new MapTileProviderBasic(getApplicationContext());
        tileProvider.setTileSource(weatherTileSource);
        TilesOverlay weatherOverlay = new TilesOverlay(tileProvider, this);
        miniMap.getOverlays().add(weatherOverlay);
    }

    private void updateInterfaceByWeather(int weatherId) {
        if (mainLayout == null || frameLayout == null) return;
        android.graphics.drawable.GradientDrawable bgShape = (android.graphics.drawable.GradientDrawable) frameLayout.getBackground();
        int textColor;


        if (weatherId >= 600 && weatherId <= 622) {
            //SNOW

            mainLayout.setBackgroundResource(R.drawable.snow_background);
            if(sunIcon != null) sunIcon.setImageResource(R.drawable.snow);


            textColor = android.graphics.Color.parseColor("#E4F1F9");
            if (bgShape != null) bgShape.setColor(android.graphics.Color.parseColor("#99B8CC"));

        } else if (weatherId >= 200 && weatherId <= 232) {
            // DÔNG BÃO
            mainLayout.setBackgroundResource(R.drawable.rain_background);
            if(sunIcon != null) sunIcon.setImageResource(R.drawable.rain);
            if (bgShape != null) bgShape.setColor(android.graphics.Color.parseColor("#40666A"));
            textColor = android.graphics.Color.parseColor("#C9E8E0");

        } else if ((weatherId >= 300 && weatherId <= 321) || (weatherId >= 500 && weatherId <= 531)) {
            // MƯA
            mainLayout.setBackgroundResource(R.drawable.rain_background);
            if(sunIcon != null) sunIcon.setImageResource(R.drawable.rain);
            if (bgShape != null) bgShape.setColor(android.graphics.Color.parseColor("#40666A"));
            textColor = android.graphics.Color.parseColor("#C9E8E0");

        } else if (weatherId == 800) {
            // TRỜI QUANG
            mainLayout.setBackgroundResource(R.drawable.sunny_background);
            if(sunIcon != null) sunIcon.setImageResource(R.drawable.sunny);
            if (bgShape != null) bgShape.setColor(android.graphics.Color.parseColor("#FAE2BD"));
            textColor = android.graphics.Color.parseColor("#EFAA82");

        } else {
            // CÁC TRƯỜNG HỢP CÒN LẠI (Mây, Sương mù...) -> Coi là Mây
            mainLayout.setBackgroundResource(R.drawable.cloud_background);
            if(sunIcon != null) sunIcon.setImageResource(R.drawable.cloud_black);
            if (bgShape != null) bgShape.setColor(android.graphics.Color.parseColor("#91B4C6"));
            textColor = android.graphics.Color.parseColor("#CAD7DF");
        }

        setTopLayoutTextColor(textColor);
    }

    private void setTopLayoutTextColor(int color) {
        if (locationText != null) locationText.setTextColor(color);
        if (conditionText != null) conditionText.setTextColor(color);
        if (temperatureText != null) temperatureText.setTextColor(color);
        if (minText != null) minText.setTextColor(color);
        if (maxText != null) maxText.setTextColor(color);
        if (dayText != null) dayText.setTextColor(color);
        if (dateText != null) dateText.setTextColor(color);
        if (aqiLayoutText != null) aqiLayoutText.setTextColor(color);
        if (todayText != null) todayText.setTextColor(color);
    }

    private void speakWeatherInfo() {
        if (locationText == null || locationText.getText().toString().isEmpty()) return;
        String conditionLower = conditionText.getText().toString().toLowerCase();
        String thoitiet = "bình thường";
        if (conditionLower.contains("rain")) thoitiet = "có mưa, nhớ mang dù";
        else if (conditionLower.contains("clear")) thoitiet = "quang đãng, đẹp trời";
        else if (conditionLower.contains("cloud")) thoitiet = "nhiều mây";

        String speechText = "Vị trí " + locationText.getText() +
                ". Nhiệt độ " + temperatureText.getText().toString().replace("℃", "") + " độ." +
                " Trời " + thoitiet + ". Chỉ số không khí là " + aqiRate;
        tts.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    private void startRealTimeClock(long timezoneOffsetInSeconds) {
        this.currentTimezoneOffset = timezoneOffsetInSeconds;
        if (timeRunnable != null) timeHandler.removeCallbacks(timeRunnable);
        timeRunnable = new Runnable() {
            @Override
            public void run() {
                Date now = new Date();
                java.util.TimeZone locationTimeZone = new java.util.SimpleTimeZone((int) (currentTimezoneOffset * 1000), "Local");
                SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.ENGLISH);
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy HH:mm:ss", Locale.ENGLISH);
                dayFormat.setTimeZone(locationTimeZone);
                dateFormat.setTimeZone(locationTimeZone);
                if (dayText != null) dayText.setText(dayFormat.format(now));
                if (dateText != null) dateText.setText(dateFormat.format(now));
                timeHandler.postDelayed(this, 1000);
            }
        };
        timeHandler.post(timeRunnable);
    }

    private void closeLoading(String message) {
        runOnUiThread(() -> {
            if(progressBar != null) progressBar.setVisibility(View.GONE);
            if(message != null) Toast.makeText(ManHinhChiTietActivity.this, message, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (miniMap != null) miniMap.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (miniMap != null) miniMap.onPause();
    }

    @Override
    protected void onDestroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
        if (currentCall != null) currentCall.cancel();
        if (timeHandler != null && timeRunnable != null) timeHandler.removeCallbacks(timeRunnable);
    }

    private void setupTTS() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS){
                int result = tts.setLanguage(new Locale("vi", "VN"));
                tts.setSpeechRate(0.85f);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED){
                    tts.setLanguage(Locale.US);
                }
            }
        });
    }
}