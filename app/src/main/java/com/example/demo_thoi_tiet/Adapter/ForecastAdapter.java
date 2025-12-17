//cho viec du bao thoi tiet
package com.example.demo_thoi_tiet.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.demo_thoi_tiet.R;
import java.util.List;

public class ForecastAdapter extends RecyclerView.Adapter<ForecastAdapter.ViewHolder> {
    public static class ForecastItem {
        String day;//ngay
        String temp;//nhiet do
        String iconCode;//ma icon cua api

        public ForecastItem(String day, String temp, String iconCode) {
            this.day = day;
            this.temp = temp;
            this.iconCode = iconCode;
        }
    }

    private List<ForecastItem> list;

    public ForecastAdapter(List<ForecastItem> list) {
        this.list = list;
    }

    //tao dang
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //item_forecast -> layout hien thi
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_forecast, parent, false);

        //tra ve view dang do
        return new ViewHolder(view);
    }

    //mang du lieu vao view
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        //lay du lieu cua ngay vao view tuong ung
        ForecastItem item = list.get(position);

        //gan vao view
        holder.tvTime.setText(item.day);
        holder.tvTemp.setText(item.temp);

        //set icon tuong ung id api
        setWeatherIcon(holder.imgIcon, item.iconCode);
    }

    //dem so luon -> can lam bao nhieu o
    @Override
    public int getItemCount() {
        return list.size();
    }

    //cho icon
    private void setWeatherIcon(ImageView imageView, String code) {
        if (code.contains("01")) imageView.setImageResource(R.drawable.sunny);//nang
        else if (code.contains("02")) imageView.setImageResource(R.drawable.cloud_black);//it may
        else if (code.contains("03") || code.contains("04")) imageView.setImageResource(R.drawable.conditions);//nhieu may
        else if (code.contains("09") || code.contains("10")) imageView.setImageResource(R.drawable.rain);//mua
        else if (code.contains("11")) imageView.setImageResource(R.drawable.da_sun);//giong
        else if (code.contains("13")) imageView.setImageResource(R.drawable.snow);//tuyet
        else imageView.setImageResource(R.drawable.sunny);//mac dinh
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvTemp;
        ImageView imgIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);//mang bien tu file item_forecast sang

            //chi lam 1 lan luc tao dong -> danh sach luot muot
            tvTime = itemView.findViewById(R.id.tvTime);
            tvTemp = itemView.findViewById(R.id.tvTempItem);
            imgIcon = itemView.findViewById(R.id.imgIcon);
        }
    }
}