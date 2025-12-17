package com.example.demo_thoi_tiet.Model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Goi_y_hoat_dong {
    public static String layGoiY(String thoiTiet) {
        if (thoiTiet == null) thoiTiet = "";
        String kieu = thoiTiet.toLowerCase();
        List<String> list = new ArrayList<>();

        // Rain/Bad Weather
        if (kieu.contains("rain") || kieu.contains("mưa") || kieu.contains("thunder")) {
            list.add(" Đọc sách");
            list.add(" Uống cafe nóng");
            list.add(" Xem Netflix");
            list.add(" Nấu mì cay");
        }
        //Clear/Sunny
        else if (kieu.contains("clear") || kieu.contains("nắng") || kieu.contains("sun")) {
            list.add(" Chạy bộ công viên");
            list.add(" Đi chụp ảnh");
            list.add(" Phơi quần áo");
            list.add(" Đi ăn kem");
        }
        // Clouds/Default
        else {
            list.add(" Nghe nhạc chill");
            list.add(" Đi siêu thị");
            list.add(" Dọn dẹp phòng");
        }

        if (list.isEmpty()) return "Đi dạo";
        return list.get(new Random().nextInt(list.size()));
    }
}