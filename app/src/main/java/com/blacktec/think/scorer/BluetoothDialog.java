package com.blacktec.think.scorer;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.blacktec.think.scorer.Interface.BluetoothConnectedTask;
import com.blacktec.think.scorer.Utils.BluetoothService;

/**
 * Created by Think on 2019/8/14.
 */

public class BluetoothDialog {
    private int timeOut;
    private Handler mHandler;
    private Context mContext;
    private AlertDialog mDialog;
    private BluetoothService mBTService;
    BluetoothConnectedTask mConnectedTask;
    public BluetoothDialog()
    {
    }
    public BluetoothDialog(BluetoothConnectedTask task)
    {
        mConnectedTask = task;
    }
    public void waitingForConnection(Context context) {
        mContext = context;
        timeOut=0;
        mHandler = new Handler()
        {
            @Override
            public void handleMessage(Message msg)
            {
                super.handleMessage(msg);
                switch (msg.what)
                {
                    case BluetoothService.MESSAGE_DEVICE_NAME:
                        break;
                    case BluetoothService.MESSAGE_WRITE:
                        break;
                    case BluetoothService.MESSAGE_READ:
                        break;
                    case BluetoothService.MESSAGE_STATE_CHANGE:
                        break;
                    case BluetoothService.MESSAGE_TOAST:
                        Toast.makeText(mContext,(String)msg.obj,Toast.LENGTH_SHORT).show();
                        break;
                    case BluetoothService.MESSAGE_SUCCESS:
                        Toast.makeText(mContext,String.format("连接 %s 成功！",(String)msg.obj),Toast.LENGTH_SHORT).show();
                        mDialog.dismiss();
                        if(mConnectedTask!=null) mConnectedTask.connected();
                        break;
                }
            }
        };
        mBTService = BluetoothService.getInstance(mHandler);
        final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) return;
        if (!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
        }
        mBTService.start();
        View view = View.inflate(context, R.layout.dialog_bluetooth_waiting_connection, null);
        mDialog = new AlertDialog.Builder(context)
                .setTitle("蓝牙接收(server)")
                .setView(view)
                .setNeutralButton("开放检测", null)
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        bluetoothAdapter.disable();
                        mBTService.stop();
                    }
                })
                .setCancelable(false)
                .create();
        mDialog.show();
        final Button discoverableButton = mDialog.getButton(DialogInterface.BUTTON_NEUTRAL);
        discoverableButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!bluetoothAdapter.enable())
                {
                    Toast.makeText(mContext ,"需先打开蓝牙",Toast.LENGTH_SHORT).show();
                    return;
                }
                final Handler handler = new Handler();
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        timeOut--;
                        if(timeOut==0) {
                            discoverableButton.setText("开放检测");
                            discoverableButton.setEnabled(true);
                        }
                        else {
                            discoverableButton.setText(String.format("可被检测(%d:%d)",timeOut/60,timeOut%60));
                            handler.postDelayed(this,1000);
                        }
                    }
                };
                //if (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                timeOut=60;
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, timeOut);
                mContext.startActivity(discoverableIntent);
                discoverableButton.setText("可被检测(1:00)");
                discoverableButton.setEnabled(false);
                handler.postDelayed(runnable,1000);
                //}
            }
        });
    }
}
