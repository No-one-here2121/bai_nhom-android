//nhu trang ManHinhChinhLog nhung khong co listview
package com.example.demo_thoi_tiet.Model;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
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
import com.example.demo_thoi_tiet.Utils.WeatherUtils;
import com.example.demo_thoi_tiet.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.MapTileProviderBasic;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.TilesOverlay;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ManHinhChinhNonLog extends AppCompatActivity {
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private final OkHttpClient client = WeatherUtils.getHttpClient();

    private ConstraintLayout mainLayout;
    private TextView locationText, conditionText, temperatureText, minText, maxText, dayText, dateText;
    private TextView textConditionValue, textHumidityValue, textWindValue, textSunriseValue, textSunsetValue, textSeaValue, aqiLayoutText;
    private ImageView sunIcon;
    private ImageButton imageButton2;

    // Map & List
    private MapView miniMap;
    private Marker locationMarker;
    private RecyclerView rvDailyForecast;

    // Nav Buttons
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

    private int aqiRate = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Cấu hình OSMDroid
        Configuration.getInstance().setUserAgentValue(getPackageName());

        // DÙNG CHUNG GIAO DIỆN VỚI MÀN HÌNH CHI TIẾT
        setContentView(R.layout.man_hinh_chi_tiet);

        timeHandler = new Handler(Looper.getMainLooper());

        // 2. Setup TTS
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS){
                tts.setLanguage(new Locale("vi", "VN"));
            }
        });

        // 3. Khởi tạo Views
        initViews();
        setupMiniMap();
        updateCurrentDate();

        // 4. CẤU HÌNH NAVIGATOR CHO NGƯỜI CHƯA ĐĂNG NHẬP
        setupNavigationForNonLog();

        // 5. Logic lấy dữ liệu
        loadingDialog = new ProgressDialog(this);
        loadingDialog.setMessage("Đang định vị...");
        loadingDialog.setCancelable(false);
        loadingDialog.show();

        // Luôn luôn lấy vị trí hiện tại (vì chưa đăng nhập nên không có list)
        checkPermissionAndFetchLocation();
    }

    // --- CẤU HÌNH GIAO DIỆN ---

    private void initViews() {
        mainLayout = findViewById(R.id.main);
        locationText = findViewById(R.id.locationText);
        conditionText = findViewById(R.id.conditionText);
        temperatureText = findViewById(R.id.temperatureText);
        minText = findViewById(R.id.minText);
        maxText = findViewById(R.id.maxText);
        dayText = findViewById(R.id.dayText);
        dateText = findViewById(R.id.dateText);
        sunIcon = findViewById(R.id.sunIcon);
        aqiLayoutText = findViewById(R.id.aqiLayoutText);
        imageButton2 = findViewById(R.id.imageButton2);

        textConditionValue = findViewById(R.id.textConditionValue);
        textHumidityValue = findViewById(R.id.textHumidityValue);
        textWindValue = findViewById(R.id.textWindValue);
        textSunriseValue = findViewById(R.id.textSunriseValue);
        textSunsetValue = findViewById(R.id.textSunsetValue);
        textSeaValue = findViewById(R.id.textSeaValue);

        miniMap = findViewById(R.id.miniMap);
        rvDailyForecast = findViewById(R.id.rvDailyForecast);

        // Ánh xạ các nút điều hướng
        btnMap = findViewById(R.id.btnMap);
        btnList = findViewById(R.id.btnList);
        btnProfile = findViewById(R.id.btnProfile);

        if (rvDailyForecast != null) {
            rvDailyForecast.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            forecastAdapter = new ForecastAdapter(forecastList);
            rvDailyForecast.setAdapter(forecastAdapter);
        }
    }

    private void setupNavigationForNonLog() {
        // 1. Nút Map: Ở lại trang này hoặc reload vị trí hiện tại
        if (btnMap != null) {
            btnMap.setOnClickListener(v -> checkPermissionAndFetchLocation());
        }

        // 2. Nút List: ẨN ĐI (Vì chưa đăng nhập thì không có list)
        if (btnList != null) {
            btnList.setVisibility(View.GONE);
        }

        // 3. Nút Profile: Chuyển sang trang Đăng nhập
        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> {
                // Đảm bảo bạn có Activity After_MoDau
                Intent intent = new Intent(ManHinhChinhNonLog.this, After_MoDau.class);
                startActivity(intent);
            });
        }

        if (imageButton2 != null) {
            imageButton2.setOnClickListener(v -> speakWeatherInfo());
        }

        View btnMoLich = findViewById(R.id.btn_mo_lich);
        if (btnMoLich != null) {
            btnMoLich.setVisibility(View.GONE);
        }
    }

    private void setupMiniMap() {
        if (miniMap != null) {
            miniMap.setTileSource(TileSourceFactory.MAPNIK);
            miniMap.setMultiTouchControls(true);
            miniMap.getController().setZoom(10.0);
            addWeatherOverlay("precipitation_new");
        }
    }

    // --- LOGIC VỊ TRÍ & API ---

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
                Toast.makeText(this, "Cần quyền vị trí để hoạt động", Toast.LENGTH_SHORT).show();
                fetchWeatherDetails(21.0285, 105.8542); // Mặc định Hà Nội
                if(loadingDialog != null) loadingDialog.dismiss();
            }
        }
    }

    private void fetchUserLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location == null) location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            if (location != null) {
                fetchWeatherDetails(location.getLatitude(), location.getLongitude());
                fetchForecast(location.getLatitude(), location.getLongitude());
                fetchAQI(location.getLatitude(), location.getLongitude());
            } else {
                // Không tìm thấy vị trí -> Mặc định Hà Nội
                fetchWeatherDetails(21.0285, 105.8542);
                fetchForecast(21.0285, 105.8542);
                fetchAQI(21.0285, 105.8542);
            }
        } catch (SecurityException e) { e.printStackTrace(); }
    }

    // --- API WEATHER CALLS (Sửa để dùng WeatherUtils) ---

    private void fetchWeatherDetails(double lat, double lon) {
        // Sử dụng WeatherUtils để đảm bảo xử lý JSON đúng và an toàn
        WeatherUtils.fetchCurrentWeatherByCoords(lat, lon, new WeatherUtils.WeatherCallback() {
            @Override
            public void onSuccess(JSONObject data) {
                try {
                    JSONObject main = data.getJSONObject("main");
                    JSONObject wind = data.getJSONObject("wind");
                    JSONObject sys = data.getJSONObject("sys");
                    JSONObject weatherObj = data.getJSONArray("weather").getJSONObject(0);

                    String cityName = data.getString("name");
                    int temp = (int) Math.round(main.getDouble("temp"));
                    double minTemp = main.getDouble("temp_min");
                    double maxTemp = main.getDouble("temp_max");
                    int humidity = main.getInt("humidity");
                    int pressure = main.getInt("pressure");
                    double windSpeed = wind.getDouble("speed");

                    String conditionMain = weatherObj.getString("main");
                    String conditionDesc = weatherObj.getString("description");

                    long sunrise = sys.getLong("sunrise");
                    long sunset = sys.getLong("sunset");
                    long timezoneOffset = data.getLong("timezone");

                    if (!isFinishing()) {
                        // WeatherUtils callback đã chạy trên MainThread, nhưng để chắc chắn:
                        startRealTimeClock(timezoneOffset);
                        addOrMoveMarker(new GeoPoint(lat, lon), cityName);

                        if(locationText != null) locationText.setText(cityName);
                        if(conditionText != null) conditionText.setText(conditionDesc.toUpperCase());
                        if(temperatureText != null) temperatureText.setText(temp + "℃");
                        if(minText != null) minText.setText("Min: " + String.format("%.1f", minTemp) + "℃");
                        if(maxText != null) maxText.setText("Max: " + String.format("%.1f", maxTemp) + "℃");

                        if(textConditionValue != null) textConditionValue.setText(conditionDesc);
                        if(textHumidityValue != null) textHumidityValue.setText(humidity + "%");
                        if(textWindValue != null) textWindValue.setText(windSpeed + " m/s");
                        if(textSeaValue != null) textSeaValue.setText(pressure + " hPa");

                        if(textSunriseValue != null) textSunriseValue.setText(WeatherUtils.formatTime(sunrise, timezoneOffset));
                        if(textSunsetValue != null) textSunsetValue.setText(WeatherUtils.formatTime(sunset, timezoneOffset));

                        updateInterfaceByWeather(conditionMain);
                        if(loadingDialog != null) loadingDialog.dismiss();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if(loadingDialog != null) loadingDialog.dismiss();
                }
            }

            @Override
            public void onFailure(Exception e) {
                if(loadingDialog != null) loadingDialog.dismiss();
                Toast.makeText(ManHinhChinhNonLog.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchForecast(double lat, double lon) {
        WeatherUtils.fetchForecast(lat, lon, new WeatherUtils.ForecastCallback() {
            @Override
            public void onSuccess(List<ForecastAdapter.ForecastItem> list) {
                // Cập nhật UI ở đây
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
                // Toast.makeText(getApplicationContext(), "Lỗi dự báo", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchAQI(double lat, double lon) {
        WeatherUtils.fetchAQI(lat, lon, new WeatherUtils.AQICallback() {
            String aqiStatus = "";
            @Override
            public void onSuccess(int aqi) {
                // Xử lý UI dựa trên giá trị int aqi trả về
                if (!isFinishing()) {
                    aqiRate = aqi; // Cập nhật biến toàn cục nếu cần
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

    // --- HELPER METHODS ---

    private void updateCurrentDate() {
        Date now = new Date();
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.ENGLISH);
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH);
        if (dayText != null) dayText.setText(dayFormat.format(now));
        if (dateText != null) dateText.setText(dateFormat.format(now));
    }

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

    private void updateInterfaceByWeather(String condition) {
        if (mainLayout == null) return;
        int textColor;
        switch (condition) {
            case "Clear":
                mainLayout.setBackgroundResource(R.drawable.sunny_background);
                if(sunIcon != null) sunIcon.setImageResource(R.drawable.sunny);
                textColor = android.graphics.Color.parseColor("#EFAA82");
                break;
            case "Clouds":
                mainLayout.setBackgroundResource(R.drawable.cloud_background);
                if(sunIcon != null) sunIcon.setImageResource(R.drawable.cloud_black);
                textColor = android.graphics.Color.parseColor("#CAD7DF");
                break;
            case "Rain":
            case "Drizzle":
            case "Thunderstorm":
                mainLayout.setBackgroundResource(R.drawable.rain_background);
                if(sunIcon != null) sunIcon.setImageResource(R.drawable.rain);
                textColor = android.graphics.Color.parseColor("#C9E8E0");
                break;
            case "Snow":
                mainLayout.setBackgroundResource(R.drawable.snow_background);
                if(sunIcon != null) sunIcon.setImageResource(R.drawable.snow);
                textColor = android.graphics.Color.parseColor("#E4F1F9");
                break;
            default:
                mainLayout.setBackgroundResource(R.drawable.sunny_background);
                if(sunIcon != null) sunIcon.setImageResource(R.drawable.sunny);
                textColor = android.graphics.Color.parseColor("#EFAA82");
                break;
        }
        setTopLayoutTextColor(textColor);
    }

    private void setTopLayoutTextColor(int color) {
        if(locationText != null) locationText.setTextColor(color);
        if(conditionText != null) conditionText.setTextColor(color);
        if(temperatureText != null) temperatureText.setTextColor(color);
        if(minText != null) minText.setTextColor(color);
        if(maxText != null) maxText.setTextColor(color);
        if(dayText != null) dayText.setTextColor(color);
        if(dateText != null) dateText.setTextColor(color);
        if(aqiLayoutText != null) aqiLayoutText.setTextColor(color);
    }

    private void speakWeatherInfo() {
        if (locationText == null || locationText.getText().toString().isEmpty()) return;
        String conditionLower = conditionText.getText().toString().toLowerCase();
        String thoitiet = "bình thường";
        if (conditionLower.contains("rain")) thoitiet = "có mưa";
        else if (conditionLower.contains("clear")) thoitiet = "quang đãng";

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
        if (timeHandler != null && timeRunnable != null) timeHandler.removeCallbacks(timeRunnable);
    }
}