package com.example.demo_thoi_tiet.Model;

import java.io.Serializable;

public class Cong_viec implements Serializable {
    private int id;
    private String noiDung;
    private String thoiGian;
    private String nhietDo;
    private String camGiac;

    public Cong_viec(int id, String noiDung, String thoiGian, String nhietDo, String camGiac) {
        this.id = id;
        this.noiDung = noiDung;
        this.thoiGian = thoiGian;
        this.nhietDo = nhietDo;
        this.camGiac = camGiac;
    }

    public Cong_viec(String noiDung, String thoiGian, String nhietDo, String camGiac) {
        this.noiDung = noiDung;
        this.thoiGian = thoiGian;
        this.nhietDo = nhietDo;
        this.camGiac = camGiac;
    }

    public int getId() { return id; }
    public String getNoiDung() { return noiDung; }
    public String getThoiGian() { return thoiGian; }
    public String getNhietDo() { return nhietDo; }
    public String getCamGiac() { return camGiac; }
}