package com.blacktec.think.scorer;


/**
 * Created by Think on 2019/8/15.
 */

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.blacktec.think.scorer.Interface.BluetoothDataSend;
import com.blacktec.think.scorer.Utils.BlueToothUtils;
import com.blacktec.think.scorer.Utils.BluetoothService;
import com.blacktec.think.scorer.Utils.RecyclerViewDivider;

import java.util.ArrayList;
import java.util.List;

public class BluetoothSearchingDialog extends Dialog {
    private BlueToothUtils mBTUtils;
    private BluetoothService mBTService;
    private Context mContext;
    private View mView;

    private RecyclerView mPairedRecyclerView;
    private BluetoothDeviceAdapter mPairedAdapter;

    private RecyclerView mSearchingRecyclerView;
    private BluetoothDeviceAdapter mSearchingAdapter;

    private int mButtonStatus = BUTTON_SCAN;
    private static final int BUTTON_SCAN = 0;
    private static final int BUTTON_STOP = 1;

    private Button mScanButton;
    private ProgressBar mProgressBar;
    private List<BluetoothDevice> mSearchingDevices;
    private List<BluetoothDevice> mPairedDevices;
    private Boolean mIsPairing=false;

    private Handler mHandler;

    BluetoothDataSend mBTDataSend;
    String[] mOptions;
    AlertDialog mSendDialog;
    public void setDataSendDialog(String[] options, BluetoothDataSend BTDataSend) {
        this.mBTDataSend = BTDataSend;
        mOptions = options;
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);
        mSendDialog = alertDialog.setItems(mOptions, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(mBTService.getState()!=BluetoothService.STATE_TRANSFER)
                {
                    Toast.makeText(mContext,"连接丢失，请重新连接",Toast.LENGTH_SHORT).show();
                    return;
                }
                String s = mBTDataSend.getData(which);
                mBTService.sendFileString(s);
            }
        }).create();
    }

    private BluetoothSearchingDialog(Context context, int themeResId) {
        super(context, themeResId);
    }

    public BluetoothSearchingDialog(Context context)
    {
        super(context,R.style.Theme_AppCompat_Dialog);
        mContext = context;
        LayoutInflater inflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //加载布局文件
        mView = inflater.inflate(R.layout.dialog_bluetooth_finding_devices, null);
        //添加布局文件到 Dialog
        this.addContentView(mView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(mView);
        setCancelable(true);                //用户可以点击后退键关闭 Dialog
        setCanceledOnTouchOutside(false);   //用户不可以点击外部来关闭 Dialog

        LinearLayoutManager layoutManager = new LinearLayoutManager(mContext)
        /*{
            @Override
            public void onMeasure(RecyclerView.Recycler recycler,RecyclerView.State state,int widthSpec,int heightSpec)
            {
                int count = state.getItemCount();
                if (count > 0) {
                    count = count > 3 ? 3 : count;
                    int realHeight = 0;
                    int realWidth = 0;
                    for (int i = 0; i < count; i++) {
                        View view = recycler.getViewForPosition(i);
                        if (view != null) {
                            measureChild(view, widthSpec, heightSpec);
                            int measuredWidth = View.MeasureSpec.getSize(widthSpec);
                            int measuredHeight = view.getMeasuredHeight();
                            realWidth = realWidth > measuredWidth ? realWidth : measuredWidth;
                            realHeight += measuredHeight;
                        }
                    }
                    setMeasuredDimension(realWidth, realHeight);
                } else {
                    super.onMeasure(recycler, state, widthSpec, heightSpec);
                }
            }
        }*/;
        mPairedRecyclerView = (RecyclerView) mView
                .findViewById(R.id.paired_devices_recycler_view);
        mPairedRecyclerView .setLayoutManager(new LinearLayoutManager(mContext));
        mPairedRecyclerView.addItemDecoration(new RecyclerViewDivider(context, LinearLayoutManager.VERTICAL));
        mSearchingDevices = new ArrayList<>();
        mSearchingRecyclerView = (RecyclerView) mView
                .findViewById(R.id.searching_devices_recycler_view);

        mSearchingRecyclerView .setLayoutManager(layoutManager);
        mSearchingRecyclerView.addItemDecoration(new RecyclerViewDivider(context, LinearLayoutManager.VERTICAL));
        mProgressBar = (ProgressBar)mView.findViewById(R.id.progressBar_Scanning);
        mScanButton = (Button)mView.findViewById(R.id.buttonScan);
        mScanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
             pressScanButton(mButtonStatus);
            }
        });

        mHandler = new Handler(){
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
                    case BluetoothService.MESSAGE_FAIL:
                        if(mSendDialog.isShowing())mSendDialog.dismiss();
                    case BluetoothService.MESSAGE_TOAST:
                        Toast.makeText(mContext,(String)msg.obj,Toast.LENGTH_SHORT).show();
                        break;
                    case BluetoothService.MESSAGE_SUCCESS:
                        Toast.makeText(mContext,String.format("连接 %s 成功！",(String)msg.obj),Toast.LENGTH_SHORT).show();
                        sendDataProcess();
                        break;
                }
            }
        };
        mBTService = BluetoothService.getInstance(mHandler);
        mBTUtils = BlueToothUtils.getInstance();
        mPairedDevices = mBTUtils.getBondedDevices();
        updateUI();
    }
    private void sendDataProcess()
    {
        if(mBTDataSend==null)return;//如果没有初始化，就不能执行操作
        mSendDialog.show();
    }
    private void pressScanButton(int status)
    {
        switch (status)
        {
            case BUTTON_SCAN:
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                mContext.registerReceiver(receiver,filter);
                filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
                mContext.registerReceiver(receiver,filter);
                mProgressBar.setVisibility(View.VISIBLE);
                mSearchingDevices.clear();
                mButtonStatus = BUTTON_STOP;
                mScanButton.setText("停止");
                mPairedDevices = mBTUtils.getBondedDevices();
                updateUI();
                mBTUtils.searchDevices();
                break;
            case BUTTON_STOP:
                mContext.unregisterReceiver(receiver);
                mBTUtils.getBA().cancelDiscovery();
                mProgressBar.setVisibility(View.GONE);
                mButtonStatus = BUTTON_SCAN;
                mScanButton.setText("扫描");
                break;
        }
    }
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(BluetoothDevice.ACTION_FOUND))
            {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(device.getBondState()!=BluetoothDevice.BOND_BONDED&&!mSearchingDevices.contains(device))
                {
                    mSearchingDevices.add(device);
                    updateUI();
                }
            }
            else if(action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))
            {
                pressScanButton(BUTTON_STOP);
            }
            else if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED))
            {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                switch (device.getBondState())
                {
                    case BluetoothDevice.BOND_BONDED:
                        mSearchingDevices.remove(device);
                        mPairedDevices.add(device);
                        mContext.unregisterReceiver(receiver);
                        Toast.makeText(mContext,String.format("与设备 %s 配对成功！",device.getName()),Toast.LENGTH_SHORT).show();
                        break;
                    case BluetoothDevice.BOND_BONDING:
                        Toast.makeText(mContext,String.format("与设备 %s 配对中...",device.getName()),Toast.LENGTH_SHORT).show();
                        break;
                    case BluetoothDevice.BOND_NONE:
                        break;
                }
                updateUI();
            }
        }
    };

    private void updateUI()
    {
        mIsPairing = false;
        if(mBTUtils==null)return;

        if (mPairedAdapter == null) {
            mPairedAdapter = new BluetoothDeviceAdapter(mPairedDevices);
            mPairedRecyclerView.setAdapter(mPairedAdapter);
        } else {
            mPairedAdapter.setContracts(mPairedDevices);
            mPairedAdapter.notifyDataSetChanged();
        }
        if (mSearchingAdapter == null) {
            mSearchingAdapter = new BluetoothDeviceAdapter(mSearchingDevices);
            mSearchingRecyclerView.setAdapter(mSearchingAdapter);
        } else {
            mSearchingAdapter.setContracts(mSearchingDevices);
            mSearchingAdapter.notifyDataSetChanged();
        }
    }

    public static class Builder {

        private View mView;
        private BluetoothSearchingDialog mDialog;

        public Builder(Context context) {
            mDialog = new BluetoothSearchingDialog(context, R.style.Theme_AppCompat_Dialog);
            LayoutInflater inflater =
                    (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            //加载布局文件
            mView = inflater.inflate(R.layout.dialog_bluetooth_finding_devices, null);
            //添加布局文件到 Dialog
            mDialog.addContentView(mView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

        }

        public BluetoothSearchingDialog create() {
            mDialog.setContentView(mView);
            mDialog.setCancelable(true);                //用户可以点击后退键关闭 Dialog
            mDialog.setCanceledOnTouchOutside(false);   //用户不可以点击外部来关闭 Dialog
            return mDialog;
        }
    }
    //这里借用了文件浏览器的item 布局
    private class BluetoothDeviceHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        private TextView mNameTextView;
        private ImageView mIconImageView;
        private BluetoothDevice mDevice;

        BluetoothDeviceHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            mNameTextView = (TextView) itemView.findViewById(R.id.textViewFile);
            mIconImageView = (ImageView) itemView.findViewById(R.id.imageView_file_icon);

        }

        public void bindBluetoothDevice(BluetoothDevice device) {
            mDevice = device;
            itemView.setBackgroundDrawable(ContextCompat.getDrawable(getContext(),R.drawable.touch_bg));
            if(mDevice==null)return;
            String name=mDevice.getName();
            if(name==null||name.equals(""))name=mDevice.getAddress();
            mNameTextView.setText(name);
            BluetoothClass bluetoothClass = mDevice.getBluetoothClass();
            int drawable_id =-1;
            if(bluetoothClass!=null)
            {
                switch (bluetoothClass.getMajorDeviceClass())
                {
                    case BluetoothClass.Device.Major.COMPUTER:
                        drawable_id = R.drawable.icon_computer;
                        break;
                    case BluetoothClass.Device.Major.PHONE:
                        drawable_id = R.drawable.icon_phone;
                        break;
                    case BluetoothClass.Device.Major.AUDIO_VIDEO:
                        drawable_id = R.drawable.icon_headset;
                        break;
                    case BluetoothClass.Device.Major.PERIPHERAL:
                        drawable_id = R.drawable.icon_bluetooth_dark;
                        break;
                    case BluetoothClass.Device.Major.IMAGING:
                        drawable_id = R.drawable.icon_printer;
                        break;
                    default:
                        drawable_id = R.drawable.icon_bluetooth_dark;
                }
            }
            mIconImageView.setImageDrawable(ContextCompat.getDrawable(getContext(),drawable_id));
            if(mDevice.getBondState()==BluetoothDevice.BOND_BONDING)
            {
                itemView.setBackgroundColor(ContextCompat.getColor(getContext(),R.color.color_selected1));
                mIsPairing = true;
            }
        }

        @Override
        public void onClick(View v) {
            if(mDevice.getBondState()!=BluetoothDevice.BOND_BONDED)
            {
                //未配对的设备
                if(mIsPairing)return;
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
                mContext.registerReceiver(receiver,filter);
                mBTUtils.createBond(mDevice);
                updateUI();
            }
            else
            {
                if(mBTService.getState()==BluetoothService.STATE_TRANSFER) {
                    //已连接
                    sendDataProcess();
                    //mBTService.sendData(String.format("很高兴认识你，我是 %s",mBTUtils.getBA().getName()).getBytes());
                } else {
                    mBTService.connectDevice(mDevice);//已配对但未连接
                }
                //已配对的设备
            }
        }
    }
    private class BluetoothDeviceAdapter extends RecyclerView.Adapter<BluetoothDeviceHolder> {

        private List<BluetoothDevice> mDevices;
        private int mType;

        public BluetoothDeviceAdapter(List<BluetoothDevice> devices) {
            mDevices = devices;
        }

        @Override
        public BluetoothDeviceHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(mContext);
            View view = layoutInflater.inflate(R.layout.item_file_browse, parent, false);
            return new BluetoothDeviceHolder(view);
        }

        @Override
        public void onBindViewHolder(BluetoothDeviceHolder holder, int position) {
            BluetoothDevice device = mDevices.get(position);
            holder.bindBluetoothDevice(device);
        }

        @Override
        public int getItemCount() {
            return mDevices.size();
        }

        public void setContracts(List<BluetoothDevice> devices) {
            mDevices = devices;
        }
    }
    @Override
    public void show()
    {
        super.show();

        LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.width= LayoutParams.MATCH_PARENT;
        layoutParams.height = LayoutParams.WRAP_CONTENT;

        //getWindow().getDecorView().setPadding(16,16,16,16);
        getWindow().setAttributes(layoutParams);
    }
    @Override
    public void dismiss()
    {
        mBTUtils.cancelSearching();
        //mContext.unregisterReceiver(receiver);
        mBTService.stop();
        mBTUtils.getBA().disable();
        super.dismiss();
    }
}
