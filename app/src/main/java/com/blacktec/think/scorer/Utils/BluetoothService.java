package com.blacktec.think.scorer.Utils;

/**
 * Created by Think on 2019/8/17.
 */

/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothService {
    // Debugging
    private static final String TAG = "BluetoothService";
    public static final int MESSAGE_DEVICE_NAME = 1;
    public static final int MESSAGE_WRITE = 2;
    public static final int MESSAGE_READ = 3;
    public static final int MESSAGE_TOAST = 4;
    public static final int MESSAGE_STATE_CHANGE = 5;
    public static final int MESSAGE_SUCCESS = 6;
    public static final int MESSAGE_FAIL = 7 ;
    public static final int MESSAGE_FILE_HEAD = 8 ;

    public static final int FILE_HEAD_LENGTH = 36;
    public String test="";

    // Name for the SDP record when creating server socket
    private static final String NAME_SECURE = "BluetoothSecure";

    private static final UUID MY_UUID_SECURE =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    private int mState;
    // 显示当前连接状态
    public static final int STATE_NONE = 0;       // 什么都不做
    public static final int STATE_LISTEN = 1;     // 监听连接
    public static final int STATE_CONNECTING = 2; // 正在建立连接
    public static final int STATE_TRANSFER = 3;  // 现在连接到一个远程的设备，可以进行传输

    //表示蓝牙服务的模式(文件/短消息)
    private ServerMode mMode;
    private TransferStatus mTransferStatus;
    //用来向主线程发送消息
    private static Handler uiHandler;
    private BluetoothAdapter bluetoothAdapter;
    //用来连接端口的线程
    private AcceptThread mAcceptThread;
    private TransferThread mTransferThread;
    private ConnectThread mConnectThread;
    private boolean isTransferError = false;
    //获取单例
    public static volatile BluetoothService instance = null;
    public static BluetoothService getInstance(Handler handler){
        uiHandler = handler;
        if(instance == null){
            synchronized(BluetoothService.class){
                if(instance == null){
                    instance = new BluetoothService();
                }
            }
        }
        return instance;
    }
    public static BluetoothService getInstance(){
        if(uiHandler==null)return null;
        return instance;
    }

    public BluetoothService(){
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        setMode(ServerMode.MODE_FILE);
    }

    /**
     * 开启服务监听
     */
    public synchronized void start(){
        if(mTransferThread != null){
            mTransferThread.cancel();
            mTransferThread = null;
        }

        setState(STATE_LISTEN);
        updateTransferStatus();
        if(mAcceptThread == null){
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        Log.e(TAG, "stop");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        if (mTransferThread != null) {
            mTransferThread.cancel();
            mTransferThread = null;
        }
        setState(STATE_NONE);
    }
    public synchronized void setMode(ServerMode mode) {
        mMode = mode;
        updateTransferStatus();
    }
    public synchronized void updateTransferStatus()
    {
        switch (mMode)
        {
            case MODE_FILE:
                mTransferStatus = TransferStatus.Waiting_Head;
                break;
            case MODE_MESSAGE:
                mTransferStatus = TransferStatus.Short_Message;
                break;
        }
    }

    public void setState(int state) {
        this.mState = state;
    }

    public int getState() {
        return mState;
    }

    /**
     * 连接访问
     * @param device
     */
    public synchronized void connectDevice(BluetoothDevice device) {
        Log.e(TAG, "connectDevice: ");
        // 如果有正在传输的则先关闭
        if (mState == STATE_CONNECTING) {
            if (mTransferThread != null) {mTransferThread.cancel(); mTransferThread = null;}
        }

        //如果有正在连接的则先关闭
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        sendMessageToUi(MESSAGE_TOAST , "正在与" + device.getName() + "连接");
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        //标志为正在连接
        setState(STATE_CONNECTING);
    }

    //连接等待线程
    class AcceptThread extends Thread{
        private final BluetoothServerSocket serverSocket;
        public AcceptThread(){
            //获取服务器监听端口
            BluetoothServerSocket tmp = null;
            try {
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE, MY_UUID_SECURE);
            }   catch (IOException e) {
                e.printStackTrace();
            }
            serverSocket = tmp;
        }
        @Override
        public void run() {
            super.run();
            //监听端口
            BluetoothSocket socket = null;
            while(mState != STATE_TRANSFER) {
                try {
                    Log.e(TAG, "run: AcceptThread 阻塞调用，等待连接");
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "run: ActivityThread fail");
                    break;
                }
                //获取到连接Socket后则开始通信
                if(socket != null){
                    synchronized (BluetoothService.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                //传输数据，服务器端调用
                                Log.e(TAG, "run: 服务器AcceptThread传输" );
                                sendMessageToUi(MESSAGE_TOAST , "正在与" + socket.getRemoteDevice().getName() + "连接");
                                dataTransfer(socket, socket.getRemoteDevice());
                                break;
                            case STATE_NONE:
                            case STATE_TRANSFER:
                                // 没有准备好或者终止连接
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket" + e);
                                }
                                break;
                        }
                    }
                }
            }
        }

        public void cancel(){
            Log.e(TAG, "close: activity Thread" );
            try {
                if(serverSocket != null)
                    serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "close: activity Thread fail");
            }
        }
    }

    private void sendMessageToUi(int what, String s) {
        Message message = uiHandler.obtainMessage();
        message.what = what;
        message.obj = s;
        uiHandler.sendMessage(message);
    }

    /**
     * 开始连接通讯
     * @param socket
     * @param remoteDevice 远程设备
     */
    private void dataTransfer(BluetoothSocket socket,final BluetoothDevice remoteDevice) {
        //关闭连接线程，这里只能连接一个远程设备
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        // 启动管理连接线程和开启传输
        mTransferThread = new TransferThread(socket);
        mTransferThread.start();
        //标志状态为连接
        setState(STATE_TRANSFER);
        uiHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(!isTransferError){
                    sendMessageToUi(MESSAGE_SUCCESS , remoteDevice.getName());
                }
            }
        } , 300);
    }

    /**
     * 传输数据
     * @param out
     */
    public void sendData(byte[] out){
        TransferThread r;
        synchronized (this) {
            if (mState != STATE_TRANSFER) return;
            r = mTransferThread;
        }
        r.write(out);
    }

    /**
     * 传输Json文件
     * @param s
     */
    public void sendFileString(String s){
        TransferThread r;
        synchronized (this) {
            if (mState != STATE_TRANSFER) return;
            r = mTransferThread;
        }
        byte[] bytes=s.getBytes();
        byte[] head = new byte[FILE_HEAD_LENGTH];
        String md5 = MD5Util.stringToMD5(bytes);
        if(md5!=null) System.arraycopy(md5.getBytes(),0,head,0,32);
        System.arraycopy(ByteArrayUtils.intToByteArray(bytes.length),0,head,32,4);
        r.write(head);
        r.write(bytes);
    }

    /**
     * 用来传输数据的线程
     */
    class TransferThread extends Thread{
        private final BluetoothSocket socket;
        private final OutputStream out;
        private final InputStream in;
        public TransferThread(BluetoothSocket mBluetoothSocket){
            socket = mBluetoothSocket;
            OutputStream mOutputStream = null;
            InputStream mInputStream = null;
            try {
                if(socket != null){
                    //获取连接的输入输出流
                    mOutputStream = socket.getOutputStream();
                    mInputStream = socket.getInputStream();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            out = mOutputStream;
            in = mInputStream;
            isTransferError = false;
        }
        @Override
        public void run() {
            super.run();
            //读取数据
            byte[] buffer;
            int bytes;
            while (true){
                try {
                    buffer = new byte[1024];
                    bytes = in.read(buffer,0,FILE_HEAD_LENGTH);//md5 32位 length 4位
                    if(mMode == ServerMode.MODE_FILE &&mTransferStatus==TransferStatus.Waiting_Head)
                    {
                        if(bytes!=FILE_HEAD_LENGTH)continue;
                        mTransferStatus = TransferStatus.Receiving_File;
                        uiHandler.obtainMessage(MESSAGE_FILE_HEAD,bytes,-1,buffer).sendToTarget();
                        continue;
                    }
                    bytes += in.read(buffer,FILE_HEAD_LENGTH,1024-FILE_HEAD_LENGTH);
                    //bytes = in.read(buffer);
                    //TODO 分发到主线程显示
                    uiHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                    Log.e(TAG, "run: read " + new String(buffer , 0 , bytes) );
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "run: Transform error"  + e.toString());
                    BluetoothService.this.start();
                    //TODO 连接丢失显示并重新开始连接
                    sendMessageToUi(MESSAGE_FAIL , "设备连接失败/传输关闭");
                    isTransferError = true;
                    break;
                }
            }
        }

        /**
         * 写入数据传输
         * @param buffer
         */
        public void write(byte[] buffer) {
            try {
                out.write(buffer);
                //TODO 到到UI显示
                uiHandler.obtainMessage(MESSAGE_WRITE , -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write " + e);
            }
        }

        public void cancel() {
            try {
                if(socket != null)
                    socket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed" + e);
            }
        }
    }

    class ConnectThread extends Thread{
        private final BluetoothSocket socket;
        private final BluetoothDevice device;
        public ConnectThread(BluetoothDevice device) {
            this.device = device;
            BluetoothSocket mSocket = null;
            try {
                //建立通道
                mSocket = device.createRfcommSocketToServiceRecord(
                        MY_UUID_SECURE);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "ConnectThread: fail" );
                sendMessageToUi(MESSAGE_TOAST , "连接失败，请重新连接");
            }
            socket = mSocket;
        }

        @Override
        public void run() {
            super.run();
            //建立后取消扫描
            bluetoothAdapter.cancelDiscovery();

            try {
                Log.e(TAG, "run: connectThread 等待" );
                socket.connect();
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    socket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                    Log.e(TAG, "run: unable to close" );
                }
                //TODO 连接失败显示
                sendMessageToUi(MESSAGE_TOAST , "连接失败，请重新连接");
                BluetoothService.this.start();
            }


            // 重置
            synchronized (BluetoothService.this) {
                mConnectThread = null;
            }
            //Socket已经连接上了，默认安全,客户端才会调用
            Log.e(TAG, "run: connectThread 连接上了,准备传输");
            dataTransfer(socket, device);
        }

        public void cancel(){
            try {
                if(socket != null)
                    socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private enum TransferStatus {Short_Message,Waiting_Head,Receiving_File}
    public enum ServerMode {MODE_MESSAGE,MODE_FILE}
}

