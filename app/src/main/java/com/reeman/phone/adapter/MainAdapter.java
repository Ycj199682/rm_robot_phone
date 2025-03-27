package com.reeman.phone.adapter;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.reeman.phone.FragmentActivity;
import com.reeman.phone.R;
import com.reeman.phone.call.MqttClient;
import com.reeman.phone.call.Topic;
import com.reeman.phone.constant.Constants;
import com.reeman.phone.mode.PublicHeatBeatMode;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

public class MainAdapter extends RecyclerView.Adapter<MainAdapter.ViewHolder> {

    private SharedPreferences sharedPreferencesScan;
    private List<String> foregroundData;
    private List<String> backgroundData;
    private int selectedItem = -1;
    private Context context;
    public static boolean showAllTextViews = false; // 新增一个变量来跟踪 TextView 的可见性
    private SharedPreferences sharedPreferences;
    public static final String PREFS_NAME = "DeviceNames";
    public static final String DEVICE_NAME_KEY = "device_name_";
    private boolean isClickEnabled = true;
    private String robotType;

    public MainAdapter(List<String> data,List<String> modifyData, Context context) {
        this.foregroundData = modifyData;
        this.backgroundData = data;
        this.context = context;
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadForegroundData();
        sharedPreferencesScan = context.getSharedPreferences("MulticastDevices", Context.MODE_PRIVATE);
    }

    public void showAllTextViews() {
        this.showAllTextViews = true;
        notifyDataSetChanged(); // 通知适配器数据已更改，需要刷新
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_main_item, parent, false);
        return new ViewHolder(view);
    }
    public void refreshEdit(){
        showAllTextViews=false;
        notifyDataSetChanged();
    }
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.textView.setText(foregroundData.get(position));
        holder.textView.setSelected(selectedItem == position); // 应用背景选择器
        //根据机器类型显示图标
        robotType = sharedPreferencesScan.getString(backgroundData.get(position) + "_robotType", null);
        if(robotType.equals(Constants.robotTypeForklift)){  //叉车
            holder.tv_bg_robot_icon.setImageResource(R.drawable.bg_forklift_robot);
        } else if (robotType.equals(Constants.robotTypeAGV)){  //AGV
            holder.tv_bg_robot_icon.setImageResource(R.drawable.bg_agv_robot);
        }
        if (showAllTextViews) {
            holder.tvSetRobotName.setVisibility(View.VISIBLE);
        } else {
            holder.tvSetRobotName.setVisibility(View.GONE);
        }
        holder.ll_device_list.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isClickEnabled) {
                    int position = holder.getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        //saveDeviceName(position, foregroundData.get(position));
                        ArrayList<String> arrayList = new ArrayList<>(foregroundData);
                        // 处理 TextView 的点击事件
                        Intent intent = new Intent(context, FragmentActivity.class);
                        // 传递数据到目标
                        intent.putExtra("key", backgroundData.get(position));
                        intent.putExtra("foregroundkey", foregroundData.get(position));
                        intent.putStringArrayListExtra("foregroundkeyList", arrayList);
                        context.startActivity(intent);
                    }
                }
            }
        });
        holder.tvSetRobotName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Animation animation = AnimationUtils.loadAnimation(context, R.anim.sure_scale_anim);
                // 开始动画
                holder.ll_device_list_all.startAnimation(animation);
                showChangeNameDialog(holder.getAdapterPosition());
            }
        });
    }


    public void setClickEnabled(boolean isClickEnabled) {
        this.isClickEnabled = isClickEnabled;
        notifyDataSetChanged();
    }

    private void showChangeNameDialog(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        // 加载自定义布局
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.layout_custom_dialog_layout, null);

        // 获取布局中的 EditText 和按钮
        final EditText input = dialogView.findViewById(R.id.et_device_name);
        final TextView btn_cancel = dialogView.findViewById(R.id.btn_cancel);
        final TextView btn_confirm = dialogView.findViewById(R.id.btn_confirm);

        // 创建对话框并设置自定义布局
        AlertDialog dialog = builder.create();
        dialog.setView(dialogView);

        btn_confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 获取输入的名称并保存或处理
                String newName = input.getText().toString();
                if (!newName.isEmpty()) {
                    foregroundData.set(position, newName); // 更新数据
                    saveDeviceName(position, newName);
                    notifyItemChanged(position); // 通知适配器更新特定项
                    dialog.dismiss(); // 关闭对话框
                }
            }
        });

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel(); // 取消对话框
            }
        });
        dialog.show();
        //确保对话框背景样式不覆盖自定义布局的样式
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.copyFrom(dialog.getWindow().getAttributes());
        layoutParams.width = 900; // 设置宽度
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT; // 设置高度自适应内容
        dialog.getWindow().setAttributes(layoutParams);
    }


    private void saveDeviceName(int position, String name) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(DEVICE_NAME_KEY + backgroundData.get(position), foregroundData.get(position));
        editor.apply();
    }

    public void loadForegroundData() {
        for (int i = 0; i < backgroundData.size(); i++) {
            String savedName = sharedPreferences.getString(DEVICE_NAME_KEY + backgroundData.get(i),null);
            if (savedName != null) {
                foregroundData.set(i, savedName);
            }
        }
    }

    public void onButtonClickedAdapter(View view) {

    }
    @Override
    public int getItemCount() {
        return backgroundData.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        ImageView tvSetRobotName;
        ImageView tv_bg_robot_icon;
        LinearLayout ll_device_list;
        LinearLayout ll_device_list_all;

        @SuppressLint("WrongViewCast")
        public ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.tv_device);
            ll_device_list = itemView.findViewById(R.id.ll_device_list);
            tvSetRobotName = itemView.findViewById(R.id.tv_set_robot_name);
            ll_device_list_all = itemView.findViewById(R.id.ll_device_list_all);
            tv_bg_robot_icon = itemView.findViewById(R.id.tv_bg_robot_icon);
        }
    }
}

