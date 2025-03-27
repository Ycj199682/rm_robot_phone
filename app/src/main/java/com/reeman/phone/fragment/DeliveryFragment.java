package com.reeman.phone.fragment;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.reeman.phone.FragmentActivity;
import com.reeman.phone.R;
import com.reeman.phone.adapter.DeliveryCheckedPointsAdapter;
import com.reeman.phone.adapter.DeliveryListAdapter;
import com.reeman.phone.adapter.KeyModeAdapter;
import com.reeman.phone.call.Topic;
import com.reeman.phone.constant.Constants;
import com.reeman.phone.event.Event;
import com.reeman.phone.mode.CallingModeWithMAC;
import com.reeman.phone.mode.NormalModeWithMAC;
import com.reeman.phone.mode.PublicHeatBeatMode;
import com.reeman.phone.mode.TaskResponse;
import com.reeman.phone.utils.AESUtil;
import com.reeman.phone.utils.LoadingDialogUtil;
import com.reeman.phone.call.MqttClient;
import com.reeman.phone.utils.StringSorter;
import com.reeman.phone.utils.Timbers;
import com.reeman.phone.utils.ToastUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.reeman.phone.adapter.DeliveryListAdapter.lastClickedItem;


public class DeliveryFragment extends Fragment implements DeliveryListAdapter.ClickedItemsListener, DeliveryCheckedPointsAdapter.DataCallback {
    private List<String> list = new ArrayList<>();
    private List<String> keyList = new ArrayList<>();
    private View view;
    private DeliveryListAdapter deliveryListAdapter;
    private DeliveryCheckedPointsAdapter deliveryCheckedPointsAdapter;
    private KeyModeAdapter keyListAdapter;
    private ProgressDialog dialog;
    private String hostname;
    private static final int TIMEOUT_MILLISECONDS = 5000;
    private boolean isDataReceived = false;
    private List<String> receivedClickedItems = new ArrayList<>();
    private List<String> deliveryList = new ArrayList<>();
    private List<String> deliveryMapNames = new ArrayList<>(); // 新增的地图名称列表
    private boolean isFragmentActive = false;
    private String UUID;
    private Handler handler;
    private Map<String, List<String>> dataMap = new HashMap<>();
    private RecyclerView keyRecyclerView;
    private String mapAutoName;
    public static boolean QRelevatorAutoSwitch=false;
    private LinearLayout ll_auto_mode;
    private String Autotoken;
    private String encryptKey;
    private String robotType;
    private RecyclerView rvSelectPoint;
    private LinearLayout ll_delivery_null;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tv_bg_point_null_text;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        isFragmentActive = true;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        isFragmentActive = false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        EventBus.getDefault().register(this);
        Intent activityIntent = getActivity().getIntent();
        if (activityIntent.hasExtra("key")) {
            hostname = activityIntent.getStringExtra("key");
        }
        view = inflater.inflate(R.layout.fragment_delivery, container, false);
        ll_auto_mode = view.findViewById(R.id.ll_auto_mode);
        ll_delivery_null = view.findViewById(R.id.ll_delivery_null);
        tv_bg_point_null_text= view.findViewById(R.id.tv_bg_point_null_text);

        LoadingDialogUtil.getInstance().showLoadingDialog(getActivity(), getString(R.string.get_all_point));
        //获取token
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("MulticastDevices", Context.MODE_PRIVATE);
        Autotoken = sharedPreferences.getString(hostname + "_token", null);
        encryptKey= sharedPreferences.getString(hostname + "_encryptKey", null);
        robotType = sharedPreferences.getString(hostname + "_robotType", null);
        Timbers.w(requireContext(),"进入二维码模式，获取配对时的token"+Autotoken+" , Key:"+encryptKey);
        handler = new Handler(Looper.getMainLooper());
        startTimeoutHandler();
        initRecyclerView(requireContext());
        obtainAutoPoint();
        initSwipeRefreshLayout();
        Button button = view.findViewById(R.id.delivery_start);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (deliveryList.size()%2==0 && deliveryList.size()!=0) {
                    if(Autotoken==null || encryptKey==null){
                        ToastUtils.showShortToast(requireContext().getString(R.string.error_del));
                        Timbers.w(requireContext(), "token或key为空，无法发送数据");
                        FragmentActivity.destroyActivity((FragmentActivity) requireContext());
                    }
                    // 构建 body 数据
                    List<NormalModeWithMAC.PointPair> pointPairs = new ArrayList<>();
                    for (int i = 0; i < deliveryList.size() - 1; i += 2) {
                        String mapNameFirst = deliveryMapNames.get(i); // 获取第一个点位的地图名称
                        String mapNameSecond = deliveryMapNames.get(i + 1); // 获取第二个点位的地图名称
                        if(!QRelevatorAutoSwitch){
                            mapNameFirst="null";
                            mapNameSecond="null";
                        }
                        NormalModeWithMAC.Point first = new NormalModeWithMAC.Point(mapNameFirst, deliveryList.get(i));
                        NormalModeWithMAC.Point second = new NormalModeWithMAC.Point(mapNameSecond, deliveryList.get(i + 1));
                        NormalModeWithMAC.PointPair pointPair = new NormalModeWithMAC.PointPair(first, second);
                        pointPairs.add(pointPair);
                    }
                    String encryptData = null;
                    try {
                        encryptData = AESUtil.encrypt(encryptKey, new Gson().toJson(pointPairs));
                    } catch (GeneralSecurityException e) {
                        throw new RuntimeException(e);
                    }
                    NormalModeWithMAC normalModeList = new NormalModeWithMAC(Autotoken, encryptData);
                    if(robotType.equals(Constants.robotTypeForklift)){
                        MqttClient.Publish(Topic.topicReceiveNormalModelTask(hostname), new Gson().toJson(normalModeList));
                    } else if (robotType.equals(Constants.robotTypeAGV)){
                        MqttClient.Publish(Topic.AGVtopicReceiveQRModelTask(hostname), new Gson().toJson(normalModeList));
                    }

                    lastClickedItem = "";
                    deliveryList.clear();
                    deliveryMapNames.clear(); // 清空地图名称列表
                    deliveryCheckedPointsAdapter.clearAll();
                    deliveryCheckedPointsAdapter.notifyDataSetChanged();
                } else {
                    ToastUtils.showShortToast(getString(R.string.select_location_reminder));
                }
            }
        });
        return view;
    }
    private void startTimeoutHandler() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isDataReceived && isFragmentActive && isAdded()) {
                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Timbers.w(requireContext(), "未收到机器端上报数据");
                            LoadingDialogUtil.getInstance().closeLoadingDialog();
                            //ToastUtils.showShortToast(getString(R.string.point_timeout));
                            if (swipeRefreshLayout.isRefreshing()) {
                                swipeRefreshLayout.setRefreshing(false);
                            }
                            ll_auto_mode.setVisibility(View.GONE);
                            ll_delivery_null.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }
        }, TIMEOUT_MILLISECONDS);
    }
    private void initSwipeRefreshLayout() {
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                ll_delivery_null.setVisibility(View.GONE);
                obtainAutoPoint();
                startTimeoutHandler();
            }
        });
    }

    private void obtainAutoPoint() {
        if(Autotoken==null){
            Timbers.w(requireContext(), "token为空");
            ToastUtils.showShortToast(requireContext().getString(R.string.error_del));
            FragmentActivity.destroyActivity((FragmentActivity) requireContext());
        }
        Timbers.w(requireContext(), "下达获取点位指令");
        if(robotType.equals(Constants.robotTypeForklift)){
            tv_bg_point_null_text.setText(getString(R.string.forklift_auto_point_null));
            MqttClient.Publish(Topic.topicReceiveRequestNormalModelPoints(hostname), new Gson().toJson(new PublicHeatBeatMode(Autotoken)));
        } else if (robotType.equals(Constants.robotTypeAGV)){
            //AGV二维码模式
            tv_bg_point_null_text.setText(getString(R.string.agv_jack_point_null));
            MqttClient.Publish(Topic.AGVtopicReceiveRequestQRModelPoints(hostname), new Gson().toJson(new PublicHeatBeatMode(Autotoken)));
        }
    }

    private void initRecyclerView(Context context) {
        rvSelectPoint = view.findViewById(R.id.delivery_list);
        deliveryListAdapter = new DeliveryListAdapter(list, context, hostname);
        rvSelectPoint.setAdapter(deliveryListAdapter);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(requireContext(), 2, LinearLayoutManager.VERTICAL, false);
        rvSelectPoint.setLayoutManager(gridLayoutManager);

        RecyclerView recyclerView1 = view.findViewById(R.id.delivery_choose_list);
        deliveryCheckedPointsAdapter = new DeliveryCheckedPointsAdapter(receivedClickedItems, deliveryMapNames, requireContext(), hostname);
        recyclerView1.setAdapter(deliveryCheckedPointsAdapter);
        GridLayoutManager gridLayoutManager1 = new GridLayoutManager(requireContext(), 2, LinearLayoutManager.VERTICAL, false);
        recyclerView1.setLayoutManager(gridLayoutManager1);
        deliveryListAdapter.setClickedItemsListener(this);
        deliveryCheckedPointsAdapter.setDataCallback(this);

        // 初始化键的RecyclerView
        keyRecyclerView = view.findViewById(R.id.key_delivery_list);
        keyListAdapter = new KeyModeAdapter(keyList);
        keyRecyclerView.setAdapter(keyListAdapter);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false);
        keyRecyclerView.setLayoutManager(linearLayoutManager);

        keyListAdapter.setOnItemClickListener(new KeyModeAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(String key) {
                list.clear();
                list.addAll(dataMap.get(key));
                deliveryListAdapter.notifyDataSetChanged();
            }
            @Override
            public void setMapName(int mapKey) {
                mapAutoName=keyList.get(mapKey);
            }
        });

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onItemsUpdated(String clickedItems) {
        deliveryCheckedPointsAdapter.updateData(clickedItems, mapAutoName); // 传递地图名称
    }

    @Override
    public void onDataChanged(List<String> updatedData) {
        deliveryList.clear();
        deliveryList.addAll(updatedData);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNormalEvent(Event.OnNormalEvent event) {
        if (swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }
        if(event.code!=0){
            LoadingDialogUtil.getInstance().closeLoadingDialog();
            ToastUtils.showShortToast(event.body);
            if(event.code==1001 || event.code==1002 || event.code==1){
                ll_delivery_null.setVisibility(View.VISIBLE);
            }
            //ToastUtils.showShortToast(getString(R.string.failed_point_location));
            Timbers.w(requireContext(), "获取点位失败，code值为:"+event.code);
            return;
        }
        if(!event.token.equals(Autotoken)){
            LoadingDialogUtil.getInstance().closeLoadingDialog();
            ll_delivery_null.setVisibility(View.VISIBLE);
            Timbers.w(requireContext(), "token不相等，机器端："+event.token+" , 手机端："+Autotoken);
            ToastUtils.showShortToast(getString(R.string.token_check));
            LoadingDialogUtil.getInstance().closeLoadingDialog();
            return;
        }
        Timbers.w(requireContext(), "收到上报数据:"+event.model);
        list.clear();
        dataMap = new Gson().fromJson(event.model, new TypeToken<Map<String, List<String>>>() {}.getType());
        keyList.clear();
        keyList.addAll(dataMap.keySet());
        LoadingDialogUtil.getInstance().closeLoadingDialog();
        isDataReceived = true;
        ll_delivery_null.setVisibility(View.GONE);
        ll_auto_mode.setVisibility(View.VISIBLE);
        // 获取第一个键
        String firstKey = keyList.isEmpty() ? null : keyList.get(0);
        if (firstKey != null && dataMap.containsKey(firstKey)) {
            list.addAll(dataMap.get(firstKey));  // 将第一个键对应的值添加到list中
            //list = StringSorter.sortByPrefixAndNumber(list);
        }
        deliveryListAdapter.notifyDataSetChanged();
        keyListAdapter.notifyDataSetChanged();
        QRelevatorAutoSwitch=event.elevatorModeSwitch;
        // 动态调整GridLayoutManager的列数
        int spanCount = event.elevatorModeSwitch ? 2 : 3;
        GridLayoutManager gridLayoutManager = new GridLayoutManager(requireContext(), spanCount, LinearLayoutManager.VERTICAL, false);
        rvSelectPoint.setLayoutManager(gridLayoutManager);
        if (event.elevatorModeSwitch) {
            keyRecyclerView.setVisibility(View.VISIBLE);
        } else {
            keyRecyclerView.setVisibility(View.GONE);
        }
    }
}
