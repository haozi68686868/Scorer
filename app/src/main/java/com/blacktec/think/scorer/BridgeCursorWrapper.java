package com.blacktec.think.scorer;

import android.app.ActionBar;
import android.database.Cursor;
import android.database.CursorWrapper;

import java.util.Date;
import java.util.UUID;

import static com.blacktec.think.scorer.BridgeDbSchema.*;

/**
 * Created by Think on 2017/11/27.
 */

public class BridgeCursorWrapper extends CursorWrapper {
    public BridgeCursorWrapper(Cursor cursor)
    {
        super(cursor);
    }

    public BridgeContract getContract()
    {
        int boardNum = getInt(getColumnIndex(ResultSheet.Cols.BOARD_NUM));
        String declarer = getString(getColumnIndex(ResultSheet.Cols.DECLARER));
        String contract = getString(getColumnIndex(ResultSheet.Cols.CONTRACT));
        int result = getInt(getColumnIndex(ResultSheet.Cols.RESULT));

        BridgeContract con = new BridgeContract(boardNum);
        if(contract.equals("All Pass"))con.setContract(contract);
        else
            con.setContract(declarer+contract+(result==0?"=":result<0?String.valueOf(result):"+"+String.valueOf(result)));

        return con;
    }
    public BridgeTableInfo getTableInfo()
    {
        int tableNum=getInt(getColumnIndex(TableInfo.Cols.TABLE_NUM));
        int isOpenRoom= getInt(getColumnIndex(TableInfo.Cols.IS_OPEN_ROOM));
        int totalHands = getInt(getColumnIndex(TableInfo.Cols.TOTAL_HANDS));
        String northPlayer = getString(getColumnIndex(TableInfo.Cols.N_PLAYER));
        String southPlayer = getString(getColumnIndex(TableInfo.Cols.S_PLAYER));
        String eastPlayer = getString(getColumnIndex(TableInfo.Cols.E_PLAYER));
        String westPlayer = getString(getColumnIndex(TableInfo.Cols.W_PLAYER));
        String NSTeamName = getString(getColumnIndex(TableInfo.Cols.NS_TEAM));
        String EWTeamName = getString(getColumnIndex(TableInfo.Cols.EW_TEAM));
        long date = getLong(getColumnIndex(TableInfo.Cols.DATE));

        BridgeTableInfo tableInfo=new BridgeTableInfo();
        tableInfo.setTableNum(tableNum);
        tableInfo.setOpenRoom(isOpenRoom!=0);
        tableInfo.setTotalHands(totalHands);
        tableInfo.setNorthPlayer(northPlayer);
        tableInfo.setSouthPlayer(southPlayer);
        tableInfo.setEastPlayer(eastPlayer);
        tableInfo.setWestPlayer(westPlayer);
        tableInfo.setNSTeamName(NSTeamName);
        tableInfo.setEWTeamName(EWTeamName);
        tableInfo.setDate(new Date(date));

        return tableInfo;
    }
    public BridgeTeamInfo getTeamInfo()
    {
        String openRoomId = getString(getColumnIndex(TeamGameInfo.Cols.OPEN_ROOM_UUID));
        String closedRoomId = getString(getColumnIndex(TeamGameInfo.Cols.ClOSED_ROOM_UUID));
        BridgeTeamInfo tableInfo=new BridgeTeamInfo();
        tableInfo.setOpenRoomId(UUID.fromString(openRoomId));
        tableInfo.setClosedRoomId(UUID.fromString(closedRoomId));
        return tableInfo;
    }
}
