package com.example.demo_thoi_tiet.Model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class WeatherDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "WeatherApp.db";
    private static final int DATABASE_VERSION = 2;
    private static final String TABLE_NAME = "SavedLocations";

    // Tên cột
    private static final String COL_ID = "id";
    private static final String COL_NAME = "name";
    private static final String COL_LAT = "lat";
    private static final String COL_LON = "lon";

    // Các cột để lưu cache thời tiết
    private static final String COL_TEMP = "temp";
    private static final String COL_DESC = "description";
    private static final String COL_HUMIDITY = "humidity";
    private static final String COL_WIND = "wind";
    private static final String COL_VISIBILITY = "visibility";
    private static final String COL_CLOUDS = "clouds";
    private static final String COL_AQI = "aqi";

    public WeatherDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_NAME + " TEXT UNIQUE, " +
                COL_LAT + " REAL, " +
                COL_LON + " REAL, " +
                COL_TEMP + " TEXT, " +
                COL_DESC + " TEXT, " +
                COL_HUMIDITY + " TEXT, " +
                COL_WIND + " TEXT, " +
                COL_VISIBILITY + " TEXT, " +
                COL_CLOUDS + " TEXT, " +
                COL_AQI + " INTEGER)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    //them vi tri
    public void addLocation(String name, double lat, double lon) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_NAME, name);
        values.put(COL_LAT, lat);
        values.put(COL_LON, lon);
        values.put(COL_TEMP, "--");
        values.put(COL_DESC, "Đang tải...");

        db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    //lay tat ca thong tin
    public List<LocationItem> getAllLocations() {
        List<LocationItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);

        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME));
                double lat = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LAT));
                double lon = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LON));

                String temp = cursor.getString(cursor.getColumnIndexOrThrow(COL_TEMP));
                String desc = cursor.getString(cursor.getColumnIndexOrThrow(COL_DESC));
                String hum = cursor.getString(cursor.getColumnIndexOrThrow(COL_HUMIDITY));
                String wind = cursor.getString(cursor.getColumnIndexOrThrow(COL_WIND));
                String vis = cursor.getString(cursor.getColumnIndexOrThrow(COL_VISIBILITY));
                String clouds = cursor.getString(cursor.getColumnIndexOrThrow(COL_CLOUDS));
                int aqi = cursor.getInt(cursor.getColumnIndexOrThrow(COL_AQI));

                list.add(new LocationItem(name, lat, lon, temp, desc, hum, wind, vis, clouds, aqi));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public void deleteLocation(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, COL_NAME + "=?", new String[]{name});
    }

    public void clearAll() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_NAME);
    }

    public static class LocationItem {
        public String name;
        public double lat, lon;
        public String temp, desc, hum, wind, vis, clouds;
        public int aqi;

        public LocationItem(String name, double lat, double lon, String temp, String desc, String hum, String wind, String vis, String clouds, int aqi) {
            this.name = name;
            this.lat = lat;
            this.lon = lon;
            this.temp = temp;
            this.desc = desc;
            this.hum = hum;
            this.wind = wind;
            this.vis = vis;
            this.clouds = clouds;
            this.aqi = aqi;
        }
    }

    public void updateWeatherInfo(String name, String temp, String desc, String hum, String wind, String vis, String clouds, int aqi) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TEMP, temp);
        values.put(COL_DESC, desc);
        values.put(COL_HUMIDITY, hum);
        values.put(COL_WIND, wind);
        values.put(COL_VISIBILITY, vis);
        values.put(COL_CLOUDS, clouds);
        values.put(COL_AQI, aqi);

        db.update(TABLE_NAME, values, COL_NAME + "=?", new String[]{name});
    }

    public void deleteAllLocations() {
        clearAll();
    }

    public ArrayList<Info_thoi_tiet> getAllWeatherData() {
        ArrayList<Info_thoi_tiet> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);

        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME));
                String temp = cursor.getString(cursor.getColumnIndexOrThrow(COL_TEMP));
                String desc = cursor.getString(cursor.getColumnIndexOrThrow(COL_DESC));
                String hum = cursor.getString(cursor.getColumnIndexOrThrow(COL_HUMIDITY));
                String wind = cursor.getString(cursor.getColumnIndexOrThrow(COL_WIND));
                String vis = cursor.getString(cursor.getColumnIndexOrThrow(COL_VISIBILITY));
                String clouds = cursor.getString(cursor.getColumnIndexOrThrow(COL_CLOUDS));
                int aqi = cursor.getInt(cursor.getColumnIndexOrThrow(COL_AQI));
                list.add(new Info_thoi_tiet(name, temp, "", hum, desc, wind, vis, clouds, aqi));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public void saveFullCity(String name, double lat, double lon, Info_thoi_tiet info) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COL_NAME, name);
        values.put(COL_LAT, lat);
        values.put(COL_LON, lon);

        values.put(COL_TEMP, info.temp);
        values.put(COL_DESC, info.desc);
        values.put(COL_HUMIDITY, info.humidity);
        values.put(COL_WIND, info.wind);
        values.put(COL_VISIBILITY, info.vis);
        values.put(COL_CLOUDS, info.cloud);
        values.put(COL_AQI, info.aqi);


        db.replace(TABLE_NAME, null, values);
    }
}