package com.example.smartwaterchick;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class JadwalAdapter extends RecyclerView.Adapter<JadwalAdapter.ViewHolder> {

    public interface OnJadwalActionListener {
        void onToggle(String id, boolean aktif);
        void onEdit(JadwalItem item);
        void onHapus(String id);
    }

    private final List<JadwalItem> list;
    private final OnJadwalActionListener listener;

    public JadwalAdapter(List<JadwalItem> list, OnJadwalActionListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_jadwal, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JadwalItem item = list.get(position);

        // Tampilkan jam dalam format HH:MM
        holder.tvJam.setText(String.format(Locale.getDefault(), "%02d:%02d", item.jam, item.menit));

        // Set switch tanpa trigger listener lama
        holder.switchItem.setOnCheckedChangeListener(null);
        holder.switchItem.setChecked(item.aktif);
        holder.switchItem.setOnCheckedChangeListener((btn, checked) -> {
            item.aktif = checked;
            listener.onToggle(item.id, checked);
        });

        // Tombol Edit
        holder.btnEdit.setOnClickListener(v -> listener.onEdit(item));

        // Tombol Hapus
        holder.btnHapus.setOnClickListener(v -> listener.onHapus(item.id));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvJam;
        SwitchCompat switchItem;
        ImageView btnEdit, btnHapus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvJam = itemView.findViewById(R.id.tvJam);
            switchItem = itemView.findViewById(R.id.switchJadwalItem);
            btnEdit = itemView.findViewById(R.id.btnEditJadwal);
            btnHapus = itemView.findViewById(R.id.btnHapusJadwal);
        }
    }
}
