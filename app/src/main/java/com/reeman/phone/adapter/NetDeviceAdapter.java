package com.reeman.phone.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.reeman.phone.MainActivity;
import com.reeman.phone.R;
import com.reeman.phone.constant.Constants;
import com.reeman.phone.utils.LoadingDialogUtil;

import java.util.ArrayList;
import java.util.List;

public class NetDeviceAdapter extends RecyclerView.Adapter<NetDeviceAdapter.ViewHolder>{
    private List<String> data = new ArrayList<>();
    private int selectedItem = -1;
    private OnItemSelectedListener listener;
    private SharedPreferences sharedPreferencesScan;
    private String robotType;

    public NetDeviceAdapter(MainActivity context) {
        sharedPreferencesScan = context.getSharedPreferences("MulticastDevices", Context.MODE_PRIVATE);
    }

    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        this.listener = listener;
    }
    @NonNull
    @Override
    public NetDeviceAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_add_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NetDeviceAdapter.ViewHolder holder, int position) {
        holder.textView.setText(data.get(position));
        holder.layoutRoot.setSelected(selectedItem == position);
        robotType = sharedPreferencesScan.getString(data.get(position) + "_robotType", null);
        if(robotType.equals(Constants.robotTypeForklift)){  //叉车
            holder.iv_robot_icon.setImageResource(R.drawable.bg_forklift_robot);
        } else if (robotType.equals(Constants.robotTypeAGV)){  //AGV
            holder.iv_robot_icon.setImageResource(R.drawable.bg_agv_robot);
        }
        holder.layoutRoot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 处理TextView的点击事件
                toggleSelection(position);
                if (listener != null) {
                    listener.onItemSelected(data.get(position));
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutRoot;
        ImageView iv_robot_icon;
        TextView textView;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutRoot = itemView.findViewById(R.id.layout_root);
            textView = itemView.findViewById(R.id.tv_call);
            iv_robot_icon= itemView.findViewById(R.id.iv_robot_icon);
        }
    }
    public void setData(List<String> newData) {
        data.clear();
        if (newData != null) {
            data.addAll(newData);
        }
        // 设置默认选中第一个项目
        if (!data.isEmpty()) {
            selectedItem = 0;
            if (listener != null) {
                listener.onItemSelected(data.get(0));
            }
        }
        notifyDataSetChanged();
    }
    private void toggleSelection(int position) {
        if (selectedItem == position) {
            // 取消选择项目
            selectedItem = -1;
        } else {
            // 选择项目
            selectedItem = position;
        }
        notifyDataSetChanged();
    }
    public interface OnItemSelectedListener {
        void onItemSelected(String selectedItem);
    }

}
