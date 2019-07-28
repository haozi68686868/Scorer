package com.blacktec.think.scorer;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
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
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.bigkoo.pickerview.BridgePickerData.BGOp;
import com.bigkoo.pickerview.BridgePickerView;
import com.blacktec.think.scorer.Utils.FileIOUtils;
import com.blacktec.think.scorer.Utils.RecyclerViewDivider;
import com.blacktec.think.scorer.Utils.SpannableStringUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.UUID;

/**
 * Created by Think on 2017/10/12.
 */

public class ResultListFragment extends Fragment {
    private static final String SAVED_SUBTITLE_VISIBLE = "subtitle";

    private static final String ARG_SHEET_ID = "sheet_id";
    private static final String ARG_PARENT_ID = "parent_team_id";

    private BridgeResultSheet mResultSheet;
    private RecyclerView mResultSheetRecyclerView;
    private BridgeContractAdapter mAdapter;
    private boolean mSubtitleVisible;

    private static final int REQUEST_FILE = 0;
    private static final int REQUEST_FILE_BY_SYSTEM = 1;
    private static final int REQUEST_FILE_SAVE = 10;
    private static final int REQUEST_FILE_IMPORT = 20;

    private Gson gson;

    public static ResultListFragment newInstance(UUID sheetId) {
        ResultListFragment fragment = new ResultListFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_SHEET_ID, sheetId);
        fragment.setArguments(args);
        return fragment;
    }
    public static ResultListFragment newInstance(UUID sheetId,UUID parentTeamId) {
        ResultListFragment fragment = new ResultListFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_SHEET_ID, sheetId);
        args.putSerializable(ARG_PARENT_ID, parentTeamId);
        fragment.setArguments(args);
        return fragment;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            UUID uuid = (UUID) getArguments().getSerializable(ARG_SHEET_ID);
            mResultSheet=new BridgeResultSheet(getActivity(),uuid);
            uuid = (UUID) getArguments().getSerializable(ARG_PARENT_ID);
            if(uuid!=null)
            {
                mResultSheet.setParentSheet(new BridgeResultSheetTeam(getActivity(),uuid));
            }
        }
        //setRetainInstance(true);
        setHasOptionsMenu(true);
        if(mResultSheet==null)mResultSheet = BridgeResultSheet.getRecent(getActivity());
        gson= new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").disableHtmlEscaping().create();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.recyclerview_list_including_title, container, false);

        mAdapter=null;
        mResultSheetRecyclerView = (RecyclerView) view
                .findViewById(R.id.result_recycler_view);
        mResultSheetRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mResultSheetRecyclerView.addItemDecoration(new RecyclerViewDivider(getContext(), LinearLayoutManager.VERTICAL));
        if (savedInstanceState != null) {
            mSubtitleVisible = savedInstanceState.getBoolean(SAVED_SUBTITLE_VISIBLE);
        }

        BGOp.init();
        updateUI();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        //updateUI();
    }
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser){
            updateUI();
        }
    }
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SAVED_SUBTITLE_VISIBLE, mSubtitleVisible);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_result_list,menu);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) return;
        File file;
        switch (requestCode) {
            case REQUEST_FILE_BY_SYSTEM:
                Uri uri = data.getData();//得到uri，后面就是将uri转化成file的过程。
            /*String[] proj = {MediaStore.Images.Media.DATA};
            Cursor actualimagecursor = getActivity().managedQuery(uri, proj, null, null, null);
            int actual_image_column_index = actualimagecursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            actualimagecursor.moveToFirst();
            String img_path = actualimagecursor.getString(actual_image_column_index);*/
                //File file = new File(img_path);
                Toast.makeText(getActivity(), uri.toString(), Toast.LENGTH_SHORT).show();
                break;
            case REQUEST_FILE:
                if(data==null)return;
                file = new File(data.getStringExtra(FileExplorerFragment.EXTRA_FILE_PATH));
                Toast.makeText(getActivity(), file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                break;
            case REQUEST_FILE_SAVE://
            case REQUEST_FILE_SAVE+2:
                if(data==null)return;
                file = new File(data.getStringExtra(FileExplorerFragment.EXTRA_FILE_PATH));
                BridgeFileType fileType= BridgeFileType.values()[(requestCode-REQUEST_FILE_SAVE)];

                BridgeJsonFile jsonFile=null;
                switch (fileType)
                {
                    case SingleSheet:
                        jsonFile= BridgeJsonFile.createdByResultSheet(mResultSheet);
                        break;
                    case TeamSheet:
                        jsonFile= BridgeJsonFile.createdByTeamResultSheet(mResultSheet.getParentSheet());
                        break;
                }
                if(jsonFile==null)break;
                String s=gson.toJson(jsonFile);
                try
                {
                    FileIOUtils.writeFile(file.getAbsolutePath(),s);
                    Toast.makeText(getActivity(), file.getName() +" 已保存！", Toast.LENGTH_SHORT).show();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    Toast.makeText(getActivity(), file.getName() +" 保存失败~_~", Toast.LENGTH_SHORT).show();
                }
                break;
            case REQUEST_FILE_IMPORT:
                if(data==null)return;
                file = new File(data.getStringExtra(FileExplorerFragment.EXTRA_FILE_PATH));
                try
                {
                    StringBuilder stringBuilder = FileIOUtils.readFile(file.getAbsolutePath(),"UTF-8");
                    if(stringBuilder==null) throw new Exception("读取文件失败");
                    final BridgeJsonFile importJsonFile = gson.fromJson(stringBuilder.toString(),BridgeJsonFile.class);
                    if(!importJsonFile.verify())
                    {Toast.makeText(getActivity(), "计分表格式错误！", Toast.LENGTH_SHORT).show();return;}
                    switch (importJsonFile.getFileType())
                    {
                        case SingleSheet:
                            AlertDialog checkDeleteDialog= new AlertDialog.Builder(getActivity())
                                    .setTitle("导入单桌计分表")
                                    .setMessage("导入后将覆盖已有的数据\n是否继续 !?")//设置对话框内容
                                    .setPositiveButton(R.string.string_import, new DialogInterface.OnClickListener() {//设置对话框[肯定]按钮
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            mResultSheet.importResultSheetBean(importJsonFile.getDataBaseResultSheetBean().getTables().get(0));
                                            Toast.makeText(getActivity(), "计分表导入成功！", Toast.LENGTH_SHORT).show();
                                            updateUI();
                                        }
                                    })
                                    .setNegativeButton(R.string.title_cancel, null)
                                    .setCancelable(true).create();
                            checkDeleteDialog.show();
                            checkDeleteDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED);
                            break;
                        case TeamSheet:
                            AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
                            String[] options = mResultSheet.getParentSheet()==null ? new String[]{"导入开室", "导入闭室"} : new String[]{"导入开室", "导入闭室" , "导入队式成绩(开室&闭室)"};
                            alertDialog.setItems(options, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    BridgeResultSheetTeam parentSheet= mResultSheet.getParentSheet();
                                    Boolean isOpenRoom = mResultSheet.getTableInfo().getOpenRoom();
                                    switch (which)
                                    {
                                        case 0:
                                        case 1:
                                            DataBaseResultSheetBean.BridgeResultSheetBean selectedBean=null;
                                            for(DataBaseResultSheetBean.BridgeResultSheetBean sheetBean:importJsonFile.getDataBaseResultSheetBean().getTables())
                                            {
                                                if(sheetBean.getTableInfo().getOpenRoom()==(which==0))//寻找对应桌的sheet
                                                {
                                                    selectedBean = sheetBean;
                                                    break;
                                                }
                                            }
                                            mResultSheet.importResultSheetBean(selectedBean);//该模式只导入结果
                                            if (parentSheet!=null)
                                            {
                                                mResultSheet.mTableInfo.setOpenRoom(isOpenRoom);
                                                mResultSheet.updateTableInfo(mResultSheet.mTableInfo);
                                            }
                                            break;
                                        case 2:
                                            DataBaseResultSheetBean.BridgeResultSheetBean openRoomBean = importJsonFile.getDataBaseResultSheetBean().getTables().get(0);
                                            DataBaseResultSheetBean.BridgeResultSheetBean closedRoomBean = importJsonFile.getDataBaseResultSheetBean().getTables().get(1);
                                            mResultSheet.getParentSheet().getOpenRoomResult().importResultSheetBean(openRoomBean);
                                            mResultSheet.getParentSheet().getClosedRoomResult().importResultSheetBean(closedRoomBean);
                                            break;
                                    }
                                    Toast.makeText(getActivity(), "计分表导入成功！", Toast.LENGTH_SHORT).show();
                                    updateUI();
                                }
                            });
                            alertDialog.create().show();
                            break;
                        default:
                            Toast.makeText(getActivity(), "无法导入 "+importJsonFile.getFileType().name() + " 类型的文件！\n请选择1个单桌计分表或队式计分表！", Toast.LENGTH_SHORT).show();
                            break;
                    }
                    //DataBaseResultSheetBean newBean= gson.fromJson(stringBuilder.toString(),DataBaseResultSheetBean.class);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    Toast.makeText(getActivity(), "文件 "+file.getName() +" 读取失败~_~\n错误原因:" +e.toString(), Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intentFile;
        final Bundle data;
        switch (item.getItemId()) {
            case R.id.menu_item_table_info:
                Intent intent = BridgeTableInfoActivity.newIntent(getActivity(),mResultSheet.getID());
                startActivity(intent);
                return true;
            case R.id.menu_item_setting:
                intentFile = new Intent(getActivity(),FileExplorerActivity.class);
                data = new Bundle();
                data.putSerializable(FileExplorerFragment.ARG_EXPLORE_TASK,FileExplorerTask.Normal_Explore);
                intentFile.putExtras(data);
                startActivity(intentFile);
                //暂时用来测试
                return true;
            case R.id.menu_item_hands_set:
                Hands_Set_Dialog();
                return true;
            case R.id.menu_item_import:
                intentFile = new Intent(getActivity(),FileExplorerActivity.class);
                data = new Bundle();
                data.putSerializable(FileExplorerFragment.ARG_EXPLORE_TASK,FileExplorerTask.Ask_for_file);
                intentFile.putExtras(data);
                startActivityForResult(intentFile,REQUEST_FILE_IMPORT);
                return true;
            case R.id.menu_item_open:
                intentFile = new Intent(getActivity(),FileExplorerActivity.class);
                data = new Bundle();
                data.putSerializable(FileExplorerFragment.ARG_EXPLORE_TASK,FileExplorerTask.Normal_Explore);
                intentFile.putExtras(data);
                startActivity(intentFile);
                /*
                  intentFile = new Intent(getActivity(),FileExplorerActivity.class);
                  data = new Bundle();
                  data.putSerializable(FileExplorerFragment.ARG_EXPLORE_TASK,FileExplorerTask.Ask_for_file);
                  data.putString(FileExplorerFragment.ARG_PARAM,".brg");
                  intentFile.putExtras(data);
                  */
                  //startActivityForResult(intentFile,REQUEST_FILE);
//                Intent intentFile = new Intent(Intent.ACTION_GET_CONTENT);
//                intentFile.setType("*/*");//设置类型，我这里是任意类型，任意后缀的可以这样写。
//                intentFile.addCategory(Intent.CATEGORY_OPENABLE);
//                startActivityForResult(intentFile,REQUEST_FILE_BY_SYSTEM);
                return true;
            case R.id.menu_item_save:
                intentFile = new Intent(getActivity(),FileExplorerActivity.class);
                data = new Bundle();
                data.putSerializable(FileExplorerFragment.ARG_EXPLORE_TASK,FileExplorerTask.Bridge_File_Saving);
                intentFile.putExtras(data);
                SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
                final String dateString = format.format(mResultSheet.getTableInfo().getDate());
                //startActivityForResult(intentFile,REQUEST_FILE_SAVE);
                if(mResultSheet.getParentSheet()==null)
                {
                    intentFile.putExtra(FileExplorerFragment.ARG_PARAM,"单桌计分表_"+dateString);
                    startActivityForResult(intentFile,REQUEST_FILE_SAVE+BridgeFileType.SingleSheet.ordinal());
                    return true;
                }
                final Intent intentSaveFile = intentFile;
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
                alertDialog.setItems(new String[]{"保存队式成绩单", "仅保存该桌"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which)
                        {
                            case 0:
                                intentSaveFile.putExtra(FileExplorerFragment.ARG_PARAM,"队式计分表_"+dateString);
                                startActivityForResult(intentSaveFile,REQUEST_FILE_SAVE+BridgeFileType.TeamSheet.ordinal());
                                break;
                            case 1:
                                intentSaveFile.putExtra(FileExplorerFragment.ARG_PARAM,"单桌计分表_"+dateString);
                                startActivityForResult(intentSaveFile,REQUEST_FILE_SAVE+BridgeFileType.SingleSheet.ordinal());
                                break;
                        }
                    }
                });
                alertDialog.create().show();
                /*
                List<BridgeResultSheet> resultSheets=new ArrayList<>();
                resultSheets.add(mResultSheet);
                DataBaseResultSheetBean baseResultSheetBean=new DataBaseResultSheetBean(resultSheets);
                //String s=gson.toJson(mResultSheet.getTableInfo());
                String s=gson.toJson(baseResultSheetBean);
                //Toast.makeText(getContext(),s,Toast.LENGTH_LONG).show();
                DataBaseResultSheetBean newBean= gson.fromJson(s,DataBaseResultSheetBean.class);
                if(mResultSheet.getParentSheet()!=null)
                {
                    //mResultSheet.deleteAllContracts();
                    //newBean.ExportToDatabase(getContext());
                    //updateUI();
                    //BridgeJsonFile jsonFile= BridgeJsonFile.createdByTeamResultSheet(mResultSheet.getParentSheet());
                    BridgeJsonFile jsonFile= BridgeJsonFile.createdByResultSheet(mResultSheet);
                    s=gson.toJson(jsonFile);
                    jsonFile = gson.fromJson(s,BridgeJsonFile.class);
                    s=gson.toJson(jsonFile);
                    Toast.makeText(getContext(),s,Toast.LENGTH_LONG).show();
                }
                */
                return true;
            //OutJson.createJsonFile(s)
            case R.id.menu_item_clearAll:
                mResultSheet.clearResults();
                updateUI();
                return true;
            case R.id.menu_item_about:
                new AlertDialog.Builder(getActivity())
                        .setTitle("关于")
                        .setMessage("Scorer\n" +
                                "version:0.1.1（XJTU内测）\n" +
                                "西安交通大学棋牌协会桥牌部内部使用\n" +
                                "©2017-2018 Black-Tech Studio of Sleeping Pope. All rights reserved.\n" +
                                "教皇黑科技工作室 版权所有\n\n" +
                                "功能简介：\n" +
                                "1.队式、单桌计分表(保存、导入、合并)\n" +
                                "2.文件管理器\n" +
                                "其余功能敬请期待~有任何功能期望请直接告诉我" )
                        .setPositiveButton(R.string.title_confirm,null).setCancelable(true).create().show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void Hands_Set_Dialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        View view = LayoutInflater.from(getActivity()).inflate(
                R.layout.dialog_hands_set, null);
        final CustomNumberPicker numPicker = (CustomNumberPicker) view.findViewById(R.id.numberPicker);
        Field[] pickerFields = NumberPicker.class.getDeclaredFields();
        for (Field pf : pickerFields) {
            if (pf.getName().equals("mSelectionDivider")) {
                pf.setAccessible(true);
                try {
                    //设置分割线的颜色值
                    pf.set(numPicker, new ColorDrawable(ContextCompat.getColor(getContext(), R.color.colorAccent)));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        mResultSheet.mTableInfo=mResultSheet.getTableInfo();
        numPicker.setMinValue(1);
        numPicker.setMaxValue(1896);
        numPicker.setValue(mResultSheet.mTableInfo.getTotalHands());
        alertDialogBuilder.setTitle("副数设定")
            .setView(view)
            .setPositiveButton("确定",
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub
                    int pickerValue = numPicker.getValue();
                    int originTotalHands=mResultSheet.mTableInfo.getTotalHands();
                    final int tempInt;
                    int i;
                    if(pickerValue==originTotalHands)return;
                    AlertDialog.Builder setHandsDialog= new AlertDialog.Builder(getActivity());
                    if(pickerValue==1896)
                    {
                        Toast.makeText(getContext(),"恭喜找到一枚彩蛋~\n\nXJTU和SJTU始于1896",Toast.LENGTH_LONG).show();
                        return;
                    }
                    if(pickerValue>96)
                    {
                        Toast.makeText(getContext(),"请注意身体，一次最多允许您打96副牌哦^_-",Toast.LENGTH_LONG).show();
                        pickerValue=96;
                    }
                    String dialogText= null;
                    tempInt=pickerValue;
                    if(mResultSheet.getParentSheet() == null)
                    {
                        if(originTotalHands<pickerValue)
                        {
                            mResultSheet.resetTotalHands(pickerValue);
                            updateUI();
                            Toast.makeText(getContext(),"设定成功！",Toast.LENGTH_SHORT).show();
                            return;
                        }
                        for(i=pickerValue;i<originTotalHands;i++)
                        {
                            if(!mResultSheet.getContract(i+1).mFlag.equals("None"))
                            {
                                dialogText = String.format(getString(R.string.alert_hands_set),pickerValue,"本计分表",pickerValue+1);
                                break;
                            }
                        }
                        if(dialogText==null)
                        {
                            mResultSheet.resetTotalHands(pickerValue);
                            updateUI();
                            Toast.makeText(getContext(),"设定成功！",Toast.LENGTH_SHORT).show();
                            return;
                        }
                        setHandsDialog.setMessage(dialogText)//设置对话框内容
                        .setPositiveButton(R.string.title_confirm, new DialogInterface.OnClickListener() {//设置对话框[确认]按钮
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mResultSheet.resetTotalHands(tempInt);
                                updateUI();
                                Toast.makeText(getContext(),"设定成功！",Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    else
                    {
                        final BridgeResultSheet otherTableSheet = mResultSheet.getTableInfo().getOpenRoom()? mResultSheet.getParentSheet().getClosedRoomResult() : mResultSheet.getParentSheet().getOpenRoomResult();
                        if(otherTableSheet.getTableInfo().getTotalHands()<pickerValue&&mResultSheet.getTableInfo().getTotalHands()<pickerValue)
                        {
                            mResultSheet.resetTotalHands(pickerValue);
                            otherTableSheet.resetTotalHands(pickerValue);
                            updateUI();
                            Toast.makeText(getContext(),"设定成功！",Toast.LENGTH_SHORT).show();
                            return;
                        }
                        for(i=pickerValue;i<originTotalHands;i++)
                        {
                            if(!mResultSheet.getContract(i+1).mFlag.equals("None"))
                            {
                                dialogText = String.format(getString(R.string.alert_hands_set),pickerValue,"开室与闭室计分表",pickerValue+1);
                                break;
                            }
                        }
                        if(dialogText==null)
                            for(i=pickerValue;i<otherTableSheet.getTableInfo().getTotalHands();i++)
                            {
                                if(!otherTableSheet.getContract(i+1).mFlag.equals("None"))
                                {
                                    dialogText = String.format(getString(R.string.alert_hands_set),pickerValue,"开室与闭室计分表",pickerValue+1);
                                    break;
                                }
                            }
                        if(dialogText==null)
                        {
                            mResultSheet.resetTotalHands(pickerValue);
                            otherTableSheet.resetTotalHands(pickerValue);
                            updateUI();
                            Toast.makeText(getContext(),"设定成功！",Toast.LENGTH_SHORT).show();
                            return;
                        }
                        setHandsDialog.setMessage(dialogText)//设置对话框内容
                                .setPositiveButton(R.string.title_confirm, new DialogInterface.OnClickListener() {//设置对话框[确认]按钮
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        mResultSheet.resetTotalHands(tempInt);
                                        otherTableSheet.resetTotalHands(tempInt);
                                        updateUI();
                                        Toast.makeText(getContext(),"设定成功！",Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                    AlertDialog alertDialog=setHandsDialog
                    .setNegativeButton(R.string.title_cancel, null)
                    .setCancelable(true).create();
                    alertDialog.show();
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED);

                }
            })
            .setNegativeButton("取消",
            new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub

                }
            })
            .create()
            .show();
    }

    private void updateSubtitle() {

    }

    private void updateUI() {
        if(mResultSheet==null)return;
        List<BridgeContract> contracts = mResultSheet.getContracts();

        if (mAdapter == null) {
            mAdapter = new BridgeContractAdapter(contracts);
            mResultSheetRecyclerView.setAdapter(mAdapter);
        } else {
            mAdapter.setContracts(contracts);
            mAdapter.notifyDataSetChanged();
        }

        updateSubtitle();
    }

    private class BridgeContractHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener,View.OnLongClickListener {

        private TextView mHandsNumTextView;
        private TextView mDeclarerTextView;
        private TextView mContractTextView;
        private TextView mResultTextView;
        private TextView mNsScoreTextView;
        private TextView mEwScoreTextView;

        private BridgePickerView mBridgePickerView;
        private BridgeContract mBridgeContract;
        private PopupMenu mPopupMenu;

        BridgeContractHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);

            mHandsNumTextView = (TextView) itemView.findViewById(R.id.list_item_Hands_num);
            mDeclarerTextView = (TextView) itemView.findViewById(R.id.list_item_declarer);
            mContractTextView = (TextView) itemView.findViewById(R.id.list_item_contract);
            mResultTextView = (TextView) itemView.findViewById(R.id.list_item_result);
            mNsScoreTextView = (TextView) itemView.findViewById(R.id.list_item_ns_score);
            mEwScoreTextView = (TextView) itemView.findViewById(R.id.list_item_ew_score);

            mBridgePickerView = new BridgePickerView.Builder(getContext(), new BridgePickerView.OnOptionsSelectListener() {

                @Override
                public void onOptionsSelect(int options1, int options2, int options3, int options4, int options5, View v) {
                    String temp;
                    if(options1==-1)//点击了ALL PASS
                    {
                        temp="AP";
                    }
                    else
                    {
                        temp=BGOp.directions.get(options1)+BGOp.levels.get(options2)+BGOp.suits.get(options3)+BGOp.doubles.get(options4)+BGOp.results.get(options2).get(options5);
                    }
                    mBridgeContract.setContract(temp);
                    mResultSheet.updateContract(mBridgeContract);
                    updateUI();
                }
            }).setSelectOptions(0,4,0,0,4).isDialog(true).build();
            mBridgePickerView.setBridgePicker(BGOp.directions, BGOp.levels,BGOp.suits,BGOp.doubles,BGOp.results);
        }

        public void bindBridgeContract(BridgeContract bridgeCon) {
            mBridgeContract = bridgeCon;
            mHandsNumTextView.setText(String.valueOf(mBridgeContract.getBoardNum()));

            if(!bridgeCon.mFlag.equals("Done"))
            {
                mContractTextView.setText("");
                mDeclarerTextView.setText("");
                mResultTextView.setText("");
                mNsScoreTextView.setText("");
                mEwScoreTextView.setText("");

                mHandsNumTextView.setTextColor(ContextCompat.getColor(getContext(),R.color.color_boardNum_unfinished));
                return;
            }
            mHandsNumTextView.setTextColor(ContextCompat.getColor(getContext(),R.color.color_boardNum_finished1));
            String temp=mBridgeContract.getContract();
            mContractTextView.setText(temp);
            if(temp.equals("All Pass"))
            {
                mDeclarerTextView.setText("");
                mResultTextView.setText("");
                mNsScoreTextView.setText("");
                mEwScoreTextView.setText("");
                return;
            }
            mDeclarerTextView.setText(mBridgeContract.getDeclarer());
            mResultTextView.setText(mBridgeContract.getResultString());
            int score=mBridgeContract.getScore();
            if(score>0)
            {
                mNsScoreTextView.setText(String.valueOf(score));
                mEwScoreTextView.setText("");
            }
            else
            {
                mEwScoreTextView.setText(String.valueOf(-score));
                mNsScoreTextView.setText("");
            }
        }

        @Override
        public void onClick(View v) {
            //Intent intent = CrimePagerActivity.newIntent(getActivity(), mCrime.getId());
            //startActivity(intent);
            if(mBridgeContract.mFlag.equals("Done")&mBridgeContract.getSuit()!=Suit.AP)
            {
                mBridgePickerView.setSelectOptions("NSEW".indexOf(mBridgeContract.getDeclarer()),
                        7-mBridgeContract.getLevel(),
                        mBridgeContract.getSuit().ordinal(),
                        mBridgeContract.getDouble().ordinal(),7-mBridgeContract.getLevel()-mBridgeContract.getResult());
            }
            else
            {
                mBridgePickerView.setSelectOptions(1,4,0,0,4);
            }
            mBridgePickerView.show(v);
            }

        @Override
        public boolean onLongClick(View v) {
            mPopupMenu = new PopupMenu(getActivity(),v, Gravity.RIGHT,R.attr.popupMenuStyle,0);

            MenuInflater inflater = new MenuInflater(getActivity());
            inflater.inflate(R.menu.result_popmenu,mPopupMenu.getMenu());

            //这是一个SpannableStringBuilder 的构造器，不用在意这个，大家可以自己写一个，网上也有好多类似的
            SpannableStringBuilder builder = SpannableStringUtils.getBuilder(getString(R.string.menu_item_clear))
                    .setForegroundColor(ContextCompat.getColor(getContext(),R.color.color_black))
                    .create();
            //这样就可以更改了id为report的这个menu的字体颜色了
            MenuItem menuItem=mPopupMenu.getMenu().findItem(R.id.menu_item_clear);
            menuItem.setTitle(builder);

            mPopupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
            {
                @Override
                public boolean onMenuItemClick(MenuItem item)
                {
                    switch (item.getItemId())
                    {
                        case R.id.menu_item_clear:
                            mBridgeContract.Clear();
                            mResultSheet.updateContract(mBridgeContract);
                            updateUI();
                            break;

                    }
                    return true;
                }
            });
            mPopupMenu.show();
            return true;
        }
    }

    private class BridgeContractAdapter extends RecyclerView.Adapter<BridgeContractHolder> {

        private List<BridgeContract> mContracts;

        public BridgeContractAdapter(List<BridgeContract> contracts) {
            mContracts = contracts;
        }

        @Override
        public BridgeContractHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View view = layoutInflater.inflate(R.layout.item_contract, parent, false);
            return new BridgeContractHolder(view);
        }

        @Override
        public void onBindViewHolder(BridgeContractHolder holder, int position) {
            BridgeContract contract = mContracts.get(position);
            holder.bindBridgeContract(contract);
        }

        @Override
        public int getItemCount() {
            return mContracts.size();
        }

        public void setContracts(List<BridgeContract> contracts) {
            mContracts = contracts;
        }
    }
}
