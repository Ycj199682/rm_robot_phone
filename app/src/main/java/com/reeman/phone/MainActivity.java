package com.reeman.phone;

import static com.reeman.phone.adapter.MainAdapter.DEVICE_NAME_KEY;
import static com.reeman.phone.adapter.MainAdapter.PREFS_NAME;
import static com.reeman.phone.constant.Constants.FRONT_DEVICE_NAME;
import static com.reeman.phone.utils.CustomExceptionHandler.appendToFile;
import static com.reeman.phone.utils.Timbers.LOG_FILE_SUFFIX;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.codingending.popuplayout.PopupLayout;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.reeman.phone.adapter.LanguageAdapter;
import com.reeman.phone.adapter.MainAdapter;
import com.reeman.phone.adapter.NetDeviceAdapter;
import com.reeman.phone.call.MqttClient;
import com.reeman.phone.constant.Constants;
import com.reeman.phone.utils.BlurBuilder;
import com.reeman.phone.utils.CustomExceptionHandler;
import com.reeman.phone.utils.LoadingDialogUtil;
import com.reeman.phone.utils.LocalUtil;
import com.reeman.phone.utils.LogFilesAdapter;
import com.reeman.phone.utils.LogUtils;
import com.reeman.phone.utils.MulticastReceiver;
import com.reeman.phone.utils.Timbers;
import com.reeman.phone.utils.ToastUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements LanguageAdapter.OnItemClickListener , NetDeviceAdapter.OnItemSelectedListener, MulticastReceiver.DeviceListChangeListener{
    private RecyclerView recyclerView;
    private MainAdapter mainAdapter;
    private ArrayList<String> list = new ArrayList<>();
    private ArrayList<String> foregroundList = new ArrayList<>();
    private int currentLanguage = -1;
    private LanguageAdapter adapter;
    private TextView tvConfirmButton;
    private String deviceName;
    private NetDeviceAdapter netDeviceAdapter;
    private TextView tv_device_count;
    private ImageButton bt_set_name_ok;
    private ItemTouchHelper.SimpleCallback itemTouchHelperCallback;
    private ItemTouchHelper itemTouchHelper;
    private ImageView backgroundImageView;
    private TextView textViewTitle;
    private MulticastReceiver multicastReceiver;
    private AlertDialog dialogAdd;
    private int robotNumberSign= 0;
    private static final int REQUEST_CODE_PERMISSIONS = 1001;
    private AlertDialog logFilesDialog;
    private TextView viewLog;
    private TextView tv_line_log;
    private TextView changeName;
    private LogUtils logUtils = new LogUtils(this);
    private String robotTypes;
    private TextView crash_log;
    private TextView tv_crash_log;
    private static final String PREF_NAME = "AppPrefs";
    private static final String CODE_RUN_ONCE_KEY = "codeRunOnceKey";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //appendToFile(MainActivity.this, "APP启动，准备初始化，开始创建崩溃日志...","crashs_log.txt");
        Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler(this,"主界面"));
        // 初始化运行日志写入
        if (!logUtils.checkPermissions()) {
            logUtils.requestPermissions();
            onlyCrashLog();
        } else {
            onlyCrashLog();
            Timbers.w(this, "APP启动，准备初始化...");
        }
        // 初始化 UUID
        SharedPreferences deviceID = getSharedPreferences(Constants.DEVICE_ID, MODE_PRIVATE);
        String UUID = deviceID.getString(Constants.DEVICE_ID, null);
        if (UUID == null) {
            initUUID();
        }

        // 设置状态栏文本颜色
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        // 检查用户是否已选择过语言
        SharedPreferences preferences = getSharedPreferences(Constants.LANGUAGE_SET, MODE_PRIVATE);
        int selectedLanguage = preferences.getInt(Constants.LANGUAGE_SET, -1);
        currentLanguage = selectedLanguage;

        if (selectedLanguage == -1) {
            // 初始化语言设置
            initLanguage();
            // 用户尚未选择语言，跳转到语言选择界面
            Intent intent = new Intent(this, LanguageActivity.class);
            startActivity(intent);
            finish();
            return;
        } else {
            Locale localName = LocalUtil.getLocalName(currentLanguage);
            LocalUtil.changeAppLanguage(getResources(), localName);

            // 用户已经选择过语言，直接进入 MainActivity
            setContentView(R.layout.activity_main);
            initData();
            initViewData();
            ToastUtils.init(this);
        }

        // 检查网络连接
        if (!isNetworkConnected()) {
            View view = LayoutInflater.from(this).inflate(R.layout.layout_error_dialog_layout, null);
            TextView tvText = view.findViewById(R.id.tv_text);
            tvText.setText(getString(R.string.net_error));
            Timbers.w(this, "网络异常，请检查网络连接");
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(view).setCancelable(false);
            Button btnConfirm = view.findViewById(R.id.btn_confirm);
            AlertDialog dialog = builder.create();
            btnConfirm.setOnClickListener(v -> finish());
            dialog.show();
            return;
        }
        // 初始化 MQTT 连接
        new Thread(() -> MqttClient.getInstance(this)).start();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        initData();
        initViewData();
        MqttClient.getInstance(this);
        mainAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // 获取当前选择的语言
        SharedPreferences preferences = getSharedPreferences(Constants.LANGUAGE_SET, MODE_PRIVATE);
        int selectedLanguage = preferences.getInt(Constants.LANGUAGE_SET, -1);
        if (currentLanguage != selectedLanguage) {
            currentLanguage = selectedLanguage;
            Locale localName = LocalUtil.getLocalName(currentLanguage);
            LocalUtil.changeAppLanguage(getResources(), localName);
            // 重新初始化视图和数据
            initData();
            initViewData();
            mainAdapter.notifyDataSetChanged();
        }
    }

    private void initUUID() {
        new Thread(() -> {
            String uniqueID = UUID.randomUUID().toString();
            SharedPreferences preferences = getSharedPreferences(Constants.DEVICE_ID, MODE_PRIVATE);
            preferences.edit().putString(Constants.DEVICE_ID, uniqueID).apply();
        }).start();
    }

    private void initViewData() {
        recyclerView = findViewById(R.id.device_list);
        tv_device_count = findViewById(R.id.tv_device_count);
        bt_set_name_ok = findViewById(R.id.bt_set_name_ok);
        textViewTitle = findViewById(R.id.textViewTitle);

        // 添加设备
        ImageButton addButton = findViewById(R.id.add_device);
        addButton.setOnClickListener(view -> {
            setUpItemTouchHelper(false);
            mainAdapter.refreshEdit();
            bt_set_name_ok.setVisibility(View.GONE);
            mainAdapter.setClickEnabled(true);
            showAddDataDialog();
        });

        // 菜单按钮
        ImageButton setButton = findViewById(R.id.set_button);
        setButton.setOnClickListener(view -> {
            setUpItemTouchHelper(false);
            mainAdapter.refreshEdit();
            bt_set_name_ok.setVisibility(View.GONE);
            mainAdapter.setClickEnabled(true);
            showSettingsMenu(view);
        });

        bt_set_name_ok.setOnClickListener(view -> {
            setUpItemTouchHelper(false);
            mainAdapter.refreshEdit();
            startAnimation(false);
            mainAdapter.setClickEnabled(true);
            bt_set_name_ok.setVisibility(View.GONE);
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        mainAdapter = new MainAdapter(list, foregroundList, this);
        StaggeredGridLayoutManager deviceListManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(deviceListManager);
        recyclerView.setAdapter(mainAdapter);
        robotNumberSign = mainAdapter.getItemCount();
        tv_device_count.setText(String.format(getString(R.string.set_number_devices), robotNumberSign));
    }

    public void onButtonClicked(View view) {
        // 点击按钮时触发，可以不用写任何代码
    }

    private void startAnimation(boolean anim) {
        if (anim) {
            Animation animationIn = AnimationUtils.loadAnimation(this, R.anim.button_fade_in);
            bt_set_name_ok.startAnimation(animationIn);
        } else {
            Animation animationOut = AnimationUtils.loadAnimation(this, R.anim.button_fade_out);
            bt_set_name_ok.startAnimation(animationOut);
        }
    }

    private void showSettingsMenu(View anchor) {
        // 菜单界面
        View popupView = LayoutInflater.from(MainActivity.this).inflate(R.layout.set_popup_menu, null);
        final PopupWindow popupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setAnimationStyle(R.style.PopupAnimation);
        TextView changeLanguage = popupView.findViewById(R.id.change_language);
        changeName = popupView.findViewById(R.id.change_name);
        viewLog = popupView.findViewById(R.id.view_log);
        crash_log = popupView.findViewById(R.id.crash_log);
        //查看崩溃日志
        crash_log.setOnClickListener(v -> {
            // 获取应用内部文件目录
            File dir = MainActivity.this.getFilesDir();
            //创建一个指向日志文件的 File 对象
            File logFile = new File(dir, "crashs_log.txt");
            // 调用 viewLogFile 方法
            displayLogFileContent(logFile);
        });

        tv_line_log = popupView.findViewById(R.id.tv_line_log);
        tv_crash_log = popupView.findViewById(R.id.tv_crash_log);
        // 设置语言
        changeLanguage.setOnClickListener(v -> {
            popupWindow.dismiss();
            showLanguageSelection();
        });
        // 设备管理
        changeName.setOnClickListener(v -> {
            if(mainAdapter.getItemCount()==0){
                ToastUtils.showShortToast(getString(R.string.robot_listData));
                return;
            }
            popupWindow.dismiss();
            //动画跳动
            startAnimation(true);
            bt_set_name_ok.setVisibility(View.VISIBLE);
            //侧滑删除
            setUpItemTouchHelper(true);
            mainAdapter.setClickEnabled(false);
            mainAdapter.showAllTextViews();
        });
        //显示日志界面
        Handler handler = new Handler();
        Runnable longClickRunnable = () -> {
            viewLog.setVisibility(View.VISIBLE);
            tv_line_log.setVisibility(View.VISIBLE);
            crash_log.setVisibility(View.VISIBLE);
            tv_crash_log.setVisibility(View.VISIBLE);
            ToastUtils.showShortToast(getString(R.string.open_log_look));
            Timbers.w(this, "日志查看开启");
        };
        changeName.setOnLongClickListener(view -> {
            // 延迟5秒执行长按操作
            handler.postDelayed(longClickRunnable, 5000);
            return true; // 返回true表示消耗了长按事件
        });
        // 在释放长按时移除Runnable
        changeName.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP
                    || event.getAction() == MotionEvent.ACTION_CANCEL) {
                handler.removeCallbacks(longClickRunnable);
            }
            return false;
        });
        //查看日志
        viewLog.setOnClickListener(v -> {
            popupWindow.dismiss();
            logUtils.setMachineNames(list);
            logUtils.showLogFilesDialog();
        });

        // 设置背景使其可以点击外部区域时关闭
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(true);

        // 获取屏幕宽度和高度
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;
        // 获取锚点位置
        int[] location = new int[2];
        anchor.getLocationOnScreen(location);
        // 计算相对屏幕比例的偏移量
        float xOffsetRatio = 0.47f; // 相对于屏幕宽度的47%
        float yOffsetRatio = 0.01f; // 相对于屏幕高度的1%
        // 根据屏幕比例计算实际偏移量
        int xOffset = (int) (screenWidth * xOffsetRatio);
        int yOffset = (int) (screenHeight * yOffsetRatio);
        // 调整 xOffset 和 yOffset 以相对于锚点位置显示
        xOffset = screenWidth - location[0] - popupWindow.getWidth() + xOffset;
        yOffset = location[1] + anchor.getHeight() + yOffset;
        // 显示 PopupWindow
        popupWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, xOffset, yOffset);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        logUtils.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    private void showLanguageSelection() {
        View popupView = LayoutInflater.from(MainActivity.this).inflate(R.layout.activity_language, null);
        PopupLayout popupLayout = PopupLayout.init(MainActivity.this, popupView);

        tvConfirmButton = popupView.findViewById(R.id.confirm_button);
        tvConfirmButton.setOnClickListener(view -> {
            SharedPreferences preferences = getSharedPreferences(Constants.LANGUAGE_SET, MODE_PRIVATE);
            int selectedLanguage = preferences.getInt(Constants.LANGUAGE_SET, -1);
            if (currentLanguage == selectedLanguage) {
                popupLayout.dismiss();
                return;
            }
            //点击确认按钮
            SharedPreferences sp = getSharedPreferences(Constants.LANGUAGE_SET, MODE_PRIVATE);

            sp.edit().putInt(Constants.LANGUAGE_SET, currentLanguage).apply();
            tvConfirmButton.setText(R.string.confirm);
            // 重新启动MainActivity以更新语言设置
            LocalUtil.changeAppLanguage(getResources(), LocalUtil.getLocalName(currentLanguage));
            Intent intent = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish(); // 关闭当前活动
            }
        });

        // 初始化RecyclerView，设置布局管理器和适配器
        RecyclerView rvLanguageList = popupView.findViewById(R.id.language_list);
        rvLanguageList.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        adapter = new LanguageAdapter(LocalUtil.getLocal(), MainActivity.this);
        rvLanguageList.setAdapter(adapter);
        adapter.setOnItemClickListener(MainActivity.this); // 设置监听器

        popupLayout.setDismissListener(new PopupLayout.DismissListener() {
            @Override
            public void onDismiss() {
                SharedPreferences preferences = getSharedPreferences(Constants.LANGUAGE_SET, MODE_PRIVATE);
                int selectedLanguage = preferences.getInt(Constants.LANGUAGE_SET, -1);
                if (currentLanguage != selectedLanguage) {
                    currentLanguage = selectedLanguage;
                    LocalUtil.changeAppLanguage(getResources(), LocalUtil.getLocalName(selectedLanguage));
                }
                popupLayout.dismiss();
            }
        }); // 添加监听器

        popupLayout.setUseRadius(true);
        popupLayout.show(PopupLayout.POSITION_RIGHT);
    }

    private void displayLogFileContent(File logFile) {
        try {
            //显示崩溃日志
            BufferedReader reader = new BufferedReader(new FileReader(logFile));
            StringBuilder contentBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                contentBuilder.append(line).append("\n");
            }
            reader.close();
            String content = contentBuilder.toString();

            // 或者使用 AlertDialog 来显示内容
            showContentInDialog(content);
        } catch (FileNotFoundException e) {
            Log.e("MainActivity", "File not found: " + logFile.getAbsolutePath(), e);
        } catch (IOException e) {
            Log.e("MainActivity", "Error reading file: " + logFile.getAbsolutePath(), e);
        }
    }


    // 可选的方法，用于使用 AlertDialog 显示文件内容
    private void showContentInDialog(String content) {
        // 创建一个 ScrollView 包含 TextView
        ScrollView scrollView = new ScrollView(this);
        TextView textView = new TextView(this);
        textView.setText(content);
        textView.setPadding(14, 14, 14, 14); // 添加内边距
        textView.setTextSize(14); // 设置字体大小
        textView.setTextColor(Color.BLACK); // 设置字体颜色
        textView.setMovementMethod(new ScrollingMovementMethod()); // 允许滚动
        textView.setLinksClickable(true); // 允许点击链接
        textView.setLinkTextColor(Color.BLUE); // 设置链接颜色
        scrollView.addView(textView);

        // 创建 AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Crash Log Content");
        builder.setView(scrollView); // 使用 ScrollView 作为内容视图
        builder.setPositiveButton("OK", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void setUpItemTouchHelper(boolean allowSwipe) {
        if(itemTouchHelperCallback!=null){
            if(itemTouchHelper!=null){
                itemTouchHelper.attachToRecyclerView(null);
            }
        }
        itemTouchHelperCallback = new ItemTouchHelper.SimpleCallback(0, allowSwipe ? (ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) : 0) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }
            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    String removedItem = list.get(position);
                    SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                    preferences.edit().remove(DEVICE_NAME_KEY + removedItem).apply();
                    list.remove(position);
                    foregroundList.remove(position);
                    saveData();
                    mainAdapter.notifyDataSetChanged();
                    tv_device_count.setText(String.format(getString(R.string.set_number_devices), mainAdapter.getItemCount()));
                    ToastUtils.showShortToast(getString(R.string.data_deleted));
                    Timbers.w(MainActivity.this, "已删除设备："+removedItem);
                    if(mainAdapter.getItemCount()==0){
                        bt_set_name_ok.setVisibility(View.GONE);
                    }
                }
            }
            @Override
            public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                int swipeDirs = allowSwipe ? super.getSwipeDirs(recyclerView, viewHolder) : 0;
                return swipeDirs;
            }
        };
        itemTouchHelper = new ItemTouchHelper(itemTouchHelperCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }


    private void initData() {
        SharedPreferences preferences = getSharedPreferences(Constants.DEVICE_LIST, MODE_PRIVATE);
        // 获取 JSON 字符串
        String jsonList = preferences.getString(Constants.DEVICE_LIST, null);
        String jsonListFF = preferences.getString(Constants.DEVICE_LIST_FF, null);
        if (jsonListFF != null) {
            foregroundList= new Gson().fromJson(jsonListFF, new TypeToken<ArrayList<String>>() {
            }.getType());
        } else {
            foregroundList = new ArrayList<>();
        }
        // 将 JSON 字符串转换回 List
        if (jsonList != null) {
            list = new Gson().fromJson(jsonList, new TypeToken<ArrayList<String>>() {
            }.getType());
        } else {
            list = new ArrayList<>();
        }
    }


    private void showAddDataDialog() {
        //添加设备
        SharedPreferences preferences = getSharedPreferences(Constants.LANGUAGE_SET, MODE_PRIVATE);
        int selectedLanguage = preferences.getInt(Constants.LANGUAGE_SET, -1);
        LocalUtil.changeAppLanguage(getResources(), LocalUtil.getLocalName(selectedLanguage));
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        //扫描同一网段下的设备
        View view = getLayoutInflater().inflate(R.layout.layout_choose_device_list, null);
        Timbers.w(MainActivity.this, "开始扫描网络中的设备");
        builder.setView(view);
        RecyclerView rvDeviceList = view.findViewById(R.id.net_device_list);
        netDeviceAdapter = new NetDeviceAdapter(this);
        netDeviceAdapter.setOnItemSelectedListener(this);

        multicastReceiver = new MulticastReceiver(this,this);

        rvDeviceList.setLayoutManager(new LinearLayoutManager(this));
        rvDeviceList.setAdapter(netDeviceAdapter);
        multicastReceiver.startListening();

        Button btnCancel = view.findViewById(R.id.btn_cancel);
        Button btnConfirm = view.findViewById(R.id.btn_confirm);
        Button btnQrcAdd = view.findViewById(R.id.btn_qrc_add);
        // 声明 AlertDialog 对象
        dialogAdd = builder.create();
        dialogAdd.setCancelable(false);
        Bitmap bitmap = takeScreenshot(MainActivity.this);
        // 模糊处理
        Bitmap blur = BlurBuilder.blur(MainActivity.this, bitmap);
        backgroundImageView = findViewById(R.id.background_image);
        backgroundImageView.setImageBitmap(blur);
        backgroundImageView.setVisibility(View.VISIBLE);
        dialogAdd.show();
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            btnQrcAdd.setOnClickListener(v -> {
                Timbers.w(this, "开启二维码扫描");
                IntentIntegrator intentIntegrator = new IntentIntegrator(MainActivity.this);
                intentIntegrator.setBeepEnabled(true);
                //启动自定义的扫描活动
                intentIntegrator.setCaptureActivity(ScanActivity.class);
                intentIntegrator.initiateScan();
            });
        } else {
            btnQrcAdd.setBackgroundResource(R.drawable.bg_common_button_inactive);
        }
        btnCancel.setOnClickListener(v -> {
            multicastReceiver.stopListening();
            backgroundImageView.setVisibility(View.GONE);
            dialogAdd.dismiss(); // 关闭对话框
        });

        btnConfirm.setOnClickListener(v -> {
            if (list.contains(deviceName)) {
                ToastUtils.showShortToast(getString(R.string.device_exist));
                return;
            }
            if (deviceName == null) {
                ToastUtils.showShortToast(getString(R.string.device_not_selected));
                return;
            }
            SharedPreferences sharedPreferences = getSharedPreferences("MulticastDevices", Context.MODE_PRIVATE);
            String alias = sharedPreferences.getString(deviceName + "_alias", deviceName);
            foregroundList.add(alias);
            list.add(deviceName);
            saveData();
            mainAdapter.notifyDataSetChanged();
            tv_device_count.setText(String.format(getString(R.string.set_number_devices), mainAdapter.getItemCount()));
            ToastUtils.showShortToast(getString(R.string.data_added_success));
            Timbers.w(MainActivity.this, "添加设备成功："+deviceName);
            backgroundImageView.setVisibility(View.GONE);
            dialogAdd.dismiss(); // 关闭对话框
        });
        dialogAdd.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //二维码扫描结果
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {
                // 用户成功扫描到二维码
                String QRCodeData= result.getContents();
                JSONObject jsonObject = null;
                if (isJson(QRCodeData)) {
                    try {
                        jsonObject = new JSONObject(QRCodeData);
                    } catch (JSONException e) {
                        Timbers.w(MainActivity.this, "JSON解析错误: " + e.getMessage()+",/n具体数据为："+QRCodeData);
                        return; // 提前返回以避免后续操作
                    }
                    String hostname = jsonObject.optString("hostname");
                    String alias = jsonObject.optString("alias");
                    String encryptKey = jsonObject.optString("key");
                    String token = jsonObject.optString("token");
                    int Type = jsonObject.optInt("robotType");

                    if (list.contains(hostname)) {
                        ToastUtils.showShortToast(getString(R.string.device_exist));
                        return;
                    }
                    if (hostname == null) {
                        ToastUtils.showShortToast(getString(R.string.qr_code_scanning));
                        return;
                    }
                    robotTypes = (Type == 9) ? Constants.robotTypeForklift : Constants.robotTypeAGV;
                    dialogAdd.dismiss();
                    // 保存扫描到的二维码信息
                    multicastReceiver.saveDeviceInformation(hostname, alias, encryptKey, token,robotTypes);
                    SharedPreferences sharedPreferences = getSharedPreferences("MulticastDevices", Context.MODE_PRIVATE);
                    String QRalias = sharedPreferences.getString(hostname + "_alias", hostname);
                    foregroundList.add(QRalias);
                    list.add(hostname);
                    saveData();
                    mainAdapter.notifyDataSetChanged();
                    tv_device_count.setText(String.format(getString(R.string.set_number_devices), mainAdapter.getItemCount()));
                    ToastUtils.showShortToast(getString(R.string.data_added_success));
                    Timbers.w(MainActivity.this, "添加设备成功："+hostname);
                    backgroundImageView.setVisibility(View.GONE);
                }else {
                    ToastUtils.showShortToast(getString(R.string.invalid_qr_code));
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    // 检查字符串是否是有效的JSON格式
    private boolean isJson(String data) {
        try {
            new JSONObject(data);
        } catch (JSONException e) {
            return false;
        }
        return true;
    }

    public void initLanguage() {
        SharedPreferences preferences = getSharedPreferences(Constants.LANGUAGE_SET, MODE_PRIVATE);
        int selectedLanguage = preferences.getInt(Constants.LANGUAGE_SET, -1);

        if (selectedLanguage == -1) {
            // 默认设置为中文
            selectedLanguage = 1; // 假设Constants.LANGUAGE_CHINESE是语言代码常量
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt(Constants.LANGUAGE_SET, selectedLanguage);
            editor.apply();
        }

        Locale localName = LocalUtil.getLocalName(selectedLanguage);
        LocalUtil.changeAppLanguage(getResources(), localName);
    }

    private void onlyCrashLog(){
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        boolean hasRun = prefs.getBoolean(CODE_RUN_ONCE_KEY, false);
        if (!hasRun) {
            appendToFile(MainActivity.this, "APP第一次启动，准备初始化，开始创建崩溃日志...","crashs_log.txt");
            // 这里的代码只运行一次
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(CODE_RUN_ONCE_KEY, true);
            editor.apply(); // 应用更改
        }
    }

    @Override
    public void onItemClick(int position) {
        currentLanguage = position;
        //点击事件
        Locale localName = LocalUtil.getLocalName(position);
        LocalUtil.changeAppLanguage(getResources(), localName);
        tvConfirmButton.setText(R.string.confirm);
    }

    public boolean isNetworkConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
        return false;
    }

    public void saveData() {
        SharedPreferences preferences = getSharedPreferences(Constants.DEVICE_LIST, MODE_PRIVATE);
        String jsonList = new Gson().toJson(list);
        String jsonForegroundList = new Gson().toJson(foregroundList);
        preferences.edit().putString(Constants.DEVICE_LIST, jsonList).apply();
        preferences.edit().putString(Constants.DEVICE_LIST_FF, jsonForegroundList).apply();
    }


    @Override
    public void onItemSelected(String selectedItem) {
        deviceName = selectedItem;
    }

    @Override
    public void onDeviceListChanged(List<String> deviceList) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                netDeviceAdapter.setData(deviceList);
            }
        });
    }

    private Bitmap takeScreenshot(Activity activity) {
        View view = activity.getWindow().getDecorView();
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();
        Bitmap b1 = view.getDrawingCache();
        // 获取状态栏高度
        Rect statusBar = new Rect();
        activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(statusBar);
        int statusBarHeight = statusBar.top;
        // 检查 statusBarHeight 是否有效
        if (statusBarHeight < 0) {
            statusBarHeight = 0; // 设置为0，以防万一
        }
        // 获取屏幕长和高
        int width = activity.getResources().getDisplayMetrics().widthPixels;
        int height = activity.getResources().getDisplayMetrics().heightPixels;
        // 创建 Bitmap 时扣除状态栏高度
        // 确保不会超出 b1 的高度
        int safeHeight = Math.min(height, b1.getHeight() - statusBarHeight);
        // 检查 safeHeight 是否有效
        if (safeHeight <= 0) {
            view.destroyDrawingCache();
            return null; // 返回 null 表示无法创建截图
        }
        Bitmap bmp = Bitmap.createBitmap(b1, 0, statusBarHeight, width, safeHeight);
        view.destroyDrawingCache();
        return bmp;
    }
}
