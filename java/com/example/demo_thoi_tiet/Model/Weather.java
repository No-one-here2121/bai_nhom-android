package com.example.demo_thoi_tiet.Model;

import java.io.Serializable;

public class Weather implements Serializable {
    private int id;
    private String main;        // Ví dụ: "Rain", "Clear"
    private String description; // Ví dụ: "light rain"
    private String icon;

    // Constructor
    public Weather(int id, String main, String description, String icon) {
        this.id = id;
        this.main = main;
        this.description = description;
        this.icon = icon;
    }

    // Getters
    public String getMain() {
        return main;
    }

    public String getDescription() {
        return description;
    }
}