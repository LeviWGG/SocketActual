package com.hudai.chaodai.sockettest;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.hudai.chaodai.sockettest.service.SocketService;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class MainActivity extends AppCompatActivity {
    private Unbinder unbinder;
    private UiHandler uiHandler;
    private PrintWriter out;
    private Socket mClient;
    private SocketService socketService;

    private static boolean isFinish = false;

    @BindView(R.id.btn_connect)
    Button btnConnect;

    @BindView(R.id.btn_send)
    Button btnSend;

    @BindView(R.id.text_content)
    TextView textContent;

    @BindView(R.id.btn_disconnect)
    Button btnDisConnect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        unbinder = ButterKnife.bind(this);
//        bindService();
        uiHandler = new UiHandler();
    }

    public class UiHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1 :
                    textContent.append((String)msg.obj + "\n");
                    break;
                case 2 :
                    btnSend.setEnabled(true);
                    btnConnect.setEnabled(false);
            }
        }
    }

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    private void bindService(){
        Intent intent = new Intent(MainActivity.this, SocketService.class);
//        startService(intent);
        bindService(intent,connection,BIND_AUTO_CREATE);

//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    connectSocket();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();
    }

    private void connectSocket() throws IOException {
        Socket socket = null;
        BufferedReader in = null;
        while (socket == null){
            try {
                socket = new Socket("localhost",5001);
                mClient = socket;
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(mClient.getOutputStream())),true);
                in = new BufferedReader(new InputStreamReader(mClient.getInputStream()));
                uiHandler.sendEmptyMessage(2);
            } catch (IOException e) {
                SystemClock.sleep(1000);
            }
        }
        while (!isFinish){
            final String msg = in.readLine();
            if(!TextUtils.isEmpty(msg)){
                uiHandler.obtainMessage(1,msg).sendToTarget();
            }
            SystemClock.sleep(1000);
        }
        out.close();
        in.close();
        mClient.close();
    }

    @OnClick({R.id.btn_connect,R.id.btn_send,R.id.btn_disconnect,R.id.btn_startActivity})
    public void onClick(View view){
        switch (view.getId()){
            case R.id.btn_connect :
                bindService();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            connectSocket();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
                break;
            case R.id.btn_send :
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        out.println("这条消息来自于客户端");
                    }
                }).start();
                break;
            case R.id.btn_disconnect :
                isFinish = true;
                btnConnect.setEnabled(true);
                unbindService(connection);
                break;
            case R.id.btn_startActivity :
                Intent intent = new Intent(MainActivity.this,Main2Activity.class);
                startActivity(intent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
        isFinish = true;
    }
}
