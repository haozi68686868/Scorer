package com.blacktec.think.scorer;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.bigkoo.pickerview.TimePickerView;
import com.bigkoo.pickerview.listener.CustomListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

/**
 * Created by Think on 2017/12/8.
 */
public class TableInfoFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_TABLE_ID = "table_id";

    // TODO: Rename and change types of parameters
    private UUID uuid;
    private BridgeResultSheet mResultSheet;
    
    private TimePickerView mDatePickerView;
    private TextView mDateTextView;

    private AutoCompleteTextView mNSTeamTextView;
    private AutoCompleteTextView mEWTeamTextView;
    private AutoCompleteTextView mNPlayerTextView;
    private AutoCompleteTextView mSPlayerTextView;
    private AutoCompleteTextView mEPlayerTextView;
    private AutoCompleteTextView mWPlayerTextView;

    private Button mChangeInfoButton;
    private RadioGroup mIsOpenRadioGroup;
    private RadioButton mRadioButtonOpenRoom;
    private RadioButton mRadioButtonClosedRoom;



    public TableInfoFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param tableId Parameter 1.
     * @return A new instance of fragment TableInfoFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static TableInfoFragment newInstance(UUID tableId) {
        TableInfoFragment fragment = new TableInfoFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_TABLE_ID, tableId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            uuid = (UUID) getArguments().getSerializable(ARG_TABLE_ID);
            mResultSheet=new BridgeResultSheet(getActivity(),uuid);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        
        initDatePicker();
        // Inflate the layout for this fragment
        View view =inflater.inflate(R.layout.fragment_table_info, container, false);

        mChangeInfoButton = (Button)view.findViewById(R.id.buttonChangeInfo);
        mNSTeamTextView = (AutoCompleteTextView)view.findViewById(R.id.TextView_nsTeam);
        mEWTeamTextView = (AutoCompleteTextView)view.findViewById(R.id.TextView_ewTeam);
        mNPlayerTextView = (AutoCompleteTextView)view.findViewById(R.id.EditText_N_player);
        mSPlayerTextView = (AutoCompleteTextView)view.findViewById(R.id.EditText_S_player);
        mEPlayerTextView = (AutoCompleteTextView)view.findViewById(R.id.EditText_E_player);
        mWPlayerTextView = (AutoCompleteTextView)view.findViewById(R.id.EditText_W_player);
        mIsOpenRadioGroup = (RadioGroup)view.findViewById(R.id.RadioGroup_isOpen);
        mRadioButtonOpenRoom = (RadioButton)view.findViewById(R.id.RadioBtnOpenRoom);
        mRadioButtonClosedRoom = (RadioButton)view.findViewById(R.id.RadioBtnClosedRoom);
        mDateTextView=(TextView)view.findViewById(R.id.textViewDate);
        mDateTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDatePickerView.show();
            }
        });

        BridgeTableInfo tableInfo = mResultSheet.mTableInfo;
        mNSTeamTextView.setText(tableInfo.getNSTeamName());
        mEWTeamTextView.setText(tableInfo.getEWTeamName());
        mNPlayerTextView.setText(tableInfo.getNorthPlayer());
        mSPlayerTextView.setText(tableInfo.getSouthPlayer());
        mEPlayerTextView.setText(tableInfo.getEastPlayer());
        mWPlayerTextView.setText(tableInfo.getWestPlayer());
        mDateTextView.setText(getTime(tableInfo.getDate()));
        if(tableInfo.getOpenRoom())
            mRadioButtonOpenRoom.setChecked(true);
        else
            mRadioButtonClosedRoom.setChecked(true);
        mChangeInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try
                {
                    BridgeTableInfo tableInfo = mResultSheet.mTableInfo;
                    tableInfo.setNSTeamName(mNSTeamTextView.getText().toString());
                    tableInfo.setEWTeamName(mEWTeamTextView.getText().toString());
                    tableInfo.setNorthPlayer(mNPlayerTextView.getText().toString());
                    tableInfo.setSouthPlayer(mSPlayerTextView.getText().toString());
                    tableInfo.setEastPlayer(mEPlayerTextView.getText().toString());
                    tableInfo.setWestPlayer(mWPlayerTextView.getText().toString());
                    tableInfo.setOpenRoom(mIsOpenRadioGroup.getCheckedRadioButtonId()==R.id.RadioBtnOpenRoom);
                    mResultSheet.updateTableInfo(tableInfo);
                    Toast.makeText(getActivity(),"修改成功",Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
                catch (Exception e)
                {
                    Toast.makeText(getActivity(),e.toString(),Toast.LENGTH_SHORT).show();
                }
            }
        });

        return view;
    }

    // TODO: Rename method, update argument and hook method into UI event

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private void initDatePicker() {

        /**
         * @description
         *
         * 注意事项：
         * 1.自定义布局中，id为 optionspicker 或者 timepicker 的布局以及其子控件必须要有，否则会报空指针.
         * 具体可参考demo 里面的两个自定义layout布局。
         * 2.因为系统Calendar的月份是从0-11的,所以如果是调用Calendar的set方法来设置时间,月份的范围也要是从0-11
         * setRangDate方法控制起始终止时间(如果不设置范围，则使用默认时间1900-2100年，此段代码可注释)
         */
        Calendar selectedDate = Calendar.getInstance();//系统当前时间
        selectedDate.setTime(mResultSheet.mTableInfo.getDate());
        //Calendar startDate = Calendar.getInstance();
        //startDate.set(2014, 1, 23);
        //Calendar endDate = Calendar.getInstance();
        //endDate.set(2027, 2, 28);
        //时间选择器 ，自定义布局
        mDatePickerView = new TimePickerView.Builder(getContext(), new TimePickerView.OnTimeSelectListener() {
            @Override
            public void onTimeSelect(Date date, View v) {//选中事件回调
                mDateTextView.setText(getTime(date));
                mResultSheet.mTableInfo.setDate(date);
            }
        })
                /*.setType(TimePickerView.Type.ALL)//default is all
                .setCancelText("Cancel")
                .setSubmitText("Sure")
                .setContentSize(18)
                .setTitleSize(20)
                .setTitleText("Title")
                .setTitleColor(Color.BLACK)
               /*.setDividerColor(Color.WHITE)//设置分割线的颜色
                .setTextColorCenter(Color.LTGRAY)//设置选中项的颜色
                .setLineSpacingMultiplier(1.6f)//设置两横线之间的间隔倍数
                .setTitleBgColor(Color.DKGRAY)//标题背景颜色 Night mode
                .setBgColor(Color.BLACK)//滚轮背景颜色 Night mode
                .setSubmitColor(Color.WHITE)
                .setCancelColor(Color.WHITE)*/
               /*.gravity(Gravity.RIGHT)// default is center*/
                .setDate(selectedDate)
                //.setRangDate(startDate, endDate)
                .setLayoutRes(R.layout.pickerview_custom_time, new CustomListener() {

                    @Override
                    public void customLayout(View v) {
                        final TextView tvSubmit = (TextView) v.findViewById(R.id.tv_finish);
                        ImageView ivCancel = (ImageView) v.findViewById(R.id.iv_cancel);
                        tvSubmit.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mDatePickerView.returnData();
                                mDatePickerView.dismiss();
                            }
                        });
                        ivCancel.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mDatePickerView.dismiss();
                            }
                        });
                    }
                })
                .setType(new boolean[]{true, true, true, false, false, false})
                .setLabel("年", "月", "日", "时", "分", "秒")
                .isCenterLabel(false) //是否只显示中间选中项的label文字，false则每项item全部都带有label。
                .setDividerColor(0xFF24AD9D)
                .build();

    }

    private String getTime(Date date) {//可根据需要自行截取数据显示
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        return format.format(date);
    }


}
