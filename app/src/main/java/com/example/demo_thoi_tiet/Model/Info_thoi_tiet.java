package com.example.demo_thoi_tiet.Model;

public class Info_thoi_tiet {
    public String city;
    public String temp;
    public String feelsLike;
    public String humidity;
    public String desc;
    public String wind;
    public String vis;
    public String cloud;
    public int aqi;

    public Info_thoi_tiet(String city, String temp, String feelsLike, String humidity, String desc, String wind, String vis, String cloud, int aqi) {
        this.city = city;
        this.temp = temp;
        this.feelsLike = feelsLike;
        this.humidity = humidity;
        this.desc = desc;
        this.wind = wind;
        this.vis = vis;
        this.cloud = cloud;
        this.aqi = aqi;
    }

    public int getAqi() {
        return aqi;
    }

    public void setAqi(int aqi) {
        this.aqi = aqi;
    }

    public String getCloud() {
        return cloud;
    }

    public void setCloud(String cloud) {
        this.cloud = cloud;
    }

    public String getVis() {
        return vis;
    }

    public void setVis(String vis) {
        this.vis = vis;
    }

    public String getWind() {
        return wind;
    }

    public void setWind(String wind) {
        this.wind = wind;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getHumidity() {
        return humidity;
    }

    public void setHumidity(String humidity) {
        this.humidity = humidity;
    }

    public String getFeelsLike() {
        return feelsLike;
    }

    public void setFeelsLike(String feelsLike) {
        this.feelsLike = feelsLike;
    }

    public void setTemp(String temp) {
        this.temp = temp;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getTemp() {
        return temp;
    }
}