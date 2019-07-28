package com.blacktec.think.scorer;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.bigkoo.pickerview.BridgePickerData;
import com.bigkoo.pickerview.BridgePickerData.BGOp;
import com.bigkoo.pickerview.BridgePickerView;
import com.bigkoo.pickerview.OptionsPickerView;
import com.blacktec.think.scorer.Utils.AllCapTransformationMethod;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {

    private OptionsPickerView pvNoLinkOptions;
    private Button mTestButton;
    private ArrayList<String> directions = new ArrayList<>();
    private ArrayList<String> suits = new ArrayList<>();
    private ArrayList<ArrayList<String>> results = new ArrayList<>();
    private ArrayList<String> doubles = new ArrayList<>();
    private ArrayList<String> levels = new ArrayList<>();
    private BridgePickerView mBridgePickerView;

    private EditText mContractEditText;
    private TextView mResultTextView;
    private BridgeContract mBridgeContract;

    private void getNoLinkData() {
        directions.add("N");
        directions.add("S");
        directions.add("E");
        directions.add("W");
        suits.add("NT");
        suits.add("♠");
        suits.add("♥");
        suits.add("♦");
        suits.add("♣");
        int i,j;
        ArrayList<String> temp = new ArrayList<>();
        for(i=7;i>0;i--) {
            levels.add(String.valueOf(i));
            temp=new ArrayList<>();
            for(j=7-i;j>-7-i;j--)
            {
                temp.add(j==0?"=":j<0?String.valueOf(j):"+"+String.valueOf(j));
            }
            results.add(temp);
        }
        doubles.add("");
        doubles.add("X");
        doubles.add("XX");
        //results.add("All Pass");
    }

    private void initNoLinkOptionsPicker() {// 不联动的多级选项
        pvNoLinkOptions = new OptionsPickerView.Builder(this, new OptionsPickerView.OnOptionsSelectListener() {

            @Override
            public void onOptionsSelect(int options1, int options2, int options3, View v) {
                String temp;
                if(results.get(options3).equals("All Pass"))
                {
                    temp="AP";
                }
                else
                {
                    temp=directions.get(options1)+suits.get(options2)+results.get(options3);
                }
                mContractEditText.setText(temp);
            }
        }).setSelectOptions(0,0,6).build();
        pvNoLinkOptions.setNPicker(directions, suits, results);
    }
    private void initTest() {// 不联动的多级选项
        mBridgePickerView = new BridgePickerView.Builder(this, new BridgePickerView.OnOptionsSelectListener() {

            @Override
            public void onOptionsSelect(int options1, int options2, int options3, int options4, int options5, View v) {
                String temp;
                if(options1==-1)//点击了ALL PASS
                {
                    temp="AP";
                }
                else
                {
                    temp=directions.get(options1)+levels.get(options2)+suits.get(options3)+doubles.get(options4)+results.get(options2).get(options5);
                }
                mContractEditText.setText(temp);
            }
        }).setSelectOptions(0,4,0,0,4).isDialog(true).build();
        //mBridgePickerView.setNPicker(directions, levels,suits,doubles, results);
        //mBridgePickerView.setBridgePicker(directions, levels,suits,doubles, results);
        BridgePickerData.BGOp.init();
        mBridgePickerView.setBridgePicker(BGOp.directions, BGOp.levels,BGOp.suits,BGOp.doubles,BGOp.results);
        //mBridgePickerView.setPicker(contracts);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getNoLinkData();
        initNoLinkOptionsPicker();
        initTest();

        mBridgeContract=new BridgeContract();
        mContractEditText=(EditText)findViewById(R.id.editText);
        mResultTextView=(TextView)findViewById(R.id.textView2);

        mContractEditText.setTransformationMethod(new AllCapTransformationMethod(true));
        mContractEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mBridgeContract.setContract(s.toString().toUpperCase()) ;
                if(mBridgeContract.mFlag.equals("Done"))
                {
                    int score=mBridgeContract.getScore();
                    mResultTextView.setText(String.valueOf(score));
                }
                else if(mBridgeContract.mFlag.contains("Format Error"))
                {
                    mResultTextView.setText(mBridgeContract.mFlag);
                }
                else
                    mResultTextView.setText("");
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        mTestButton = (Button) findViewById(R.id.Btn_test);
        mTestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBridgePickerView.show();
            }
        });
    }
}
