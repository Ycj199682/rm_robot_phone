package com.reeman.phone.adapter;

import static com.reeman.phone.fragment.DeliveryFragment.QRelevatorAutoSwitch;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.TextView;
import android.widget.Toast;

import com.reeman.phone.R;
import com.reeman.phone.utils.MqttUtil;
import com.reeman.phone.utils.ToastUtils;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class DeliveryListAdapter extends RecyclerView.Adapter<DeliveryListAdapter.ViewHolder> {

    private List<String> data;
    public static int lastClickedPosition = RecyclerView.NO_POSITION; // 初始化为无效位置
    private List<String> clickedItems = new ArrayList<>();
    private Context context;
    private String deviceName;
    private MqttUtil mqttUtil;
    private ClickedItemsListener itemsListener;
    private long lastClickTime = 0; // 上次点击的时间戳
    private static final long CLICK_INTERVAL = 3000; // 3秒的间隔时间
    private Toast lastToast;
    private boolean sign=true;
    public static String lastClickedItem = "";


    public void setClickedItemsListener(ClickedItemsListener listener) {
        this.itemsListener = listener;
    }

    public DeliveryListAdapter(List<String> data, Context context, String deviceName) {
        this.data = data;
        this.context = context;
        this.deviceName = deviceName;
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
        // 根据最后点击的位置应用或移除点击动画
        if(!QRelevatorAutoSwitch){
            applyOrRemoveClickAnimation(holder.textView, position);
        }
        // 处理项目点击事件的 OnClickListener
        holder.textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleItemClick(position);
            }
        });
    }

    private void handleItemClick(int position) {
        String item = data.get(position);
        // 检查当前点击的项目字符串是否与上次点击的项目字符串相同
        if (item.equals(lastClickedItem)) {
            // 当前点击的项目与上次点击的项目相同
            ToastUtils.showShortToast(context.getString(R.string.point_selection));
        } else {
            // 如果上次点击的项目和当前点击的项目是不同的
            lastClickedPosition = position;
            lastClickedItem = item; // 更新最后点击的项目字符串
            clickedItems.add(item);
            // 通知项目更改以应用/移除动画
            notifyDataSetChanged();

            // 在这里通知 Fragment，传递最新的 clickedItems
            if (itemsListener != null) {
                itemsListener.onItemsUpdated(item);
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

    // 在适配器类中定义一个回调接口
    public interface ClickedItemsListener {
        void onItemsUpdated(String clickedItem);
    }
}
