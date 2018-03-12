package com.hudai.chaodai.sockettest.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketService extends Service {
    private boolean isConnect = false;
    private PrintWriter out;
    private MyBind myBind;
    public static int SOCKET_PORT = 5001;

    public SocketService() {
    }

    private class MyBind extends Binder {
        public SocketService getService(){
            return SocketService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
//        // TODO: Return the communication channel to the service.
//        throw new UnsupportedOperationException("Not yet implemented");
        Log.d("server","onbind");
        new Thread(new TCPServer(SOCKET_PORT)).start();
        SOCKET_PORT++;
        return myBind;
    }

    private class TCPServer implements Runnable{
        private int port = 5001;
        TCPServer(int port){
            this.port = port;
        }

        @Override
        public void run() {
            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(port);
                Log.d("server","服务器已创建: "+port);
            } catch (IOException e) {
                e.printStackTrace();
                Log.d("server","服务器创建失败");
            }

            if(!isConnect){
                try {
                    Log.d("server","等待中...");
                    Socket client = serverSocket.accept();
                    Log.d("server","客户端接入");
                    new Thread(responseClient(client)).start();
                    new Thread(new HeartBeats()).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private Runnable responseClient(final Socket client){
        return new Runnable() {
            @Override
            public void run() {
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));

                    out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream())),true);
                    out.println("服务器已连接");

                    while (!isConnect){
                        if(client.isClosed()){
                            isConnect = true;
                            return;
                        }
                        Log.d("server","接收中...");
                        String inStr = in.readLine();
                        Log.d("server","收到客户端："+inStr);

                        out.println("服务器已收到："+inStr);
                    }
                    in.close();
                    out.close();
                    client.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }

    private class HeartBeats implements Runnable{
        @Override
        public void run() {
            int i = 1;
            while (!isConnect){
                if(out != null){
                    out.println("心跳: "+i);
                    i++;
                    SystemClock.sleep(1000);
                }
            }
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d("server","unbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isConnect = true;
    }
}
