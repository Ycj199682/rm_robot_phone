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
import com.reeman.phone.adapter.JackCheckPointsAdapter;
import com.reeman.phone.adapter.JackListAdapter;
import com.reeman.phone.adapter.KeyModeAdapter; // 引入新的适配器
import com.reeman.phone.call.Topic;
import com.reeman.phone.constant.Constants;
import com.reeman.phone.event.Event;
import com.reeman.phone.mode.PublicHeatBeatMode;
import com.reeman.phone.mode.QrcodeModeWithMAC;
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
import java.util.List;
import java.util.Map;

import static com.reeman.phone.adapter.JackListAdapter.pointSign;
import static com.reeman.phone.adapter.JackListAdapter.JackClickedItem;

public class JackFragment extends Fragment implements JackListAdapter.ClickedItemsListener, JackCheckPointsAdapter.ClickedItemsListener {
    private List<String> keyList = new ArrayList<>();
    private List<String> jackList = new ArrayList<>();
    private List<String> receivedClickedItems = new ArrayList<>();
    private Map<String, List<String>> dataMap;
    private View view;
    private KeyModeAdapter keyListAdapter; // 使用新的适配器
    private JackListAdapter jackListAdapter;
    private JackCheckPointsAdapter jackCheckPointsAdapter;
    private ProgressDialog dialog;
    private String hostname;
    private static final int TIMEOUT_MILLISECONDS = 5000;
    private boolean isDataReceived = false;
    private boolean isFragmentActive = false;
    private String UUID;
    private Handler handler;
    private RecyclerView keyRecyclerView;
    public static String manualMapName;
    public static boolean elevatorManualSwitch=false;
    private RecyclerView chooseRecyclerView;
    private RecyclerView jackRecyclerView;
    private LinearLayout ll_jack;
    private String manualToken;
    private String encryptKey;
    private LinearLayout ll_manual_go;
    private String robotType;
    private Button button;
    private LinearLayout ll_jack_null;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout ll_jack_main;
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
        view = inflater.inflate(R.layout.fragment_jack, container, false);
        ll_jack = view.findViewById(R.id.ll_jack);
        ll_jack_null = view.findViewById(R.id.ll_jack_null);
        ll_jack_main = view.findViewById(R.id.ll_jack_main);
        tv_bg_point_null_text = view.findViewById(R.id.tv_bg_point_null_text);
        LoadingDialogUtil.getInstance().showLoadingDialog(getActivity(), getString(R.string.get_all_point));
        //获取token
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("MulticastDevices", Context.MODE_PRIVATE);
        manualToken = sharedPreferences.getString(hostname + "_token", null);
        encryptKey=sharedPreferences.getString(hostname + "_encryptKey", null);
        robotType = sharedPreferences.getString(hostname + "_robotType", null);
        Timbers.w(requireContext(),"进入普通模式，获取配对时的token"+manualToken+" , Key:"+encryptKey);
        handler = new Handler(Looper.getMainLooper());
        startTimeoutHandler();
        initRecyclerView(requireContext());
        obtainManualPoint();
        initSwipeRefreshLayout();

        //隐藏第三列表
        ll_manual_go = view.findViewById(R.id.ll_manual_go);
        button = view.findViewById(R.id.jack_start);
        button.setOnClickListener(view -> {
            if (receivedClickedItems.size() >0) {
                if(manualToken==null || encryptKey==null){
                    Timbers.w(requireContext(), "token或key为空");
                    ToastUtils.showShortToast(requireContext().getString(R.string.error_del));
                    FragmentActivity.destroyActivity((FragmentActivity) requireContext());
                    return;
                }
                // 创建一个包含多个 AGVManualBody 的列表
                List<QrcodeModeWithMAC.AGVManualBody> agvBodyList = new ArrayList<>();
                for (String item : receivedClickedItems) {
                    agvBodyList.add(new QrcodeModeWithMAC.AGVManualBody(manualMapName, item));
                }

                String encryptData = null;
                try {
                    encryptData = AESUtil.encrypt(encryptKey, new Gson().toJson(agvBodyList));
                } catch (GeneralSecurityException e) {
                    throw new RuntimeException(e);
                }

                QrcodeModeWithMAC qrcodeModeWithMAC = new QrcodeModeWithMAC(manualToken, encryptData);
                String jsonPayload = new Gson().toJson(qrcodeModeWithMAC);
                MqttClient.Publish(Topic.AGVtopicReceiveOrdinaryeModelTask(hostname), jsonPayload);

                pointSign=true;
                JackClickedItem=" ";
                receivedClickedItems.clear();
                jackCheckPointsAdapter.notifyDataSetChanged();
            }
        });
        return view;
    }

    private void startTimeoutHandler() {
        handler.postDelayed(() -> {
            if (isAdded() && !isDataReceived) {
                requireActivity().runOnUiThread(() -> {
                    Timbers.w(requireContext(), "未收到机器端数据");
                    LoadingDialogUtil.getInstance().closeLoadingDialog();
                    //ToastUtils.showShortToast(getString(R.string.point_timeout));
                    if (swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                    swipeRefreshLayout.setVisibility(View.GONE);
                    ll_jack_null.setVisibility(View.VISIBLE);
                });
            }
        }, TIMEOUT_MILLISECONDS);
    }

    private void initSwipeRefreshLayout() {
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                ll_jack_null.setVisibility(View.GONE);
                swipeRefreshLayout.setVisibility(View.VISIBLE);
                obtainManualPoint();
                startTimeoutHandler();
            }
        });
    }

    private void obtainManualPoint() {
        if(manualToken==null){
            ToastUtils.showShortToast(requireContext().getString(R.string.error_del));
            Timbers.w(requireContext(), "token为空，无法发送获取点位指令");
            FragmentActivity.destroyActivity((FragmentActivity) requireContext());
        }
        if(robotType.equals(Constants.robotTypeForklift)){
            //手动
            tv_bg_point_null_text.setText(getString(R.string.forklift_Manual_point_null));
            MqttClient.Publish(Topic.topicReceiveRequestQRCodeModelPoints(hostname), new Gson().toJson(new PublicHeatBeatMode(manualToken)));
        } else if (robotType.equals(Constants.robotTypeAGV)){
            //AGV普通模式
            tv_bg_point_null_text.setText(getString(R.string.agv_ordinary_point_null));
            MqttClient.Publish(Topic.AGVtopicReceiveRequestOrdinaryModelPoints(hostname), new Gson().toJson(new PublicHeatBeatMode(manualToken)));
        }
    }

    private void initRecyclerView(Context context) {

        // 初始化第一个RecyclerView（key_jack_list）
        keyRecyclerView = view.findViewById(R.id.key_jack_list);
        keyListAdapter = new KeyModeAdapter(keyList); // 使用新的适配器
        keyRecyclerView.setAdapter(keyListAdapter);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false);
        keyRecyclerView.setLayoutManager(linearLayoutManager);

        // 初始化第二个RecyclerView（jack_list）
        jackRecyclerView = view.findViewById(R.id.jack_list);
        jackListAdapter = new JackListAdapter(jackList, context, hostname,robotType);
        jackRecyclerView.setAdapter(jackListAdapter);
        GridLayoutManager gridLayoutManager1 = new GridLayoutManager(requireContext(), 2, LinearLayoutManager.VERTICAL, false);
        jackRecyclerView.setLayoutManager(gridLayoutManager1);

        // 初始化第三个RecyclerView（jack_choose_list）
        chooseRecyclerView = view.findViewById(R.id.jack_choose_list);
        jackCheckPointsAdapter = new JackCheckPointsAdapter(receivedClickedItems, requireContext());
        chooseRecyclerView.setAdapter(jackCheckPointsAdapter);
        GridLayoutManager gridLayoutManager2 = new GridLayoutManager(requireContext(), 2, LinearLayoutManager.VERTICAL, false);
        chooseRecyclerView.setLayoutManager(gridLayoutManager2);

        keyListAdapter.setOnItemClickListener(new KeyModeAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(String key) {
                if (dataMap != null && dataMap.containsKey(key)) {
                    jackList.clear();
                    jackList.addAll(dataMap.get(key));
                    jackListAdapter.notifyDataSetChanged();
                }
            }
            @Override
            public void setMapName(int mapKey) {
                if(elevatorManualSwitch){
                    manualMapName=keyList.get(mapKey);
                }else {
                    manualMapName="null";
                }
            }
        });

        jackListAdapter.setClickedItemsListener(this);
        jackCheckPointsAdapter.setClickedItemsListener(this);

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onItemClick(String item) {
        if (jackList.contains(item)) {
            receivedClickedItems.add(item);
            jackCheckPointsAdapter.notifyDataSetChanged();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onQrcodeEvent(Event.OnQrcodeEvent event) {
        if (swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }
        if(event.code!=0){
            LoadingDialogUtil.getInstance().closeLoadingDialog();
            ToastUtils.showShortToast(event.body);
            if(event.code==1001 || event.code==1002 || event.code==1){
                swipeRefreshLayout.setVisibility(View.GONE);
                ll_jack_null.setVisibility(View.VISIBLE);

            }
            //ToastUtils.showShortToast(getString(R.string.failed_point_location));
            Timbers.w(requireContext(), "获取点位失败，code值为:"+event.code);
            return;
        }
        if(!event.token.equals(manualToken)){
            LoadingDialogUtil.getInstance().closeLoadingDialog();
            swipeRefreshLayout.setVisibility(View.GONE);
            ll_jack_null.setVisibility(View.VISIBLE);
            ToastUtils.showShortToast(getString(R.string.token_check));
            Timbers.w(requireContext(), "token不相等，机器端："+event.token+" , 手机端："+manualToken);
            LoadingDialogUtil.getInstance().closeLoadingDialog();
            return;
        }
        Timbers.w(requireContext(), "收到机器端上报数据"+event.model);
        Log.d("4433","event.body:"+event.model);
        keyList.clear();
        jackList.clear();
        dataMap = new Gson().fromJson(event.model, new TypeToken<Map<String, List<String>>>() {}.getType());
        keyList.addAll(dataMap.keySet());
        LoadingDialogUtil.getInstance().closeLoadingDialog();
        isDataReceived = true;
        ll_jack_null.setVisibility(View.GONE);
        swipeRefreshLayout.setVisibility(View.VISIBLE);
        ll_jack.setVisibility(View.VISIBLE);
        String firstKey = keyList.isEmpty() ? null : keyList.get(0);
        if (firstKey != null && dataMap.containsKey(firstKey)) {
            jackList.addAll(dataMap.get(firstKey));  // 将第一个键对应的值添加到list中
            //jackList = StringSorter.sortByPrefixAndNumber(jackList);
        }
        jackListAdapter.notifyDataSetChanged();
        keyListAdapter.notifyDataSetChanged();
        elevatorManualSwitch=event.elevatorModeSwitch;
        if(robotType.equals(Constants.robotTypeForklift)){
            ll_manual_go.setVisibility(View.GONE);
        } else if (robotType.equals(Constants.robotTypeAGV)){
            //AGV普通模式
            ll_manual_go.setVisibility(View.VISIBLE);
        }
        ll_jack_main.setVisibility(View.VISIBLE);
        // 动态调整GridLayoutManager的列数
        int spanCount = event.elevatorModeSwitch ? 2 : 3;
        GridLayoutManager gridLayoutManager1 = new GridLayoutManager(requireContext(), spanCount, LinearLayoutManager.VERTICAL, false);
        jackRecyclerView.setLayoutManager(gridLayoutManager1);
        if (event.elevatorModeSwitch) {
            keyRecyclerView.setVisibility(View.VISIBLE);
        } else {
            keyRecyclerView.setVisibility(View.GONE);
        }
    }
}
