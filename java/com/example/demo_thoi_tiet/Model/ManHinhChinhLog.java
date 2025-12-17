package com.example.demo_thoi_tiet.Model;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
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
//Chay ngam (Sau a thoi gian thi cap nhat lai thong tin)
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.demo_thoi_tiet.Class.ForecastWorker;
import com.example.demo_thoi_tiet.Adapter.ForecastAdapter;
import com.example.demo_thoi_tiet.Model.Lv_thoi_tiet;
import com.example.demo_thoi_tiet.Model.MapActivity;
import com.example.demo_thoi_tiet.Model.ProfileActivity;
import com.example.demo_thoi_tiet.R;
import com.example.demo_thoi_tiet.Utils.WeatherUtils;
//Cho viec luu du lieu nguoi dung (tai khoan,vi tri,...)
import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONArray;
import org.json.JSONObject;
//Map cho phan radar (OpenStressMap)
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.MapTileProviderBasic;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.TilesOverlay;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

//Dung ket noi mang (gui yeu cau mang,goi api)
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ManHinhChinhLog extends AppCompatActivity {
    //Ma cho viec yeu cau vi tri
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    //Bien mang
    private final OkHttpClient client = WeatherUtils.getHttpClient(); //Lay phuong thuc tu trang chung
    private Call currentCall;

    // --- BIẾN GIAO DIỆN ---
    private ConstraintLayout mainLayout;
    private TextView locationText, conditionText, temperatureText, minText, maxText, dayText, dateText;
    private TextView textConditionValue, textHumidityValue, textWindValue, textSunriseValue, textSunsetValue, textSeaValue, aqiLayoutText;
    private ImageView sunIcon;
    private TextView searchView;
    private ImageButton imageButton2;

    private MapView miniMap;
    private Marker locationMarker;
    private RecyclerView rvDailyForecast;
    private ImageButton btnMap, btnList, btnProfile;

    //Doc thong tin du lieu thoi tiet ra tieng
    private TextToSpeech tts;

    //Cap nhat thoi gian
    private Handler timeHandler;
    private Runnable timeRunnable;
    private long currentTimezoneOffset = 0;

    private ArrayList<ForecastAdapter.ForecastItem> forecastList = new ArrayList<>();
    //De hien danh sach du bao thoi tiet
    private ForecastAdapter forecastAdapter;

    //Vi do nguoi dung
    private double currentLat = 0;

    //Kinh do nguoi dung
    private double currentLon = 0;

    //Bien aqi (chat luong khong khi)
    private int aqiRate = 0;

    //Cho viec dang nhap
    private FirebaseAuth mAuth;

    //Cho viec luu du lieu tren may (sqlite)
    private WeatherDatabaseHelper localDb;

    private android.widget.ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Set map config (OpenStressMap)
        Configuration.getInstance().setCacheMapTileCount((short)12);
        Configuration.getInstance().setTileFileSystemCacheMaxBytes(100L * 1024 * 1024);
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_man_hinh_chinh_log);

        //Cac bien khoi dau (dang nhap,database,thoi gian)
        mAuth = FirebaseAuth.getInstance();
        localDb = new WeatherDatabaseHelper(this);
        timeHandler = new Handler(Looper.getMainLooper()); //Cap nhat thoi gian
        progressBar = findViewById(R.id.progressBar); //progressBar cho viec loading

        checkPermissions();//Xin quyen thong bao
        setupWorkers();//Setup phan chay ngam
        initViews();//Noi cac bien trong file voi giao dien
        setupMiniMap();//Cho cai map radar nho o man hinh chinh
        setupTTS();//Doc thong tin thoi tiet thanh tieng
        setupNavigation();//Thanh dieu huong o duoi cung
        loadDataFromCache();//Lay du lieu tu cache
        checkPermissionAndFetchLocation();//Xin quyen vi tri + lay vi tri vua xin
    }

    private void cacheUIData(String temp, String city, String desc, String iconCode) {
        WeatherUtils.saveStringToPrefs(this, WeatherUtils.CACHE_NAME_HOME, "cached_temp", temp);
        WeatherUtils.saveStringToPrefs(this, WeatherUtils.CACHE_NAME_HOME, "cached_city", city);
        WeatherUtils.saveStringToPrefs(this, WeatherUtils.CACHE_NAME_HOME, "cached_desc", desc);
        WeatherUtils.saveStringToPrefs(this, WeatherUtils.CACHE_NAME_HOME, "cached_icon", iconCode);
    }

    private void loadDataFromCache() {
        String temp = WeatherUtils.getStringFromPrefs(this, WeatherUtils.CACHE_NAME_HOME, "cached_temp", "--");
        String city = WeatherUtils.getStringFromPrefs(this, WeatherUtils.CACHE_NAME_HOME, "cached_city", "Đang định vị...");
        String desc = WeatherUtils.getStringFromPrefs(this, WeatherUtils.CACHE_NAME_HOME, "cached_desc", "");

        if (locationText != null) locationText.setText(city);
        if (temperatureText != null) temperatureText.setText(temp);
        if (conditionText != null) conditionText.setText(desc.toUpperCase());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (miniMap != null) miniMap.onResume();
        checkPermissionAndFetchLocation();
    }

    private android.widget.FrameLayout frameLayout;
    private TextView todayText;


    private void initViews() {
        try {
            frameLayout = findViewById(R.id.frameLayout);
            todayText = findViewById(R.id.todayText);
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
            btnProfile = findViewById(R.id.btnProfile);

            if (rvDailyForecast != null) {
                rvDailyForecast.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
                forecastAdapter = new ForecastAdapter(forecastList);
                rvDailyForecast.setAdapter(forecastAdapter);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void setupNavigation() {
        if (btnMap != null) {
            btnMap.setOnClickListener(v -> {
                Intent intent = new Intent(ManHinhChinhLog.this, MapActivity.class);
                intent.putExtra("CITY_NAME", locationText.getText().toString());
                intent.putExtra("LAT", currentLat);
                intent.putExtra("LON", currentLon);
                startActivity(intent);
            });
        }
        if (btnList != null) {
            btnList.setOnClickListener(v -> startActivity(new Intent(ManHinhChinhLog.this, Lv_thoi_tiet.class)));
        }
        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> startActivity(new Intent(ManHinhChinhLog.this, ProfileActivity.class)));
        }
        if (imageButton2 != null) {
            imageButton2.setOnClickListener(v -> speakWeatherInfo());
        }
        View btnMoLich = findViewById(R.id.btn_mo_lich);
        if (btnMoLich != null) {
            btnMoLich.setOnClickListener(v -> {
                Intent intent = new Intent(this, Lich_Activity.class);

                // Lấy thông tin thời tiết hiện tại từ TextView
                String thoiTietHienTai = "";
                if (conditionText != null) {
                    thoiTietHienTai = conditionText.getText().toString();
                }
                // Gửi dữ liệu sang Lich_Activity
                intent.putExtra("KEY_THOI_TIET", thoiTietHienTai);

                String nhietDoHienTai = "--";
                if (temperatureText != null) {
                    nhietDoHienTai = temperatureText.getText().toString();
                }
                intent.putExtra("KEY_NHIET_DO", nhietDoHienTai);

                startActivity(intent);
            });
        }
    }

    //Xin quyen vi tri
    private void checkPermissionAndFetchLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) { //Da cap quyen chua?
            //Neu chua cap quyen thi xin quyen
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            //Neu da cap quyen thi lay vi tri hien tai
            fetchUserLocation();
        }
    }


    //Lay vi tri hien tai
    private void fetchUserLocation() {
        //Goi cai quan ly vi tri cua android
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);//Lay vi tri luu trong cache cua gps
            if (location == null) location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);//Neu khong co thi lay tu mang

            //Neu co vi tri thi lay thong tin thoi tiet
            if (location != null) {
                fetchWeatherDetails(location.getLatitude(), location.getLongitude());//Lay thong tin thoi tiet
                fetchForecast(location.getLatitude(), location.getLongitude());//Lay du bao thoi tiet
                fetchAQI(location.getLatitude(), location.getLongitude());//Lay chat luong khong khi
            } else {
                Toast.makeText(this, "Không lấy được vị trí", Toast.LENGTH_SHORT).show();
            }
        } catch (SecurityException e) { e.printStackTrace(); }
    }

    //Lay toa do roi lay thong tin thoi tiet
    private void getCoordinatesAndFetchWeather(String cityName) {
        try {
            //Xu ly chuoi thanh chuoi duoc ma hoa
            //Do url khong su cho phep dau cach -> ma hoa lai dang co the gui di (ha+noi,ha%noi)
            String encodedCity = URLEncoder.encode(cityName, "UTF-8");

            //Chuyen tu ten vi tri thanh toa do
            //"limit = 1" de chi lay ket qua dung nhat (tranh lay noi trung ten)
            String url = "https://api.openweathermap.org/geo/1.0/direct?q=" + encodedCity + "&limit=1&appid=" + WeatherUtils.API_KEY;

            //Gui yeu cau
            Request request = new Request.Builder().url(url).build();

            //Bao rang khong can doi ket qua
            //Giup khong lag neu mang cham
            client.newCall(request).enqueue(new Callback() {
                //Neu mang loi
                @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    //Bao loi
                    closeLoading("Lỗi kết nối!");
                }

                //Neu tra ve ket qua
                @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    //Xem co lay duoc ket qua khong? co du lieu khong?
                    //Ma 200 = thanh cong
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            //Api tra ve ket qua duoi dang Array (mang) cac dia diem tim duoc
                            JSONArray data = new JSONArray(response.body().string());
                            if (data.length() > 0) {
                                //Lay dia diem dau tien trong danh sach (so 0)
                                JSONObject loc = data.getJSONObject(0);

                                //Lay kinh do,vi do
                                double lat = loc.getDouble("lat");
                                double lon = loc.getDouble("lon");

                                //Da co vi tri thi lay thong tin thoi tiet
                                if (!isFinishing()) {
                                    fetchWeatherDetails(lat, lon);
                                    fetchForecast(lat, lon);
                                    fetchAQI(lat, lon);
                                }
                            } else {
                                //Neu api tra ve mang rong thi thong bao
                                closeLoading("Không tìm thấy địa điểm");
                            }
                        } catch (Exception e) { e.printStackTrace(); }
                    }
                }
            });
        } catch (Exception e) { e.printStackTrace(); }
    }

    //lay va xu ky thong tin thoi tiet
    private void fetchWeatherDetails(double lat, double lon) {
        //Kinh do,vi do dau vao
        currentLat = lat;
        currentLon = lon;

        //Goi ham lay thong tin thoi tiet
        WeatherUtils.fetchCurrentWeatherByCoords(lat, lon, new WeatherUtils.WeatherCallback() {
            //Neu thanh cong
            @Override
            public void onSuccess(JSONObject data) {
                try {
                    //lay du lieu de hien thi
                    JSONObject main = data.getJSONObject("main");
                    JSONObject wind = data.getJSONObject("wind");
                    JSONObject sys = data.getJSONObject("sys");
                    JSONObject weatherObj = data.getJSONArray("weather").getJSONObject(0);

                    //Ma thoi tiet
                    //tien cho viec thay doi du lieu dua vao thoi tiet
                    int weatherId = weatherObj.getInt("id");

                    String cityName = data.getString("name");//ten vi tri
                    int temp = (int) Math.round(main.getDouble("temp"));//nhiet do
                    double minTemp = main.getDouble("temp_min");//nhiet do thap nhat
                    double maxTemp = main.getDouble("temp_max");//nhiet do cao nhat
                    int humidity = main.getInt("humidity");//do am
                    int pressure = main.getInt("pressure");//ap suat khong khi
                    double windSpeed = wind.getDouble("speed");//toc do gio

                    int feelsLike = (int) Math.round(main.getDouble("feels_like"));//ban cam thay nhiet do bao nhieu?

                    String conditionDesc = weatherObj.getString("description");//mo ta
                    String mainCondition = weatherObj.getString("main");
                    String iconCode = weatherObj.getString("icon");//icon biet thi trang thai thoi tiet
                    long timezoneOffset = data.getLong("timezone");//mui gio

                    long sunrise = sys.getLong("sunrise");//mat troi rang
                    long sunset = sys.getLong("sunset");//hoang hon

                    //cap nhat thoi gian
                    startRealTimeClock(timezoneOffset);

                    //di chuyen vi tri tren map radar tren man hinh
                    addOrMoveMarker(new GeoPoint(lat, lon), cityName);

                    //dua du lieu vao widget
                    android.content.SharedPreferences sharedPreferences = getSharedPreferences("WidgetData", MODE_PRIVATE);
                    android.content.SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("city", cityName);
                    editor.putString("temp", temp + "");
                    editor.putString("status", conditionDesc);
                    editor.putString("humidity", "Độ ẩm: " + humidity + "%");
                    editor.putString("feels_like", "Cảm giác: " + feelsLike + "°C");
                    editor.putString("icon", iconCode);
                    editor.putInt("weatherId", weatherId);
                    editor.apply();

                    //neu co du lieu thi hien thi
                    if(locationText != null) locationText.setText(cityName);
                    if(conditionText != null) conditionText.setText(conditionDesc.toUpperCase());
                    if(temperatureText != null) temperatureText.setText(temp + "℃");
                    if(minText != null) minText.setText("Min: " + String.format("%.1f", minTemp) + "℃");
                    if(maxText != null) maxText.setText("Max: " + String.format("%.1f", maxTemp) + "℃");


                    //cap nhat giao dien dua theo thoi tiet
                    updateInterfaceByWeather(weatherId);

                    Intent intent = new Intent(ManHinhChinhLog.this, com.example.demo_thoi_tiet.Model.Widget_thoi_tiet.class);
                    intent.setAction(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                    int[] ids = android.appwidget.AppWidgetManager.getInstance(getApplication())
                            .getAppWidgetIds(new android.content.ComponentName(getApplication(), com.example.demo_thoi_tiet.Model.Widget_thoi_tiet.class));
                    intent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                    sendBroadcast(intent);

                    //Dua du lieu vao cache
                    //khi tu trang khac ve trang chinh se khong can phai thuc hien viec goi / khi mat mang van se hien gi day
                    //tu cap nhat sau 1 gio (do ForecastWorker thuc hien) ngam
                    cacheUIData(temp + "°C", cityName, conditionDesc, iconCode);

                    //van la co du lieu thi hien thi
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

                    //ket thuc man loading
                    closeLoading(null);

                } catch (Exception e) {
                    e.printStackTrace();
                    closeLoading(null);
                    Toast.makeText(ManHinhChinhLog.this, "Lỗi xử lý dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            //neu that bai
            @Override
            public void onFailure(Exception e) {
                e.printStackTrace();
                closeLoading(null);
                Toast.makeText(ManHinhChinhLog.this, "Lỗi kết nối mạng.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    //lay du bao thoi tiet
    private void fetchForecast(double lat, double lon) {
        //goi ham thuc hien lay du bao thoi tiet
        WeatherUtils.fetchForecast(lat, lon, new WeatherUtils.ForecastCallback() {
            @Override
            public void onSuccess(List<ForecastAdapter.ForecastItem> list) {
                //man hinh chinh dang mo khong? (tranh crash)
                if (!isFinishing()) {
                    forecastList.clear();//xoa du lieu cu
                    forecastList.addAll(list);//them du lieu moi
                    if (forecastAdapter != null) {
                        forecastAdapter.notifyDataSetChanged();//cap nhat giao dien
                    }
                }
            }

            @Override
            public void onFailure(Exception e) {
                e.printStackTrace();
            }
        });
    }

    //lay chat luong khong khi
    private void fetchAQI(double lat, double lon) {
        //goi ham thuc hien lay chat luong khong khi
        WeatherUtils.fetchAQI(lat, lon, new WeatherUtils.AQICallback() {
            //tao bien trong ham de luu du lieu
            String aqiStatus = "";
            @Override
            public void onSuccess(int aqi) {
                if (!isFinishing()) {//man hinh chinh dang mo khong? (tranh crash)
                    //nho xoa 1 bien di,dang bi trung
                    aqiRate = aqi;
                    aqiRate = aqi;//dua vao muc do se cho mo ta khac nhau
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
                        //ghep lai du lieu
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

    //cac ham xu ly vi tri radar

    //them marker tren radar
    private void addOrMoveMarker(GeoPoint point, String name) {
        if (miniMap == null) return;//dua phan map den vung moi
        //animation chuyen vi tri (luot di)
        miniMap.getController().animateTo(point);
        //zoom
        miniMap.getController().setZoom(13.0);

        //ghim chua?
        if (locationMarker != null) {//neu roi
            locationMarker.setPosition(point);//mang no sang vi tri moi
            locationMarker.setTitle(name);
        } else {//neu chua
            //tao ghim moi
            locationMarker = new Marker(miniMap);
            locationMarker.setPosition(point);
            locationMarker.setTitle(name);

            //de cai mui nhon cua ghim chi vao toa do
            //center,bottom:toa do o giua theo chieu ngang va day
            locationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

            //them ghim vao
            miniMap.getOverlays().add(locationMarker);
        }
        //cap nhat lai map
        miniMap.invalidate();
    }

    //set up map tren man hinh
    private void setupMiniMap() {
        if (miniMap != null) {
            //set loai ban do
            //MAPNIK: ban do thuong
            //co the chuyen sang cac loai khac,tu tao custom
            miniMap.setTileSource(TileSourceFactory.MAPNIK);

            //khoa khong cho di chuyen map
            //tat zoom + dieu khien
            miniMap.setMultiTouchControls(false);

            //tat nut dieu khien zoom
            miniMap.setBuiltInZoomControls(false);

            //khong cho cham
            miniMap.setOnTouchListener((v, event) -> true);

            //set zoom mac dinh
            miniMap.getController().setZoom(10.0);

            //ten layer radar
            //o truong hop nay la cuong do mua
            addWeatherOverlay("precipitation_new");
        }
    }

    //them layer radar
    //hoat dong nhu google map neu bam "vi tri hien tai" nhung co them layer o giua ghim va map
    private void addWeatherOverlay(String layerName) {
        if (miniMap == null) return;
        //khai bao noi tai layer
        XYTileSource weatherTileSource = new XYTileSource(
                layerName,//layer gi?
                0, 18, 256,//zoom min,max,kich co anh (layer)
                ".png",//png do co background trong suot
                new String[]{"https://tile.openweathermap.org/map/" + layerName + "/"},
                "OpenWeatherMap") {
            @Override
            public String getTileURLString(long pMapTileIndex) {//lay url cua tile
                //toa do trong ban do
                int z = org.osmdroid.util.MapTileIndex.getZoom(pMapTileIndex);
                int x = org.osmdroid.util.MapTileIndex.getX(pMapTileIndex);
                int y = org.osmdroid.util.MapTileIndex.getY(pMapTileIndex);
                //toa thanh url de lay thong tin
                return String.format("https://tile.openweathermap.org/map/%s/%d/%d/%d.png?appid=%s",
                        layerName, z, x, y, WeatherUtils.API_KEY);
            }
        };
        MapTileProviderBasic tileProvider = new MapTileProviderBasic(getApplicationContext());
        tileProvider.setTileSource(weatherTileSource);
        TilesOverlay weatherOverlay = new TilesOverlay(tileProvider, this);

        //dua cai layer day len map
        miniMap.getOverlays().add(weatherOverlay);
    }

//Set trang thai giao dien dua theo thoi tiet

    private void updateInterfaceByWeather(int weatherId) {
        if (mainLayout == null) return;

        //chuan bi de thuc hien
        android.graphics.drawable.GradientDrawable bgShape = null;//tao bien
        if (frameLayout != null && frameLayout.getBackground() instanceof android.graphics.drawable.GradientDrawable) {//check xem cai frame co chua va cai background co dang .png
            bgShape = (android.graphics.drawable.GradientDrawable) frameLayout.getBackground();//tao ra mot bien de bien thanh background
        }

        int textColor;

        //dua tren id thoi tiet cua api
        if (weatherId >= 200 && weatherId <= 232) {
            //Bao
            mainLayout.setBackgroundResource(R.drawable.rain_background);//doi backgroud
            if(sunIcon != null) sunIcon.setImageResource(R.drawable.rain);//doi icon
            if (bgShape != null) bgShape.setColor(android.graphics.Color.parseColor("#40666A"));//doi mau sac phan tren cung
            textColor = android.graphics.Color.parseColor("#C9E8E0");//mau chu

        } else if ((weatherId >= 300 && weatherId <= 321) || (weatherId >= 500 && weatherId <= 531)) {
            //Mua
            mainLayout.setBackgroundResource(R.drawable.rain_background);
            if(sunIcon != null) sunIcon.setImageResource(R.drawable.rain);
            if (bgShape != null) bgShape.setColor(android.graphics.Color.parseColor("#40666A"));
            textColor = android.graphics.Color.parseColor("#C9E8E0");

        } else if (weatherId >= 600 && weatherId <= 622) {
            //Tuyet
            mainLayout.setBackgroundResource(R.drawable.snow_background);
            if(sunIcon != null) sunIcon.setImageResource(R.drawable.snow);
            // Set Shape Color to #99B8CC
            if (bgShape != null) bgShape.setColor(android.graphics.Color.parseColor("#99B8CC"));
            textColor = android.graphics.Color.parseColor("#E4F1F9");

        } else if (weatherId >= 701 && weatherId <= 781) {
            //Suong mu,bui,...
            mainLayout.setBackgroundResource(R.drawable.cloud_background);
            if(sunIcon != null) sunIcon.setImageResource(R.drawable.cloud_black);
            if (bgShape != null) bgShape.setColor(android.graphics.Color.parseColor("#91B4C6"));
            textColor = android.graphics.Color.parseColor("#CAD7DF");

        } else if (weatherId == 800) {
            //Troi quang
            mainLayout.setBackgroundResource(R.drawable.sunny_background);
            if(sunIcon != null) sunIcon.setImageResource(R.drawable.sunny);
            if (bgShape != null) bgShape.setColor(android.graphics.Color.parseColor("#FAE2BD"));
            textColor = android.graphics.Color.parseColor("#EFAA82");

        } else if (weatherId >= 801 && weatherId <= 804) {
            //Co may
            mainLayout.setBackgroundResource(R.drawable.cloud_background);
            if(sunIcon != null) sunIcon.setImageResource(R.drawable.cloud_black);
            if (bgShape != null) bgShape.setColor(android.graphics.Color.parseColor("#91B4C6"));
            textColor = android.graphics.Color.parseColor("#CAD7DF");

        } else {
            //Mac dinh
            mainLayout.setBackgroundResource(R.drawable.sunny_background);
            if(sunIcon != null) sunIcon.setImageResource(R.drawable.sunny);
            if (bgShape != null) bgShape.setColor(android.graphics.Color.parseColor("#FAE2BD"));
            textColor = android.graphics.Color.parseColor("#EFAA82");
        }

        setTopLayoutTextColor(textColor);
    }

    //hien thong tin cho phan tren
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

    //setup text-to-spech
    private void setupTTS() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS){
                int result = tts.setLanguage(new Locale("vi", "VN"));//set tieng viet
                tts.setSpeechRate(0.85f);//set toc do doc
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED){//neu khong thay ngon ngu hoac ngon ngu khong ho tro
                    tts.setLanguage(Locale.US);//set mac dinh la tieng anh
                }
            }
        });
    }

    //doc thong tin thoi tiet
    private void speakWeatherInfo() {
        if (locationText == null || locationText.getText().toString().isEmpty()) return;//neu chua co vi tri -> chua load -> khong noi

        //xu ly + dich don gian
        String conditionLower = conditionText.getText().toString().toLowerCase();
        String thoitiet = "bình thường";
        if (conditionLower.contains("rain")) thoitiet = "có mưa";
        else if (conditionLower.contains("clear")) thoitiet = "quang đãng";

        //dang noi thong tin
        String speechText = "Vị trí " + locationText.getText() +
                ". Nhiệt độ " + temperatureText.getText().toString().replace("℃", "") + " độ." +
                " Trời " + thoitiet + ". Chỉ số không khí là " + aqiRate;

        //noi
        //TextToSpeech.QUEUE_FLUSH:neu dang noi ->bo qua cai dang noi ma noi cai moi
        tts.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    //cho phan hien gio
    private void startRealTimeClock(long timezoneOffsetInSeconds) {
        this.currentTimezoneOffset = timezoneOffsetInSeconds;//lay mui gio,tinh xem dang chenh gio quoc te bao giay?
        if (timeRunnable != null) timeHandler.removeCallbacks(timeRunnable);//reset dong ho dang chay
        timeRunnable = new Runnable() {
            @Override
            public void run() {
                Date now = new Date();//lay gio cua may

                //set mui gio dua tren do lech giay vua lay o tren
                java.util.TimeZone locationTimeZone = new java.util.SimpleTimeZone((int) (currentTimezoneOffset * 1000), "Local");

                //dinh dang thoi gian
                SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.ENGLISH);
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy HH:mm:ss", Locale.ENGLISH);
                dayFormat.setTimeZone(locationTimeZone);
                dateFormat.setTimeZone(locationTimeZone);

                //hien thi thoi gian len giao dien
                if (dayText != null) dayText.setText(dayFormat.format(now));
                if (dateText != null) dateText.setText(dateFormat.format(now));

                //update theo giay (giup phan giay thay doi lien tuc)
                timeHandler.postDelayed(this, 1000);
            }
        };
        timeHandler.post(timeRunnable);
    }

    //check xem da co quyen thong bao chua
    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 999);
            }
        }
    }

    //setup cac cong viec
    private void setupWorkers() {
        //lam ngay khi vua vao
        androidx.work.Data inputData = new androidx.work.Data.Builder()
                .putBoolean(ForecastWorker.KEY_IS_APP_LAUNCH, true)
                .build();
        OneTimeWorkRequest appLaunchRequest = new OneTimeWorkRequest.Builder(ForecastWorker.class)
                .setInputData(inputData)
                .build();
        WorkManager.getInstance(this).enqueue(appLaunchRequest);

        //lam dinh ky
        PeriodicWorkRequest backgroundRequest = new PeriodicWorkRequest.Builder(ForecastWorker.class, 1, TimeUnit.HOURS).build();//cap nhat thoi tiet hien tai sau moi a gio
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("aqi_monitor_work", ExistingPeriodicWorkPolicy.KEEP, backgroundRequest);//neu da co thi bo qua
    }

    //ket qua xin quyen
    //set ma quyen vi tri la 1001
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {//neu dung ma
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {//neu co quyen
                //lay vi tri
                fetchUserLocation();
            } else {//neu khong co quyen
                //hien thong bao tren man hinh
                Toast.makeText(this, "Cần quyền vị trí", Toast.LENGTH_SHORT).show();
                //lay vi tri mac dinh
                getCoordinatesAndFetchWeather("Hanoi");
            }
        }
    }

    //ket thuc viec loading
    private void closeLoading(String message) {
        runOnUiThread(() -> {
            if(progressBar != null) progressBar.setVisibility(View.GONE);
            if(message != null) Toast.makeText(ManHinhChinhLog.this, message, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (miniMap != null) miniMap.onPause();
    }

    //neu chuyen sang man khac
    @Override
    protected void onDestroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
        if (currentCall != null) currentCall.cancel();
        if (timeHandler != null && timeRunnable != null) timeHandler.removeCallbacks(timeRunnable);
    }
}