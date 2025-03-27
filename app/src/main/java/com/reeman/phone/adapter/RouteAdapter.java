package com.reeman.phone.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.Gson;
import com.reeman.phone.FragmentActivity;
import com.reeman.phone.R;
import com.reeman.phone.call.Topic;
import com.reeman.phone.constant.Constants;
import com.reeman.phone.mode.RouteModeWithMAC;
import com.reeman.phone.utils.AESUtil;
import com.reeman.phone.call.MqttClient;
import com.reeman.phone.utils.Timbers;
import com.reeman.phone.utils.ToastUtils;

import java.security.GeneralSecurityException;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class RouteAdapter extends RecyclerView.Adapter<RouteAdapter.ViewHolder> {

    private List<String> data;
    private int selectedItem = -1;
    private Context context;
    private String hostname;
    private String robotType;

    public RouteAdapter(List<String> data, Context context, String hostname, String robotType) {
        this.data = data;
        this.context = context;
        this.hostname = hostname;
        this.robotType = robotType;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_call_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.textView.setText(data.get(position));
        holder.textView.setSelected(selectedItem == position); // 应用背景选择器
        holder.textView.setOnClickListener(v -> toggleSelection(position));
    }

    @Override
    public int getItemCount() {
        return data.size();
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
        showConfirmationDialog(data.get(position), position);
    }

    private void showConfirmationDialog(String item, final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.layout_custom_dialog_layout, null);
        final EditText input = view.findViewById(R.id.et_device_name);
        input.setVisibility(View.GONE);
        builder.setView(view);

        TextView btnCancel = view.findViewById(R.id.btn_cancel);
        TextView btnConfirm = view.findViewById(R.id.btn_confirm);

        TextView tvText = view.findViewById(R.id.tv_text_setname);
        tvText.setVisibility(View.VISIBLE);
        TextView tvDialogText = view.findViewById(R.id.tv_dialog_title);
        if (robotType.equals(Constants.robotTypeForklift)) {
            tvDialogText.setText(context.getString(R.string.task_list));
            tvText.setText(context.getString(R.string.task_are_sure));
        } else if (robotType.equals(Constants.robotTypeAGV)) {
            tvDialogText.setText(context.getString(R.string.route));
            tvText.setText(context.getString(R.string.confirm_route_point));
        }

        AlertDialog dialog = builder.create(); // 声明并创建 AlertDialog 对象

        btnConfirm.setOnClickListener(v -> {
            SharedPreferences sharedPreferences = context.getSharedPreferences("MulticastDevices", Context.MODE_PRIVATE);
            String token = sharedPreferences.getString(hostname + "_token", null);
            String encryptKey = sharedPreferences.getString(hostname + "_encryptKey", null);

            if (token == null || encryptKey == null) {
                ToastUtils.showShortToast(context.getString(R.string.error_del));
                Timbers.w(context, "任务列表模式匹配数据有误，token或key为空");
                FragmentActivity.destroyActivity((FragmentActivity) context);
                return;
            }

            String encryptData;
            try {
                encryptData = AESUtil.encrypt(encryptKey, item);
            } catch (GeneralSecurityException e) {
                Timbers.w(context, "加密失败：" + e.getMessage());
                return;
            }

            RouteModeWithMAC routeModeWithMAC = new RouteModeWithMAC(token, encryptData);
            if (robotType.equals(Constants.robotTypeForklift)) {
                MqttClient.Publish(Topic.topicReceiveRouteModelTask(hostname), new Gson().toJson(routeModeWithMAC));
            } else if (robotType.equals(Constants.robotTypeAGV)) {
                MqttClient.Publish(Topic.AGVtopicReceiveRouteModelTask(hostname), new Gson().toJson(routeModeWithMAC));
            }

            selectedItem = -1;
            notifyDataSetChanged();
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> {
            selectedItem = -1;
            notifyDataSetChanged();
            dialog.dismiss();
        });

        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.setCancelable(false);
        dialog.show();

        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.copyFrom(dialog.getWindow().getAttributes());
        layoutParams.width = 900; // 设置宽度
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT; // 设置高度自适应内容
        dialog.getWindow().setAttributes(layoutParams);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.tv_call);
        }
    }
}
