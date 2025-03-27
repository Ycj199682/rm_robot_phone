package com.reeman.phone.call;

import android.util.Log;

public class Topic {

    /**
     * 手机端获取呼叫模式点位
     * @param hostname
     * @return
     */
    public static String topicReceiveRequestCallingModelPoints(String hostname){
        return "reeman/calling/phone/" + hostname + "/forklift/points/request/calling_model";
    }

    /**
     * 手机端下发呼叫模式任务
     * @param hostname
     * @return
     */
    public static String topicReceiveCallingModelTask(String hostname){
        return "reeman/calling/phone/" + hostname + "/forklift/task/calling_model";
    }

    /**
     * 机器人端推送呼叫模式点位
     * @param hostname
     * @return
     */
    public static String topicPublishCallingModelPoints(String hostname){
        return "reeman/calling/robot/"+hostname+"/forklift/points/response/calling_model";
    }

    /**
     * 手机端获取自动模式点位
     * @param hostname
     * @return
     */
    public static String topicReceiveRequestNormalModelPoints(String hostname){
        return "reeman/calling/phone/" + hostname + "/forklift/points/request/auto_model";
    }

    /**
     * 手机端下发自动模式任务
     * @param hostname
     * @return
     */
    public static String topicReceiveNormalModelTask(String hostname){
        return "reeman/calling/phone/" + hostname + "/forklift/task/auto_model";
    }

    /**
     * 机器人端推送自动模式点位
     * @param hostname
     * @return
     */
    public static String topicPublishNormalModelPoints(String hostname){
        return "reeman/calling/robot/"+hostname+"/forklift/points/response/auto_model";
    }

    /**
     * 手机端获取本地任务列表
     * @param hostname
     * @return
     */
    public static String topicReceiveRequestRouteModelPoints(String hostname){
        return "reeman/calling/phone/" + hostname + "/forklift/points/request/task_model";
    }

    /**
     * 手机端下发本地任务
     * @param hostname
     * @return
     */
    public static String topicReceiveRouteModelTask(String hostname){
        return "reeman/calling/phone/" + hostname + "/forklift/task/task_model";
    }

    /**
     * 机器人端推送本地任务列表
     * @param hostname
     * @return
     */
    public static String topicPublishRouteModelPoints(String hostname){
        return "reeman/calling/robot/"+hostname+"/forklift/points/response/task_model";
    }

    /**
     * 手机端获取手动模式点位
     * @param hostname
     * @return
     */
    public static String topicReceiveRequestQRCodeModelPoints(String hostname){
        return "reeman/calling/phone/" + hostname + "/forklift/points/request/manual_model";
    }

    /**
     * 手机端下发手动点位
     * @param hostname
     * @return
     */
    public static String topicReceiveQRCodeModelTask(String hostname){
        return "reeman/calling/phone/" + hostname + "/forklift/task/manual_model";
    }

    /**
     * 机器人端推手动模式列表
     * @param hostname
     * @return
     */
    public static String topicPublishQRCodeModelPoints(String hostname){
        return "reeman/calling/robot/"+hostname+"/forklift/points/response/manual_model";
    }

    /**
     * 心跳检测
     * @param hostname
     * @return
     */
    public static String topicPublicHeartBeat(String hostname){
        return "reeman/calling/phone/"+hostname+"/forklift/heartbeat";
    }

    /**
     * 接收机器端心跳
     * @param hostname
     * @return
     */
    public static String topicReceivedHeartBeat(String hostname){
        return "reeman/calling/robot/"+hostname+"/forklift/heartbeat";
    }
    /**
     * 任务执行响应
     * @param hostname
     * @return
     */
    public static String TaskExecutionResponse(String hostname){
        return "reeman/calling/robot/"+hostname+"/forklift/task/response";
    }


    /**
     * AGV手机发布心跳
     * @param hostname
     * @return
     */
    public static String AGVtopicPublicHeartBeat(String hostname){
        return "reeman/calling/phone/"+hostname+"/v2/heartbeat";
    }
    /**
     * AGV接收机器端心跳
     * @param hostname
     * @return
     */
    public static String AGVtopicReceivedHeartBeat(String hostname){
        return "reeman/calling/robot/"+hostname+"/v2/heartbeat";
    }
    /**
     * AGV手机端获取呼叫模式点位
     * @param hostname
     * @return
     */
    public static String AGVtopicReceiveRequestCallingModelPoints(String hostname){
        return "reeman/calling/phone/" + hostname + "/v2/points/request/calling_model";
    }

    /**
     * AGV手机端下发呼叫模式任务
     * @param hostname
     * @return
     */
    public static String AGVtopicReceiveCallingModelTask(String hostname){
        return "reeman/calling/phone/" + hostname + "/v2/task/calling_model";
    }

    /**
     * AGV机器人端推送呼叫模式点位
     * @param hostname
     * @return
     */
    public static String AGVtopicPublishCallingModelPoints(String hostname){
        return "reeman/calling/robot/"+hostname+"/v2/points/response/calling_model";
    }

    /**
     * AGV手机端获取二维码模式点位
     * @param hostname
     * @return
     */
    public static String AGVtopicReceiveRequestQRModelPoints(String hostname){
        return "reeman/calling/phone/" + hostname + "/v2/points/request/qrcode_model";
    }

    /**
     * AGV手机端下发二维码模式任务
     * @param hostname
     * @return
     */
    public static String AGVtopicReceiveQRModelTask(String hostname){
        return "reeman/calling/phone/" + hostname + "/v2/task/qrcode_model";
    }

    /**
     * AGV机器人端推送二维码模式点位
     * @param hostname
     * @return
     */
    public static String AGVtopicPublishQRModelPoints(String hostname){
        return "reeman/calling/robot/"+hostname+"/v2/points/response/qrcode_model";
    }

    /**
     * 手机端获取路线
     * @param hostname
     * @return
     */
    public static String AGVtopicReceiveRequestRouteModelPoints(String hostname){
        return "reeman/calling/phone/" + hostname + "/v2/points/request/route_model";
    }

    /**
     * 手机端下路线模式任务
     * @param hostname
     * @return
     */
    public static String AGVtopicReceiveRouteModelTask(String hostname){
        return "reeman/calling/phone/" + hostname + "/v2/task/route_model";
    }

    /**
     * 机器人端推送路线任务
     * @param hostname
     * @return
     */
    public static String AGVtopicPublishRouteModelPoints(String hostname){
        return "reeman/calling/robot/"+hostname+"/v2/points/response/route_model";
    }

    /**
     * 手机端获取普通模式点位
     * @param hostname
     * @return
     */
    public static String AGVtopicReceiveRequestOrdinaryModelPoints(String hostname){
        return "reeman/calling/phone/" + hostname + "/v2/points/request/normal_model";
    }

    /**
     * 手机端下发普通点位
     * @param hostname
     * @return
     */
    public static String AGVtopicReceiveOrdinaryeModelTask(String hostname){
        return "reeman/calling/phone/" + hostname + "/v2/task/normal_model";
    }

    /**
     * 机器人端推普通模式列表
     * @param hostname
     * @return
     */
    public static String AGVtopicPublishOrdinaryModelPoints(String hostname){
        return "reeman/calling/robot/"+hostname+"/v2/points/response/normal_model";
    }

    /**
     * 任务执行响应
     * @param hostname
     * @return
     */
    public static String AGVTaskExecutionResponse(String hostname){
        return "reeman/calling/robot/"+hostname+"/v2/task/response";
    }

    /**
     * AGV手机端发布回充任务
     * @param hostname
     * @return
     */
    public static String AGVGoChargePointModel(String hostname){
        return "reeman/calling/phone/"+hostname+"/v2/task/charge_model";
    }
    /**
     * AGV手机端发布返航任务
     * @param hostname
     * @return
     */
    public static String AGVGoReturnPointModel(String hostname){
        return "reeman/calling/phone/"+hostname+"/v2/task/return_model";
    }
}
