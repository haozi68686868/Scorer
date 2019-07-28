package com.blacktec.think.scorer;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.blacktec.think.scorer.BridgeDbSchema.*;
import static com.blacktec.think.scorer.DataBaseResultSheetBean.*;

/**
 * Created by Think on 2017/10/12.
 */

public class BridgeResultSheet {

    private UUID mId;
    private Context mContext;
    private SQLiteDatabase mDatabase;
    private BridgeResultSheetTeam mParentSheet;
    public BridgeTableInfo mTableInfo;

    public static BridgeResultSheet getRecent(Context context)
    {
        SharedPreferences preferences;
        SharedPreferences.Editor editor;
        BridgeResultSheet resultSheet;
        preferences = context.getSharedPreferences("recentSheet",Context.MODE_PRIVATE);
        String id = preferences.getString("recentSheetId",null);
        if(id!=null)
        {
            return new BridgeResultSheet(context,UUID.fromString(id));
        }
        resultSheet = new BridgeResultSheet(context);
        editor=preferences.edit();
        editor.putString("recentSheetId",resultSheet.getID().toString());
        editor.apply();
        return resultSheet;
    }
    public static void setRecent(Context context,BridgeResultSheet resultSheet)
    {
        SharedPreferences.Editor editor;
        editor=context.getSharedPreferences("recentSheet",Context.MODE_PRIVATE).edit();
        editor.putString("recentSheetId",resultSheet.getID().toString());
        editor.apply();
    }
    public BridgeResultSheet(Context context)
    {
        this(context,UUID.randomUUID());
    }
    public BridgeResultSheet(Context context,UUID id)
    {
        this(context,id,16);
    }
    public BridgeResultSheet(Context context,int totalHands)
    {
        this(context,UUID.randomUUID(),totalHands);
    }
    //若已经存在该UUID，则该最后一个参数不起作用
    public BridgeResultSheet(Context context,UUID id,int totalHands)
    {
        mId=id;
        mContext=context;
        mDatabase=new BridgeBaseHelper(mContext).getWritableDatabase();
        //判断id是否已经在roominfo表里出现，若有，则设置totalHands为无效的，否则设置totalhands并初始化
        mTableInfo = getTableInfo();
        if(mTableInfo==null)
        {
            mTableInfo = new BridgeTableInfo();
            mTableInfo.setTotalHands(totalHands);
            addTableInfo(mTableInfo);
        }
        init();
    }
    public BridgeResultSheet(Context context,UUID id,BridgeTableInfo tableInfo)
    {
        mId=id;
        mContext=context;
        mDatabase=new BridgeBaseHelper(mContext).getWritableDatabase();
        mTableInfo = getTableInfo();
        if(mTableInfo==null)
        {
            mTableInfo=tableInfo;
            addTableInfo(mTableInfo);
        }
        else
        {
            mTableInfo=tableInfo;
            updateTableInfo(mTableInfo);
        }
    }
    private void init()
    {
        if(!getContracts().isEmpty())return;
        for(int i=0;i<mTableInfo.getTotalHands();i++)
        {
            BridgeContract con=new BridgeContract(i+1);
            addContract(con);
        }
    }
    public void clearResults()
    {
        List<BridgeContract> contracts = getContracts();
        for (BridgeContract b:contracts) {
            b.Clear();
            updateContract(b);
        }
    }
    public void resetTotalHands(int totalHands)
    {
        mTableInfo=getTableInfo();
        int origin=mTableInfo.getTotalHands();
        if(totalHands==origin)return;
        if(totalHands>origin)
        {
            for(int i=origin;i<totalHands;i++)
            {
                BridgeContract con=new BridgeContract(i+1);
                addContract(con);
            }
        }
        else
        {
            List<BridgeContract> contracts = getContracts();
            for (BridgeContract b:contracts) {
                if(b.getBoardNum()>totalHands)
                    deleteContract(b);
            }
        }
        mTableInfo.setTotalHands(totalHands);
        updateTableInfo(mTableInfo);
    }

    public List<BridgeContract> getContracts() {
        List<BridgeContract> contracts = new ArrayList<>();

        BridgeCursorWrapper cursor = queryContracts(ResultSheet.Cols.TABLE_UUID + " = ?",new String[]{mId.toString()});

        try{
            cursor.moveToFirst();
            while (!cursor.isAfterLast())
            {
                contracts.add(cursor.getContract());
                cursor.moveToNext();
            }
        }finally {
            cursor.close();
        }
        return contracts;
    }

    public void setContracts(List<BridgeContract> contracts) {
        //mContracts = contracts;
    }

    public UUID getID() {
        return mId;
    }

    public Context getContext() {
        return mContext;
    }
    public void setID(UUID ID) {
        mId = ID;
    }

    public void addTableInfo(BridgeTableInfo tableInfo)
    {
        ContentValues values = getContentValues(tableInfo);
        mDatabase.insert(TableInfo.NAME, null , values);
    }
    public void updateTableInfo(BridgeTableInfo tableInfo)
    {
        ContentValues values = getContentValues(tableInfo);
        mDatabase.update(TableInfo.NAME,values, TableInfo.Cols.TABLE_UUID + " = ?" ,new String[]{mId.toString()});
    }
    public void deleteTableInfo(BridgeTableInfo tableInfo)
    {
        ContentValues values = getContentValues(tableInfo);
        mDatabase.delete(TableInfo.NAME,TableInfo.Cols.TABLE_UUID + " = ?" ,new String[]{mId.toString()});
    }
    public BridgeTableInfo getTableInfo() {
        BridgeCursorWrapper cursor = queryTableInfo(
                TableInfo.Cols.TABLE_UUID + " = ?" ,new String[]{mId.toString()});
        try {
            if (cursor.getCount() == 0) return null;
            cursor.moveToFirst();
            return cursor.getTableInfo();
        }finally
        {
            cursor.close();
        }
    }
    private ContentValues getContentValues(BridgeTableInfo table)
    {
        ContentValues values = new ContentValues();
        values.put(TableInfo.Cols.TABLE_UUID,mId.toString());
        values.put(TableInfo.Cols.TABLE_NUM,table.getTableNum());
        values.put(TableInfo.Cols.IS_OPEN_ROOM,table.getOpenRoom() ? 1 : 0);
        values.put(TableInfo.Cols.TOTAL_HANDS,table.getTotalHands());
        values.put(TableInfo.Cols.N_PLAYER,table.getNorthPlayer());
        values.put(TableInfo.Cols.S_PLAYER,table.getSouthPlayer());
        values.put(TableInfo.Cols.E_PLAYER,table.getEastPlayer());
        values.put(TableInfo.Cols.W_PLAYER,table.getWestPlayer());
        values.put(TableInfo.Cols.NS_TEAM,table.getNSTeamName());
        values.put(TableInfo.Cols.EW_TEAM,table.getEWTeamName());
        values.put(TableInfo.Cols.DATE,table.getDate().getTime());

        return values;
    }
    private ContentValues getContentValues(BridgeContract contract)
    {
        ContentValues values = new ContentValues();
        values.put(ResultSheet.Cols.TABLE_UUID,mId.toString());
        values.put(ResultSheet.Cols.BOARD_NUM,contract.getBoardNum());
        values.put(ResultSheet.Cols.DECLARER,contract.getDeclarer());
        values.put(ResultSheet.Cols.CONTRACT,contract.getContract());
        values.put(ResultSheet.Cols.RESULT,contract.getResult());
        return values;
    }
    public void deleteContract(BridgeContract contract)
    {
        mDatabase.delete(ResultSheet.NAME,ResultSheet.Cols.TABLE_UUID + " = ?" + " AND " +
                ResultSheet.Cols.BOARD_NUM + " = "+String.valueOf(contract.getBoardNum()),new String[]{mId.toString()});
    }
    public void deleteAllContracts()
    {
        mDatabase.delete(ResultSheet.NAME,ResultSheet.Cols.TABLE_UUID + " = ?",new String[]{mId.toString()});
        mTableInfo=getTableInfo();
        mTableInfo.setTotalHands(0);
        updateTableInfo(mTableInfo);
    }
    public void delete()
    {
        deleteAllContracts();
        deleteTableInfo(mTableInfo);
    }

    public void addContract(BridgeContract contract)
    {
        ContentValues values = getContentValues(contract);
        mDatabase.insert(ResultSheet.NAME, null , values);
    }

    public void updateContract(BridgeContract contract)
    {
        int boardNum =contract.getBoardNum();
        ContentValues values = getContentValues(contract);
        mDatabase.update(ResultSheet.NAME,values,
                ResultSheet.Cols.TABLE_UUID + " = ?" + " AND " +
                ResultSheet.Cols.BOARD_NUM + " = "+String.valueOf(boardNum),new String[]{mId.toString()});
    }

    public BridgeContract getContract(int boardNum) {
        BridgeCursorWrapper cursor = queryContracts(
                ResultSheet.Cols.TABLE_UUID + " = ?" + " AND " +
                        ResultSheet.Cols.BOARD_NUM + " = "+String.valueOf(boardNum),new String[]{mId.toString()});
        try {
            if (cursor.getCount() == 0) return null;
            cursor.moveToFirst();
            return cursor.getContract();
        }finally
        {
            cursor.close();
        }
    }

    private BridgeCursorWrapper queryContracts(String whereClause,String[] whereArgs)
    {
        Cursor cursor = mDatabase.query(
                ResultSheet.NAME,
                null,
                whereClause,
                whereArgs,
                null, null, null
        );
        return new BridgeCursorWrapper(cursor);
    }

    private BridgeCursorWrapper queryTableInfo(String whereClause,String[] whereArgs)
    {
        Cursor cursor = mDatabase.query(
                TableInfo.NAME,
                null,
                whereClause,
                whereArgs,
                null, null, null
        );
        return new BridgeCursorWrapper(cursor);
    }
    public void importResultSheetBean(DataBaseResultSheetBean.BridgeResultSheetBean bean)//仅导入牌局数据,id不变
    {
        bean.setTableId(mId.toString());
        delete();
        bean.ExportToDatabase(mContext);
    }

    public BridgeResultSheetTeam getParentSheet() {
        return mParentSheet;
    }

    public void setParentSheet(BridgeResultSheetTeam parentSheet) {
        mParentSheet = parentSheet;
    }



}
