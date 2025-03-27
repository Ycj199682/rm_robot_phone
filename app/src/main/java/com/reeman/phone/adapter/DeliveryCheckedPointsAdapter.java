package com.reeman.phone.adapter;

import static com.reeman.phone.adapter.DeliveryListAdapter.lastClickedItem;
import static com.reeman.phone.adapter.DeliveryListAdapter.lastClickedPosition;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.reeman.phone.R;
import com.reeman.phone.utils.MqttUtil;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;

public class DeliveryCheckedPointsAdapter extends RecyclerView.Adapter<DeliveryCheckedPointsAdapter.ViewHolder> {
    private List<String> data;
    private List<String> mapNames; // 新增的地图名称列表
    private Context context;
    private String deviceName;
    private MqttUtil mqttUtil;
    private DataCallback dataCallback;

    public DeliveryCheckedPointsAdapter(List<String> data, List<String> mapNames, Context context, String deviceName) {
        this.data = data;
        this.mapNames = mapNames; // 初始化地图名称列表
        this.context = context;
        this.deviceName = deviceName;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_item_delivery_point, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.textView.setText(data.get(position));

        // 在偶数位置显示删除图标
        if (position % 2 == 1) {
            holder.appCompatImageView.setVisibility(View.VISIBLE);
            holder.bgConnection.setVisibility(View.GONE);
        } else {
            holder.appCompatImageView.setVisibility(View.GONE);
            holder.bgConnection.setVisibility(View.VISIBLE);
        }

        // 为每个项的删除按钮设置点击事件
        holder.appCompatImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int clickedPosition = holder.getAdapterPosition();
                if (clickedPosition != RecyclerView.NO_POSITION && clickedPosition % 2 == 1) {
                    removeDeliveryPointItem(clickedPosition);
                }
            }
        });
    }

    public void removeDeliveryPointItem(int position) {
        if (position % 2 == 1 && position > 0) {
            String itemToRemove = data.get(position);
            String previousItemToRemove = data.get(position - 1);

            data.remove(position);
            data.remove(position - 1);
            mapNames.remove(position); // 删除对应的地图名称
            mapNames.remove(position - 1); // 删除对应的地图名称
            notifyItemRemoved(position);
            notifyItemRemoved(position - 1);
            notifyItemRangeChanged(position - 1, data.size() - (position - 1));

            // 更新 lastClickedItem 和 lastClickedPosition
            if (itemToRemove.equals(lastClickedItem) || previousItemToRemove.equals(lastClickedItem)) {
                lastClickedItem = "";
                lastClickedPosition = RecyclerView.NO_POSITION;
            }

            if (dataCallback != null) {
                dataCallback.onDataChanged(data);
            }
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    // 更新数据的方法
    public void updateData(String newData, String mapName) { // 添加参数 mapName
        data.add(newData);
        mapNames.add(mapName); // 添加对应的地图名称
        dataCallback.onDataChanged(data);
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        AppCompatImageView bgConnection;
        TextView textView;
        AppCompatImageView appCompatImageView;
        Button start;

        public ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.tv_delivery_point);
            appCompatImageView = itemView.findViewById(R.id.iv_delete_delivery_point);
            bgConnection = itemView.findViewById(R.id.bg_connection);

        }
    }

    public void clearAll() {
        data.clear();
        mapNames.clear(); // 清空地图名称列表
    }

    public interface DataCallback {
        void onDataChanged(List<String> updatedData);
    }

    public void setDataCallback(DataCallback callback) {
        this.dataCallback = callback;
    }
}

