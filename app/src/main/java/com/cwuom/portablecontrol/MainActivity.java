package com.cwuom.portablecontrol;

import static com.cwuom.portablecontrol.NetworkUtils.getLocalIPAddress;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.mmin18.widget.RealtimeBlurView;
import com.google.android.material.snackbar.Snackbar;
import com.kongzue.dialogx.DialogX;
import com.kongzue.dialogx.dialogs.BottomDialog;
import com.kongzue.dialogx.dialogs.BottomMenu;
import com.kongzue.dialogx.dialogs.FullScreenDialog;
import com.kongzue.dialogx.dialogs.InputDialog;
import com.kongzue.dialogx.dialogs.MessageDialog;
import com.kongzue.dialogx.dialogs.WaitDialog;
import com.kongzue.dialogx.interfaces.BaseDialog;
import com.kongzue.dialogx.interfaces.OnBindView;
import com.kongzue.dialogx.interfaces.OnIconChangeCallBack;
import com.kongzue.dialogx.style.IOSStyle;
import com.lky.toucheffectsmodule.TouchEffectsManager;
import com.lky.toucheffectsmodule.factory.TouchEffectsFactory;
import com.lky.toucheffectsmodule.types.TouchEffectsViewType;
import com.lky.toucheffectsmodule.types.TouchEffectsWholeType;
import com.ncorti.slidetoact.SlideToActView;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

/**
 * @author cwuom 
 * bilibili@im-cwuom
 * */


public class MainActivity extends AppCompatActivity {
    
    // 点击效果设置
    static {
        TouchEffectsManager.build(TouchEffectsWholeType.SCALE)
                .addViewType(TouchEffectsViewType.ALL)
                .setListWholeType(TouchEffectsWholeType.RIPPLE);
    }

    protected static final float FLIP_DISTANCE = 50;
    GestureDetector mDetector;

    private String volume = "no connection";

    private TextView mTvVol;
    private TextView mTvTime;
    private CircularProgressView circularProgressView;
    private BottomMenu devicesMenu;

    private boolean running = false;

    private String PC_IP = "NULL";


    private String date; // 日期
    private boolean state; // 加减音量状态 False - True +
//    private  devices;
    private final ArrayList<String> devices = new ArrayList<>(); // 设备列表
    private final ArrayList<Thread> threads = new ArrayList<>(); // 线程列表
    private final Handler handler = new MyHandler(this);
    private SharedPreferences.Editor editor; // 绑定IP的时候会用到

    private int px = 0; // 扫描进度

    private WaitDialog waitDialog = WaitDialog.build(); // 扫描进度等待框
    private WaitDialog waitDialog2 = WaitDialog.build(); // 建立通讯等待框

    private boolean m = true; // 静音状态

    private boolean isExit = false; // 是否跳转到另一个act，提前结束进程

    private RealtimeBlurView realtimeBlurView;  // 遮罩模糊

    private BottomDialog wrongDialog; // 搜索失败，无设备
    private BottomMenu menu;

    private int rx = 0; // 模糊值

    private RelativeLayout mRlPower;  // 确认执行电源操作控件，默认为gone

    private int power_mode = -1;  // 电源选项

    private int HeartBeat = 0;  // 心跳包计数器

    private Snackbar snackbar;

    private String MY_IP = "";  // 当前设备IP

    private boolean isReceived = false; // 是否收到PC端响应

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        TouchEffectsFactory.initTouchEffects(this);  // 初始化触摸效果
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 初始化UI库，误删会闪
        DialogX.init(this);

        mTvTime = findViewById(R.id.tv_time);
        mTvVol = findViewById(R.id.tv_vol);
        circularProgressView = findViewById(R.id.CircularProgressView);

        ImageButton mBtnPaused = findViewById(R.id.paused);
        ImageButton mBtnPrevious = findViewById(R.id.previous);
        ImageButton mBtnNext = findViewById(R.id.next);

        Button mBtnMenu = findViewById(R.id.menu);

        realtimeBlurView = findViewById(R.id.blur);

        mRlPower = findViewById(R.id.rl_power);
        // 取消电源操作
        TextView mTvCancel = findViewById(R.id.tv_cancel);
        // 滑动执行电源操作
        SlideToActView slideToActView = findViewById(R.id.st_ok);


        // 初始化
        init();


        // 播放/暂停、下一首、上一首
        mBtnPaused.setOnClickListener(v -> SendToPC("!Paused"));
        mBtnNext.setOnClickListener(v -> SendToPC("!Next"));
        mBtnPrevious.setOnClickListener(v -> SendToPC("!Previous"));

        // 菜单
        mBtnMenu.setOnClickListener(v -> {
            ClickVibrator(60);
            menu = BottomMenu.show(new String[]{"绑定设备...", "滑动控制", "小键盘", "电源管理"})
                    .setOnIconChangeCallBack(new OnIconChangeCallBack(true) {
                        @Override
                        public int getIcon(BaseDialog dialog, int index, String menuText) {
                            switch (menuText) {
                                case "绑定设备...":
                                    return R.drawable.search;
                                case "滑动控制":
                                    return R.drawable.slide;
                                case "小键盘":
                                    return R.drawable.num0;
                                case "电源管理":
                                    return R.drawable.shutdown;
                            }

                            return 0;
                        }
                    })
                    .setOnMenuItemClickListener((dialog, text, index) -> {
                    if (text == "绑定设备..."){
                        BindDevice();
                    }

                    if (text == "滑动控制") {
                        isExit = true;
                        Intent intent = new Intent(MainActivity.this, TouchCtrlActivity.class);
                        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(MainActivity.this).toBundle());
                        finish();
                    }
                    if (text == "小键盘") {
                        isExit = true;
                        Intent intent = new Intent(MainActivity.this, NumInputActivity.class);
                        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(MainActivity.this).toBundle());
                        finish();
                    }

                    if (text == "电源管理") {
                        PowerCtrl();
                    }
                    return true;
                    });
        });

        mTvCancel.setOnClickListener(view -> {
            new Thread(this::CancelBlur).start();

            power_mode = -1;
            mRlPower.setVisibility(View.GONE);
        });

        slideToActView.setOnSlideCompleteListener(slideToActView1 -> {
            if (power_mode == 1){ // 判断电源模式
                SendToPC("!sleep");
            }else{
                SendToPC("!shutdown");
            }

            new Thread(this::CancelBlur).start();

            // 重置设置
            slideToActView1.resetSlider();

            power_mode = -1;
            mRlPower.setVisibility(View.GONE);
        });

    }


    @SuppressLint("HandlerLeak")
    private class MyHandler extends Handler {

        //弱引用持有HandlerActivity , GC 回收时会被回收掉
        private final WeakReference<MainActivity> weakReference;

        public MyHandler(MainActivity activity) {
            this.weakReference = new WeakReference<>(activity);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(Message msg) { // 子线程不能更新UI
            weakReference.get();
            super.handleMessage(msg);
            if (msg.what == 0) {  // 设置时间
                mTvTime.setText(date);
            }
            if (msg.what == 1){  // 音量监听
                if (Objects.equals(volume, "!m = False")){
                    m = false;
                    circularProgressView.setProgColor(R.color.deep_red, R.color.deep_red);
                    SendToPC("!GetVolume");
                } else if (Objects.equals(volume, "!m = True")){
                    m = true;
                    circularProgressView.setProgColor(R.color.white, R.color.blue);
                    SendToPC("!GetVolume");
                } else{
                    try {
                        mTvVol.setText(volume);
                        circularProgressView.setProgress(Integer.parseInt(volume), 300);
                    } catch (NumberFormatException e) {
                        mTvVol.setText("no connection");
                    }

                }
            }
            if (msg.what == 2){
                String[] devices_list = new String[devices.size()];
                for (int x = 0; x < devices.size(); x++) {
                    devices_list[x] = devices.get(x);
                }
                if (devicesMenu != null && devicesMenu.isShow()){
                    devicesMenu.dismiss();
                }
                devicesMenu = BottomMenu.show(devices_list)
                        .setMessage("设备列表，点击选择您的设备")
                        .setOnMenuItemClickListener((dialog, text, index) -> {
                            Log.e("write::", devices_list[index]);
                            editor.putString("IP", devices_list[index]);
                            editor.commit();
                            PC_IP = devices_list[index];
                            waitDialog.doDismiss();
                            SendToPC("!GetVolume");
                            SendToPC("!GetM");
                            return false;
                        });
            }

            if (msg.what == 3){
                realtimeBlurView.setBlurRadius(rx);
            }

            if (msg.what == 4){
                mRlPower.setVisibility(View.VISIBLE);
            }


            if (msg.what == 404){
                wrongDialog = BottomDialog.show("搜索失败", "没有搜索到任何设备，在运行PC服务端的情况下可尝试点击'重新搜索'来绑定您的PC！")
                        .setCancelButton("取消", (dialog, v) -> false)
                        .setOkButton("重新搜索", (baseDialog, v) -> {
                            try {
                                String ip = MY_IP;
                                ip = ip.split("\\.")[0] + "." + ip.split("\\.")[1] + "." + ip.split("\\.")[2] + ".";
                                FindDevice(ip);
                            } catch (SocketException e) {
                                throw new RuntimeException(e);
                            }
                            return false;
                        });
            }

            if (msg.what == 114514){
                new InputDialog("输入您的设备", "通过手动输入IPv4地址来绑定您的PC。", "确定", "取消", "")
                        .setCancelable(false)
                        .setOkButton((baseDialog, v, inputStr) -> {
                            waitDialog2 = WaitDialog.build().setStyle(IOSStyle.style());
                            waitDialog2.setMessageContent("正在建立通讯...");
                            waitDialog2.show();
                            SendToPC_Bind(inputStr);
                            return false;
                        })
                        .show();
            }

            if (msg.what == 5){
                ShowWaitDialog("连接失败", WaitDialog.TYPE.ERROR);
            }

            if (msg.what == 6){
                ShowWaitDialog("绑定成功", WaitDialog.TYPE.SUCCESS);
            }


        }
    }

    // 拦截返回键 -> 静音
    @Override
    public void onBackPressed() {
        ClickVibrator(500);
        new Thread(() -> SendToPC("!Mute")).start();
    }

    // 拦截触摸事件
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mDetector.onTouchEvent(event);
    }

    void BindDevice(){
        String ip = MY_IP;
        ip = ip.split("\\.")[0] + "." + ip.split("\\.")[1] + "." + ip.split("\\.")[2] + ".";
        BottomDialog.show("找不到您的设备？", "若在PC服务端运行的情况下无法正常通讯，您可点击'是'来重新扫描局域网中的所有设备。\n点击'手动输入..'可自行输入设备IP\n扫描范围: "+ ip + "*")
                .setOkButton("是", (dialog, v) -> {
                    try {
                        FindDeviceOnClick();

                    } catch (SocketException e) {
                        throw new RuntimeException(e);
                    }
                    return false;
                })
                .setCancelButton("手动输入..", (baseDialog, v) -> {
                    handler.sendEmptyMessage(114514);
                    return false;
                });
    }
    
    void PowerCtrl(){
        FullScreenDialog.show(new OnBindView<FullScreenDialog>(R.layout.layout_power) {
            @Override
            public void onBind(FullScreenDialog dialog, View v) {
                TextView tv_close = v.findViewById(R.id.tv_close);
                ImageButton btn_shutdown = v.findViewById(R.id.btn_shutdown);
                ImageButton btn_sleep = v.findViewById(R.id.btn_sleep);

                tv_close.setOnClickListener(view -> dialog.dismiss());

                btn_sleep.setOnClickListener(view -> {
                    handler.sendEmptyMessage(4);
                    power_mode = 1;
                    menu.dismiss();
                    dialog.dismiss();
                });
                btn_shutdown.setOnClickListener(view -> {
                    handler.sendEmptyMessage(4);
                    power_mode = 2;
                    menu.dismiss();
                    dialog.dismiss();
                });
            }
        });
    }

    void ShowWaitDialog(String content, WaitDialog.TYPE TYPE){
        WaitDialog waitDialog = WaitDialog.build().setStyle(IOSStyle.style());
        waitDialog.setMessageContent(content);
        waitDialog.setTipType(TYPE);
        waitDialog.show();
    }

    // 点击搜索设备时会触发
    void FindDeviceOnClick() throws SocketException {
        String ip = MY_IP;
        ip = ip.split("\\.")[0] + "." + ip.split("\\.")[1] + "." + ip.split("\\.")[2] + ".";
        if (!running){
            FindDevice(ip);
        }else{
            MessageDialog.show("请不要重复搜索！", "搜索设备需要对当前局域网中的所有设备逐一扫描并等待回复，请耐心等待。", "确定");
        }
    }

    void ClickVibrator(int time){
        Vibrator vibrator = (Vibrator)MainActivity.this.getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(time);
    }

    void FindDevice(String ip) throws SocketException {
        devices.clear();
        threads.clear();
        px = 0;
        waitDialog = WaitDialog.build().setStyle(IOSStyle.style());
        waitDialog.setMessageContent("扫描中...");
        waitDialog.setProgress(0.0F);
        waitDialog.doDismiss();
        waitDialog.show();

        for(int x = 0; x <= 255; x++){
            String ip_s = ip + x;
            Log.e("scan", ip_s);
            Thread t =  new Thread(() -> {
                running = true;
                try {
                    //IP地址和端口号（对应服务端），我这的IP是本地路由器的IP地址
                    int portStr = 3333;
                    //发送给服务端的消息
                    try (Socket socket = new Socket(ip_s, portStr)) {
                        String message = "!Find";
                        //第二个参数为True则为自动flush
                        PrintWriter out = new PrintWriter(
                                new BufferedWriter(new OutputStreamWriter(
                                        socket.getOutputStream())), true);
                        out.println(message);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    //关闭Socket
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                waitDialog.setMessageContent("搜索设备中..." + ((px+1f) / 256f)*100 + "%");
                waitDialog.setProgress((px+1f) / 256f);
                px = px + 1;
                Log.e("setProgress", String.valueOf(px));
            });
            t.start();
            threads.add(t);
        }

        new Thread(() -> {
            for (int x = 0; x < threads.size(); x++){
                try {
                    handler.sendEmptyMessage(3);
                    threads.get(x).join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            if (devices.size() == 0){
                handler.sendEmptyMessage(404);
            }

            waitDialog.doDismiss();

            running = false;

        }).start();
    }


    void SendToPC_Bind(String IP) {
        SendIP();
        new Thread(() -> {
            try {
                //IP地址和端口号（对应服务端），我这的IP是本地路由器的IP地址
                int portStr = 3333;
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(IP, portStr), 3000);
                //发送给服务端的消息
                try {
                    //第二个参数为True则为自动flush
                    PrintWriter out = new PrintWriter(
                            new BufferedWriter(new OutputStreamWriter(
                                    socket.getOutputStream())), true);
                    out.println("!Bind");

                    editor.putString("IP", IP);
                    editor.commit();
                    PC_IP = IP;
                    waitDialog2.doDismiss();
                    handler.sendEmptyMessage(6);


                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    //关闭Socket
                    socket.close();
                }
            } catch (IOException e1) {
                waitDialog2.doDismiss();
                handler.sendEmptyMessage(114514);
                handler.sendEmptyMessage(5);
                e1.printStackTrace();
            }
        }).start();
    }


    // 对服务端发送消息
    void SendToPC(String content){
        new Thread(() -> {
            //IP地址和端口号（对应服务端），我这的IP是本地路由器的IP地址
            String addStr = PC_IP;
            int portStr = 3333;
            //发送给服务端的消息
            try (Socket socket = new Socket(addStr, portStr)) {
                //第二个参数为True则为自动flush
                PrintWriter out = new PrintWriter(
                        new BufferedWriter(new OutputStreamWriter(
                                socket.getOutputStream())), true);
                out.println(content);
            } catch (Exception e) {
                e.printStackTrace();
            }
            //关闭Socket
        }).start();
    }

    void SendIP(){
        if (!isReceived){
            new Thread(() -> {
                try {
                    //IP地址和端口号（对应服务端），我这的IP是本地路由器的IP地址
                    String addStr = PC_IP;
                    int portStr = 3333;
                    //发送给服务端的消息
                    try (Socket socket = new Socket(addStr, portStr)) {
                        String message = "IP::" + MY_IP;
                        //第二个参数为True则为自动flush
                        PrintWriter out = new PrintWriter(
                                new BufferedWriter(new OutputStreamWriter(
                                        socket.getOutputStream())), true);
                        out.println(message);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    //关闭Socket
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }
    }


    void ShowSnackbar(String info1, String info2, int LENGTH){
        View rootView = MainActivity.this.getWindow().getDecorView();
        View coordinatorLayout = rootView.findViewById(android.R.id.content);
        snackbar = Snackbar.make(coordinatorLayout, "", LENGTH);
        snackbar.getView().setBackgroundColor(Color.TRANSPARENT);
        Snackbar.SnackbarLayout snackbarView = (Snackbar.SnackbarLayout) snackbar.getView();
        ViewGroup.LayoutParams layoutParams = snackbarView.getLayoutParams();
        FrameLayout.LayoutParams fl = new FrameLayout.LayoutParams(layoutParams.width, layoutParams.height);
        fl.gravity = Gravity.BOTTOM;
        snackbarView.setLayoutParams(fl);
        @SuppressLint("InflateParams") View inflate = LayoutInflater.from(snackbar.getView().getContext()).inflate(R.layout.layout_snackbar_view, null);
        TextView text = inflate.findViewById(R.id.tv_snackbar);
        text.setText(info1);
        TextView text2 = inflate.findViewById(R.id.tv_progress);
        text2.setText(info2);
        snackbarView.addView(inflate);
        snackbar.show();
    }

    void StartBlur() {
        for (int x = 0; x < 80; x++) {
            rx = x;
            handler.sendEmptyMessage(3);
            try {
                Thread.sleep(4);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    void CancelBlur(){
        for (int x = 80; x >= 0; x--){
            rx = x;
            handler.sendEmptyMessage(3);
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);

            }
        }
        rx = 0;
        handler.sendEmptyMessage(3);
    }


    // 初始化，创建必要线程
    void init(){
        waitDialog = WaitDialog.build().setStyle(IOSStyle.style());

        // 设置进入、退出动画
        Transition t1 = TransitionInflater.from(MainActivity.this).inflateTransition(android.R.transition.slide_top);
        Transition t2 = TransitionInflater.from(MainActivity.this).inflateTransition(android.R.transition.slide_bottom);
        getWindow().setEnterTransition(t1);
        getWindow().setExitTransition(t2);

        waitDialog2 = WaitDialog.build().setStyle(IOSStyle.style());
        waitDialog2.setMessageContent("正在建立通讯...");

        try {
            MY_IP = getLocalIPAddress();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

        // 轻量存储，用于存储绑定的设备IP
        SharedPreferences sharedPreferences = getSharedPreferences("devices", MODE_PRIVATE);
        editor = sharedPreferences.edit();
        PC_IP = sharedPreferences.getString("IP","NULL");
        if (PC_IP.equals("NULL")){
            wrongDialog = BottomDialog.show("无设备", "你没有绑定任何设备，请在运行PC服务端的情况下点击下方'搜索'再继续使用！")
                    .setOkButton("搜索", (baseDialog, v) -> {
                        try {
                            FindDeviceOnClick();
                        } catch (SocketException e) {
                            throw new RuntimeException(e);
                        }
                        return false;
                    });
        }


        // 创建心跳包
        new Thread(() -> {
            while (!isExit) {
                SendToPC("!HeartBeat");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                SendToPC("!GetVolume");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                SendIP();
            }
        }).start();

        new Thread(() -> {
            while (true){
                if (!Objects.equals(MY_IP, "")){  // 若本地IP不为空，则发送自己的IP地址
                    SendIP();
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    SendToPC("!GetVolume");
                    if (isReceived){
                        break;
                    }
                }
            }
        }).start();

        new Thread(() -> {
            while (!isExit) {
                if (HeartBeat > 15 && !PC_IP.equals("NULL")){
                    volume = "连接丢失";
                    handler.sendEmptyMessage(1);
                    ShowSnackbar(PC_IP+"断开连接，请检查服务端连接状态", "", Snackbar.LENGTH_LONG);
                    while (HeartBeat != 0){}
                }else if(HeartBeat > 10 && !PC_IP.equals("NULL")){
                    volume = "正在重连";
                    handler.sendEmptyMessage(1);
                    ShowSnackbar(PC_IP+"断开连接，正在重连..", (HeartBeat - 10)+"/5", Snackbar.LENGTH_SHORT);
                }
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                HeartBeat = HeartBeat + 1;
            }
        }).start();

        // 创建监听音量线程
        new Thread(() -> {
            while (!isExit) {
                if (menu != null && menu.isShow() && power_mode == -1){
                    StartBlur();
                    while (true){
                        if (!menu.isShow() && power_mode == -1){
                            CancelBlur();
                            break;
                        }
                    }
                }
            }
        }).start();

        // 创建监听音量线程
        new Thread(() -> {
            while (!isExit) {
                if (wrongDialog != null && wrongDialog.isShow()){
                    StartBlur();
                    while (true){
                        if (!wrongDialog.isShow()){
                            CancelBlur();
                            break;
                        }
                    }
                }
            }
        }).start();

        // 初始化圆环进度条的颜色 渐变
        circularProgressView.setProgColor(R.color.white, R.color.blue);

        mDetector = new GestureDetector(this, new GestureDetector.OnGestureListener() {

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public void onShowPress(MotionEvent e) {
                // TODO Auto-generated method stub

            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                // TODO Auto-generated method stub

            }

            /**
             *
             * e1 The first down motion event that started the fling. e2 The
             * move motion event that triggered the current onFling.
             */

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1.getX() - e2.getX() > FLIP_DISTANCE) {
                    if (!m){
                        SendToPC("!Mute");
                    }
                    Log.i("TOUCH", "向左滑...");
                    ClickVibrator(50);

                    handler.sendEmptyMessage(1);

                    SendToPC("!Sub");
                    state = false;
                    return true;
                }
                if (e2.getX() - e1.getX() > FLIP_DISTANCE) {
                    if (!m){
                        SendToPC("!Mute");
                    }
                    Log.i("TOUCH", "向右滑...");
                    ClickVibrator(50);

                    handler.sendEmptyMessage(1);

                    SendToPC("!Add");
                    state = true;
                    return true;
                }
                if (e1.getY() - e2.getY() > FLIP_DISTANCE) {
                    if (!m){
                        SendToPC("!Mute");
                    }
                    Log.i("TOUCH", "向上滑...");
                    ClickVibrator(50);

                    handler.sendEmptyMessage(1);

                    SendToPC("!Add");
                    state = true;
                    return true;
                }
                if (e2.getY() - e1.getY() > FLIP_DISTANCE) {
                    if (!m){
                        SendToPC("!Mute");
                    }
                    Log.i("TOUCH", "向下滑...");
                    ClickVibrator(50);

                    handler.sendEmptyMessage(1);
                    SendToPC("!Sub");
                    state = false;
                    return true;
                }

                Log.d("TAG", e2.getX() + " " + e2.getY());

                return false;
            }

            @Override
            public boolean onDown(MotionEvent e) {
                // TODO Auto-generated method stub
                return false;
            }
        });

        // 创建更新时间线程
        new Thread(() -> {
            while (!isExit) {
                try {
                    Thread.sleep(500);
                    @SuppressLint("SimpleDateFormat") SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");// HH:mm:ss
                    date = simpleDateFormat.format(new Date(System.currentTimeMillis()));
                    handler.sendEmptyMessage(0);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

        }).start();

        // 接收服务端的消息
        new Thread(() -> {
            try {
                ServerSocket serverSocket = null;
                while (!isExit){
                    try {
                        System.out.println("Server: Connecting...");
                        serverSocket = new ServerSocket(4333);
                        SendToPC("!GetVolume");
                        break;
                    } catch (IOException ignored) {}
                }
                while (true) {
                    if (isExit){
                        try {
                            Objects.requireNonNull(serverSocket).close();
                        } catch (IOException ignored) {}

                        break;
                    }
                    System.out.println("Server:Connected.");
                    try (Socket client = serverSocket.accept()) {
                        System.out.println("Server: Receiving...");
                        //接受数据，不允许有中文
                        DataInputStream in = new DataInputStream(client.getInputStream());
                        byte[] buffer = new byte[1024];  //缓冲区的大小
                        in.read(buffer);               //处理接收到的报文，转换成字符串
                        String str = new String(buffer, "GB2312").trim();
                        Log.e("Server", "Received: " + str);
                        isReceived = true;
                        if (Objects.equals(str, "OK!!")) {
                            devices.add(String.valueOf(client.getInetAddress()).replace("/", ""));
                            handler.sendEmptyMessage(2);

                        } else if (Objects.equals(str, "!!HeartBeat")) {
                            HeartBeat = 0;
                            snackbar.dismiss();
                        }
                        else {
                            volume = str;
                        }

                        handler.sendEmptyMessage(1);
                    } catch (Exception e) {
                        Log.e("Server", "Error");
                        e.printStackTrace();
                    } finally {
                        Log.e("Server", "Done.");
                    }
                }
            } catch (Exception e) {
                Log.e("Server","Error");
                e.printStackTrace();
            }

            try {
                Thread.sleep(8000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();

    }

}