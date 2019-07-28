package com.blacktec.think.scorer;

/**
 * Created by Think on 2018/4/23.
 */

public class BridgeIMPInfo {
    private int mBoardNum;
    private String mOpenRoomContract;
    private String mClosedRoomContract;
    private int mScoreDifference;
    private int mIMP;

    BridgeIMPInfo()
    {
        init();
    }
    BridgeIMPInfo(int boardNum)
    {
        init();
        mBoardNum=boardNum;
    }
    private void init()
    {
        mBoardNum = -1;//未设置时为-1
        mOpenRoomContract = "";
        mClosedRoomContract= "";
        mScoreDifference=0;
        mIMP=0;
    }
    public boolean isFinished()
    {
        return mOpenRoomContract.equals("")&mClosedRoomContract.equals("");
    }
    public String getClosedRoomContract() {
        return mClosedRoomContract;
    }

    public void setClosedRoomContract(String closedRoomContract) {
        mClosedRoomContract = closedRoomContract;
    }

    public String getOpenRoomContract() {
        return mOpenRoomContract;
    }

    public void setOpenRoomContract(String openRoomContract) {
        mOpenRoomContract = openRoomContract;
    }

    public int getBoardNum() {
        return mBoardNum;
    }

    public void setBoardNum(int boardNum) {
        mBoardNum = boardNum;
    }

    public int getScoreDifference() {
        return mScoreDifference;
    }

    public void setScoreDifference(int scoreDifference) {
        mScoreDifference = scoreDifference;
    }

    public int getIMP() {
        return mIMP;
    }

    public void setIMP(int IMP) {
        mIMP = IMP;
    }

}
