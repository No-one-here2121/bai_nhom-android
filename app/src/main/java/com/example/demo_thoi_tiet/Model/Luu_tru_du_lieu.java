package com.example.demo_thoi_tiet.Model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class Luu_tru_du_lieu extends SQLiteOpenHelper {

    private static final String TEN_DATABASE = "LichThoiTiet.db";
    private static final int PHIEN_BAN = 2;

    public Luu_tru_du_lieu(Context context) {
        super(context, TEN_DATABASE, null, PHIEN_BAN);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String taoBang = "CREATE TABLE cong_viec (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "noi_dung TEXT, " +
                "thoi_gian TEXT, " +
                "nhiet_do TEXT, " +
                "cam_giac TEXT)";
        db.execSQL(taoBang);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS cong_viec");
        onCreate(db);
    }

    public void themCongViec(Cong_viec cv) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("noi_dung", cv.getNoiDung());
        values.put("thoi_gian", cv.getThoiGian());
        values.put("nhiet_do", cv.getNhietDo());
        values.put("cam_giac", cv.getCamGiac());
        db.insert("cong_viec", null, values);
        db.close();
    }

    public List<Cong_viec> layDanhSach() {
        List<Cong_viec> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM cong_viec ORDER BY thoi_gian ASC", null);
        if (cursor.moveToFirst()) {
            do {
                list.add(new Cong_viec(
                        cursor.getInt(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getString(4)
                ));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    public void xoaCongViec(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("cong_viec", "id=?", new String[]{String.valueOf(id)});
        db.close();
    }
}