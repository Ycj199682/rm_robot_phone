package com.reeman.phone.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.reeman.phone.R;
import com.reeman.phone.constant.Constants;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;

import static android.content.Context.MODE_PRIVATE;

public class LanguageAdapter extends RecyclerView.Adapter<LanguageAdapter.ViewHolder> {
    private List<String> data;
    private Context context;
    private int selectedPosition = 1; // 初始化为无效位置

    public LanguageAdapter(List<String> data, Context context) {
        this.data = data;
        this.context = context;
        //获取语言数据选中当前的语言

        SharedPreferences preferences = context.getSharedPreferences(Constants.LANGUAGE_SET, MODE_PRIVATE);
        if (preferences.getInt(Constants.LANGUAGE_SET,-1) == -1){
            selectedPosition = 1;//默认中文
        }else {
            int languagePosition = preferences.getInt(Constants.LANGUAGE_SET,-1);
            selectedPosition = languagePosition;
        }
    }
    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_language_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.textView.setText(data.get(position));

        // 设置选中和未选中状态
        if (selectedPosition == position) {
            holder.imageView.setImageResource(R.drawable.icon_language_checked);
        } else {
            holder.imageView.setImageResource(R.drawable.icon_language_normal);
        }

        // 点击项时切换选中状态
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (selectedPosition != position) {
                    int previousSelected = selectedPosition;
                    selectedPosition = position;
                    notifyItemChanged(previousSelected); // 取消先前选中项的选中状态
                    notifyItemChanged(selectedPosition); // 设置当前选中项的选中状态
                    if (listener != null) {
                        listener.onItemClick(position); // 触发接口回调
                    }
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        ImageView imageView;
        public ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.tv_language_name);
            imageView = itemView.findViewById(R.id.iv_language_state);
        }
    }
    public interface OnItemClickListener {
        void onItemClick(int position);
    }
}