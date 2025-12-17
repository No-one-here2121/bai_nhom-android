package com.example.demo_thoi_tiet.Model;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.demo_thoi_tiet.R;
import com.example.demo_thoi_tiet.Utils.WeatherUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.MapTileProviderBasic;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.TilesOverlay;

public class MapActivity extends AppCompatActivity {
    private MapView mapView;
    private double lat = 0;
    private double lon = 0;
    private String cityName = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_map);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        mapView = findViewById(R.id.osmMap);
        TextView tvMapCity = findViewById(R.id.tvMapCity);
        FloatingActionButton fabBack = findViewById(R.id.fabBack);

        fabBack.setOnClickListener(v -> finish());

        if (getIntent() != null) {
            cityName = getIntent().getStringExtra("CITY_NAME");
            lat = getIntent().getDoubleExtra("LAT", 0);
            lon = getIntent().getDoubleExtra("LON", 0);
        }
        if (cityName != null) {
            tvMapCity.setText(cityName);
        }

        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(9.5);

        GeoPoint point;
        if (lat != 0 || lon != 0) {
            point = new GeoPoint(lat, lon);
        } else {

            point = new GeoPoint(21.0278, 105.8342);
        }
        mapView.getController().setCenter(point);

        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle(cityName != null ? cityName : "Vị trí");
        mapView.getOverlays().add(marker);

        addWeatherOverlay("precipitation_new");
    }

    private void addWeatherOverlay(String layerName) {
        XYTileSource weatherTileSource = new XYTileSource(
                layerName,
                0, 18, 256, ".png",
                new String[]{"https://tile.openweathermap.org/map/" + layerName + "/"},
                "OpenWeatherMap") {
            @Override
            public String getTileURLString(long pMapTileIndex) {
                int z = org.osmdroid.util.MapTileIndex.getZoom(pMapTileIndex);
                int x = org.osmdroid.util.MapTileIndex.getX(pMapTileIndex);
                int y = org.osmdroid.util.MapTileIndex.getY(pMapTileIndex);
                return String.format("https://tile.openweathermap.org/map/%s/%d/%d/%d.png?appid=%s",
                        layerName, z, x, y, WeatherUtils.API_KEY);
            }
        };

        MapTileProviderBasic tileProvider = new MapTileProviderBasic(getApplicationContext());
        tileProvider.setTileSource(weatherTileSource);
        TilesOverlay weatherOverlay = new TilesOverlay(tileProvider, this);

        if (mapView != null) {
            mapView.getOverlays().add(weatherOverlay);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }
}