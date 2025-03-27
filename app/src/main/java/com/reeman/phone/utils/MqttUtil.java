package com.reeman.phone.utils;


import android.os.Build;
import android.util.Log;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.RequiresApi;

public class MqttUtil {

    private Mqtt3AsyncClient mqttClient;
    private OnMessageReceivedListener messageReceivedListener;

    public void setOnMessageReceivedListener(OnMessageReceivedListener listener) {
        this.messageReceivedListener = listener;
    }

    public MqttUtil(String serverUri, String clientId) {
        mqttClient = MqttClient.builder()
                .useMqttVersion3()
                .identifier(clientId)
                .serverHost(serverUri)
                .buildAsync();
    }

    /**
     * 连接mqtt服务器
     *
     * @param username
     * @param password
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void connect(String username, String password) {
        mqttClient.connectWith()
                .simpleAuth()
                .username(username)
                .password((password).getBytes())
                .applySimpleAuth()
                .send()
                .whenComplete((connAck, throwable) -> {
                    if (throwable != null) {
                        // handle failure
                        Log.e("TAG", "connect: 连接失败" + throwable);
                    } else {
                        // setup subscribes or start publishing
                        Log.e("TAG", "connect: 连接成功");
                    }
                });
    }

    /**
     * 订阅服务器
     *
     * @param topic
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void subscribe(String topic) {
        boolean hasSentMessage = false;
        mqttClient.subscribeWith()
                .topicFilter(topic)
                .qos(MqttQos.AT_LEAST_ONCE)
                .callback(publish -> {
                    // Process the received message
                    String receivedMessage = new String(publish.getPayloadAsBytes());
                    receivedMessage = receivedMessage.replace("[", "").replace("]", ""); // 去除方括号
                    String[] stringArray = receivedMessage.split(",");
                    List<String> stringList = new ArrayList<>(Arrays.asList(stringArray));
                    Log.e("TAG", "subscribe: " + stringList);
                    //收到订阅消息过后回调出去
                    if (messageReceivedListener != null) {
                        messageReceivedListener.onMessageReceived(stringList);
                    }

                })
                .send()
                .whenComplete((subAck, throwable) -> {
                    if (throwable != null) {
                        // Handle failure to
                        Log.e("TAG", "subscribe: 订阅失败");
                    } else {
                        // Handle successful subscription, e.g. logging or incrementing a metric
                        Log.e("TAG", "subscribe: 订阅成功");
                    }
                });
    }

    /**
     * 发布消息
     *
     * @param topic
     * @param message
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void publish(String topic, String message) {
        mqttClient.publishWith()
                .topic(topic)
                .payload(message.getBytes())
                .qos(MqttQos.AT_LEAST_ONCE)
                .send()
                .whenComplete((publish, throwable) -> {
                    if (throwable != null) {
                        // handle failure to publish
                        Log.e("TAG", "publish: 发布失败" + throwable);
                    } else {
                        // handle successful publish, e.g. logging or incrementing a metric
                        Log.e("TAG", "publish: 发布成功" + message);
                    }
                });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void connectAndPublish(String username, String password, String topic, String message) {
        mqttClient.connectWith()
                .simpleAuth()
                .username(username)
                .password((password).getBytes())
                .applySimpleAuth()
                .send()
                .whenComplete((connAck, throwable) -> {
                    if (throwable != null) {
                        // 处理连接失败
                        Log.e("TAG", "connect: 连接失败" + throwable);
                    } else {
                        // 连接成功后触发消息发布
                        Log.e("TAG", "connect: 连接成功");
                        publish(topic, message); // 在连接成功后触发消息发布
                    }
                });
    }

    /**
     * 关闭链接
     */
    public void disconnect() {
        mqttClient.toAsync().disconnect();
    }

    public interface OnMessageReceivedListener {
        void onMessageReceived(List<String> messageList);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void connectAndSendMessage(String username, String password, String deviceName, String topic, String messageToSend) {
        mqttClient.connectWith()
                .simpleAuth()
                .username(username)
                .password((password).getBytes())
                .applySimpleAuth()
                .send()
                .whenComplete((connAck, throwable) -> {
                    if (throwable != null) {
                        // handle failure
                        Log.e("TAG", "connect: 连接失败" + throwable);
                    } else {
                        // setup subscribes or start publishing
                        Log.e("TAG", "connect: 连接成功");

                        // 在连接成功后发送消息
                        publish(topic, messageToSend);
                    }
                });
    }
    public void connectAndSubscribe(String username, String password, String topic) {
        mqttClient.connectWith()
                .simpleAuth()
                .username(username)
                .password((password).getBytes())
                .applySimpleAuth()
                .send()
                .whenComplete((connAck, throwable) -> {
                    if (throwable != null) {
                        // handle failure
                        Log.e("TAG", "connect: 连接失败" + throwable);
                    } else {
                        // setup subscribes or start publishing
                        Log.e("TAG", "connect: 连接成功");
                        subscribe(topic);
                    }
                });
    }

}
