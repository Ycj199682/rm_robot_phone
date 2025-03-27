package com.reeman.phone;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;

import com.reeman.phone.adapter.LanguageAdapter;
import com.reeman.phone.constant.Constants;
import com.reeman.phone.utils.LocalUtil;

import java.util.Locale;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class LanguageActivity extends AppCompatActivity implements LanguageAdapter.OnItemClickListener {
    private LanguageAdapter adapter;
    private TextView tvConfirmButton;
    private int currentLanguage;

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 初始化语言设置
        SharedPreferences preferences = getSharedPreferences(Constants.LANGUAGE_SET, MODE_PRIVATE);
        currentLanguage = preferences.getInt(Constants.LANGUAGE_SET, -1);
        if (currentLanguage != -1) {
            Locale localName = LocalUtil.getLocalName(currentLanguage);
            LocalUtil.changeAppLanguage(getResources(), localName);
        }

        setContentView(R.layout.activity_language);
        initRecyclerView();
    }

    private void initRecyclerView() {
        RecyclerView rvLanguageList = findViewById(R.id.language_list);
        tvConfirmButton = findViewById(R.id.confirm_button);
        tvConfirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //点击确认按钮
                SharedPreferences sp = getSharedPreferences(Constants.LANGUAGE_SET, MODE_PRIVATE);
                sp.edit().putInt(Constants.LANGUAGE_SET, currentLanguage).apply();
                // 重新启动MainActivity以更新语言设置
                Intent intent = new Intent(LanguageActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });
        adapter = new LanguageAdapter(LocalUtil.getLocal(), this);
        rvLanguageList.setLayoutManager(new LinearLayoutManager(this));
        adapter.setOnItemClickListener(this); // 设置监听器
        rvLanguageList.setAdapter(adapter);
    }

    @Override
    public void onItemClick(int position) {
        currentLanguage = position;
        //点击事件
        Locale localName = LocalUtil.getLocalName(position);
        LocalUtil.changeAppLanguage(getResources(), localName);
        Log.w("点击", "onItemClick: " + position);
        adapter.notifyDataSetChanged();
        tvConfirmButton.setText(R.string.confirm);
    }
}
