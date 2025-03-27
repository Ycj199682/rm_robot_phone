// JackListAdapter.java

package com.reeman.phone.adapter;

import static com.reeman.phone.fragment.JackFragment.elevatorManualSwitch;
import static com.reeman.phone.fragment.JackFragment.manualMapName;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.Gson;
import com.reeman.phone.FragmentActivity;
import com.reeman.phone.R;
import com.reeman.phone.call.MqttClient;
import com.reeman.phone.call.Topic;
import com.reeman.phone.constant.Constants;
import com.reeman.phone.mode.QrcodeModeWithMAC;
import com.reeman.phone.utils.AESUtil;
import com.reeman.phone.utils.Timbers;
import com.reeman.phone.utils.ToastUtils;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class JackListAdapter extends RecyclerView.Adapter<JackListAdapter.ViewHolder>{
    private List<String> data;
    private int lastClickedPosition = RecyclerView.NO_POSITION; // 初始化为无效位置
    private List<String> clickedItems = new ArrayList<>();
    private Context context;
    private String deviceName;
    private ClickedItemsListener itemsListener;
    public static String JackClickedItem = "";
    //只能添加一个点位判断
    public static boolean pointSign=true;
    private int selectedItem = -1;
    private String robotType;


    public void setClickedItemsListener(ClickedItemsListener listener) {
        this.itemsListener = listener;
    }

    public JackListAdapter(List<String> data, Context context, String deviceName, String robotType) {
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
        //holder.textView.setSelected(selectedItem == position); // 应用背景选择器
        // 根据最后点击的位置应用或移除点击动画
        if (!elevatorManualSwitch){
            applyOrRemoveClickAnimation(holder.textView, position);
        }
        holder.textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
        if(robotType.equals(Constants.robotTypeForklift)){
            showConfirmationDialog(data.get(position), position);
        } else if (robotType.equals(Constants.robotTypeAGV)){
            handleItemClick(position);
        }
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
        tvDialogText.setText(context.getString(R.string.manual_limit));
        AlertDialog dialog = builder.create(); // 声明 AlertDialog 对象
        tvText.setText(context.getString(R.string.manual_mode_point));
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
                    Timbers.w(context, "手动模式匹配数据有误，token或key为空");
                    FragmentActivity.destroyActivity((FragmentActivity) context);
                }
                QrcodeModeWithMAC.ManualBody body = new QrcodeModeWithMAC.ManualBody(manualMapName, item);
                String encryptData = null;
                try {
                    encryptData = AESUtil.encrypt(encryptKey, new Gson().toJson(body));
                } catch (GeneralSecurityException e) {
                    throw new RuntimeException(e);
                }
                QrcodeModeWithMAC qrcodeModeWithMAC = new QrcodeModeWithMAC(token, encryptData);
                String jsonPayload = new Gson().toJson(qrcodeModeWithMAC);
                MqttClient.Publish(Topic.topicReceiveQRCodeModelTask(deviceName), jsonPayload);

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


    private void handleItemClick(int position) {
        // 更新最后点击的位置
        lastClickedPosition = position;
        String item = data.get(position);
        if (item.equals(JackClickedItem)) {
            // 当前点击的项目与上次点击的项目相同
            ToastUtils.showShortToast(context.getString(R.string.point_selection));
        } else {
            // 如果上次点击的项目和当前点击的项目是不同的
            lastClickedPosition = position;
            JackClickedItem = item; // 更新最后点击的项目字符串
            clickedItems.add(item);
            notifyDataSetChanged();

            // 在这里通知 Fragment，传递最新的 clickedItems
            if (itemsListener != null) {
                itemsListener.onItemClick(item);
            }
        }
    }

    private void applyOrRemoveClickAnimation(View view, int position) {
        if (position == lastClickedPosition) {
            applyClickAnimation(view);
        } else {
            removeClickAnimation(view);
        }
    }

    private void applyClickAnimation(View view) {
        ScaleAnimation scaleAnimation = new ScaleAnimation(
                1.0f, 0.9f, 1.0f, 0.9f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        scaleAnimation.setDuration(100);
        scaleAnimation.setFillAfter(false);
        view.startAnimation(scaleAnimation);
    }

    private void removeClickAnimation(View view) {
        view.clearAnimation();
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

    public interface ClickedItemsListener {
        void onItemClick(String clickedItem);
    }
}
