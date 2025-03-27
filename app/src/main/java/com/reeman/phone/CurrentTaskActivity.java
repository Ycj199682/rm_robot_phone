package com.reeman.phone;

import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.reeman.phone.R;
import com.reeman.phone.utils.MyItemRecyclerViewAdapter;
import com.reeman.phone.utils.placeholder.PlaceholderContent;
import com.reeman.phone.mode.CurrentTaskMode;

import java.util.ArrayList;
import java.util.List;

//待添加的活动，显示机器当前正在执行的任务

public class CurrentTaskActivity extends AppCompatActivity {

    private static final String ARG_COLUMN_COUNT = "column-count";
    private int mColumnCount = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_current_task);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        // 设置 RecyclerView
        RecyclerView recyclerView = findViewById(R.id.recycler_view);

        // 设置布局管理器
        if (mColumnCount <= 1) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
        } else {
            recyclerView.setLayoutManager(new GridLayoutManager(this, mColumnCount));
        }

        // 模拟数据
        List<CurrentTaskMode> realItems = fetchDataFromSource();
        PlaceholderContent.initialize(realItems);

        // 设置适配器
        recyclerView.setAdapter(new MyItemRecyclerViewAdapter(PlaceholderContent.ITEMS));
    }

    // 模拟获取数据的方法
    private List<CurrentTaskMode> fetchDataFromSource() {
        List<CurrentTaskMode> items = new ArrayList<>();
        items.add(new CurrentTaskMode("1", List.of("内容1", "内容2"), List.of("详细信息1", "详细信息2")));
        items.add(new CurrentTaskMode("2", List.of("内容3", "内容4"), List.of("详细信息3", "详细信息4")));
        return items;
    }
}
