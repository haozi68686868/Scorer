package com.blacktec.think.scorer;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Think on 2018/11/8.
 */

public class DataBaseTeamInfoBean {
    private List<BridgeTeamInfoBean> teamInfos;
    public DataBaseTeamInfoBean(){}
    public DataBaseTeamInfoBean(List<BridgeResultSheetTeam> list)
    {
        teamInfos= new ArrayList<>();
        for(BridgeResultSheetTeam sheet: list)
        {
            BridgeTeamInfoBean infoBean= new BridgeTeamInfoBean();
            infoBean.setGameId(sheet.getId().toString());
            infoBean.setInfo(sheet.getTeamInfo());
            teamInfos.add (infoBean);
        }
    }
    void ExportToDatabase(Context context)
    {
        for(BridgeTeamInfoBean teamInfoBean:teamInfos)
        {
            BridgeResultSheetTeam resultSheet=new BridgeResultSheetTeam(context, UUID.fromString(teamInfoBean.getGameId()),teamInfoBean.getInfo());
            //judge existing first (if true update ,if false add)
            //without initialization
        }
    }



    public class BridgeTeamInfoBean {
        //注意变量名与字段名一致
        private String gameId;
        private BridgeTeamInfo info;
        public String getGameId() {
            return gameId;
        }

        public void setGameId(String gameId) {
            this.gameId = gameId;
        }

        public BridgeTeamInfo getInfo() {
            return info;
        }

        public void setInfo(BridgeTeamInfo info) {
            this.info = info;
        }

    }
}
