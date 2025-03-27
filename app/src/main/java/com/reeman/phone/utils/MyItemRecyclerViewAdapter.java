package com.reeman.phone.utils;

import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.reeman.phone.utils.placeholder.PlaceholderContent.PlaceholderItem;
import com.reeman.phone.databinding.FragmentCurrentTaskBinding;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link PlaceholderItem}.
 */
public class MyItemRecyclerViewAdapter extends RecyclerView.Adapter<MyItemRecyclerViewAdapter.ViewHolder> {

    private final List<PlaceholderItem> mValues;
    private boolean[] mExpandedStates; // 记录每个项的展开状态

    public MyItemRecyclerViewAdapter(List<PlaceholderItem> items) {
        mValues = items;
        mExpandedStates = new boolean[items.size()]; // 初始化状态
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(FragmentCurrentTaskBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.mIdView.setText(mValues.get(position).id+" .");

        // 将内容和详细信息连接成字符串
        holder.mContentFirst.setText(mValues.get(position).content.get(0)); // 显示内容列表
        holder.mContentEnd.setText(mValues.get(position).content.get(1));
        holder.mDetailView.setText(mValues.get(position).details.toString()); // 显示详细信息列表

        // 根据状态设置详细信息的可见性
        holder.mDetailView.setVisibility(mExpandedStates[position] ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mExpandedStates[position] = !mExpandedStates[position]; // 切换状态
                notifyItemChanged(position); // 更新该项
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView mIdView;
        public final TextView mContentFirst;
        public final TextView mContentEnd;
        public final TextView mDetailView; // 新增详细信息视图
        public PlaceholderItem mItem;

        public ViewHolder(FragmentCurrentTaskBinding binding) {
            super(binding.getRoot());
            mIdView = binding.itemNumber;
            mContentFirst = binding.tvContentFirst;
            mContentEnd = binding.tvContentEnd;
            mDetailView = binding.tvDetail; // 绑定详细信息视图
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentFirst.getText() + "'"+ mContentEnd.getText() + "'";
        }
    }

    // 添加更新数据的方法
    public void updateData(List<PlaceholderItem> newItems) {
        mValues.clear();
        mValues.addAll(newItems);
        mExpandedStates = new boolean[newItems.size()]; // 重置展开状态
        notifyDataSetChanged();
    }
}
