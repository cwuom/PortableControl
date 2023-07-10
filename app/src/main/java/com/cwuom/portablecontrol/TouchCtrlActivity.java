package com.cwuom.portablecontrol;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Vibrator;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.TextView;

import com.kongzue.dialogx.DialogX;
import com.kongzue.dialogx.dialogs.BottomDialog;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
public class TouchCtrlActivity extends AppCompatActivity {

    private float x1, y1;
    private float x2, y2;

    private String PC_IP;

    private MotionEvent event_;
    private boolean moving;
    private boolean break_pd;

    private int ClickTime = 0;
    private boolean LongClick;

    private boolean DoubleClick = false;

    private TextView mTvData;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_touch_ctrl);

        DialogX.init(this);

        mTvData = findViewById(R.id.tv_data);

        Transition t1 = TransitionInflater.from(TouchCtrlActivity.this).inflateTransition(android.R.transition.slide_top);
        Transition t2 = TransitionInflater.from(TouchCtrlActivity.this).inflateTransition(android.R.transition.slide_bottom);
        getWindow().setEnterTransition(t1);
        getWindow().setExitTransition(t2);


        SharedPreferences sharedPreferences = getSharedPreferences("devices", MODE_PRIVATE);
        @SuppressLint("CommitPrefEdits") SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("running", true);
        PC_IP = sharedPreferences.getString("IP", "NULL");
        if (PC_IP.equals("NULL")) {
            BottomDialog.show("无法使用", "你没有绑定任何设备，无法使用此服务。请在运行PC服务端的情况下点击下方'退出'后在主界面选择'搜索设备'来绑定您的设备！")
                    .setOkButton("退出", (baseDialog, v) -> {
                        finish();
                        return false;
                    });
        }
    }


    @SuppressLint("SetTextI18n")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        event_ = event;
        //继承了Activity的onTouchEvent方法，直接监听点击事件
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            break_pd = false;
            ClickTime = 0;
            //当手指按下的时候
            x1 = event.getX();
            y1 = event.getY();
            new Thread(() -> {
                while (!break_pd && !DoubleClick) {
                    ClickTime += 1;
                    Log.e("ClickTime", String.valueOf(ClickTime));
                    if (ClickTime > 300 && ClickTime <= 500) {
                        Vibrator vibrator = (Vibrator) TouchCtrlActivity.this.getSystemService(VIBRATOR_SERVICE);
                        vibrator.vibrate(300);
                    }
                    if (ClickTime > 500) {
                        SendToPC("!LongClick");
                        LongClick = true;
                        break_pd = true;
                    }
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }

            }).start();
        }
        if(event.getAction() == MotionEvent.ACTION_MOVE) {
            //当手指移动的时候
            x2 = event.getX();
            y2 = event.getY();

            moving = true;
            break_pd = true;

            mTvData.setText("x:"+(-(x2 - x1))+" | y:"+(-(y2 - y1)));

            if (!DoubleClick){
                new Thread(() -> {
                    SendToPC("{\"x\":\""+((x2 - x1))+"\", \"y\":\""+((y2 - y1))+"\"}");
                    Log.e("SendData","{\"x\":\""+((x2 - x1))+"\", \"y\":\""+((y2 - y1))+"\"}");
                }).start();
            }else{
                SendToPC("S::"+(y2 - y1));
                Log.e("(y2 - y1)", String.valueOf((y2 - y1)));
            }


        }
        if (event.getAction()==MotionEvent.ACTION_UP){
            break_pd = true;
            DoubleClick = false;
            if(!moving){
                if (ClickTime >= 300 && ClickTime <= 500){
                    SendToPC("!RClick");
                }else{
                    if (LongClick){
                        SendToPC("!Up");
                        Vibrator vibrator = (Vibrator)TouchCtrlActivity.this.getSystemService(VIBRATOR_SERVICE);
                        vibrator.vibrate(10);
                        vibrator.vibrate(10);
                        LongClick = false;
                    }else{
                        SendToPC("!LClick");
                        Vibrator vibrator = (Vibrator)TouchCtrlActivity.this.getSystemService(VIBRATOR_SERVICE);
                        vibrator.vibrate(30);
                    }

                }

            }
            moving = false;
            ClickTime = 0;
        }


        if (event.getAction() == MotionEvent.ACTION_POINTER_2_DOWN){
            DoubleClick = true;
        }




        return super.onTouchEvent(event);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(TouchCtrlActivity.this, MainActivity.class);
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(TouchCtrlActivity.this).toBundle());
        super.onBackPressed();
    }

    // 对服务端发送消息
    void SendToPC(String content) {
        new Thread(() -> {
            try {
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

                    x1 = event_.getX();
                    y1 = event_.getY();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

}
