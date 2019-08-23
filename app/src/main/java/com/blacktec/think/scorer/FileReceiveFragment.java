package com.blacktec.think.scorer;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableStringBuilder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.DialogInterface.OnClickListener;

import com.blacktec.think.scorer.Utils.ArrayUtils;
import com.blacktec.think.scorer.Utils.BackHandlerHelper;
import com.blacktec.think.scorer.Utils.BluetoothService;
import com.blacktec.think.scorer.Utils.ByteArrayUtils;
import com.blacktec.think.scorer.Utils.CustomFileFilter;
import com.blacktec.think.scorer.Utils.CustomItemDecoration;
import com.blacktec.think.scorer.Utils.ExternCallUtils;
import com.blacktec.think.scorer.Utils.ExternStorage;
import com.blacktec.think.scorer.Utils.FileIOUtils;
import com.blacktec.think.scorer.Utils.FileUtils;
import com.blacktec.think.scorer.Utils.FragmentBackHandler;
import com.blacktec.think.scorer.Utils.MD5Util;
import com.blacktec.think.scorer.Utils.SpannableStringUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Created by Think on 2019/8/17.
 */

public class FileReceiveFragment extends Fragment implements FragmentBackHandler{

    private static final int REQUEST_FILE = 0;
    private static final int REQUEST_FOLDER = 1;
    private static final int SAVE_BRIDGE_FILE = 2;

    public static final String ARG_EXPLORE_TASK = "arg_explore_task";
    public static final String ARG_PARAM = "arg_param";

    public static final String EXTRA_FILE_PATH = "extra_file_path";

    private FileExplorerStatus mExplorerStatus = FileExplorerStatus.Normal_Explore;
    private FileExplorerTask mExplorerTask = FileExplorerTask.Normal_Explore;

    private RecyclerView mFileRecyclerView;
    private FileAdapter mFileAdapter;

    private EditText mFileSaveEditText;
    private TextView mExtensionNameTextView;
    private View mFileSaveRootView;
    private String mFileSaveNameDefault;

    private File mCacheDir;
    private File mCurrentParent;
    private File[] mCurrentFiles;

    private File mSelectedFile;
    private File mCopiedFile;

    private CustomFileFilter mFileFilter;
    private CustomFileFilter mFileFilterIgnoreFolder;
    private volatile ArrayList<File> mFilteredFiles;
    private fileFilterTask mTaskFileFilter;

    private MenuItem mNewFolderMenuItem;
    private MenuItem mPasteMenuItem;
    private MenuItem mCheckMenuItem;

    private Gson gson;
    private Context mContext;

    /*
    *   文件名格式：
    *   1.正在接收状态： MD5(32位) + 状态位 + 文件长度
    *   2.已接收状态：   MD5(32位) + 状态位 + 文件类型编号(这里是BridgeFileType)
    *   状态位：1 = 接收完成 / 2 = 正在接收 / 3 = 损坏
    */

    private Handler mHandler= new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            try {
                switch (msg.what) {
                    case BluetoothService.MESSAGE_DEVICE_NAME:
                        break;
                    case BluetoothService.MESSAGE_WRITE:
                        break;
                    case BluetoothService.MESSAGE_READ:
                        byte[] readBuf = (byte[]) msg.obj;
                        if (mOutputStream == null)
                        {
                            throw new Exception("数据流丢失!");
                        }

                        mOutputStream.write(readBuf,0,msg.arg1);
                        mReceivedSize += msg.arg1;
                        if (mReceivedSize >= mTotalFileSize && mTotalFileSize != 0) {
                            mOutputStream.flush();
                            mOutputStream.close();
                            receivedFileVerification();
                        }
                        refresh();
                        updateUI();
                        break;
                    case BluetoothService.MESSAGE_STATE_CHANGE:
                        break;
                    case BluetoothService.MESSAGE_FAIL:
                    case BluetoothService.MESSAGE_TOAST:
                        Toast.makeText(getContext(), (String) msg.obj, Toast.LENGTH_SHORT).show();
                        break;
                    case BluetoothService.MESSAGE_SUCCESS:
                        break;
                    case BluetoothService.MESSAGE_FILE_HEAD:
                        byte[] headBuf = (byte[]) msg.obj;
                        if (msg.arg1 != BluetoothService.FILE_HEAD_LENGTH) break;
                        String fileName = (new String(headBuf, 0, 32)) + "2"+
                                String.valueOf(ByteArrayUtils.byteArrayToInt(headBuf,32));
                        mReceivingFile = new File(mCurrentParent.getAbsolutePath() + "/" + fileName);
                        FileUtils.createFile(mReceivingFile, FileUtils.DEFAULT_FILE_OPERATE_MODE);
                        if (mReceivingFile.exists()) {
                            mOutputStream = new BufferedOutputStream(new FileOutputStream(mReceivingFile));
                        }
                        mTotalFileSize = ByteArrayUtils.byteArrayToInt(headBuf, 32);
                        mReceivedSize = 0;
                        refresh();
                        updateUI();
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "文件读写失败，请重新传输~_~\n错误原因："+e.toString(), Toast.LENGTH_SHORT).show();
                resetTransfer();
            }
        }
    };
    private BluetoothService mBTService;
    private File mReceivingFile;//正在接收的文件
    private int mTotalFileSize = 0;
    private int mReceivedSize = 0;
    private BufferedOutputStream mOutputStream;

    public static FileReceiveFragment newInstance(FileExplorerTask task) {
        FileReceiveFragment fragment = new FileReceiveFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_EXPLORE_TASK, task);
        fragment.setArguments(args);
        return fragment;
    }

    public static FileReceiveFragment newInstance(FileExplorerTask task, String param) {
        FileReceiveFragment fragment = new FileReceiveFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_EXPLORE_TASK, task);
        args.putString(ARG_PARAM, param);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (getArguments() != null) {
            mExplorerTask = (FileExplorerTask) getArguments().getSerializable(ARG_EXPLORE_TASK);
            String s = getArguments().getString(ARG_PARAM);
            if (s != null) {
                switch (mExplorerTask) {
                    case Bridge_File_Saving:
                        mFileSaveNameDefault = s;
                        break;
                    default:
                        String[] ss = s.split("\\|");
                        mFileFilter = new CustomFileFilter(ss, true);
                        mFileFilterIgnoreFolder = new CustomFileFilter(ss);
                        break;
                }
            } else
                mFileFilter = null;
        }
        mCacheDir = getContext().getCacheDir().getAbsoluteFile();
        setCurrentParent(mCacheDir);
        switch (mExplorerTask) {
            case File_Receive:
                mBTService = BluetoothService.getInstance(mHandler);
                mBTService.setMode(BluetoothService.ServerMode.MODE_FILE);
                if(mBTService.getState()!=BluetoothService.STATE_TRANSFER)
                    mBTService.start();
                break;
            case Ask_for_file:
                break;
        }
        mContext=getContext();
        gson= new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").disableHtmlEscaping().create();
    }

    public void receivedFileVerification()
    {
        try {
            if (mReceivingFile == null || !mReceivingFile.exists()) {
                throw new Exception("文件数据错误,请重新传输");
            }
            String md5 = mReceivingFile.getName().substring(0, 32);
            byte[] bytes = new byte[mReceivedSize];
            FileInputStream in = new FileInputStream(mReceivingFile);
            in.read(bytes);
            String data = new String(bytes);
            String md5check = MD5Util.stringToMD5(data);
            if (!md5check.equals(md5)) {
                mReceivingFile.delete();
                throw new Exception("文件数据错误,请重新传输");
            }
            BridgeJsonFile bridgeJsonFile = gson.fromJson(data,BridgeJsonFile.class);
            if(!bridgeJsonFile.verify())
            {
                mReceivingFile.delete();
                throw new Exception("数据格式错误,请重新传输");
            }
            File withFileName = new File(mCurrentParent.getAbsolutePath() + "/"+md5+"1"+String.valueOf(bridgeJsonFile.getFileType().ordinal()));
            if(mReceivingFile.renameTo(withFileName))
            {
                Toast.makeText(getContext(),"接收数据成功！",Toast.LENGTH_SHORT).show();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Toast.makeText(getContext(),e.toString()+"\n文件传输已重置,请重新传输", Toast.LENGTH_SHORT).show();
        }
        resetTransfer();
    }
    public void resetTransfer()
    {
        mOutputStream=null;
        mTotalFileSize=0;
        mReceivedSize=0;
        mReceivingFile = null;
        //mBTService = BluetoothService.getInstance(mHandler);
        mBTService.setMode(BluetoothService.ServerMode.MODE_FILE);
    }
    @Override
    public void onResume()
    {
        super.onResume();
        mBTService = BluetoothService.getInstance(mHandler);
    }
    @Override
    public boolean onBackPressed() {
        if(mSelectedFile!=null)
        {
            mSelectedFile = null;
            updateUI();
            return true;
        }
        return BackHandlerHelper.handleBackPress(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_file_received, container, false);

        mFileRecyclerView = (RecyclerView) view.findViewById(R.id.file_explorer_recycler_view);
        mFileRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mFileRecyclerView.addItemDecoration(new CustomItemDecoration(ContextCompat.getDrawable(getContext(),R.drawable.divider_file)));

        mFileSaveRootView = view.findViewById(R.id.file_save_root_view);
        mFileSaveEditText = (EditText) mFileSaveRootView.findViewById(R.id.editText_fileSave);
        if(mExplorerTask == FileExplorerTask.Bridge_File_Saving && mFileSaveNameDefault!=null)
        {
            mFileSaveEditText.setText(mFileSaveNameDefault);
        }
        mExtensionNameTextView = (TextView) mFileSaveRootView.findViewById(R.id.textView_file_extension);

        return view;
    }
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_file_explorer,menu);
        mNewFolderMenuItem=menu.findItem(R.id.menu_item_new_folder);
        mPasteMenuItem=menu.findItem(R.id.menu_item_paste);
        mCheckMenuItem=menu.findItem(R.id.menu_item_check);
        updateUI();
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_check:
                switch (mExplorerTask)
                {
                    case File_Receive:
                    case Ask_for_file:
                        if(mSelectedFile==null||!mSelectedFile.exists())
                        {
                            Toast.makeText(getContext(),"选中的文件不存在！",Toast.LENGTH_SHORT).show();
                            updateUI();
                            return true;
                        }
                        Intent intent = new Intent();
                        intent.putExtra(EXTRA_FILE_PATH,mSelectedFile.getAbsolutePath());
                        getActivity().setResult(Activity.RESULT_OK,intent);
                        getActivity().finish();
                        break;
                    case Ask_for_folder:
                        break;
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,@NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) return;
        switch (requestCode)
        {
            case REQUEST_FILE:
                if(data==null)return;
                switch (mExplorerTask) {
                    case Ask_for_file:
                        Uri uri = data.getData();
                        try {
                            File file = new File(new URI(uri.toString()));
                            if(mFileFilter!=null && !mFileFilter.accept(file))
                            {
                                Toast.makeText(getContext(),"请选择合适类型的文件！",Toast.LENGTH_SHORT).show();
                                return;
                            }
                            Intent intent = new Intent();
                            intent.putExtra(EXTRA_FILE_PATH,file.getAbsolutePath());
                            getActivity().setResult(Activity.RESULT_OK,intent);
                            getActivity().finish();
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                        break;
                }
                break;

        }
    }

    private void refresh()
    {
        mCurrentFiles=mCurrentParent.listFiles();
        if(mSelectedFile!=null&&!mSelectedFile.exists())mSelectedFile=null;
        if(mCopiedFile!=null&&!mCopiedFile.exists())mCopiedFile=null;
    }

    private void updateUI() {
        mFileSaveRootView.setVisibility(View.GONE);
        mNewFolderMenuItem.setVisible(false);
        switch (mExplorerStatus)
        {
            case Normal_Explore:
                mPasteMenuItem.setVisible(false);
                break;
            case Moving:
            case Copying:
                //1.查找Copied File 是否在当前显示列表中
                //2.Moving 染成半透明或者灰色
                //Copying 染成主题色匹配的浅
                mPasteMenuItem.setVisible(true);
                break;
        }
        switch (mExplorerTask)
        {
            case Normal_Explore:
                mCheckMenuItem.setVisible(false);
                break;
            case File_Receive:
            case Ask_for_file:
                if(mSelectedFile!=null)
                {
                    mCheckMenuItem.setVisible(true);
                }
                else
                    mCheckMenuItem.setVisible(false);
                break;
            case Ask_for_folder:
                if(mExplorerStatus==FileExplorerStatus.Normal_Explore)
                    mCheckMenuItem.setVisible(true);
                else
                    mCheckMenuItem.setVisible(false);
                break;
            case Bridge_File_Saving:
                mFileSaveRootView.setVisibility(View.VISIBLE);
                mCheckMenuItem.setVisible(false);
                break;
        }
        if (mFileAdapter == null) {
            mFileAdapter = new FileAdapter(mCurrentFiles);
            mFileRecyclerView.setAdapter(mFileAdapter);
        } else {
            mFileAdapter.setFiles(mCurrentFiles);
            mFileAdapter.notifyDataSetChanged();
        }

    }
    private class fileFilterTask extends AsyncTask<Void,Void,Void>
    {
        @Override
        protected Void doInBackground(Void... params)
        {
            for(File f:mCurrentFiles)
            {
                if((f.isDirectory() && f.listFiles(mFileFilterIgnoreFolder).length!=0)
                        ||mFileFilterIgnoreFolder.accept(f))
                    mFilteredFiles.add(f);
            }
            return null;
        }
    }
    private void setCurrentParent(File parent)
    {
        mCurrentParent=parent;
        if(mFileFilter==null)
            mCurrentFiles=mCurrentParent.listFiles();
        else
        {
            mCurrentFiles = mCurrentParent.listFiles(mFileFilter);
            mFilteredFiles = new ArrayList<>();
            if(mTaskFileFilter!=null && mTaskFileFilter.getStatus()!= AsyncTask.Status.FINISHED)
            {
                mTaskFileFilter.cancel(false);
            }
            mTaskFileFilter = new fileFilterTask();
            mTaskFileFilter.execute();
        }
        mSelectedFile = null;
    }

    private class FileHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener,View.OnLongClickListener,View.OnTouchListener{

        TextView mFileTextView;
        ImageView mFileIconImageView;
        TextView mFileStatusTextView;
        TextView mTimeTextView;
        File mFile;

        FileHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
            itemView.setOnTouchListener(this);
            mFileTextView=(TextView) itemView.findViewById(R.id.textViewFile);
            mFileIconImageView = (ImageView) itemView.findViewById(R.id.imageView_file_icon);
            mFileStatusTextView = (TextView) itemView.findViewById(R.id.textViewFileStatus);
            mTimeTextView = (TextView) itemView.findViewById(R.id.textViewTime);
        }

        public void bindFile(File file) {
            mFile=file;
            itemView.setBackgroundDrawable(ContextCompat.getDrawable(getContext(),R.drawable.touch_bg));
            itemView.setAlpha(1.0f);
            mFileTextView.setTextColor(Color.BLACK);
            String fileName=file.getName();
            mTimeTextView.setText(getTimeString(new Date(mFile.lastModified())));
            if(mFile.isDirectory())
            {
                mFileTextView.setText(fileName);
                mFileIconImageView.setImageDrawable(ContextCompat.getDrawable(getContext(),R.drawable.icon_folder));
            }
            else
            {
                if(fileName.length()>=34)
                {
                    long fileCurrentSize = mFile.length();

                    switch (fileName.charAt(32))
                    {
                        case '1'://接收成功
                            mFileTextView.setText(BridgeFileType.getDescription((int)fileName.charAt(33)-48));
                            mFileStatusTextView.setText(String.format(getString(R.string.string_fileStatus_done),FileUtils.getFileSizeString(fileCurrentSize)));
                            break;
                        case '2'://正在接收
                            long fileTotalSize = Long.parseLong(fileName.substring(33));
                            mFileTextView.setText(getString(R.string.string_data_receiving));
                            mFileStatusTextView.setText(String.format(getString(R.string.string_fileStatus_receiving),FileUtils.getFileSizeString(fileTotalSize),fileCurrentSize*100/fileTotalSize));
                            break;
                        case '3'://损坏
                            mFileTextView.setText(getString(R.string.string_data_receiving));
                            mFileStatusTextView.setText("");
                            break;
                    }
                }
                else
                {mFileTextView.setText(fileName);}
                String postfix=fileName.substring(fileName.lastIndexOf(".")+1).toLowerCase();
                Drawable d;
                switch (postfix)
                {
                    case "doc": case "docx":case "wps":
                    d=ContextCompat.getDrawable(getContext(),R.drawable.icon_doc);break;
                    case "ppt": case "pptx":case "ppsx":case"pps":
                    d=ContextCompat.getDrawable(getContext(),R.drawable.icon_ppt);break;
                    case "xls": case "xlsx": case "xlsm":
                    d=ContextCompat.getDrawable(getContext(),R.drawable.icon_xls);break;
                    case "jpg":case "png":case "gif":case "bmp":case "tiff":case "jpeg":case"raw":
                    d=ContextCompat.getDrawable(getContext(),R.drawable.icon_image);break;
                    case "mp4":case "avi":case "rmvb":case "mov":case "mpeg":case "mkv":case "rm":case "mts":
                    case "vob":case "wmv":case "3gp":
                    d=ContextCompat.getDrawable(getContext(),R.drawable.icon_video);break;
                    case "txt":
                        d=ContextCompat.getDrawable(getContext(),R.drawable.icon_txt);break;
                    case "mp3":
                        d=ContextCompat.getDrawable(getContext(),R.drawable.icon_music_mp3);break;
                    case "m4a":
                        d=ContextCompat.getDrawable(getContext(),R.drawable.icon_music_m4a);break;
                    case "amr":
                        d=ContextCompat.getDrawable(getContext(),R.drawable.icon_music_amr);break;
                    case "wav":
                        d=ContextCompat.getDrawable(getContext(),R.drawable.icon_music_wav);break;
                    case "wma":
                        d=ContextCompat.getDrawable(getContext(),R.drawable.icon_music_wma);break;
                    case "html":
                        d=ContextCompat.getDrawable(getContext(),R.drawable.icon_html);break;
                    case "xml":
                        d=ContextCompat.getDrawable(getContext(),R.drawable.icon_xml);break;
                    case "rar":
                        d=ContextCompat.getDrawable(getContext(),R.drawable.icon_rar);break;
                    case "zip":
                        d=ContextCompat.getDrawable(getContext(),R.drawable.icon_zip);break;
                    case "7z":
                        d=ContextCompat.getDrawable(getContext(),R.drawable.icon_7z);break;
                    case "pdf":
                        d=ContextCompat.getDrawable(getContext(),R.drawable.icon_pdf);break;
                    case "apk":
                        d=ContextCompat.getDrawable(getContext(),R.drawable.icon_apk);break;
                    case "brg":
                        d=ContextCompat.getDrawable(getContext(),R.drawable.icon_brg);break;
                    default:
                        d=ContextCompat.getDrawable(getContext(),R.drawable.icon_download);
                }
                mFileIconImageView.setImageDrawable(d);
            }
            if(mFileFilter!=null)
            {
                if(mFilteredFiles !=null && mFilteredFiles.contains(mFile))
                {
                    mFileTextView.setTextColor(ContextCompat.getColor(getContext(),R.color.color_holo_red_light));
                }
            }
            switch (mExplorerStatus)
            {
                case Normal_Explore:
                    if(mSelectedFile!=null && mSelectedFile.equals(mFile))
                    {
                        itemView.setBackgroundColor(ContextCompat.getColor(getContext(),R.color.color_selected1));
                    }
                    break;
                case Moving:
                    if(mCopiedFile!=null && mCopiedFile.equals(mFile))
                    {
                        itemView.setBackgroundColor(Color.LTGRAY);
                        itemView.setAlpha(0.5f);
                    }
                    break;
                case Copying:
                    if(mCopiedFile!=null &&mCopiedFile.equals(mFile))
                    {
                        itemView.setBackgroundColor(ContextCompat.getColor(getContext(),R.color.color_copied));
                    }
                    break;
            }
        }
        @Override
        public void onClick(View v) {
            if(mFile.isDirectory())
            {
                setCurrentParent(mFile);
                updateUI();
            }
            else
            {
                if(mExplorerTask == FileExplorerTask.Bridge_File_Saving &&mExplorerStatus == FileExplorerStatus.Normal_Explore)
                {
                    int index=mFile.getName().lastIndexOf(".");
                    if(index>0)
                        mFileSaveEditText.setText(mFile.getName().substring(0,index));
                    else
                        mFileSaveEditText.setText(mFile.getName());
                }
                //Toast.makeText(getActivity(),"正在打开"+mFile.getName(),Toast.LENGTH_SHORT).show();
                if(mExplorerTask == FileExplorerTask.Ask_for_file ||mExplorerTask ==FileExplorerTask.File_Receive
                        && mExplorerStatus == FileExplorerStatus.Normal_Explore)
                {
                    String fileName=mFile.getName();
                    if(fileName.length()>=33&& fileName.charAt(32)=='1') {//1代表已接收完成
                        mSelectedFile = mFile;
                        updateUI();
                    }
                }
                if(mExplorerTask == FileExplorerTask.Normal_Explore && mExplorerStatus ==FileExplorerStatus.Normal_Explore)
                {
                    try {
                        ExternCallUtils.openFile(getContext(), mFile);
                    }
                    catch (Exception e)
                    {
                        Toast.makeText(getActivity(),"打开文件 "+mFile.getName()+" 似乎遇到了一些麻烦emmm..\n"+e.toString(),Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                    updateUI();
                }
            }
        }
        @Override
        public boolean onLongClick(View v) {
            PopupMenu mPopupMenu = new PopupMenu(getActivity(),v, Gravity.RIGHT,0,R.style.MyPopupMenu_non_overlap);

            MenuInflater inflater = new MenuInflater(getActivity());
            inflater.inflate(R.menu.file_receive_popmenu,mPopupMenu.getMenu());

            SpannableStringBuilder builder = SpannableStringUtils.getBuilder(getString(R.string.title_delete))
                    .setForegroundColor(ContextCompat.getColor(getContext(),R.color.color_black))
                    .create();
            //这样就可以更改了id为report的这个menu的字体颜色了
            MenuItem menuItem=mPopupMenu.getMenu().findItem(R.id.menu_item_delete);
            menuItem.setTitle(builder);

            mPopupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
            {
                @Override
                public boolean onMenuItemClick(MenuItem item)
                {
                    switch (item.getItemId())
                    {
                        case R.id.menu_item_delete:
                            AlertDialog checkDeleteDialog= new AlertDialog.Builder(getActivity())
                                    .setMessage("是否删除 "+mFile.getName()+ " !?")//设置对话框内容
                                    .setPositiveButton(R.string.title_delete, new OnClickListener() {//设置对话框[肯定]按钮
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if(!mFile.isDirectory())
                                            {
                                                if (mFile.delete()) {
                                                    refresh();
                                                    updateUI();
                                                    Toast.makeText(getActivity(),
                                                            "文件 " + mFile.getName() + " 已删除",
                                                            Toast.LENGTH_SHORT).show();
                                                } else {
                                                    Toast.makeText(getActivity(),
                                                            "文件 " + mFile.getName() + " 删除失败",
                                                            Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                            else
                                            {
                                                if (deleteDirWihtFile(mFile)) {
                                                    refresh();
                                                    updateUI();
                                                    Toast.makeText(getActivity(),
                                                            "文件夹 " + mFile.getName() + " 已删除",
                                                            Toast.LENGTH_SHORT).show();
                                                } else {
                                                    Toast.makeText(getActivity(),
                                                            "文件夹 " + mFile.getName() + " 删除失败",
                                                            Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        }
                                    })
                                    .setNegativeButton(R.string.title_cancel, null)
                                    .setCancelable(true).create();
                            checkDeleteDialog.show();
                            checkDeleteDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED);
                            break;
                    }
                    return true;
                }
            });
            mPopupMenu.show();
            return true;
        }
        @Override
        public boolean onTouch(View v, MotionEvent event)
        {
            /*switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN://0
                    v.setBackgroundColor(ContextCompat.getColor(getContext(),R.color.color_pressed));
                    break;
                case MotionEvent.ACTION_UP://1
                    v.setBackgroundColor(ContextCompat.getColor(getContext(),R.color.color_snow));
                    break;
                case MotionEvent.ACTION_MOVE://2
                    break;
                case MotionEvent.ACTION_CANCEL://3
                    v.setBackgroundColor(ContextCompat.getColor(getContext(),R.color.color_snow));
                    break;
            }*/
            return false;
        }
    }

    private class FileAdapter extends RecyclerView.Adapter<FileHolder>
    {
        public void setFiles(File[] files) {
            mFiles = files;
            sortFiles();
        }

        private File[] mFiles;
        FileAdapter(File[] files)
        {
            mFiles=files;
            sortFiles();
        }

        private void sortFiles()
        {
            List fileList = Arrays.asList(mFiles);
            Collections.sort(fileList, new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    if (o1.isDirectory() && o2.isFile())
                        return -1;
                    if (o1.isFile() && o2.isDirectory())
                        return 1;
                    return o1.lastModified() < o2.lastModified() ? 1 : -1;
                }
            });
        }
        @Override
        public FileHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View view = layoutInflater.inflate(R.layout.item_file_receive, parent, false);
            return new FileHolder(view);
        }
        @Override
        public void onBindViewHolder(FileHolder holder, int position) {
            holder.bindFile(mFiles[position]);
        }
        @Override
        public int getItemCount() {
            return mFiles.length;
        }
    }

    public static boolean isValidFileName(String fileName) { if (fileName == null || fileName.length() > 255) return false; else return fileName.matches("[^\\s\\\\/:\\*\\?\\\"<>\\|](\\x20|[^\\s\\\\/:\\*\\?\\\"<>\\|])*[^\\s\\\\/:\\*\\?\\\"<>\\|\\.]$"); }

    public void showReasonUnwritable()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        {
            Toast.makeText(getActivity(),"android 4.4以上系统需要root权限才可写入外置储存卡    " +
                    "请在内部储存操作",Toast.LENGTH_SHORT).show();
        }
        else
        {
            Toast.makeText(getActivity(),"写入外部储存卡权限不足    请在内部储存操作",Toast.LENGTH_SHORT).show();
        }
    }
    public static boolean deleteDirWihtFile(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory())
            return false;
        for (File file : dir.listFiles()) {
            if (file.isFile())
                file.delete(); // 删除所有文件
            else if (file.isDirectory())
                deleteDirWihtFile(file); // 递规的方式删除文件夹
        }
        return dir.delete();// 删除目录本身
    }
    private String getTimeString(Date date) {//可根据需要自行截取数据显示
        SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd \n HH:mm:ss");
        return format.format(date);
    }

}

