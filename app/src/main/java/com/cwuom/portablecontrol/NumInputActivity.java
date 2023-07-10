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
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import com.kongzue.dialogx.dialogs.BottomDialog;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class NumInputActivity extends AppCompatActivity {
    private String PC_IP;

    private Button mBtnNum1;
    private Button mBtnNum2;
    private Button mBtnNum3;
    private Button mBtnNum4;
    private Button mBtnNum5;
    private Button mBtnNum6;
    private Button mBtnNum7;
    private Button mBtnNum8;
    private Button mBtnNum9;
    private Button mBtnNum0;

    private Button mBtnEnter;

    private float x1;


    @SuppressLint({"MissingInflatedId", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_numpad);

        Transition t1 = TransitionInflater.from(NumInputActivity.this).inflateTransition(android.R.transition.slide_top);
        Transition t2 = TransitionInflater.from(NumInputActivity.this).inflateTransition(android.R.transition.slide_bottom);
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

        mBtnNum0 = findViewById(R.id.btn_0);
        mBtnNum1 = findViewById(R.id.btn_1);
        mBtnNum2 = findViewById(R.id.btn_2);
        mBtnNum3 = findViewById(R.id.btn_3);
        mBtnNum4 = findViewById(R.id.btn_4);
        mBtnNum5 = findViewById(R.id.btn_5);
        mBtnNum6 = findViewById(R.id.btn_6);
        mBtnNum7 = findViewById(R.id.btn_7);
        mBtnNum8 = findViewById(R.id.btn_8);
        mBtnNum9 = findViewById(R.id.btn_9);
        mBtnEnter = findViewById(R.id.btn_enter);


        mBtnNum0.setOnTouchListener(buttonListener);
        mBtnNum1.setOnTouchListener(buttonListener);
        mBtnNum2.setOnTouchListener(buttonListener);
        mBtnNum3.setOnTouchListener(buttonListener);
        mBtnNum4.setOnTouchListener(buttonListener);
        mBtnNum5.setOnTouchListener(buttonListener);
        mBtnNum6.setOnTouchListener(buttonListener);
        mBtnNum7.setOnTouchListener(buttonListener);
        mBtnNum8.setOnTouchListener(buttonListener);
        mBtnNum9.setOnTouchListener(buttonListener);
        mBtnEnter.setOnTouchListener(buttonListener);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            //当手指按下的时候
            x1 = event.getX();
        }
        if(event.getAction() == MotionEvent.ACTION_UP) {
            //当手指离开的时候
            float x2 = event.getX();
            if(x1 - x2 > 50) {
                ClickVibrator();
                SendToPC("BackSpace");
            } else if(x2 - x1 > 50) {
                ClickVibrator();
                SendToPC("CtrlZ");
            }
        }
        return super.onTouchEvent(event);
    }


    @Override
    public void onBackPressed() {
        Intent intent = new Intent(NumInputActivity.this, MainActivity.class);
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(NumInputActivity.this).toBundle());
        super.onBackPressed();
    }

    void ClickVibrator(){
        Vibrator vibrator = (Vibrator)NumInputActivity.this.getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(30);
    }


    private final View.OnTouchListener buttonListener = new View.OnTouchListener() {
        @SuppressLint("ClickableViewAccessibility")
        public boolean onTouch(View arg0, MotionEvent event) {
            // TODO Auto-generated method stub
            int action = event.getAction();
            if(action == MotionEvent.ACTION_DOWN) {
                ClickVibrator();
                if (arg0.getId() == mBtnNum0.getId()){
                    SendToPC("NumPad::0DOWN");
                }
                if (arg0.getId() == mBtnNum1.getId()){
                    SendToPC("NumPad::1DOWN");
                }
                if (arg0.getId() == mBtnNum2.getId()){
                    SendToPC("NumPad::2DOWN");
                }
                if (arg0.getId() == mBtnNum3.getId()){
                    SendToPC("NumPad::3DOWN");
                }
                if (arg0.getId() == mBtnNum4.getId()){
                    SendToPC("NumPad::4DOWN");
                }
                if (arg0.getId() == mBtnNum5.getId()){
                    SendToPC("NumPad::5DOWN");
                }
                if (arg0.getId() == mBtnNum6.getId()){
                    SendToPC("NumPad::6DOWN");
                }
                if (arg0.getId() == mBtnNum7.getId()){
                    SendToPC("NumPad::7DOWN");
                }
                if (arg0.getId() == mBtnNum8.getId()){
                    SendToPC("NumPad::8DOWN");
                }
                if (arg0.getId() == mBtnNum9.getId()){
                    SendToPC("NumPad::9DOWN");
                }
                if (arg0.getId() == mBtnEnter.getId()){
                    SendToPC("NumPad::EnterDOWN");
                }
            }
            if (action == MotionEvent.ACTION_UP) {
                if (arg0.getId() == mBtnNum0.getId()){
                    SendToPC("NumPad::0UP");
                }
                if (arg0.getId() == mBtnNum1.getId()){
                    SendToPC("NumPad::1UP");
                }
                if (arg0.getId() == mBtnNum2.getId()){
                    SendToPC("NumPad::2UP");
                }
                if (arg0.getId() == mBtnNum3.getId()){
                    SendToPC("NumPad::3UP");
                }
                if (arg0.getId() == mBtnNum4.getId()){
                    SendToPC("NumPad::4UP");
                }
                if (arg0.getId() == mBtnNum5.getId()){
                    SendToPC("NumPad::5UP");
                }
                if (arg0.getId() == mBtnNum6.getId()){
                    SendToPC("NumPad::6UP");
                }
                if (arg0.getId() == mBtnNum7.getId()){
                    SendToPC("NumPad::7UP");
                }
                if (arg0.getId() == mBtnNum8.getId()){
                    SendToPC("NumPad::8UP");
                }
                if (arg0.getId() == mBtnNum9.getId()){
                    SendToPC("NumPad::9UP");
                }
                if (arg0.getId() == mBtnEnter.getId()){
                    SendToPC("NumPad::EnterUP");
                }


            }
            return false;

        }
    };




    // 对服务端发送消息
    void SendToPC(String content){
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