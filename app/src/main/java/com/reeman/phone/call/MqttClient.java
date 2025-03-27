package com.reeman.phone.call;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.reeman.phone.R;
import com.reeman.phone.constant.Constants;
import com.reeman.phone.event.Event;
import com.reeman.phone.mode.CallingModeWithMAC;
import com.reeman.phone.mode.PublicHeatBeatMode;
import com.google.gson.Gson;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.message.auth.Mqtt5SimpleAuth;
import com.reeman.phone.utils.Timbers;
import com.reeman.phone.utils.ToastUtils;

import org.greenrobot.eventbus.EventBus;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class MqttClient {
    private static volatile Mqtt5Client server;
    private static Gson gson;
    private static String topic;
    private static ScheduledExecutorService executorService;
    private static final int MAX_RECONNECT_ATTEMPTS = 3;
    private static int currentReconnectAttempts = 0;
    private static int currentDelay = 0;
    private static Context mcontext;
    private static boolean isConnected = false; // 连接状态标志位

    public static Mqtt5Client getInstance(Context con) {
        if (server == null || !isConnected) {
            Log.d("mylog", "开始连接mqtt");
            synchronized (Mqtt5Client.class) {
                gson = new Gson();
                mcontext = con;
                if (server == null || !isConnected) {
                    // 定义 MQTT 认证信息（用户名和密码）
                    String username = Constants.USER_NAME;
                    String password = Constants.PASSWORD;

                    // 创建 MQTT 认证对象
                    Mqtt5SimpleAuth auth = Mqtt5SimpleAuth.builder()
                            .username(username)
                            .password(password.getBytes(StandardCharsets.UTF_8)) // 将密码转换为字节数组
                            .build();
                    server = Mqtt5Client.builder()
                            .identifier(UUID.randomUUID().toString())
                            .serverHost(Constants.SERVER_URLS)
                            .serverPort(Constants.SERVER_PORT)
                            .simpleAuth(auth)
                            .addConnectedListener(context -> {
                                Log.d("mylog", "mqtt连接成功");
                                isConnected = true;
                                EventBus.getDefault().post(Event.getOnReconnectSuccess("重新连接成功"));
                                if(currentReconnectAttempts!=0){
                                    Timbers.w(con, "重新连接成功");
                                    ToastUtils.showShortToast( mcontext.getString(R.string.mqtt_reconnect_successfully));
                                }
                                currentReconnectAttempts = 0;
                                currentDelay = 0;
                            })
                            .addDisconnectedListener(context -> {
                                isConnected = false;
                                if (currentReconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                                    Timbers.w(con, "连接断开开始重连第" + currentReconnectAttempts + "次");
                                    scheduleReconnect();
                                } else {
                                    Timbers.w(con, "到达最大次数,mqtt连接失败");
                                    stopReconnecting();
                                    if (mcontext instanceof Activity && !((Activity) mcontext).isFinishing() && !((Activity) mcontext).isDestroyed()) {
                                        ((Activity) mcontext).runOnUiThread(() -> {
                                            AlertDialog.Builder builder = new AlertDialog.Builder(mcontext);
                                            View view = LayoutInflater.from(mcontext).inflate(R.layout.layout_error_dialog_layout, null);
                                            builder.setView(view);
                                            AlertDialog dialog = builder.create();
                                            dialog.setCancelable(false);
                                            Button button = view.findViewById(R.id.btn_confirm);
                                            TextView txView = view.findViewById(R.id.tv_text);
                                            button.setText(mcontext.getString(R.string.reconnect));
                                            txView.setText(mcontext.getString(R.string.manual_connect));
                                            button.setOnClickListener(v -> {
                                                dialog.dismiss();
                                                scheduleReconnect();
                                            });
                                            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                                            dialog.show();
                                        });
                                    } else {
                                        Timbers.w(con, "Activity 已经销毁或不可用，无法显示对话框");
                                    }
                                }
                            })
                            .build();
                    server.toBlocking().connectWith()
                            .cleanStart(false)
                            .sessionExpiryInterval(TimeUnit.HOURS.toSeconds(1))
                            .send();
                }
            }
        }
        return server;
    }

    public static void disconnect() {
        if (isConnected) {
            Mqtt5AsyncClient mqtt5AsyncClient = server.toAsync();
            mqtt5AsyncClient.unsubscribeWith().topicFilter(topic).send();
            mqtt5AsyncClient.disconnect();
            isConnected = false;
        }
    }

    public static void Subscribe(String hostname) {
        SharedPreferences sharedPreferences = mcontext.getSharedPreferences("MulticastDevices", Context.MODE_PRIVATE);
        String robotType = sharedPreferences.getString(hostname + "_robotType", null);
        if(robotType.equals(Constants.robotTypeAGV)){
            topic = "reeman/calling/robot/" + hostname + "/"+"v2"+"/#";
        }else if(robotType.equals(Constants.robotTypeForklift)){
            topic = "reeman/calling/robot/" + hostname + "/"+"forklift"+"/#";
        }else if(topic==null){
            Timbers.w(mcontext, "接收心跳时mqtt主题加载为空");
            ToastUtils.showShortToast( mcontext.getString(R.string.load_error));
            return;
        }
        server.toAsync().subscribeWith()
                .topicFilter(topic)
                .callback(publish -> {
                    if (publish != null) {
                        try {
                            String receivedTopic = publish.getTopic().toString();
                            byte[] payloadBytes = publish.getPayloadAsBytes();
                            if (payloadBytes != null) {
                                String payload = new String(payloadBytes, StandardCharsets.UTF_8);
                                EventBus.getDefault().post(Event.getOnMqttPayloadEvent(receivedTopic, payload));
                            } else {
                                Timbers.w(mcontext, "收到具有空有效载荷的MQTT消息");
                            }
                        } catch (Exception e) {
                            Timbers.w(mcontext,"处理MQTT消息时出错");
                        }
                    } else {
                        Timbers.w(mcontext, "收到空MQTT消息");
                    }
                })
                .send();
    }

    public static void Publish(String topic, String payload) {
        if (isConnected) {
            server.toAsync().publishWith()
                    .topic(topic)
                    .payload(payload.getBytes(StandardCharsets.UTF_8))
                    .send();
            Timbers.w(mcontext, "手机端发送主题：" + topic);
            Timbers.w(mcontext, "手机端发送消息：" + payload);
        } else {
            Timbers.w(mcontext, "MQTT未连接，无法发布消息");
            ToastUtils.showIconToast( mcontext.getString(R.string.mqtt_not));
        }
    }

    public static void heartBeat(String hostname, String macAddress) {
        Log.d("mylog", "isConnected:" + isConnected);
        if (isConnected) {
            SharedPreferences sharedPreferences = mcontext.getSharedPreferences("MulticastDevices", Context.MODE_PRIVATE);
            String robotType = sharedPreferences.getString(hostname + "_robotType", null);
            String topic = null;
            if(robotType.equals(Constants.robotTypeAGV)){
                topic = Topic.AGVtopicPublicHeartBeat(hostname);
            }else if(robotType.equals(Constants.robotTypeForklift)){
                topic = Topic.topicPublicHeartBeat(hostname);
            }else if(topic==null){
                Timbers.w(mcontext, "发起心跳时，根据不同机器类型获取的mqtt主题加载为空");
                ToastUtils.showShortToast( mcontext.getString(R.string.load_error));
                return;
            }
            PublicHeatBeatMode publicHeatBeatMode = new PublicHeatBeatMode(macAddress);
            String messageToPublish = gson.toJson(publicHeatBeatMode);
            Log.d("mylog", "发起心跳:" + topic+",消息："+ messageToPublish);
            server.toAsync().publishWith()
                    .topic(topic)
                    .payload(messageToPublish.getBytes(StandardCharsets.UTF_8))
                    .send();
        } else {
            Timbers.w(mcontext, "MQTT未连接，无法发送心跳");
            if (mcontext instanceof Activity) {
                Activity activity = (Activity) mcontext;
                if (!activity.isFinishing() && !activity.isDestroyed()) {
                    ToastUtils.showIconToast(mcontext.getString(R.string.mqtt_not_line));
                }
            }
        }
    }

    private static void scheduleReconnect() {
        currentReconnectAttempts++;
        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.schedule(MqttClient::reconnect, calculateDelay(), TimeUnit.SECONDS);
    }

    private static long calculateDelay() {
        long delay = 2; // 每次重连的固定延迟为2秒
        return delay;
    }

    private static void reconnect() {
        server.toBlocking().connect();
    }

    private static void stopReconnecting() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
