package com.blacktec.think.scorer;

import com.blacktec.think.scorer.Utils.APKVersionCodeUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Think on 2018/11/5.
 */

public class BridgeJsonFile {
    private int fileVersion;
    private BridgeFileType fileType;
    private DataBaseResultSheetBean mDataBaseResultSheetBean;
    private DataBaseTeamInfoBean mDataBaseTeamInfoBean;

    public BridgeJsonFile(int version)
    {
        fileVersion = version;
    }
    public static BridgeJsonFile createdByResultSheet(final BridgeResultSheet resultSheet)
    {
        if(resultSheet==null)return null;
        BridgeJsonFile bridgeJsonFile = new BridgeJsonFile(APKVersionCodeUtils.getVersionCode(resultSheet.getContext()));
        bridgeJsonFile.setFileType(BridgeFileType.SingleSheet);
        bridgeJsonFile.setDataBaseResultSheetBean(
                new DataBaseResultSheetBean(new ArrayList<BridgeResultSheet>(){{add(resultSheet);}}));
        bridgeJsonFile.setDataBaseTeamInfoBean(null);
        return bridgeJsonFile;
    }
    public static BridgeJsonFile createdByResultSheets(List<BridgeResultSheet> resultSheets)
    {
        if(resultSheets.isEmpty())return null;
        BridgeJsonFile bridgeJsonFile = new BridgeJsonFile(APKVersionCodeUtils.getVersionCode(resultSheets.get(0).getContext()));
        bridgeJsonFile.setFileType(BridgeFileType.MultiTableSheet);
        bridgeJsonFile.setDataBaseResultSheetBean(new DataBaseResultSheetBean(resultSheets));
        bridgeJsonFile.setDataBaseTeamInfoBean(null);
        return bridgeJsonFile;
    }
    public static BridgeJsonFile createdByTeamResultSheet(final BridgeResultSheetTeam teamSheet)
    {
        if(teamSheet==null)return null;
        BridgeJsonFile bridgeJsonFile = new BridgeJsonFile(APKVersionCodeUtils.getVersionCode(teamSheet.getContext()));
        bridgeJsonFile.setFileType(BridgeFileType.TeamSheet);
        bridgeJsonFile.setDataBaseResultSheetBean(new DataBaseResultSheetBean(
                new ArrayList<BridgeResultSheet>() {{add(teamSheet.getOpenRoomResult());add(teamSheet.getClosedRoomResult());}}));
        bridgeJsonFile.setDataBaseTeamInfoBean(new DataBaseTeamInfoBean(new ArrayList<BridgeResultSheetTeam>(){{add(teamSheet);}}));
        return bridgeJsonFile;
    }
    public int getFileVersion() {
        return fileVersion;
    }

    public void setFileVersion(int fileVersion) {
        this.fileVersion = fileVersion;
    }

    public BridgeFileType getFileType() {
        return fileType;
    }

    public void setFileType(BridgeFileType fileType) {
        this.fileType = fileType;
    }

    public DataBaseResultSheetBean getDataBaseResultSheetBean() {
        return mDataBaseResultSheetBean;
    }

    public void setDataBaseResultSheetBean(DataBaseResultSheetBean dataBaseResultSheetBean) {
        mDataBaseResultSheetBean = dataBaseResultSheetBean;
    }

    public DataBaseTeamInfoBean getDataBaseTeamInfoBean() {
        return mDataBaseTeamInfoBean;
    }

    public void setDataBaseTeamInfoBean(DataBaseTeamInfoBean dataBaseTeamInfoBean) {
        mDataBaseTeamInfoBean = dataBaseTeamInfoBean;
    }

    public Boolean verify()
    {
        if(fileType==null)return false;
        switch (fileType)
        {
            case SingleSheet:
                if(mDataBaseResultSheetBean==null)return false;
                if(mDataBaseResultSheetBean.getTables().isEmpty())return false;
                break;
            case TeamSheet:
                if(mDataBaseResultSheetBean==null)return false;
                if(mDataBaseResultSheetBean.getTables().size()!=2)return false;
                if(!mDataBaseResultSheetBean.getTables().get(0).getTableInfo().getOpenRoom()
                        ||mDataBaseResultSheetBean.getTables().get(1).getTableInfo().getOpenRoom())
                    return false;
                break;
            case MultiTableSheet:
                break;
            case RoundSheet:
                break;
            case EventGameSheet:
                break;
        }
        return true;
    }
}




