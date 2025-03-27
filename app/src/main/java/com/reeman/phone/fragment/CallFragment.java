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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.reeman.phone.FragmentActivity;
import com.reeman.phone.R;
import com.reeman.phone.adapter.CallListAdapter;
import com.reeman.phone.adapter.MapListAdapter;
import com.reeman.phone.call.Topic;
import com.reeman.phone.constant.Constants;
import com.reeman.phone.event.Event;
import com.reeman.phone.mode.PublicHeatBeatMode;
import com.reeman.phone.mode.TaskResponse;
import com.reeman.phone.utils.GlobalFlag;
import com.reeman.phone.utils.LoadingDialogUtil;
import com.reeman.phone.call.MqttClient;
import com.reeman.phone.utils.StringSorter;
import com.reeman.phone.utils.Timbers;
import com.reeman.phone.utils.ToastUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CallFragment extends Fragment {
    private List<String> list = new ArrayList<>();
    private List<String> mapList = new ArrayList<>();
    private Map<String, List<String>> dataMap = new HashMap<>();
    private View view; // 定义视图以设置片段的布局
    // 自定义RecyclerView适配器
    private CallListAdapter callListAdapter;
    private MapListAdapter mapListAdapter;
    private ProgressDialog dialog;
    private String hostname;
    private static final int TIMEOUT_MILLISECONDS = 5000; // 超时时间，单位：毫秒
    private boolean isDataReceived = false; // 用于标记是否已收到数据
    private Gson gson;
    private Handler handler = new Handler();
    String UUID;
    private RecyclerView recyclerView1;

    private boolean elevatorCallSwitch=false;
    private RecyclerView recyclerView;
    private String callToken;
    private String robotType;
    private LinearLayout ll_route_task_list;
    private LinearLayout ll_route_task_null;
    private TextView tv_bg_point_null_text;
    private SwipeRefreshLayout swipeRefreshLayout;
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
        view = inflater.inflate(R.layout.fragment_call, container, false);
        LoadingDialogUtil.getInstance().showLoadingDialog(getActivity(), getString(R.string.get_all_point));//开启加载动画
        //获取token
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("MulticastDevices", Context.MODE_PRIVATE);
        callToken = sharedPreferences.getString(hostname + "_token", null);
        robotType = sharedPreferences.getString(hostname + "_robotType", null);
        Timbers.w(requireContext(), "进入呼叫模式,获取配对时的token:"+callToken);
        gson = new Gson();
        startTimeoutHandler();
        initRecyclerView(requireContext());
        initSwipeRefreshLayout();
        obtainCallPoint(requireContext());

        return view;
    }

    private void startTimeoutHandler() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isAdded() && !isDataReceived) {
                    // 如果在超时时间内没有收到数据，结束加载动画
                    Timbers.w(requireContext(), "呼叫模式未收到机器端上报数据");
                    LoadingDialogUtil.getInstance().closeLoadingDialog();//关闭动画
                    //ToastUtils.showShortToast(getString(R.string.point_timeout));
                    if (swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                    ll_route_task_null.setVisibility(View.VISIBLE);
                }
            }
        }, TIMEOUT_MILLISECONDS);
    }

    private void obtainCallPoint(Context context) {
        if(callToken ==null){
            Timbers.w(requireContext(), "token匹配码为空");
            ToastUtils.showShortToast(getString(R.string.error_del));
            FragmentActivity.destroyActivity((FragmentActivity) context);
        }
        Timbers.w(requireContext(), "下达获取呼叫点位指令");
        if(robotType.equals(Constants.robotTypeForklift)){
            MqttClient.Publish(Topic.topicReceiveRequestCallingModelPoints(hostname), new Gson().toJson(new PublicHeatBeatMode(callToken)));
        } else if (robotType.equals(Constants.robotTypeAGV)){
            MqttClient.Publish(Topic.AGVtopicReceiveRequestCallingModelPoints(hostname), new Gson().toJson(new PublicHeatBeatMode(callToken)));
        }

    }

    private void initRecyclerView(Context context) {
        recyclerView1 = view.findViewById(R.id.map_list);
        ll_route_task_list = view.findViewById(R.id.ll_route_task_list);
        ll_route_task_null = view.findViewById(R.id.ll_route_task_null);
        tv_bg_point_null_text = view.findViewById(R.id.tv_bg_point_null_text);
        tv_bg_point_null_text.setText(getString(R.string.robot_call_point_null));
        // 设置MapListAdapter
        mapListAdapter = new MapListAdapter(mapList);
        mapListAdapter.setOnItemClickListener(new MapListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(List<String> points) {
                list.clear();
                list.addAll(points);
                callListAdapter.notifyDataSetChanged();
            }
            @Override
            public void onPosition(int position) {
                if(elevatorCallSwitch){
                    callListAdapter.setMapName(mapList.get(position));
                }else {
                    callListAdapter.setMapName(null);
                }
                onItemClick(dataMap.get(mapList.get(position)));
            }
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false);
        recyclerView1.setLayoutManager(layoutManager);
        recyclerView1.setAdapter(mapListAdapter);

        // 显示或隐藏recyclerView1
        recyclerView1.setVisibility(GlobalFlag.getInstance().getElevatorMode() ? View.VISIBLE : View.GONE);
        recyclerView = view.findViewById(R.id.rv_data_list);
        callListAdapter = new CallListAdapter(list, context, hostname,robotType);
        recyclerView.setAdapter(callListAdapter);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(requireContext(), 2, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(gridLayoutManager);
    }

    private void initSwipeRefreshLayout() {
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                ll_route_task_null.setVisibility(View.GONE);
                obtainCallPoint(requireContext());
                startTimeoutHandler();
            }
        });
    }

    private void onItemClick(List<String> points) {
        list.clear();
        list.addAll(points);
        callListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCallingEvent(Event.OnCallingEvent event) {
        if (swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }
        if(event.abnormalState){
            LoadingDialogUtil.getInstance().closeLoadingDialog();
            Timbers.w(requireContext(), "接收呼叫数据发生异常");
            return;
        }
        if(event.code!=0){
            LoadingDialogUtil.getInstance().closeLoadingDialog();
            Timbers.w(requireContext(), "获取点位失败，code值为:"+event.code);
            if(event.code==1001 || event.code==1002 || event.code==1){
                ll_route_task_null.setVisibility(View.VISIBLE);
            }
            ToastUtils.showShortToast(event.body);
            return;
        }
        if(!event.token.equals(callToken)){
            LoadingDialogUtil.getInstance().closeLoadingDialog();
            ToastUtils.showShortToast(getString(R.string.token_check));
            Timbers.w(requireContext(), "token不相等，机器端："+event.token+" , 手机端："+callToken);
            LoadingDialogUtil.getInstance().closeLoadingDialog();
            return;
        }
        Timbers.w(requireContext(), "呼叫模式下收到上报数据:"+event.model);
        Log.d("4433","event.body:"+event.model);
        list.clear();
        mapList.clear();
        dataMap = new Gson().fromJson(event.model, new TypeToken<Map<String, List<String>>>() {}.getType());
        mapList.addAll(dataMap.keySet());
        LoadingDialogUtil.getInstance().closeLoadingDialog();
        isDataReceived = true;
        ll_route_task_null.setVisibility(View.GONE);
        ll_route_task_list.setVisibility(View.VISIBLE);

        // 获取第一个键
        String firstKey = mapList.isEmpty() ? null : mapList.get(0);
        if (firstKey != null && dataMap.containsKey(firstKey)) {
            list.addAll(dataMap.get(firstKey));  // 将第一个键对应的值添加到list中
            //list = StringSorter.sortByPrefixAndNumber(list);
        }
        callListAdapter.notifyDataSetChanged();
        mapListAdapter.notifyDataSetChanged();
        elevatorCallSwitch=event.elevatorModeSwitch;
        recyclerView1.setVisibility(event.elevatorModeSwitch ? View.VISIBLE : View.GONE);
        // 动态调整GridLayoutManager的列数
        int spanCount = event.elevatorModeSwitch ? 2 : 3;
        GridLayoutManager gridLayoutManager = new GridLayoutManager(requireContext(), spanCount, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(gridLayoutManager);
    }
}
