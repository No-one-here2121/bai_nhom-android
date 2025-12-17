package com.example.demo_thoi_tiet.Model;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.demo_thoi_tiet.R;

import java.util.List;

public class Cong_viec_Adapter extends RecyclerView.Adapter<Cong_viec_Adapter.ViewHolder> {
    private Context context;
    private List<Cong_viec> list;
    private Luu_tru_du_lieu db;

    public Cong_viec_Adapter(Context context, List<Cong_viec> list, Luu_tru_du_lieu db) {
        this.context = context;
        this.list = list;
        this.db = db;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cong_viec, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Cong_viec item = list.get(position);
        holder.txtGio.setText(item.getThoiGian());
        holder.txtNoiDung.setText(item.getNoiDung());
        holder.txtNhietDo.setText(item.getNhietDo());

        holder.btnXoa.setOnClickListener(v -> {
            db.xoaCongViec(item.getId());
            list.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, list.size());
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtGio, txtNoiDung, txtNhietDo;
        ImageButton btnXoa;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtGio = itemView.findViewById(R.id.txt_gio);
            txtNoiDung = itemView.findViewById(R.id.txt_noi_dung);
            txtNhietDo = itemView.findViewById(R.id.txt_nhiet_do);
            btnXoa = itemView.findViewById(R.id.btn_xoa);
        }
    }
}