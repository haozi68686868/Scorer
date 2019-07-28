package com.blacktec.think.scorer;

import java.util.Date;
import java.util.UUID;

/**
 * Created by Think on 2017/12/28.
 */

public class BridgeTableInfo {

    private int mTableNum;
    private String mNorthPlayer;
    private String mSouthPlayer;
    private String mEastPlayer;
    private String mWestPlayer;

    private String mNSTeamName;
    private String mEWTeamName;
    private Boolean IsOpenRoom;

    private Date mDate;
    private int mTotalHands;//总副数

    public BridgeTableInfo()
    {
        init();
    }

    private void init()
    {
        mTableNum = -1;//未设置时为-1
        mTotalHands = -1;
        IsOpenRoom = true;
        mNorthPlayer="";
        mSouthPlayer="";
        mEastPlayer="";
        mWestPlayer="";
        mNSTeamName="";
        mEWTeamName="";
        mTotalHands=16;
        mDate=new Date();
    }

    public String getNorthPlayer() {
        return mNorthPlayer;
    }

    public void setNorthPlayer(String northPlayer) {
        mNorthPlayer = northPlayer;
    }

    public String getSouthPlayer() {
        return mSouthPlayer;
    }

    public void setSouthPlayer(String southPlayer) {
        mSouthPlayer = southPlayer;
    }

    public String getEastPlayer() {
        return mEastPlayer;
    }

    public void setEastPlayer(String eastPlayer) {
        mEastPlayer = eastPlayer;
    }

    public String getWestPlayer() {
        return mWestPlayer;
    }

    public void setWestPlayer(String westPlayer) {
        mWestPlayer = westPlayer;
    }

    public String getNSTeamName() {
        return mNSTeamName;
    }

    public void setNSTeamName(String NSTeamName) {
        mNSTeamName = NSTeamName;
    }

    public String getEWTeamName() {
        return mEWTeamName;
    }

    public void setEWTeamName(String EWTeamName) {
        mEWTeamName = EWTeamName;
    }

    public Boolean getOpenRoom() {
        return IsOpenRoom;
    }

    public void setOpenRoom(Boolean openRoom) {
        IsOpenRoom = openRoom;
    }

    public int getTableNum() {
        return mTableNum;
    }

    public void setTableNum(int tableNum) {
        mTableNum = tableNum;
    }

    public Date getDate() {
        return mDate;
    }

    public void setDate(Date date) {
        mDate = date;
    }

    public int getTotalHands() {
        return mTotalHands;
    }

    public void setTotalHands(int totalHands) {
        mTotalHands = totalHands;
    }
}
