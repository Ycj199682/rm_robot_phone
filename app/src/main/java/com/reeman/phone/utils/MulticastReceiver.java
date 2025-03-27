package com.reeman.phone.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.reeman.phone.MainActivity;
import com.reeman.phone.R;
import com.reeman.phone.constant.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MulticastReceiver {
    private Context context;
    private DeviceListChangeListener listener;
    private String robotType;
    public interface DeviceListChangeListener {
        void onDeviceListChanged(List<String> deviceList);
    }

    public MulticastReceiver(Context context, DeviceListChangeListener listener) {
        this.context = context;
        this.listener = listener;
    }

    private boolean isListening = false;
    private Thread listeningThread;

    public void startListening() {
        stopListening(); // 先停止之前的监听
        isListening = true;
        listeningThread = new Thread(this::getDeviceList);
        listeningThread.start();
    }

    public void stopListening() {
        isListening = false;
        if (listeningThread != null) {
            listeningThread.interrupt();
            listeningThread = null;
        }
    }

    private void getDeviceList() {
        String multicastAddr = "239.0.0.1";
        int port = 7979;
        List<String> deviceList = new ArrayList<>();
        HashMap<String, Integer> messageMap = new HashMap<>();

        try {
            MulticastSocket socket = new MulticastSocket(port);
            InetAddress group = InetAddress.getByName(multicastAddr);
            socket.joinGroup(group);

            byte[] buffer = new byte[1024];
            while (!Thread.currentThread().isInterrupted()) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String initialMessage = new String(packet.getData(), 0, packet.getLength());

                try {
                    JSONObject jsonObject = new JSONObject(initialMessage);
                    String hostname = jsonObject.optString("hostname");
                    String alias = jsonObject.optString("alias");
                    String encryptKey = jsonObject.optString("key");
                    String token = jsonObject.optString("token");
                    int Type = jsonObject.optInt("robotType");
                    robotType = (Type == 9) ? Constants.robotTypeForklift : Constants.robotTypeAGV;
                    // 保存组播数据
                    saveDeviceInformation(hostname, alias, encryptKey, token,robotType);
                    if (!messageMap.containsKey(hostname)) {
                        messageMap.put(hostname, 1);
                        Timbers.w(context, "发现机器："+hostname+" , 机器类型："+Type);
                        deviceList.add(hostname);
                        if (listener != null) {
                            Executor executor = Executors.newSingleThreadExecutor();
                            CompletableFuture.runAsync(() -> listener.onDeviceListChanged(deviceList), executor);
                        }
                    } else {
                        int count = messageMap.get(hostname);
                        messageMap.put(hostname, count + 1);
                    }
                } catch (JSONException e) {
                    Timbers.w(context, "组播解析数据异常：:："+e.getMessage());
                }
           }
        } catch (IOException e) {
            Timbers.w(context, "网络连接组播异常：:："+e.getMessage());
        }
    }

    public void saveDeviceInformation(String hostname, String alias, String encryptKey, String token,String robotType) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("MulticastDevices", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(hostname + "_alias", alias);
        editor.putString(hostname + "_encryptKey", encryptKey);
        editor.putString(hostname + "_token", token);
        editor.putString(hostname + "_robotType", robotType);
        editor.apply();
    }
}
