package com.example.demo_thoi_tiet.Class;

import java.io.Serializable;
import java.util.HashMap;

public class TaiKhoan implements Serializable {
    String taiKhoan;
    String matKhau;
    public HashMap<String, Object> convertToHashmap(){
        HashMap<String, Object> result = new HashMap<>();
        result.put("taiKhoan", taiKhoan);
        result.put("matKhau", matKhau);
        return result;
    }

    public TaiKhoan() {
    }

    public TaiKhoan(String taiKhoan, String matKhau) {
        this.taiKhoan = taiKhoan;
        this.matKhau = matKhau;
    }

    public String getTaiKhoan() {
        return taiKhoan;
    }

    public void setTaiKhoan(String taiKhoan) {
        this.taiKhoan = taiKhoan;
    }

    public String getMatKhau() {
        return matKhau;
    }

    public void setMatKhau(String matKhau) {
        this.matKhau = matKhau;
    }
}
