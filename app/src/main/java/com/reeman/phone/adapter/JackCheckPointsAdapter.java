// JackCheckPointsAdapter.java

package com.reeman.phone.adapter;

import static com.reeman.phone.adapter.DeliveryListAdapter.lastClickedItem;
import static com.reeman.phone.adapter.DeliveryListAdapter.lastClickedPosition;
import static com.reeman.phone.adapter.JackListAdapter.JackClickedItem;
import static com.reeman.phone.adapter.JackListAdapter.pointSign;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.reeman.phone.R;

import java.util.List;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;

public class JackCheckPointsAdapter extends RecyclerView.Adapter<JackCheckPointsAdapter.ViewHolder>{
    private List<String> data;
    private Context context;
    private String deviceName;
    private ClickedItemsListener itemsListener;

    public JackCheckPointsAdapter(List<String> data, Context context) {
        this.data = data;
        this.context = context;
        this.deviceName = deviceName;
    }

    public void setClickedItemsListener(ClickedItemsListener listener) {
        this.itemsListener = listener;
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

        // 为每个项的删除按钮设置点击事件
        holder.appCompatImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int clickedPosition = holder.getAdapterPosition();
                if (clickedPosition != RecyclerView.NO_POSITION) {
                    removeDeliveryPointItem(clickedPosition);
                }
            }
        });
    }

    public void removeDeliveryPointItem(int position) {
        pointSign=true;
        String itemToRemove = data.get(position);
        data.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, data.size() - position);
        if (itemToRemove.equals(JackClickedItem)) {
            JackClickedItem = " ";
        }
        Log.w("TAG", "removeDeliveryPointItem: " + data);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        AppCompatImageView bg_connection;
        TextView textView;
        AppCompatImageView appCompatImageView;
        Button start;

        public ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.tv_delivery_point);
            appCompatImageView = itemView.findViewById(R.id.iv_delete_delivery_point);
            bg_connection = itemView.findViewById(R.id.bg_connection);
            appCompatImageView.setVisibility(View.VISIBLE);
            bg_connection.setVisibility(View.GONE);
        }
    }

    public interface ClickedItemsListener {
        void onItemClick(String clickedItem);
    }
}
