package com.example.demo_thoi_tiet.Model;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.demo_thoi_tiet.Adapter.Adapter_thoi_tiet;
import com.example.demo_thoi_tiet.Utils.WeatherUtils;
import com.example.demo_thoi_tiet.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Lv_thoi_tiet extends AppCompatActivity {
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5";
    private final OkHttpClient client = new OkHttpClient();

    // Giao diện
    ListView lvWeather;
    EditText edtSearchCityList;
    ImageButton btnMap, btnList, btnProfile;

    // Gợi ý tìm kiếm
    private RecyclerView suggestionsRecycler;
    private SuggestionAdapter suggestionAdapter;
    private Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    //Sort
    private ImageView ivSortAQI;

    // Lịch sử & Database
    private ChipGroup cgHistory;
    private List<String> searchHistoryList;
    private WeatherDatabaseHelper localDb;

    Adapter_thoi_tiet weatherAdapter;
    public ArrayList<Info_thoi_tiet> weatherList = new ArrayList<>();

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    @Override
    protected void onStart() {
        super.onStart();
        // Kiểm tra xem người dùng đã đăng nhập chưa
        if (mAuth.getCurrentUser() != null) {
            loadCitiesFromFirebase();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDataFromSQLite();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lv_thoi_tiet);

        // Khởi tạo Database SQLite
        localDb = new WeatherDatabaseHelper(this);

        Toolbar toolbar = findViewById(R.id.myToolbar);
        setSupportActionBar(toolbar);

        //ÁNH XẠ
        lvWeather = findViewById(R.id.lv_danh_sach);
        edtSearchCityList = findViewById(R.id.edtSearchCityList);
        suggestionsRecycler = findViewById(R.id.recycler_suggestions);
        cgHistory = findViewById(R.id.cgHistory);
        btnMap = findViewById(R.id.btnMap);
        btnList = findViewById(R.id.btnList);
        btnProfile = findViewById(R.id.btnProfile);
        ivSortAQI = findViewById(R.id.ivSortAQI);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        //Setup List Chính
        weatherAdapter = new Adapter_thoi_tiet(this, weatherList);
        lvWeather.setAdapter(weatherAdapter);

        //Lấy dữ liệu từ sqlite để đưa vào màn hình
        loadDataFromSQLite();

        //Setup Lịch sử tìm kiếm
        loadHistoryData();
        for (String keyword : searchHistoryList) {
            addChipToGroup(keyword);
        }

        //Setup Gợi ý tìm kiếm
        if (suggestionsRecycler != null) {
            suggestionsRecycler.setLayoutManager(new LinearLayoutManager(this));
            suggestionAdapter = new SuggestionAdapter(new ArrayList<>(), (lat, lon, name) -> {
                edtSearchCityList.setText("");

                addToFireBase(name);
                suggestionsRecycler.setVisibility(View.GONE);

                //Lưu lịch sử tìm kiếm
                if (!searchHistoryList.contains(name)) {
                    searchHistoryList.add(0, name);
                    saveHistory(searchHistoryList);
                    addChipToGroup(name);
                }

                //Lưu vào SQLite + Gọi hàm hiển thị
                localDb.addLocation(name, lat, lon);
                fetchCurrentWeatherToLV(lat, lon, name);
            });
            suggestionsRecycler.setAdapter(suggestionAdapter);
        }

        //Xử lý nhập liệu tìm kiếm
        edtSearchCityList.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (weatherAdapter != null) weatherAdapter.getFilter().filter(s);
            }
            @Override
            public void afterTextChanged(Editable s) {
                if (searchRunnable != null) debounceHandler.removeCallbacks(searchRunnable);
                if (s.length() >= 2) {
                    searchRunnable = () -> fetchOWMSuggestions(s.toString());
                    debounceHandler.postDelayed(searchRunnable, 500);
                } else {
                    if (suggestionsRecycler != null) suggestionsRecycler.setVisibility(View.GONE);
                }
            }
        });

        //Xử lý Navigator
        if (btnMap != null) btnMap.setOnClickListener(v -> {
            Intent intent = new Intent(Lv_thoi_tiet.this, ManHinhChinhLog.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        if (btnProfile != null) btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(Lv_thoi_tiet.this, ProfileActivity.class);
            startActivity(intent);
        });

        //Bấm vào ListView
        lvWeather.setOnItemClickListener((parent, view, position, id) -> {
            Info_thoi_tiet item = (Info_thoi_tiet) parent.getItemAtPosition(position);
            Intent intent = new Intent(Lv_thoi_tiet.this, ManHinhChiTietActivity.class);
            intent.putExtra("CITY_NAME", item.city);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        //Sort
        ivSortAQI.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Collections.sort(weatherList, new Comparator<Info_thoi_tiet>() {
                    @Override
                    public int compare(Info_thoi_tiet o1, Info_thoi_tiet o2) {
                        int result = Integer.compare(o1.getAqi(), o2.getAqi());
                        if (result == 0){
                            result = o1.getTemp().compareToIgnoreCase(o2.getTemp());
                        }
                        return result;
                    }
                });
                weatherAdapter.notifyDataSetChanged();
            }
        });

        //Xóa item (Cập nhật cả SQLite và Firebase)
        lvWeather.setOnItemLongClickListener((parent, view, position, id) -> {
            Info_thoi_tiet itemToDelete = (Info_thoi_tiet) parent.getItemAtPosition(position);
            AlertDialog.Builder builder = new AlertDialog.Builder(Lv_thoi_tiet.this);
            builder.setTitle("Xóa thành phố");
            builder.setMessage("Xóa " + itemToDelete.city + "?");
            builder.setPositiveButton("Xóa", (dialog, which) -> {

                //Xóa SQLite
                localDb.deleteLocation(itemToDelete.city);

                //Xóa Firebase
                if (mAuth.getCurrentUser() != null) {
                    String userId = mAuth.getCurrentUser().getUid();
                    db.collection("users").document(userId)
                            .update("citiesSaved", FieldValue.arrayRemove(itemToDelete.city));
                }

                //Xóa List hiển thị
                weatherList.remove(itemToDelete);

                if (weatherAdapter != null) {
                    weatherAdapter.updateList(weatherList);
                }

                Toast.makeText(Lv_thoi_tiet.this, "Đã xóa", Toast.LENGTH_SHORT).show();
            });
            builder.setNegativeButton("Hủy", null);
            builder.show();
            return true;
        });
    }

    //Lấy dữ liệu từ sqlite
    private void loadDataFromSQLite() {
        //Lấy danh sách từ DB
        List<WeatherDatabaseHelper.LocationItem> dbData = localDb.getAllLocations();

        weatherList.clear();

        if (!dbData.isEmpty()) {
            for (WeatherDatabaseHelper.LocationItem item : dbData) {
                //Nếu có dữ liệu cache (temp khác null) -> hiển thị ngay lập tức (Offline)
                if (item.temp != null) {
                    Info_thoi_tiet cachedInfo = new Info_thoi_tiet(
                            item.name, item.temp, "Cảm giác: --", item.hum,
                            item.desc, item.wind, item.vis, item.clouds, item.aqi
                    );

                    //Kiểm tra trùng trước khi add
                    boolean exists = false;
                    for (Info_thoi_tiet i : weatherList) {
                        if (i.city.equalsIgnoreCase(item.name)) {
                            exists = true; break;
                        }
                    }
                    if (!exists) {
                        weatherList.add(cachedInfo);
                    }
                }

                //Gọi API để cập nhật dữ liệu mới nhất (Online)
                fetchCurrentWeatherToLV(item.lat, item.lon, item.name);
            }
            //Cập nhật giao diện ngay với dữ liệu offline
            weatherAdapter.notifyDataSetChanged();
        }
    }


    //Goi y
    private void fetchOWMSuggestions(String query) {
        // Sử dụng API Geocoding của OWM để đảm bảo dữ liệu đồng bộ
        String url = "https://api.openweathermap.org/geo/1.0/direct?q=" + URLEncoder.encode(query) + "&limit=5&appid=" + WeatherUtils.API_KEY;

        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {}
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray jsonArray = new JSONArray(response.body().string());
                        ArrayList<SuggestionItem> temp = new ArrayList<>();

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject item = jsonArray.getJSONObject(i);

                            // Lấy các trường chuẩn của OWM
                            String name = item.getString("name");
                            String country = item.getString("country");
                            double lat = item.getDouble("lat");
                            double lon = item.getDouble("lon");

                            // Xử lý hiển thị tiểu bang/tỉnh (nếu có)
                            String state = item.optString("state", "");
                            String details = state.isEmpty() ? country : state + ", " + country;

                            // Thêm vào danh sách gợi ý
                            temp.add(new SuggestionItem(name, details, lat, lon));

                        }

                        runOnUiThread(() -> {
                            if (!temp.isEmpty()) {
                                suggestionAdapter.updateData(temp);
                                if(suggestionsRecycler != null) suggestionsRecycler.setVisibility(View.VISIBLE);
                            } else {
                                if(suggestionsRecycler != null) suggestionsRecycler.setVisibility(View.GONE);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void saveToListView(String locationName) {
        //Kiểm tra trùng trong List hiển thị
        for (Info_thoi_tiet info : weatherList) {
            if (info.city.equalsIgnoreCase(locationName)) {
                Toast.makeText(this, "Đã có trong danh sách", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        //Gọi API lấy tọa độ
        String url = "https://api.openweathermap.org/geo/1.0/direct?q=" + URLEncoder.encode(locationName) + "&limit=1&appid=" + WeatherUtils.API_KEY;
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {}
            @Override public void onResponse(Call call, Response response) throws IOException {
                try {
                    JSONArray data = new JSONArray(response.body().string());
                    if (data.length() > 0) {
                        JSONObject loc = data.getJSONObject(0);
                        double lat = loc.getDouble("lat");
                        double lon = loc.getDouble("lon");
                        String name = loc.getString("name");

                        //Lưu vào SQLite
                        localDb.addLocation(name, lat, lon);

                        //Hiển thị lên List
                        fetchCurrentWeatherToLV(lat, lon, name);
                        addToFireBase(name);
                    }
                } catch (Exception e) {}
            }
        });
    }

    private void fetchCurrentWeatherToLV(double lat, double lon, String locationName) {

        //Gọi hàm từ untils
        WeatherUtils.fetchCurrentWeatherByCoords(lat, lon, new WeatherUtils.WeatherCallback() {
            @Override
            public void onSuccess(JSONObject data) {

                try {
                    int temp = (int) Math.round(data.getJSONObject("main").getDouble("temp"));
                    int feelsLike = (int) Math.round(data.getJSONObject("main").getDouble("feels_like"));
                    int humidity = data.getJSONObject("main").getInt("humidity");
                    double windSpeed = data.getJSONObject("wind").getDouble("speed");
                    String description = data.getJSONArray("weather")
                            .getJSONObject(0).getString("description");
                    double visibility = data.optDouble("visibility", 0) / 1000;
                    int cloudiness = data.getJSONObject("clouds").getInt("all");

                    // Chuẩn hóa chuỗi
                    String strTemp = temp + "°";
                    String strFeels = "Cảm giác: " + feelsLike + "°";
                    String strHum = "Độ ẩm \n" +humidity + "%";
                    String strWind = "Tốc gió\n"+String.format("%.1f m/s", windSpeed);
                    String strVis = "Tầm nhìn\n" + String.format("%.1f km", visibility);
                    String strCloud = "Mây\n" + cloudiness + "%";

                    //Gọi hàm lấy aqi
                    WeatherUtils.fetchAQI(lat, lon, new WeatherUtils.AQICallback() {
                        @Override
                        public void onSuccess(int aqiVal) {

                            Info_thoi_tiet info = new Info_thoi_tiet(
                                    locationName, strTemp, strFeels, strHum,
                                    description, strWind, strVis, strCloud, aqiVal
                            );

                            localDb.saveFullCity(locationName, lat, lon, info);

                            //Lưu SQLite
                            localDb.updateWeatherInfo(locationName,
                                    strTemp, description, strHum, strWind, strVis, strCloud, aqiVal);

                            //update listview
                            runOnUiThread(() -> {

                                boolean found = false;
                                for (int i = 0; i < weatherList.size(); i++) {
                                    if (weatherList.get(i).city.equalsIgnoreCase(locationName)) {
                                        weatherList.set(i, info); //Cập nhật nếu đã có
                                        found = true;
                                        break;
                                    }
                                }

                                if (!found) {
                                    weatherList.add(info);
                                }

                                //Cập nhật cả list tìm kiếm dự phòng
                                if (weatherAdapter != null) {
                                    weatherAdapter.updateList(weatherList);
                                }

                                //Lưu Firebase
                                if (mAuth.getCurrentUser() != null) {
                                    db.collection("users")
                                            .document(mAuth.getCurrentUser().getUid())
                                            .update("citiesSaved",
                                                    FieldValue.arrayUnion(locationName));
                                }
                            });
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Log.e("AQI", "Lỗi AQI: " + e.getMessage());
                        }
                    });

                } catch (Exception e) {
                    Log.e("PARSE", "Lỗi parse JSON: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("API", "Lỗi API chính: " + e.getMessage());
            }
        });
    }


    //Lịch sử tìm kiếm
    private void addChipToGroup(String keyword) {
        if (cgHistory == null) return;
        Chip chip = new Chip(this);
        chip.setText(keyword);
        chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#333333")));
        chip.setTextColor(Color.WHITE);
        chip.setOnClickListener(v -> saveToListView(keyword.replace("City", "")));
        chip.setOnLongClickListener(v -> {
            searchHistoryList.remove(keyword);
            saveHistory(searchHistoryList);
            cgHistory.removeView(v);
            return true;
        });
        cgHistory.addView(chip, 0);
    }

    private void saveHistory(List<String> historyList) {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        Gson gson = new Gson();
        prefs.edit().putString("history_key", gson.toJson(historyList)).apply();
    }

    private void loadHistoryData() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String json = prefs.getString("history_key", null);
        searchHistoryList = new Gson().fromJson(json, new TypeToken<ArrayList<String>>(){}.getType());
        if (searchHistoryList == null) searchHistoryList = new ArrayList<>();
    }

    //Bien gợi ý (mục gợi ý)
    public static class SuggestionItem {
        String name, details; double lat, lon;
        public SuggestionItem(String name, String details, double lat, double lon) {
            this.name = name; this.details = details; this.lat = lat; this.lon = lon;
        }
    }

    public static class SuggestionAdapter extends RecyclerView.Adapter<SuggestionAdapter.ViewHolder> {
        private ArrayList<SuggestionItem> list;
        private final OnItemClick listener;
        public interface OnItemClick { void onClick(double lat, double lon, String name); }
        public SuggestionAdapter(ArrayList<SuggestionItem> list, OnItemClick listener) { this.list = list; this.listener = listener; }
        public void updateData(ArrayList<SuggestionItem> newList) { this.list = newList;
            notifyDataSetChanged(); }

        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false));
        }
        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            SuggestionItem item = list.get(position);
            holder.text1.setText(item.name);
            holder.text2.setText(item.details);
            holder.itemView.setOnClickListener(v -> listener.onClick(item.lat, item.lon, item.name));
        }
        @Override public int getItemCount() { return list.size(); }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1, text2;
            public ViewHolder(View itemView) {
                super(itemView);
                text1 = itemView.findViewById(android.R.id.text1);
                text2 = itemView.findViewById(android.R.id.text2);
                text1.setTextColor(Color.WHITE);
                text2.setTextColor(Color.LTGRAY);
            }
        }
    }
    //Ham xu li firebase

    private void loadCitiesFromFirebase() {
        String currentUid = mAuth.getCurrentUser().getUid();

        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // lay mang citiesSaved tu firestore
                        ArrayList<String> cloudCities = (ArrayList<String>) documentSnapshot.get("citiesSaved");
                        localDb.deleteAllLocations();
                        weatherList.clear();
                        if (cloudCities != null && !cloudCities.isEmpty()) {
                            for (String city : cloudCities) {
                                String cityNot = city.replace("City", "");
                                saveToListViewFB(cityNot);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firebase", "Lỗi tải dữ liệu: " + e.getMessage());
                });
    }

    private void addToFireBase(String name){
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Bạn chưa đăng nhập!", Toast.LENGTH_SHORT).show();
            return;
        }
        for (Info_thoi_tiet info : weatherList) {
            if (info.city.equalsIgnoreCase(name)) {
//                Toast.makeText(this, "Đã có trong danh sách", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        String currentUid = mAuth.getCurrentUser().getUid();
        Map<String, Object> userData = new HashMap<>();
        userData.put("citiesSaved", FieldValue.arrayUnion(name));

        db.collection("users").document(currentUid)
                .set(userData, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Toast.makeText(Lv_thoi_tiet.this, "Đã thêm vị trí thành công", Toast.LENGTH_SHORT).show();

                    //if (edtSearchCityList != null) {}
                    String nameNonCity = name.replace("City", "");
                    fetchOWMSuggestions(nameNonCity);
                })
                .addOnFailureListener(e -> {
                    Log.w("Firebase", "Lỗi thêm city", e);
                    Toast.makeText(Lv_thoi_tiet.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    public void saveToListViewFB(String locationName) {
        //Kiểm tra trùng trong List hiển thị
        for (Info_thoi_tiet info : weatherList) {
            if (info.city.equalsIgnoreCase(locationName)) {
//                Toast.makeText(this, "Đã có trong danh sách", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        //Gọi API lấy tọa độ
        String url = "https://api.openweathermap.org/geo/1.0/direct?q=" + URLEncoder.encode(locationName) + "&limit=1&appid=" + WeatherUtils.API_KEY;
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {}
            @Override public void onResponse(Call call, Response response) throws IOException {
                try {
                    JSONArray data = new JSONArray(response.body().string());
                    if (data.length() > 0) {
                        JSONObject loc = data.getJSONObject(0);
                        double lat = loc.getDouble("lat");
                        double lon = loc.getDouble("lon");
                        String name = loc.getString("name");
                        localDb.addLocation(name, lat, lon);
                        // Hiển thị lên List
                        fetchCurrentWeatherToLV(lat, lon, name);
                    }
                } catch (Exception e) {}
            }
        });
    }
}