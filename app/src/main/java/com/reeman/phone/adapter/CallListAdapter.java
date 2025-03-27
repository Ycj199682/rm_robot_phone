package com.reeman.phone.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
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
import com.reeman.phone.mode.CallingModeWithMAC;
import com.reeman.phone.mode.PublicHeatBeatMode;
import com.reeman.phone.utils.AESUtil;
import com.reeman.phone.utils.MqttUtil;
import com.reeman.phone.call.MqttClient;
import com.reeman.phone.utils.Timbers;
import com.reeman.phone.utils.ToastUtils;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.security.GeneralSecurityException;
import java.util.List;

public class CallListAdapter extends RecyclerView.Adapter<CallListAdapter.ViewHolder> {

    //    private List<String> data;
    private List<String> data;

    private int selectedItem = -1;
    private Context context;
    private String deviceName;
    private String mapName;
    private String robotType;

    public CallListAdapter(List<String> data, Context context, String deviceName, String robotType) {
        this.data = data;
        this.context = context;
        this.deviceName = deviceName;
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
        holder.textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 处理TextView的点击事件
                toggleSelection(position);
            }
        });

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
        tvDialogText.setText(context.getString(R.string.call));
        AlertDialog dialog = builder.create(); // 声明 AlertDialog 对象
        tvText.setText(context.getString(R.string.confirm_call_point));
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss(); // 关闭对话框
                // 用户取消选择，重置选择状态
                selectedItem = -1;
                notifyDataSetChanged();
            }
        });
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //获取token
                SharedPreferences sharedPreferences = context.getSharedPreferences("MulticastDevices", Context.MODE_PRIVATE);
                String token = sharedPreferences.getString(deviceName + "_token", null);
                String encryptKey=sharedPreferences.getString(deviceName + "_encryptKey", null);
                if(token==null || encryptKey==null){
                    ToastUtils.showShortToast(context.getString(R.string.error_del));
                    Timbers.w(context, "匹配数据有误，token或key为空");
                    FragmentActivity.destroyActivity((FragmentActivity) context);
                }

                String encryptData= null;
                try {
                    encryptData = AESUtil.encrypt(encryptKey, new Gson().toJson(new CallingModeWithMAC.Body(mapName, item)));
                } catch (GeneralSecurityException e) {
                    throw new RuntimeException(e);
                }
                if(robotType.equals(Constants.robotTypeForklift)){
                    MqttClient.Publish(Topic.topicReceiveCallingModelTask(deviceName),new Gson().toJson(new CallingModeWithMAC(token,encryptData)));
                } else if (robotType.equals(Constants.robotTypeAGV)){
                    MqttClient.Publish(Topic.AGVtopicReceiveCallingModelTask(deviceName),new Gson().toJson(new CallingModeWithMAC(token,encryptData)));
                }

                selectedItem = -1;
                notifyDataSetChanged();
                dialog.dismiss();
            }
        });
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.setCancelable(false);
        dialog.show();
        // 设置对话框大小
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.copyFrom(dialog.getWindow().getAttributes());
        layoutParams.width = 900; // 设置宽度
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        dialog.getWindow().setAttributes(layoutParams);
    }


    @Override
    public int getItemCount() {
        return data.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.tv_call);
        }
    }
    public void setMapName(String mapName) {
        this.mapName = mapName;
        notifyDataSetChanged();
    }
}
