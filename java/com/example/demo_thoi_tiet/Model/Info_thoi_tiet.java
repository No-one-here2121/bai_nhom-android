package com.example.demo_thoi_tiet.Model;

public class Info_thoi_tiet {
    public String city, temp, feelsLike, humidity, desc, wind, vis, cloud;

    public Info_thoi_tiet(String city, String temp, String feelsLike, String humidity, String desc, String wind, String vis, String cloud) {
        this.city = city;
        this.temp = temp;
        this.feelsLike = feelsLike;
        this.humidity = humidity;
        this.desc = desc;
        this.wind = wind;
        this.vis = vis;
        this.cloud = cloud;
    }
}
