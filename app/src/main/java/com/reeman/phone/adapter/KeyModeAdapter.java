package com.reeman.phone.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.reeman.phone.R;

import java.util.List;

public class KeyModeAdapter extends RecyclerView.Adapter<KeyModeAdapter.ViewHolder> {

    private List<String> keys;
    private OnItemClickListener listener;
    private int selectedPosition = 0; // 添加选中项标记

    private String mapName;

    public interface OnItemClickListener {
        void onItemClick(String key);
        void setMapName(int mapKey);
    }

    public KeyModeAdapter(List<String> keys) {
        this.keys = keys;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_map_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String key = keys.get(position);
        holder.keyTextView.setText(key);
        // 设置选中项的状态
        holder.keyTextView.setSelected(position == selectedPosition);
        listener.setMapName(selectedPosition);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(key);
                //listener.setMapName(selectedPosition);
                // 更新选中项的位置
                notifyItemChanged(selectedPosition); // 取消上一个选中项的状态
                selectedPosition = holder.getAdapterPosition(); // 更新选中项的位置
                notifyItemChanged(selectedPosition); // 更新当前选中项的状态
            }
        });
    }

    @Override
    public int getItemCount() {
        return keys.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView keyTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            keyTextView = itemView.findViewById(R.id.tv_map_group);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 更新选中项的位置
                    notifyItemChanged(selectedPosition); // 取消上一个选中项的状态
                    selectedPosition = getAdapterPosition(); // 更新选中项的位置
                    notifyItemChanged(selectedPosition); // 更新当前选中项的状态
                }
            });
        }
    }
}


