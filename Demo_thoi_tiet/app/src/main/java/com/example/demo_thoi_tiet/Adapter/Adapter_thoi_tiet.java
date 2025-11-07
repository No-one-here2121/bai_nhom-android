package com.example.demo_thoi_tiet.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.example.demo_thoi_tiet.Model.Info_thoi_tiet;
import com.example.demo_thoi_tiet.R;
import java.util.List;

public class Adapter_thoi_tiet extends BaseAdapter {
    private Context context;
    private List<Info_thoi_tiet> listWeather;

    public Adapter_thoi_tiet(Context context, List<Info_thoi_tiet> listWeather) {
        this.context = context;
        this.listWeather = listWeather;
    }

    @Override
    public int getCount() { return listWeather.size(); }
    @Override
    public Object getItem(int i) { return listWeather.get(i); }
    @Override
    public long getItemId(int i) { return i; }

    @Override
    public View getView(int i, View view, ViewGroup parent) {
        if (view == null)
            view = LayoutInflater.from(context).inflate(R.layout.layout_thoi_tiet, parent, false);
        Info_thoi_tiet w = listWeather.get(i);

        ((TextView) view.findViewById(R.id.textView2)).setText(w.city);
        ((TextView) view.findViewById(R.id.textView3)).setText(w.temp);
        ((TextView) view.findViewById(R.id.textView5)).setText(w.feelsLike);
        ((TextView) view.findViewById(R.id.textView16)).setText(w.desc);
        ((TextView) view.findViewById(R.id.textView9)).setText(w.humidity);
        ((TextView) view.findViewById(R.id.textView10)).setText(w.wind);
        ((TextView) view.findViewById(R.id.textView11)).setText(w.vis);
        ((TextView) view.findViewById(R.id.textView12)).setText(w.cloud);

        return view;
    }
}
