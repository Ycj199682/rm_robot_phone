package com.reeman.phone;

import static com.reeman.phone.constant.Constants.FRONT_DEVICE_NAME;
import static com.reeman.phone.constant.Constants.isChargingState;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;
import com.reeman.phone.call.Topic;
import com.reeman.phone.constant.Constants;

import com.reeman.phone.event.Event;
import com.reeman.phone.fragment.CallFragment;
import com.reeman.phone.fragment.DeliveryFragment;
import com.reeman.phone.fragment.JackFragment;
import com.reeman.phone.fragment.RouteFragment;
import com.reeman.phone.mode.CurrentTaskMode;
import com.reeman.phone.mode.PublicHeatBeatMode;
import com.reeman.phone.mode.ReceiveHeartBeatMode;

import com.reeman.phone.utils.CustomExceptionHandler;
import com.reeman.phone.utils.GlobalFlag;
import com.reeman.phone.utils.HeartBeatUtil;
import com.reeman.phone.utils.LoadingDialogUtil;
import com.reeman.phone.utils.LocalUtil;
import com.reeman.phone.call.MqttClient;
import com.reeman.phone.utils.MyItemRecyclerViewAdapter;
import com.reeman.phone.utils.Timbers;
import com.reeman.phone.utils.ToastUtils;
import com.reeman.phone.utils.placeholder.PlaceholderContent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FragmentActivity extends AppCompatActivity {
    private int selectedFragmentId = -1; // 追踪当前选中的 Fragment ID
    private TextView selectedTextView = null;
    private String hostname;
    public String robotCrashLogName;
    private String setHostName;
    private boolean isTransactionInProgress = false;
    private static final int TIMEOUT_DELAY = 10000; // 10s
    private static final int REQUEST_CODE_OVERLAY_PERMISSION = 1;
    private TextView callTextView;
    private TextView TaskListTextView;
    private TextView manualTextView;
    private TextView autoTextView;
    private Handler handler;
    private Runnable checkHeartbeatRunnable;
    private String encryptKey;
    private ProgressBar batteryLevelProgress;
    private TextView tvPercentage;
    private String Token;
    private TextView viewById;
    private ArrayList<String> foregroundkeyList= new ArrayList<>();
    private String robotType;
    private ImageView ivChargingLogo;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle drawerToggle;
    private Button btnCharge;
    private Button btnReturnHome;
    private ImageView tv_fragment_finish;
    private ImageView btnToggleSidebar;
    private boolean robotLineSign=true;
    private int connectionFailures=0;
    private TextView currentTaskShow;
    private Button btnMachineStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences preferences = getSharedPreferences(Constants.LANGUAGE_SET, MODE_PRIVATE);
        int selectedLanguage = preferences.getInt(Constants.LANGUAGE_SET, -1);
        LoadingDialogUtil.getInstance().closeLoadingDialog();
        LocalUtil.changeAppLanguage(getResources(), LocalUtil.getLocalName(selectedLanguage));
        setContentView(R.layout.fragment_content);
        Toolbar toolbar = findViewById(R.id.back_toolbar);
        setSupportActionBar(toolbar);
        EventBus.getDefault().register(this);
        // 设置状态栏文本颜色
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.fragment_background));
//        }
        if (getIntent().hasExtra("key")) {
            hostname = getIntent().getStringExtra("key");
            robotCrashLogName=hostname;
            FRONT_DEVICE_NAME=hostname+"\n";
        }
        Timbers.w(this, "进入控制界面，机器编号："+hostname);
        if (getIntent().hasExtra("foregroundkey")) {
            setHostName = getIntent().getStringExtra("foregroundkey");
        }
        Intent intent = getIntent();
        foregroundkeyList = intent.getStringArrayListExtra("foregroundkeyList");
        Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler(this,hostname));

        SharedPreferences sharedPreferences = this.getSharedPreferences("MulticastDevices", Context.MODE_PRIVATE);
        encryptKey = sharedPreferences.getString(hostname + "_encryptKey", null);
        Token= sharedPreferences.getString(hostname + "_token", null);
        robotType = sharedPreferences.getString(hostname + "_robotType", null);
        Timbers.w(this, "获取Key："+encryptKey+" , token："+Token);
        batteryLevelProgress = findViewById(R.id.battery_level_progress); // 用于显示电池电量
        ivChargingLogo = findViewById(R.id.iv_charging_logo);
        tv_fragment_finish = findViewById(R.id.tv_fragment_finish);
        tvPercentage = findViewById(R.id.battery_percentage);
        batteryLevelProgress.setVisibility(View.GONE);
        tvPercentage.setVisibility(View.GONE);
        // 启用返回按钮
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            viewById = findViewById(R.id.call_title);
            viewById.setText(setHostName);
            actionBar.setDisplayShowTitleEnabled(false); // 隐藏默认标题
        }
        LoadingDialogUtil.getInstance().showLoadingDialog(this, getString(R.string.connect_device));//开启加载动画
        ExtraFragmentMenu();
        //开始心跳
        HeartBeatUtil.getInstance().startHeartbeat(this,hostname);
        handler = new Handler();
        checkHeartbeatRunnable = () -> {
            if (isFirst) { // 表示没有收到心跳
                FRONT_DEVICE_NAME=null;
                Timbers.w(FragmentActivity.this, "没有收到心跳");
                LoadingDialogUtil.getInstance().closeLoadingDialog();
                ToastUtils.showShortToast(getString(R.string.connect_error));
                finish();
            }
        };
        tv_fragment_finish.setOnClickListener(v -> finish());
        // 提交任务
        handler.postDelayed(checkHeartbeatRunnable, TIMEOUT_DELAY);

    }

    private void ExtraFragmentMenu() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        btnToggleSidebar = findViewById(R.id.btn_toggle_sidebar);
        btnCharge = findViewById(R.id.btn_charge);
        btnReturnHome = findViewById(R.id.btn_return_home);
        btnMachineStatus = findViewById(R.id.btn_machine_status);

        // 为按钮设置点击监听
        btnCharge.setOnClickListener(v -> handleButtonClick(v, () -> {
            MqttClient.Publish(Topic.AGVGoChargePointModel(hostname), new Gson().toJson(new PublicHeatBeatMode(Token)));
            Timbers.w(FragmentActivity.this, "按钮执行返回充电桩充电");
        }));

        btnReturnHome.setOnClickListener(v -> handleButtonClick(v, () -> {
            MqttClient.Publish(Topic.AGVGoReturnPointModel(hostname), new Gson().toJson(new PublicHeatBeatMode(Token)));
            Timbers.w(FragmentActivity.this, "按钮执行返回出品点");
        }));

        btnMachineStatus.setOnClickListener(v -> handleButtonClick(v, () -> {
            Intent intent = new Intent(FragmentActivity.this, CurrentTaskActivity.class);
            startActivity(intent);
            Timbers.w(FragmentActivity.this, "查询机器当前任务");
        }));

        btnToggleSidebar.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.closeDrawer(GravityCompat.END);
            } else {
                drawerLayout.openDrawer(GravityCompat.END);
            }
        });
    }

    // 按钮点击动画
    private void handleButtonClick(View v, Runnable action) {
        v.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(100)
                .withEndAction(() -> v.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(100)
                        .start())
                .start();

        action.run(); // 执行传入的操作
        new Handler().postDelayed(() -> drawerLayout.closeDrawer(GravityCompat.END), 350);
    }



    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void resetTextColor(TextView textView) {
        textView.setTextColor(getResources().getColor(R.color.default_text_color));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
    }

    private void setSelectedTextColor(TextView textView) {
        textView.setTextColor(getResources().getColor(R.color.selected_text_color));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
    }


    public void clickButton(View view) {
        // 检查是否已有事务正在进行
        if (isTransactionInProgress) {
            return;
        }
        isTransactionInProgress = true;

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        CallFragment callFragment = new CallFragment();
        DeliveryFragment deliveryFragment = new DeliveryFragment();
        JackFragment jackFragment = new JackFragment();
        RouteFragment routeFragment = new RouteFragment();

        int clickedId = view.getId();

        if (selectedFragmentId == clickedId) {
            isTransactionInProgress = false;
            return; // 如果点击的是当前选中的 Fragment，则不执行任何操作
        }

        // 获取当前选中的 TextView
        TextView previousSelectedTextView = selectedTextView;

        // 恢复之前选中的 TextView 的文本颜色
        if (previousSelectedTextView != null) {
            resetTextColor(previousSelectedTextView);
        }

        // 更新选中的 TextView 和 Fragment ID
        selectedTextView = (TextView) view;
        setSelectedTextColor(selectedTextView);
        selectedFragmentId = clickedId;

        // 移除之前的 Fragment
        for (Fragment fragment : fragmentManager.getFragments()) {
            fragmentTransaction.remove(fragment);
        }

        // 根据点击的 TextView 切换对应的 Fragment
        switch (clickedId) {
            case R.id.tv_call_menu:
                fragmentTransaction.add(R.id.lv, callFragment);
                break;
            case R.id.tv_auto_menu:
                fragmentTransaction.add(R.id.lv, deliveryFragment);
                break;
            case R.id.tv_manual_menu:
                fragmentTransaction.add(R.id.lv, jackFragment);
                break;
            case R.id.tv_task_list_menu:
                fragmentTransaction.add(R.id.lv, routeFragment);
                break;
        }
        // 提交事务
        fragmentTransaction.commit();
        // 设置一个延迟，在一定时间后重置标志
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                isTransactionInProgress = false;
            }
        }, 500); // 根据需要调整此延迟
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            // 如果侧边栏打开，则关闭侧边栏
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            FRONT_DEVICE_NAME=null;
            if (LoadingDialogUtil.getInstance().isLoading()) {
                LoadingDialogUtil.getInstance().closeLoadingDialog();
                handler.removeCallbacks(checkHeartbeatRunnable);
                HeartBeatUtil.getInstance().stopHeartbeat();
            }
            super.onBackPressed();
        }

    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onHeartBeatEvent(Event.OnHeartBeatEvent event) {
        //判断网络状态，发起心跳
        MqttClient.heartBeat(hostname, event.MacAddress);
        if(!robotLineSign){
            connectionFailures++;
            //两次未收到心跳提示用户从新连接
            if(connectionFailures==2){
                ToastUtils.showShortToast(getString(R.string.connect_error));
            }
        }
        robotLineSign=false;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMqttPayloadEvent(Event.OnMqttPayloadEvent event){
        Timbers.w(this, "收到机器端mqtt主题："+event.topic+" , 消息："+event.payload);
        robotLineSign=true;
        connectionFailures=0;
        try {
            if (isValidPayload(event.payload)) {
                if (event.topic.equals(Topic.topicPublishCallingModelPoints(hostname))) { // 机器人端推送呼叫模式点位
                    EventBus.getDefault().post(Event.getOnCallingEvent(event.payload,encryptKey));
                }  else if (event.topic.equals(Topic.topicPublishNormalModelPoints(hostname))) {// 机器人端推送自动模式点位
                    EventBus.getDefault().post(Event.getOnNormalEvent(event.payload,encryptKey));
                } else if (event.topic.equals(Topic.topicPublishRouteModelPoints(hostname))) {//机器人端推送本地任务列表
                    EventBus.getDefault().post(Event.getOnRouteEvent(event.payload,encryptKey));
                } else if (event.topic.equals(Topic.topicPublishQRCodeModelPoints(hostname))) { //机器人端推送手动模式路线名称
                    EventBus.getDefault().post(Event.getOnQrcodeEvent(event.payload,encryptKey));
                } else if (event.topic.equals(Topic.topicReceivedHeartBeat(hostname))) {    //接收机器端心跳
                    EventBus.getDefault().post(Event.getOnResponseHeartBeat(event.payload,encryptKey));
                } else if (event.topic.equals(Topic.TaskExecutionResponse(hostname))) {    //叉车任务执行响应
                    EventBus.getDefault().post(Event.getOnTaskResponse(event.payload,encryptKey));
                }else if (event.topic.equals(Topic.AGVtopicReceivedHeartBeat(hostname))) {    //AGV接收心跳
                    EventBus.getDefault().post(Event.getOnResponseHeartBeat(event.payload,encryptKey));
                }else if (event.topic.equals(Topic.AGVtopicPublishCallingModelPoints(hostname))) {    //AGV接收呼叫
                    EventBus.getDefault().post(Event.getOnCallingEvent(event.payload,encryptKey));
                }else if (event.topic.equals(Topic.AGVtopicPublishQRModelPoints(hostname))) {// AGV接收二维码模式点位
                    EventBus.getDefault().post(Event.getOnNormalEvent(event.payload,encryptKey));
                } else if (event.topic.equals(Topic.AGVtopicPublishRouteModelPoints(hostname))) {//机器人端推送本地任务列表
                    EventBus.getDefault().post(Event.getOnRouteEvent(event.payload,encryptKey));
                } else if (event.topic.equals(Topic.AGVtopicPublishOrdinaryModelPoints(hostname))) { //机器人端推送普通模式路线名称
                    EventBus.getDefault().post(Event.getOnQrcodeEvent(event.payload,encryptKey));
                }else if (event.topic.equals(Topic.AGVTaskExecutionResponse(hostname))) {    //叉车任务执行响应
                    EventBus.getDefault().post(Event.getOnTaskResponse(event.payload,encryptKey));
                }
            }else {
                Timbers.w(FragmentActivity.this, "接收到的消息格式无效: " + event.payload);
            }
        } catch (Exception e) {
            Timbers.w(FragmentActivity.this, "处理MQTT消息时发生异常: " + e.getMessage());
        }
    }

    private boolean isValidPayload(String payload) {
        // 添加你的消息有效性检查逻辑，例如检查是否为空、格式是否正确等
        return payload != null && !payload.isEmpty();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        GlobalFlag.getInstance().setFlag(false);
        GlobalFlag.getInstance().setElevatorMode(false);
        EventBus.getDefault().unregister(this);
        //LoadingDialogUtil.getInstance().closeLoadingDialog();
        HeartBeatUtil.getInstance().stopHeartbeat();

        // 移除 checkHeartbeatRunnable
        if (handler != null && checkHeartbeatRunnable != null) {
            handler.removeCallbacks(checkHeartbeatRunnable);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        MqttClient.Subscribe(hostname);
    }

    public static void destroyActivity(FragmentActivity activity) {
        if (activity != null) {
            activity.finish();
        }
    }

    private boolean isFirst = true;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onResponseHeartBeat(Event.OnResponseHeartBeat event) {
        Timbers.w(this, "收到机器端心跳！");
        String payload = event.payload;
        ReceiveHeartBeatMode receiveHeartBeatMode = new Gson().fromJson(payload, ReceiveHeartBeatMode.class);
        if (!receiveHeartBeatMode.getToken().equals(Token)) {
            ToastUtils.showShortToast(getString(R.string.token_check));
            LoadingDialogUtil.getInstance().closeLoadingDialog();
            return;
        }

        if (isFirst) {
            initializeUI(receiveHeartBeatMode);
        }

        updateChargingState(receiveHeartBeatMode.getChargeState());
        updateBatteryLevel(receiveHeartBeatMode.getLevel());
        updateFlags(receiveHeartBeatMode);

        // 更新设备别名
        Log.d("test1101","别名："+receiveHeartBeatMode.getAlias());
        updateDeviceAlias(receiveHeartBeatMode.getAlias());

        isFirst = false;
    }

    private void initializeUI(ReceiveHeartBeatMode receiveHeartBeatMode) {
        LoadingDialogUtil.getInstance().closeLoadingDialog();
        callTextView = findViewById(R.id.tv_call_menu);
        TaskListTextView = findViewById(R.id.tv_task_list_menu);
        manualTextView = findViewById(R.id.tv_manual_menu);
        autoTextView = findViewById(R.id.tv_auto_menu);

        if (robotType.equals(Constants.robotTypeForklift)) {
            TaskListTextView.setText(R.string.task_list);
            manualTextView.setText(R.string.auto);
            autoTextView.setText(R.string.automatic);
            btnToggleSidebar.setVisibility(View.GONE);
        } else if (robotType.equals(Constants.robotTypeAGV)) {
            TaskListTextView.setText(R.string.route);
            manualTextView.setText(R.string.delivery);
            autoTextView.setText(R.string.lift);
            btnToggleSidebar.setVisibility(View.VISIBLE);
        }

        callTextView.setVisibility(View.VISIBLE);
        TaskListTextView.setVisibility(View.VISIBLE);
        manualTextView.setVisibility(View.VISIBLE);
        autoTextView.setVisibility(View.VISIBLE);

        if (receiveHeartBeatMode.getRobotType() == 7) {
            if (receiveHeartBeatMode.isElevatorMode()) {
                GlobalFlag.getInstance().setElevatorMode(true);
            }
            GlobalFlag.getInstance().setFlag(true);
        }
        clickButton(callTextView);
    }

    private void updateChargingState(int chargeState) {
        switch (chargeState) {
            case 1:
                isChargingState = false;
                ivChargingLogo.setVisibility(View.GONE);
                break;
            case 2:
            case 3:
                isChargingState = true;
                ivChargingLogo.setVisibility(View.VISIBLE);
                break;
            case 8:
                Timbers.w(this, "正在对接充电");
                break;
            default:
                if (chargeState > 8) {
                    Timbers.w(this, "对接充电失败");
                    ivChargingLogo.setVisibility(View.GONE);
                }
                break;
        }
    }

    private void updateBatteryLevel(int level) {
        batteryLevelProgress.setVisibility(View.VISIBLE);
        tvPercentage.setVisibility(View.VISIBLE);
        tvPercentage.setText(String.valueOf(level));

        if (level <= 20) {
            batteryLevelProgress.setProgressDrawable(ContextCompat.getDrawable(this, R.drawable.progress_red));
        } else if (level == 100 && isChargingState) {
            batteryLevelProgress.setProgressDrawable(ContextCompat.getDrawable(this, R.drawable.progress_green));
        } else {
            batteryLevelProgress.setProgressDrawable(ContextCompat.getDrawable(this, R.drawable.custom_progressbar));
        }

        if (level < 0 || level > 100) {
            batteryLevelProgress.setProgress(0);
        } else {
            batteryLevelProgress.setProgress(level);
        }
    }

    private void updateFlags(ReceiveHeartBeatMode receiveHeartBeatMode) {
        if (receiveHeartBeatMode.isMapping()) {
            Timbers.w(this, "正在建图中");
        }
        if (receiveHeartBeatMode.isLifting()) {
            Timbers.w(this, "正在进行顶升或对接卡板");
        }
    }

    private void updateDeviceAlias(String alias) {
        for (int i = 0; i < foregroundkeyList.size(); i++) {
            if (foregroundkeyList.get(i).equals(setHostName)) {
                foregroundkeyList.set(i, alias);

            }
        }
        SharedPreferences preferences = getSharedPreferences(Constants.DEVICE_LIST, MODE_PRIVATE);
        String jsonForegroundList = new Gson().toJson(foregroundkeyList);
        preferences.edit().putString(Constants.DEVICE_LIST_FF, jsonForegroundList).apply();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTaskResponse(Event.OnTaskResponse event) {
        if(!event.token.equals(Token)){
            ToastUtils.showShortToast(getString(R.string.token_check));
            Timbers.w(this, "token不相等，机器端："+event.token+" , 手机端："+Token);
            return;
        }
        Timbers.w(this, "手机端下发任务，机器端执行响应："+event.body);
        if (event.body.indexOf(':') != -1) {
            String result = event.body.substring(event.body.indexOf(':') + 1).trim();
            ToastUtils.showShortToast(result);
        } else {
            ToastUtils.showShortToast(event.body);
        }
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onReconnectSuccess(Event.OnReconnectSuccess event) {
        Timbers.w(this, "mqtt重新连接成功，恢复心跳");
        MqttClient.Subscribe(hostname);
        //断开后恢复心跳
        HeartBeatUtil.getInstance().stopHeartbeat();
        HeartBeatUtil.getInstance().startHeartbeat(this,hostname);
    }
}
