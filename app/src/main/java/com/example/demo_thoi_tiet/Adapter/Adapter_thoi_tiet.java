//lay du lieu tu List<Info_thoi_tiet>,mang vao listview,co them phan filter
package com.example.demo_thoi_tiet.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import com.example.demo_thoi_tiet.Model.Info_thoi_tiet;
import com.example.demo_thoi_tiet.R;
import java.util.ArrayList;
import java.util.List;

public class Adapter_thoi_tiet extends BaseAdapter implements Filterable {
    private Context context;
    private List<Info_thoi_tiet> listWeather;//list dang hien thi
    private List<Info_thoi_tiet> listWeatherOld;//list goc

    public Adapter_thoi_tiet(Context context, List<Info_thoi_tiet> listWeather) {
        this.context = context;
        this.listWeather = listWeather;
        //copy dữ liệu sang listWeatherOld
        //khi tim kiem thi listweather bi cat bot -> can listweatherold de hien thi lai
        this.listWeatherOld = new ArrayList<>(listWeather);
    }

    //hien thi bao nhieu dong?
    @Override
    public int getCount() {return listWeather != null ? listWeather.size() : 0;}

    //hien thi dong nao?
    @Override
    public Object getItem(int i) {
        return listWeather.get(i);
    }

    //lay id cua dong nao?
    @Override
    public long getItemId(int i) {
        return i;
    }

    //hien thi tung dong
    @Override
    public View getView(int i, View view, ViewGroup parent) {
        //tao ra view moi
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.layout_thoi_tiet, parent, false);
        }

        //lay du lieu dong hien tai
        Info_thoi_tiet w = listWeather.get(i);

        //du lieu vao textview
        ((TextView) view.findViewById(R.id.textView2)).setText(w.city);
        ((TextView) view.findViewById(R.id.textView3)).setText(w.temp);
        ((TextView) view.findViewById(R.id.textView5)).setText(w.feelsLike);
        ((TextView) view.findViewById(R.id.textView16)).setText(w.desc);
        ((TextView) view.findViewById(R.id.textView9)).setText(w.humidity);
        ((TextView) view.findViewById(R.id.textView10)).setText(w.wind);
        ((TextView) view.findViewById(R.id.textView11)).setText(w.vis);
        ((TextView) view.findViewById(R.id.textView12)).setText(w.cloud);

        //cho aqi
        TextView tvAQI = view.findViewById(R.id.tvAQI);
        String status = "";
        int color = Color.GRAY;

        //dua vao chi so aqi de doi mau va trang thai
        switch (w.aqi) {
            case 1: status = "Tốt"; color = Color.parseColor("#9dd84e"); break;
            case 2: status = "TB"; color = Color.parseColor("#fbcf38"); break;
            case 3: status = "Kém"; color = Color.parseColor("#f89049"); break;
            case 4: status = "Xấu"; color = Color.parseColor("#fe6a68"); break;
            case 5: status = "Rất xấu"; color = Color.parseColor("#a97abc"); break;
            default: status = "--";
        }

        //hien thi aqi
        tvAQI.setText("Mức: " + w.aqi + "\n" + status);
        tvAQI.setBackgroundColor(color);
        tvAQI.setTextColor(Color.BLACK);

        //tra ve view hien thi
        return view;
    }

    //cho thanh tim kiem
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                String strSearch = (constraint == null) ? "" : constraint.toString();
                List<Info_thoi_tiet> listResult;

                if (strSearch.isEmpty()) {//neu thanh nay rong
                    //tra ve list goc
                    listResult = listWeatherOld;
                } else {//neu co tu khoa
                    //tao list moi de luu ket qua tim duoc
                    List<Info_thoi_tiet> list = new ArrayList<>();

                    //duyet qua danh sach goc de tim kiem
                    for (Info_thoi_tiet info : listWeatherOld) {
                        //co tu khoa khong?
                        if (info.city != null && info.city.toLowerCase().contains(strSearch.toLowerCase())) {
                            list.add(info);//co thi them vao danh sach
                        }
                    }
                    listResult = list;
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = listResult;
                filterResults.count = listResult.size();
                return filterResults;
            }

            //hien thi ket qua tim kiem
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                //mang ket qua vao danh sach
                listWeather = (List<Info_thoi_tiet>) results.values;
                //cap nhat giao dien
                notifyDataSetChanged();
            }
        };
    }

    //set nhanh du lieu ngoai,khong can phai tao adpter moi
    public void updateList(List<Info_thoi_tiet> newList) {
        this.listWeather = newList;
        this.listWeatherOld = new ArrayList<>(newList);
        notifyDataSetChanged();   // Luôn cập nhật UI
    }
}