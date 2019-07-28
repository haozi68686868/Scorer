package com.blacktec.think.scorer;

import android.Manifest;
import android.app.Activity;
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
import com.blacktec.think.scorer.Utils.CustomFileFilter;
import com.blacktec.think.scorer.Utils.CustomItemDecoration;
import com.blacktec.think.scorer.Utils.ExternCallUtils;
import com.blacktec.think.scorer.Utils.ExternStorage;
import com.blacktec.think.scorer.Utils.FileIOUtils;
import com.blacktec.think.scorer.Utils.FileUtils;
import com.blacktec.think.scorer.Utils.FragmentBackHandler;
import com.blacktec.think.scorer.Utils.SpannableStringUtils;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Think on 2018/2/3.
 */

public class FileExplorerFragment extends Fragment implements FragmentBackHandler {

    private static final int REQUEST_FILE = 0;
    private static final int REQUEST_FOLDER = 1;
    private static final int SAVE_BRIDGE_FILE = 2 ;

    public static final String ARG_EXPLORE_TASK= "arg_explore_task";
    public static final String ARG_PARAM = "arg_param";

    public static final String EXTRA_FILE_PATH = "extra_file_path";

    private FileExplorerStatus mExplorerStatus = FileExplorerStatus.Normal_Explore;
    private FileExplorerTask mExplorerTask = FileExplorerTask.Normal_Explore;
    private boolean mWritableExternal;

    private RecyclerView mFilePathRecyclerView;
    private FilePathAdapter mPathAdapter;
    private RecyclerView mFileRecyclerView;
    private FileAdapter mFileAdapter;
    private TextView mRootPathTextView;

    private EditText mFileSaveEditText;
    private TextView mExtensionNameTextView;
    private Button mFileSaveButton;
    private View mFileSaveRootView;
    private String mFileSaveNameDefault;

    private String mCatalogShown;
    private String[] mFilePaths;

    private File mRootPath;
    private File mSDCardDir;
    private File mInternalStorageDir;
    private File mDefaultDir;
    private File mFileManager;
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

    public static FileExplorerFragment newInstance(FileExplorerTask task) {
        FileExplorerFragment fragment = new FileExplorerFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_EXPLORE_TASK, task);
        fragment.setArguments(args);
        return fragment;
    }
    public static FileExplorerFragment newInstance(FileExplorerTask task,String param) {
        FileExplorerFragment fragment = new FileExplorerFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_EXPLORE_TASK, task);
        args.putString(ARG_PARAM,param);
        fragment.setArguments(args);
        return fragment;
    }
    @Override
    public boolean onBackPressed() {
        if(mSelectedFile!=null)
        {
            mSelectedFile = null;
            updateUI();
            return true;
        }
        switch (mExplorerStatus)
        {
            case Normal_Explore:
                if(mRootPath==null)break;
                if(mRootPath.equals(mCurrentParent))
                {
                    mRootPath=null;
                    updateUI();
                    return true;
                }
                else
                {
                    File parent=mCurrentParent;
                    mCurrentParent=mCurrentParent.getParentFile();
                    setCurrentParent(mCurrentParent);
                    updateUI();
                    int index = ArrayUtils.getIndex(mCurrentFiles,parent);
                    if(index !=-1)
                    {
                        ((LinearLayoutManager)mFileRecyclerView.getLayoutManager()).scrollToPositionWithOffset(index,0);
                    }
                    return true;
                }
            case Copying:
            case Moving:
                mExplorerStatus = FileExplorerStatus.Normal_Explore;
                updateUI();
                return true;
        }
        return BackHandlerHelper.handleBackPress(this);
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (getArguments() != null) {
            mExplorerTask = (FileExplorerTask) getArguments().getSerializable(ARG_EXPLORE_TASK);
            String s=getArguments().getString(ARG_PARAM);
            if(s!=null)
            {
                switch (mExplorerTask)
                {
                    case Bridge_File_Saving:
                        mFileSaveNameDefault = s;
                        break;
                    default:
                        String[] ss=s.split("\\|");
                        mFileFilter = new CustomFileFilter(ss,true);
                        mFileFilterIgnoreFolder = new CustomFileFilter(ss);
                        break;
                }
            }
            else
                mFileFilter = null;
        }
        mWritableExternal=false;
        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
        {
            int permissionCheck1,permissionCheck2;
            permissionCheck2 = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (Build.VERSION.SDK_INT >= 16) {
                permissionCheck1 = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE);
                if (permissionCheck1 != PackageManager.PERMISSION_GRANTED || permissionCheck2 != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(getActivity(),
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.READ_EXTERNAL_STORAGE},
                            124);
                }//实际询问权限应在程序开始
            }
            else if (permissionCheck2 != PackageManager.PERMISSION_GRANTED ) {
                    ActivityCompat.requestPermissions(getActivity(),
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            124);
            }
            int permissionCheck3=ContextCompat.checkSelfPermission(getContext(), Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS);
            mWritableExternal=permissionCheck3==PackageManager.PERMISSION_GRANTED;
            try
            {
                mInternalStorageDir=Environment.getExternalStorageDirectory();
                mSDCardDir= ExternStorage.getStorageFile(getContext(),true);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                    mDefaultDir=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
                else
                    mDefaultDir=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);//后续选择新建一个自己的文件夹

                mFileManager=new File("fileManager");
                mRootPath=null;
                if(mExplorerTask == FileExplorerTask.Bridge_File_Saving)
                {
                    mCurrentParent =mDefaultDir;
                    setCurrentParent(mCurrentParent);
                    mRootPath = mInternalStorageDir;
                }
            }
            catch (Exception e)
            {
                Toast.makeText(getActivity(),e.getStackTrace().toString(),Toast.LENGTH_SHORT).show();
            }
        }
        mCatalogShown = getResources().getString(R.string.string_catalog);

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_file_explorer, container, false);

        mFilePathRecyclerView = (RecyclerView) view.findViewById(R.id.file_path_recycler_view);
        LinearLayoutManager ms= new LinearLayoutManager(getActivity());
        ms.setOrientation(LinearLayoutManager.HORIZONTAL);
        mFilePathRecyclerView.setLayoutManager(ms);

        mFileRecyclerView = (RecyclerView) view.findViewById(R.id.file_explorer_recycler_view);
        mFileRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mFileRecyclerView.addItemDecoration(new CustomItemDecoration(ContextCompat.getDrawable(getContext(),R.drawable.divider_file)));

        View rootPathItem=view.findViewById(R.id.item_root_path);
        mRootPathTextView=(TextView) rootPathItem.findViewById(R.id.textViewFilePath);
        mRootPathTextView.setText(mCatalogShown);
        mRootPathTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRootPath= null;
                mSelectedFile = null;
                updateUI();
            }
        });

        mFileSaveRootView = view.findViewById(R.id.file_save_root_view);
        mFileSaveEditText = (EditText) mFileSaveRootView.findViewById(R.id.editText_fileSave);
        if(mExplorerTask == FileExplorerTask.Bridge_File_Saving && mFileSaveNameDefault!=null)
        {
            mFileSaveEditText.setText(mFileSaveNameDefault);
        }
        mFileSaveButton = (Button) mFileSaveRootView.findViewById(R.id.buttonFileSave);
        mExtensionNameTextView = (TextView) mFileSaveRootView.findViewById(R.id.textView_file_extension);
        mFileSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (mExplorerTask) {
                    case Bridge_File_Saving:
                        if (mRootPath == mSDCardDir && !mWritableExternal) {
                            showReasonUnwritable();
                            return;
                        }
                        if(mFileSaveEditText.getText().toString().isEmpty())
                        {
                            Toast.makeText(getActivity(),
                                    "请输入文件名",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        final String fileName = mFileSaveEditText.getText().toString() + ".brg";
                        if (!isValidFileName(fileName)) {
                            Toast.makeText(getActivity(),
                                    "文件名不合法！\n 不可包含? * : \" < > \\ / |",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        final File fileSave = new File(mCurrentParent.getAbsolutePath() + "/" + fileName);
                        if (fileSave.exists()) {
                            AlertDialog checkDeleteDialog = new AlertDialog.Builder(getActivity())
                                    .setMessage("文件 " + fileSave.getName() + " 已存在 \n是否替换!?")//设置对话框内容
                                    .setPositiveButton(R.string.title_replace, new OnClickListener() {//设置对话框[肯定]按钮
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent intent = new Intent();
                                            intent.putExtra(EXTRA_FILE_PATH,fileSave.getAbsolutePath());
                                            getActivity().setResult(Activity.RESULT_OK,intent);
                                            getActivity().finish();
                                        }
                                    })
                                    .setNegativeButton(R.string.title_cancel, null)
                                    .setCancelable(true).create();
                            checkDeleteDialog.show();
                            checkDeleteDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED);
                            return;
                        } else {
                            Intent intent = new Intent();
                            intent.putExtra(EXTRA_FILE_PATH,fileSave.getAbsolutePath());
                            getActivity().setResult(Activity.RESULT_OK,intent);
                            getActivity().finish();
                        }
                        break;
                    default:
                        return;
                }
            }
        });

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
            case R.id.menu_item_new_folder:
                if(mRootPath==mSDCardDir&&!mWritableExternal)
                {
                    showReasonUnwritable();
                    return true;
                }
                LayoutInflater layoutInflater = getActivity().getLayoutInflater();
                //为了能在下面的OnClickListener中获取布局上组件的数据，必须定义为final类型.
                final View customLayout = layoutInflater.inflate(R.layout.custom_edit_dialog,(ViewGroup)getActivity().findViewById(R.id.custom_dialog));
                final EditText editText = (EditText) customLayout.findViewById(R.id.editText_fileName);
                final TextView dialogTitle = (TextView) customLayout.findViewById(R.id.textViewTitle);
                editText.setText("新建文件夹");
                dialogTitle.setText("输入文件夹名称");
                final AlertDialog inputDialog =
                        new AlertDialog.Builder(getActivity())
                                .setView(customLayout)
                                .setPositiveButton("确定", null).create();
                inputDialog.show();
                editText.setFocusable(true);
                editText.selectAll();
                inputDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(!isValidFileName(editText.getText().toString()))
                        {
                            Toast.makeText(getActivity(),
                                    "文件名不合法！\n 不可包含? * : \" < > \\ / |",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        File newFolder = new File(mCurrentParent.getAbsolutePath() + "/" + editText.getText().toString());
                        if (newFolder.exists()) {
                            Toast.makeText(getActivity(),
                                    "文件夹 \"" + editText.getText().toString() + "\" 已存在",
                                    Toast.LENGTH_SHORT).show();
                        }
                        else {
                            if(newFolder.mkdirs())
                            {
                                Toast.makeText(getActivity(),
                                        "文件夹\"" + editText.getText().toString() + "\" 创建成功！",
                                        Toast.LENGTH_SHORT).show();
                                refresh();
                                updateUI();
                                inputDialog.dismiss();
                            }
                            else
                            {
                                Toast.makeText(getActivity(),
                                        "文件夹\"" + editText.getText().toString() + "\" 创建失败",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
                return true;
            case R.id.menu_item_check:
                switch (mExplorerTask)
                {
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
            case R.id.menu_item_paste:
                if(mRootPath==mSDCardDir&&!mWritableExternal)
                {
                    showReasonUnwritable();
                    return true;
                }
                if(mExplorerStatus==FileExplorerStatus.Normal_Explore)return true;
                File desFile=new File(mCurrentParent.getAbsolutePath(),mCopiedFile.getName());
                Boolean success=false;
                String mode="";
                if(desFile.equals(mCopiedFile))
                {
                    if(mExplorerStatus==FileExplorerStatus.Moving) {
                        Toast.makeText(getActivity(), "目标文件 与 源文件相同", Toast.LENGTH_LONG).show();
                        return true;
                    }
                    if(mExplorerStatus==FileExplorerStatus.Copying)
                    {
                        String tempStr= desFile.getAbsolutePath();
                        String extenStr = FileIOUtils.getFileExtension(tempStr);
                        tempStr = FileIOUtils.getAbsoluteFileNameWithoutExtension(tempStr);
                        if(!extenStr.equals(""))
                        {
                            extenStr = "." + extenStr;
                        }
                        desFile=new File(tempStr+"_副本"+extenStr);
                        int index=1;
                        while(desFile.exists())
                        {
                            desFile=new File(tempStr+"_副本"+String.valueOf(index)+extenStr);
                            index++;
                        }

                    }
                }
                if(desFile.exists())
                {
                    Toast.makeText(getActivity(),"文件"+(mCopiedFile.isDirectory()?"夹 ":" ")+desFile.getName()+" 已存在！",Toast.LENGTH_LONG).show();
                    return true;
                }
                if(desFile.getAbsolutePath().lastIndexOf(mCopiedFile.getAbsolutePath()+"/")!=-1)
                {
                    Toast.makeText(getActivity(),"目标文件夹 是 源文件夹的子文件夹！",Toast.LENGTH_LONG).show();
                    return true;
                }
                try {
                    switch (mExplorerStatus) {
                        case Moving:
                            mode = "移动";
                            if (mCopiedFile.isDirectory()) {
                                success = FileUtils.moveFolder(mCopiedFile, desFile);
                            } else {
                                success = FileUtils.moveFile(mCopiedFile, desFile);
                            }
                            break;
                        case Copying:
                            mode = "复制";
                            if (mCopiedFile.isDirectory()) {
                                success = FileUtils.copyFolder(mCopiedFile, desFile);
                            } else {
                                success = FileUtils.copyFile(mCopiedFile, desFile);
                            }
                            break;
                    }
                }
                catch (Exception e)
                {
                    success=false;
                }
                String s=(mCopiedFile.isDirectory()? "文件夹 " : "文件 ") +
                        desFile.getName() +" "+ mode + (success ? "成功 !" : "失败 ~");
                if(success)
                {
                    mCopiedFile=null;
                    mExplorerStatus=FileExplorerStatus.Normal_Explore;
                }
                refresh();
                updateUI();
                Toast.makeText(getActivity(),s,Toast.LENGTH_SHORT).show();
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
        if(mRootPath!=null) {
            mNewFolderMenuItem.setVisible(true);
            mFilePaths = mCurrentParent.getAbsolutePath().replace(mRootPath.getAbsolutePath(), "").split("/");
            if(mRootPath.equals(mInternalStorageDir))
                mFilePaths[0] = getResources().getString(R.string.string_internal_storage);
            else if(mRootPath.equals(mSDCardDir))
                mFilePaths[0] = getResources().getString(R.string.string_SDCard);
            /*String[] s=mCurrentParent.getAbsolutePath().replace(mRootPath.getAbsolutePath(),"").split("/");
            mFilePaths=new String[s.length-1];
            for(int i=0;i<mFilePaths.length;i++)
                mFilePaths[i]=s[i+1];*/
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
        }
        else
        {
            mNewFolderMenuItem.setVisible(false);
            mCheckMenuItem.setVisible(false);
            mPasteMenuItem.setVisible(false);
            mFilePaths=new String[0];
            if(mSDCardDir==null)
                mCurrentFiles = new File[]{mDefaultDir,mInternalStorageDir,mFileManager};
            else
                mCurrentFiles = new File[]{mDefaultDir,mInternalStorageDir,mSDCardDir,mFileManager};
        }
            if (mPathAdapter == null) {
                mPathAdapter = new FilePathAdapter(mFilePaths);
                mFilePathRecyclerView.setAdapter(mPathAdapter);
            } else {
                mPathAdapter.setPaths(mFilePaths);
                mPathAdapter.notifyDataSetChanged();
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

    private class FilePathHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener{
        TextView mPathTextView;
        String mPath;
        int mParentLevel;
        //表示该级地址相对于当前窗口目录的层数，每深入一层文件夹，每个Path的该值会+1

        FilePathHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            mPathTextView = (TextView)itemView.findViewById(R.id.textViewFilePath);
        }
        public void bindFilePath(String path,int parentLevel) {
            mPath=path;
            mPathTextView.setText(mPath);
            mParentLevel=parentLevel;
        }
        @Override
        public void onClick(View v) {
            if(mParentLevel==0)return;
            for(int i=0;i<mParentLevel-1;i++)
            {
                mCurrentParent=mCurrentParent.getParentFile();
            }
            File parent=mCurrentParent;
            mCurrentParent=mCurrentParent.getParentFile();
            setCurrentParent(mCurrentParent);
            updateUI();
            int index = ArrayUtils.getIndex(mCurrentFiles,parent);
            if(index !=-1)
            {
                ((LinearLayoutManager)mFileRecyclerView.getLayoutManager()).scrollToPositionWithOffset(index,0);
            }
        }
    }

    private class FilePathAdapter extends RecyclerView.Adapter<FilePathHolder>
    {
        private String[] mPaths;

        FilePathAdapter(String[] filePaths)
        {
            mPaths=filePaths;
        }
        @Override
        public FilePathHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View view = layoutInflater.inflate(R.layout.item_file_path, parent, false);
            return new FilePathHolder(view);
        }
        @Override
        public void onBindViewHolder(FilePathHolder holder, int position) {
            holder.bindFilePath(mPaths[position],mPaths.length-position-1);
        }
        @Override
        public int getItemCount() {return mPaths.length;}

        public void setPaths(String[] paths) {
            mPaths = paths;
        }
    }

    private class FileHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener,View.OnLongClickListener,View.OnTouchListener{

        TextView mFileTextView;
        ImageView mFileIconImageView;
        File mFile;

        FileHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
            itemView.setOnTouchListener(this);
            mFileTextView=(TextView) itemView.findViewById(R.id.textViewFile);
            mFileIconImageView = (ImageView) itemView.findViewById(R.id.imageView_file_icon);

        }

        public void bindFile(File file) {
            mFile=file;
            itemView.setBackgroundDrawable(ContextCompat.getDrawable(getContext(),R.drawable.touch_bg));
            itemView.setAlpha(1.0f);
            mFileTextView.setTextColor(Color.BLACK);
            if(mRootPath==null)
            {
                if(mFile==mSDCardDir)
                {
                    mFileTextView.setText(getResources().getString(R.string.string_SDCard));
                    mFileIconImageView.setImageDrawable(ContextCompat.getDrawable(getContext(),R.drawable.ic_classify_sd_card));
                }
                else if(mFile==mInternalStorageDir)
                {
                    mFileTextView.setText(getResources().getString(R.string.string_internal_storage));
                    mFileIconImageView.setImageDrawable(ContextCompat.getDrawable(getContext(),R.drawable.ic_classify_device));
                }
                else if(mFile==mDefaultDir)
                {
                    mFileTextView.setText(getResources().getString(R.string.string_default_path));
                    mFileIconImageView.setImageDrawable(ContextCompat.getDrawable(getContext(),R.drawable.ic_classify_mydocument));
                }
                else if(mFile==mFileManager)
                {
                    mFileTextView.setText(getResources().getString(R.string.string_file_manager));
                    mFileIconImageView.setImageDrawable(ContextCompat.getDrawable(getContext(),R.drawable.ic_classify_filemanager));
                }
                return;
            }
            String fileName=file.getName();
            mFileTextView.setText(fileName);
            if(mFile.isDirectory())
            {
                mFileIconImageView.setImageDrawable(ContextCompat.getDrawable(getContext(),R.drawable.icon_folder));
            }
            else
            {
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
                        d=ContextCompat.getDrawable(getContext(),R.drawable.icon_unknown);
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
            if(mFile==mFileManager)
            {
                Intent intentFile = new Intent(Intent.ACTION_GET_CONTENT);
                intentFile.setType("*/*");//设置类型，我这里是任意类型，任意后缀的可以这样写。
                intentFile.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intentFile,REQUEST_FILE);
                return;
            }
            if(mFile.isDirectory())
            {
                setCurrentParent(mFile);
                if(mRootPath==null)
                {
                    if(mFile==mSDCardDir)
                        mRootPath=mSDCardDir;
                    else
                        mRootPath=mInternalStorageDir;
                }
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
                if(mExplorerTask == FileExplorerTask.Ask_for_file && mExplorerStatus == FileExplorerStatus.Normal_Explore)
                {
                    mSelectedFile = mFile;
                    updateUI();
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
            if(mRootPath==null)return false;
            PopupMenu mPopupMenu = new PopupMenu(getActivity(),v, Gravity.RIGHT,0,R.style.MyPopupMenu_non_overlap);

            MenuInflater inflater = new MenuInflater(getActivity());
            inflater.inflate(R.menu.file_popmenu,mPopupMenu.getMenu());
            SpannableStringBuilder builder = SpannableStringUtils.getBuilder(getString(R.string.title_move))
                    .setForegroundColor(ContextCompat.getColor(getContext(),R.color.color_black))
                    .create();
            //这样就可以更改了id为report的这个menu的字体颜色了
            MenuItem menuItem=mPopupMenu.getMenu().findItem(R.id.menu_item_move);
            menuItem.setTitle(builder);

            builder = SpannableStringUtils.getBuilder(getString(R.string.title_copy))
                    .setForegroundColor(ContextCompat.getColor(getContext(),R.color.color_black))
                    .create();
            //这样就可以更改了id为report的这个menu的字体颜色了
            menuItem=mPopupMenu.getMenu().findItem(R.id.menu_item_copy);
            menuItem.setTitle(builder);

            builder = SpannableStringUtils.getBuilder(getString(R.string.title_delete))
                    .setForegroundColor(ContextCompat.getColor(getContext(),R.color.color_black))
                    .create();
            //这样就可以更改了id为report的这个menu的字体颜色了
            menuItem=mPopupMenu.getMenu().findItem(R.id.menu_item_delete);
            menuItem.setTitle(builder);

            builder = SpannableStringUtils.getBuilder(getString(R.string.title_rename))
                    .setForegroundColor(ContextCompat.getColor(getContext(),R.color.color_black))
                    .create();
            menuItem=mPopupMenu.getMenu().findItem(R.id.menu_item_rename);
            menuItem.setTitle(builder);
            mPopupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
            {
                @Override
                public boolean onMenuItemClick(MenuItem item)
                {
                    switch (item.getItemId())
                    {
                        case R.id.menu_item_delete:
                            if(mRootPath==mSDCardDir&&!mWritableExternal)
                            {
                                showReasonUnwritable();
                                return true;
                            }
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
                        case R.id.menu_item_rename:
                            if(mRootPath==mSDCardDir&&!mWritableExternal)
                            {
                                showReasonUnwritable();
                                return true;
                            }
                            LayoutInflater layoutInflater = getActivity().getLayoutInflater();
                            //为了能在下面的OnClickListener中获取布局上组件的数据，必须定义为final类型.
                            final View customLayout = layoutInflater.inflate(R.layout.custom_edit_dialog,(ViewGroup)getActivity().findViewById(R.id.custom_dialog));
                            final EditText editText = (EditText) customLayout.findViewById(R.id.editText_fileName);
                            final TextView dialogTitle = (TextView) customLayout.findViewById(R.id.textViewTitle);
                            editText.setText(mFile.getName());

                            dialogTitle.setText("重命名");

                            final AlertDialog inputDialog =
                                    new AlertDialog.Builder(getActivity())
                                     .setView(customLayout)
                                     .setPositiveButton("确定",null).create();
                            inputDialog.show();
                            inputDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener()  {
                                        @Override
                                        public void onClick(View v) {
                                            if(!isValidFileName(editText.getText().toString()))
                                            {
                                                Toast.makeText(getActivity(),
                                                        "文件名不合法！\n 不可包含? * : \" < > \\ / |",
                                                        Toast.LENGTH_SHORT).show();
                                                return;
                                            }
                                            File newFile = new File(mCurrentParent.getAbsolutePath()+"//"+editText.getText().toString());
                                            if(newFile.exists())
                                            {
                                                Toast.makeText(getActivity(),newFile.getName()+
                                                        " 已存在!" ,
                                                        Toast.LENGTH_SHORT).show();
                                                return;
                                            }
                                            //执行重命名
                                            if(mFile.renameTo(newFile))
                                            {
                                                refresh();
                                                updateUI();
                                                Toast.makeText(getActivity(),
                                                        "重命名为 " + editText.getText().toString(),
                                                        Toast.LENGTH_SHORT).show();
                                                inputDialog.dismiss();
                                            }
                                            else
                                            {
                                                Toast.makeText(getActivity(),
                                                        "重命名失败.." ,
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                            editText.setFocusable(true);//弹出输入法
                            if(mFile.isDirectory())
                            {
                                editText.selectAll();
                            }
                            else
                            {
                                int index=mFile.getName().lastIndexOf(".");
                                if(index>0)
                                    editText.setSelection(0,index);
                                else
                                    editText.selectAll();
                            }
                            break;
                        case R.id.menu_item_move:
                            if(mRootPath==mSDCardDir&&!mWritableExternal)
                            {
                                showReasonUnwritable();
                                return true;
                            }
                            mExplorerStatus=FileExplorerStatus.Moving;
                            Toast.makeText(getActivity(),"已选中 "+mFile.getName()+"\n请选择目标文件夹",Toast.LENGTH_SHORT).show();
                            mCopiedFile = mFile;
                            mSelectedFile = null;
                            updateUI();
                            break;
                        case R.id.menu_item_copy:
                            mExplorerStatus=FileExplorerStatus.Copying;
                            Toast.makeText(getActivity(),"已复制 "+mFile.getName()+"\n请选择目标文件夹",Toast.LENGTH_SHORT).show();
                            mCopiedFile = mFile;
                            mSelectedFile = null;
                            updateUI();
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
                    return o1.getName().compareToIgnoreCase(o2.getName());
                }
            });
        }
        @Override
        public FileHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View view = layoutInflater.inflate(R.layout.item_file_browse, parent, false);
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

}
