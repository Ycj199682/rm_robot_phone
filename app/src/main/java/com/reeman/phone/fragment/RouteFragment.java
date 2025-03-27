package com.reeman.phone.fragment;

import android.annotation.SuppressLint;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.reeman.phone.FragmentActivity;
import com.reeman.phone.R;
import com.reeman.phone.adapter.RouteAdapter;
import com.reeman.phone.call.Topic;
import com.reeman.phone.constant.Constants;
import com.reeman.phone.event.Event;
import com.reeman.phone.mode.PublicHeatBeatMode;
import com.reeman.phone.mode.TaskResponse;
import com.reeman.phone.utils.LoadingDialogUtil;
import com.reeman.phone.call.MqttClient;
import com.reeman.phone.utils.Timbers;
import com.reeman.phone.utils.ToastUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;


public class RouteFragment extends Fragment {
    private List<String> list = new ArrayList<>();
    private View view; // 定义视图以设置片段的布局
    // 自定义RecyclerView适配器
    private RouteAdapter routeAdapter;
    private ProgressDialog dialog;
    private String hostname;
    private static final int TIMEOUT_MILLISECONDS = 4000; // 超时时间，单位：毫秒
    private boolean isDataReceived = false; // 用于标记是否已收到数据
    private String UUID;
    private Handler handler = new Handler();;
    private RecyclerView recyclerView;
    private String taskListToken;
    private String robotType;
    private LinearLayout ll_route_task_list;
    private LinearLayout ll_route_task_null;
    private TextView tv_bg_point_null_text;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Intent activityIntent = getActivity().getIntent();
        if (activityIntent.hasExtra("key")) {
            hostname = activityIntent.getStringExtra("key");
        }
        view = inflater.inflate(R.layout.fragment_call, container, false);
        LoadingDialogUtil.getInstance().showLoadingDialog(getActivity(), getString(R.string.get_all_point));
        // 获取token
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("MulticastDevices", Context.MODE_PRIVATE);
        taskListToken = sharedPreferences.getString(hostname + "_token", null);
        robotType = sharedPreferences.getString(hostname + "_robotType", null);
        Timbers.w(requireContext(), "进入路线模式界面，获取配对时的token" + taskListToken);
        initRecyclerView(requireContext());
        startTimeoutHandler();
        initSwipeRefreshLayout();
        obtainTaskList();
        return view;
    }

    private void startTimeoutHandler() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isAdded() && !isDataReceived) {
                    Timbers.w(requireContext(), "未收到机器端上报数据");
                    LoadingDialogUtil.getInstance().closeLoadingDialog();
                    if (swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                    ll_route_task_null.setVisibility(View.VISIBLE);
                }
            }
        }, TIMEOUT_MILLISECONDS);
    }

    private void obtainTaskList() {
        if (taskListToken == null) {
            Timbers.w(requireContext(), "token为空");
            ToastUtils.showShortToast(requireContext().getString(R.string.error_del));
            FragmentActivity.destroyActivity((FragmentActivity) requireContext());
        }
        Timbers.w(requireContext(), "发获取任务列表数据指令");
        if (robotType.equals(Constants.robotTypeForklift)) {
            tv_bg_point_null_text.setText(getString(R.string.forklift_task_point_null));
            MqttClient.Publish(Topic.topicReceiveRequestRouteModelPoints(hostname), new Gson().toJson(new PublicHeatBeatMode(taskListToken)));
        } else if (robotType.equals(Constants.robotTypeAGV)) {
            // AGV路线模式
            tv_bg_point_null_text.setText(getString(R.string.agv_route_point_null));
            MqttClient.Publish(Topic.AGVtopicReceiveRequestRouteModelPoints(hostname), new Gson().toJson(new PublicHeatBeatMode(taskListToken)));
        }
    }

    @SuppressLint("WrongViewCast")
    private void initRecyclerView(Context context) {
        recyclerView = view.findViewById(R.id.rv_data_list);
        ll_route_task_list = view.findViewById(R.id.ll_route_task_list);
        ll_route_task_null = view.findViewById(R.id.ll_route_task_null);
        tv_bg_point_null_text = view.findViewById(R.id.tv_bg_point_null_text);
        routeAdapter = new RouteAdapter(list, context, hostname, robotType);
        recyclerView.setAdapter(routeAdapter);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(requireContext(), 1, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(gridLayoutManager);
    }

    private void initSwipeRefreshLayout() {
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                ll_route_task_null.setVisibility(View.GONE);
                obtainTaskList();
                startTimeoutHandler();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().unregister(this);
        handler.removeCallbacksAndMessages(null); // 移除所有挂起的处理程序
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRouteEvent(Event.OnRouteEvent event) {
        if (swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }
        if (event.code !=0) {
            LoadingDialogUtil.getInstance().closeLoadingDialog();
            ToastUtils.showShortToast(event.body);
            if(event.code==1001 || event.code==1002 || event.code==1){
                ll_route_task_null.setVisibility(View.VISIBLE);
            }
            Timbers.w(requireContext(), "获取点位失败，code值为:" + event.code);
            return;
        }
        if (!event.token.equals(taskListToken)) {
            LoadingDialogUtil.getInstance().closeLoadingDialog();
            ll_route_task_null.setVisibility(View.VISIBLE);
            ToastUtils.showShortToast(getString(R.string.token_check));
            Timbers.w(requireContext(), "token不相等，机器端：" + event.token + " , 手机端：" + taskListToken);
            return;
        }
        Timbers.w(requireContext(), "收到机器端上报数据：" + event.body);
        list.clear();
        List<String> routeMode = new Gson().fromJson(event.body, new TypeToken<List<String>>() {
        }.getType());
        LoadingDialogUtil.getInstance().closeLoadingDialog();
        list.addAll(routeMode);
        isDataReceived = true; // 设置数据已收到的标志
        ll_route_task_null.setVisibility(View.GONE);
        ll_route_task_list.setVisibility(View.VISIBLE);
        routeAdapter.notifyDataSetChanged();
    }

}
