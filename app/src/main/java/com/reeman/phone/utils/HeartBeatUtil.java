package com.reeman.phone.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.reeman.phone.constant.Constants;
import com.reeman.phone.event.Event;

import org.greenrobot.eventbus.EventBus;

import static android.content.Context.MODE_PRIVATE;

public class HeartBeatUtil {

    private static HeartBeatUtil instance;
    private Handler handler;
    private Runnable heartbeatTask;
    private Context context;
    private static final long HEARTBEAT_INTERVAL = 4000;
    private String Hostname;

    private HeartBeatUtil() {
        handler = new Handler(Looper.getMainLooper());
        heartbeatTask = new Runnable() {
            @Override
            public void run() {
                //获取token
                SharedPreferences sharedPreferences = context.getSharedPreferences("MulticastDevices", Context.MODE_PRIVATE);
                String token = sharedPreferences.getString(Hostname + "_token", null);
                EventBus.getDefault().post(Event.getOnHeartBeatEvent(token));
                // 任务完成后，再次调度下一次任务
                handler.postDelayed(this, HEARTBEAT_INTERVAL);
            }
        };
    }

    public static HeartBeatUtil getInstance() {
        if (instance == null) {
            synchronized (HeartBeatUtil.class) {
                if (instance == null) {
                    instance = new HeartBeatUtil();
                }
            }
        }
        return instance;
    }

    public void startHeartbeat(Context context,String hostname) {
        Log.d("mylog","开始第一次心跳");
        Hostname=hostname;
        instance.context = context;
        // 立即执行一次
        handler.post(heartbeatTask);
    }

    public void stopHeartbeat() {
        // 停止任务
        instance = null;
        handler.removeCallbacks(heartbeatTask);
    }
}
