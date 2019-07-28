package com.blacktec.think.scorer;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import static com.blacktec.think.scorer.BridgeDbSchema.*;

/**
 * Created by Think on 2017/11/26.
 */

public class BridgeBaseHelper extends SQLiteOpenHelper {
    private static final int VERSION = 1;
    private static final String DATABASE_NAME = "crimeBase.db";

    public BridgeBaseHelper(Context context)
    {
        super(context,DATABASE_NAME,null,VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        db.execSQL("create table " + ResultSheet.NAME + "(" +
            " _id integer primary key autoincrement, "+
            ResultSheet.Cols.TABLE_UUID +", "+
            ResultSheet.Cols.BOARD_NUM +", "+
            ResultSheet.Cols.DECLARER +", "+
            ResultSheet.Cols.CONTRACT +", "+
            ResultSheet.Cols.RESULT +")"
        );

        db.execSQL("create table " + TableInfo.NAME + "(" +
                " _id integer primary key autoincrement, "+
                TableInfo.Cols.TABLE_UUID +", "+
                TableInfo.Cols.TABLE_NUM +", "+
                TableInfo.Cols.IS_OPEN_ROOM +", "+
                TableInfo.Cols.TOTAL_HANDS +", "+
                TableInfo.Cols.N_PLAYER +", "+
                TableInfo.Cols.S_PLAYER +", "+
                TableInfo.Cols.E_PLAYER +", "+
                TableInfo.Cols.W_PLAYER +", "+
                TableInfo.Cols.NS_TEAM +", "+
                TableInfo.Cols.EW_TEAM +", "+
                TableInfo.Cols.DATE +")"
        );
        db.execSQL("create table " + TeamGameInfo.NAME + "(" +
                " _id integer primary key autoincrement, "+
                TeamGameInfo.Cols.TEAM_GAME_UUID +", "+
                TeamGameInfo.Cols.OPEN_ROOM_UUID +", "+
                TeamGameInfo.Cols.ClOSED_ROOM_UUID  +")"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {

    }
}
