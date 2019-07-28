package com.blacktec.think.scorer;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;

/**
 * Created by Think on 2018/3/20.
 */

public class BridgeResultSheetTeam {
    public static int[] IMPdata={20,50,90,130,170,220,270,320,370,430,500,600,750,900,1100,
    1300,1500,1750,2000,2250,2500,3000,3500,4000,8000};
    private UUID mId;

    private float mHomeVp;
    private float mAwayVp;
    private int mHomeIMP;
    private int mAwayIMP;

    private List<BridgeIMPInfo> mIMPInfoList;
    private BridgeResultSheet mOpenRoomResult;
    private BridgeResultSheet mClosedRoomResult;
    private Context mContext;
    private SQLiteDatabase mDatabase;
    public BridgeTeamInfo mTeamInfo;

    public static BridgeResultSheetTeam getRecent(Context context)
    {
        SharedPreferences preferences;
        SharedPreferences.Editor editor;
        BridgeResultSheetTeam resultSheet;
        preferences = context.getSharedPreferences("recentSheetTeam",Context.MODE_PRIVATE);
        String id = preferences.getString("recentSheetTeamId",null);
        if(id!=null)
        {
            return new BridgeResultSheetTeam(context,UUID.fromString(id));
        }
        resultSheet = new BridgeResultSheetTeam(context);
        editor=preferences.edit();
        editor.putString("recentSheetTeamId",resultSheet.getId().toString());
        editor.apply();
        return resultSheet;
    }
    public BridgeResultSheetTeam(Context context)
    {
        this(context,UUID.randomUUID());
    }
    public BridgeResultSheetTeam(Context context,UUID id)//只包含基本信息初始化，需手动设置Sheet
    {
        mId=id;
        mContext=context;
        mDatabase=new BridgeBaseHelper(mContext).getWritableDatabase();
        mTeamInfo=getTeamInfo();
        if(mTeamInfo==null)
        {
            mTeamInfo = new BridgeTeamInfo();
            mTeamInfo.randomGenerateId();
            addTeamInfo(mTeamInfo);
        }
        generateResultSheets();
    }
    public BridgeResultSheetTeam(Context context,UUID id,BridgeTeamInfo teamInfo)
    {
        mId=id;
        mContext=context;
        mDatabase=new BridgeBaseHelper(mContext).getWritableDatabase();
        mTeamInfo=getTeamInfo();
        if(mTeamInfo==null)
        {
            mTeamInfo = teamInfo;
            addTeamInfo(mTeamInfo);
        }
        else
        {
            mTeamInfo=teamInfo;
            updateTeamInfo(mTeamInfo);
        }
    }
    private BridgeResultSheetTeam(Context context,int totalHands)
    {
        this(context,UUID.randomUUID(),totalHands);
    }
    public BridgeResultSheetTeam(Context context,UUID id,int totalHands)//包含totalHands则包含创建两个表
    {
        mId=id;
        mContext=context;
        mDatabase=new BridgeBaseHelper(mContext).getWritableDatabase();
        if(totalHands>0) {
            mOpenRoomResult = new BridgeResultSheet(mContext, totalHands);
            mClosedRoomResult = new BridgeResultSheet(mContext, totalHands);
        }
        generateResultSheets();
        //判断id是否已经在roominfo表里出现，若有，则设置totalHands为无效的，否则设置totalhands并初始化
        /*mTableInfo = getTableInfo();
        if(mTableInfo==null)
        {
            mTableInfo = new BridgeTableInfo();
            mTableInfo.setTotalHands(totalHands);
            addTableInfo(mTableInfo);
        }
        init();*/
    }
    public UUID getId() {
        return mId;
    }
    public void setId(UUID id) {
        mId = id;
    }
    public Context getContext() {
        return mContext;
    }

    private ContentValues getContentValues(BridgeTeamInfo teamInfo)
    {
        ContentValues values = new ContentValues();
        values.put(BridgeDbSchema.TeamGameInfo.Cols.TEAM_GAME_UUID,mId.toString());
        values.put(BridgeDbSchema.TeamGameInfo.Cols.OPEN_ROOM_UUID,teamInfo.getOpenRoomId().toString());
        values.put(BridgeDbSchema.TeamGameInfo.Cols.ClOSED_ROOM_UUID,teamInfo.getClosedRoomId().toString());
        return values;
    }
    private BridgeCursorWrapper queryTeamGameInfo(String whereClause,String[] whereArgs)
    {
        Cursor cursor = mDatabase.query(
                BridgeDbSchema.TeamGameInfo.NAME,
                null,
                whereClause,
                whereArgs,
                null, null, null
        );
        return new BridgeCursorWrapper(cursor);
    }
    public void addTeamInfo(BridgeTeamInfo teamInfo)
    {
        ContentValues values = getContentValues(teamInfo);
        mDatabase.insert(BridgeDbSchema.TeamGameInfo.NAME, null , values);
    }
    public void updateTeamInfo(BridgeTeamInfo teamInfo)
    {
        ContentValues values = getContentValues(teamInfo);
        mDatabase.update(BridgeDbSchema.TeamGameInfo.NAME,values, BridgeDbSchema.TeamGameInfo.Cols.TEAM_GAME_UUID + " = ?" ,new String[]{mId.toString()});
    }
    public BridgeTeamInfo getTeamInfo() {
        BridgeCursorWrapper cursor = queryTeamInfo(
                BridgeDbSchema.TeamGameInfo.Cols.TEAM_GAME_UUID + " = ?" ,new String[]{mId.toString()});
        try {
            if (cursor.getCount() == 0) return null;
            cursor.moveToFirst();
            return cursor.getTeamInfo();
        }finally
        {
            cursor.close();
        }
    }
    private BridgeCursorWrapper queryTeamInfo(String whereClause,String[] whereArgs)
    {
        Cursor cursor = mDatabase.query(
                BridgeDbSchema.TeamGameInfo.NAME,
                null,
                whereClause,
                whereArgs,
                null, null, null
        );
        return new BridgeCursorWrapper(cursor);
    }
    public BridgeResultSheet getOpenRoomResult() {
        return mOpenRoomResult;
    }

    public void setOpenRoomResult(BridgeResultSheet openRoomResult) {
        mOpenRoomResult = openRoomResult;
        mTeamInfo.setOpenRoomId(mOpenRoomResult.getID());
        updateTeamInfo(mTeamInfo);
    }

    public BridgeResultSheet getClosedRoomResult() {
        return mClosedRoomResult;
    }

    public void setClosedRoomResult(BridgeResultSheet closedRoomResult) {
        mClosedRoomResult = closedRoomResult;
        mTeamInfo.setClosedRoomId(mClosedRoomResult.getID());
        updateTeamInfo(mTeamInfo);
    }
    public void generateResultSheets()
    {
        if(mOpenRoomResult==null)mOpenRoomResult=new BridgeResultSheet(mContext,mTeamInfo.getOpenRoomId());
        if(mClosedRoomResult==null)mClosedRoomResult=new BridgeResultSheet(mContext,mTeamInfo.getClosedRoomId());

        mOpenRoomResult.setParentSheet(this);
        mClosedRoomResult.setParentSheet(this);
        mOpenRoomResult.mTableInfo.setOpenRoom(true);
        mOpenRoomResult.updateTableInfo(mOpenRoomResult.mTableInfo);
        mClosedRoomResult.mTableInfo.setOpenRoom(false);
        mClosedRoomResult.updateTableInfo(mClosedRoomResult.mTableInfo);
        /*mTeamInfo.setOpenRoomId(mOpenRoomResult.getID());
        mTeamInfo.setClosedRoomId(mClosedRoomResult.getID());
        updateTeamInfo(mTeamInfo);*/
    }
    private void calculateIMP()
    {
        mHomeIMP=0;
        mAwayIMP=0;
        mIMPInfoList=new ArrayList<BridgeIMPInfo>();
        List<BridgeContract> contracts = mOpenRoomResult.getContracts();
        for (BridgeContract c:contracts) {
            int tempBoardNum=c.getBoardNum();
            BridgeIMPInfo info=new BridgeIMPInfo(tempBoardNum);
            BridgeContract closedC=mClosedRoomResult.getContract(tempBoardNum);
            if(closedC == null)continue;
            info.setOpenRoomContract(c.getResultShown());
            info.setClosedRoomContract(closedC.getResultShown());
            if(c.mFlag.equals("Done")&closedC.mFlag.equals("Done"))
            {
                int scoreDiffe=c.getScore()-closedC.getScore();
                info.setScoreDifference(scoreDiffe);
                int tempImp=scoreToIMP(scoreDiffe);
                info.setIMP(tempImp);
                if(tempImp>=0)
                    mHomeIMP += tempImp;
                else
                    mAwayIMP -= tempImp;
            }
            mIMPInfoList.add(info);
        }
    }
    private void calculateVp()
    {
        //double total=mOpenRoomResult.getTableInfo().getTotalHands();
        double total = mIMPInfoList.size();
        double imps=mHomeIMP-mAwayIMP;
        double temp=Math.abs(imps);
        double base=Math.sqrt(total)*15;
        double vp=10 * (1 - Math.pow(0.618033989,3 * temp / base)) / (1 - Math.pow(0.618033989,3));
        if(vp>10)vp=10;
        if(imps>0)
            mHomeVp=(float)(10+vp);
        else
            mHomeVp=(float)(10-vp);
        BigDecimal b=new BigDecimal(mHomeVp);
        mHomeVp=b.setScale(2,BigDecimal.ROUND_HALF_UP).floatValue();
        mAwayVp=20-mHomeVp;
    }
    public boolean calculateResult()
    {
        if(mOpenRoomResult==null|mClosedRoomResult==null)return false;
        //需保证两个表的副数相同
        //if(mOpenRoomResult.getTableInfo().getTotalHands()!=mClosedRoomResult.getTableInfo().getTotalHands())return false;
        calculateIMP();
        calculateVp();
        return true;
    }
    private int scoreToIMP(int score)
    {
        int temp=1;
        if(score<0)
        {
            temp=-1;
            score=-score;
        }
        for(int i=0;i<25;i++)
            if(score<IMPdata[i])return i*temp;
        return 25;
    }

    public List<BridgeIMPInfo> getIMPInfoList() {
        return mIMPInfoList;
    }
    public float getHomeVp() {
        return mHomeVp;
    }

    public float getAwayVp() {
        return mAwayVp;
    }

    public int getHomeIMP() {
        return mHomeIMP;
    }

    public int getAwayIMP() {
        return mAwayIMP;
    }

    public void autoAdjustTableInfo()
    {
        BridgeTableInfo open=mOpenRoomResult.getTableInfo();
        BridgeTableInfo closed=mClosedRoomResult.getTableInfo();
        closed.setDate(open.getDate());
        if(open.getTotalHands()>closed.getTotalHands())
        {
            mClosedRoomResult.resetTotalHands(open.getTotalHands());
            closed.setTotalHands(open.getTotalHands());
        }
        else
        {
            mOpenRoomResult.resetTotalHands(closed.getTotalHands());
            open.setTotalHands(closed.getTotalHands());
        }
        open.setOpenRoom(true);
        closed.setOpenRoom(false);
        mOpenRoomResult.updateTableInfo(open);
        mClosedRoomResult.updateTableInfo(closed);
    }
}

