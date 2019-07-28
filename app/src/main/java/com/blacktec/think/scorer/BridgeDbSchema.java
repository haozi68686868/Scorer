package com.blacktec.think.scorer;

/**
 * Created by Think on 2017/11/26.
 */

public class BridgeDbSchema {
    public static final class ResultSheet
    {
        public static final String NAME ="result_sheet";

        public static final class Cols
        {
            public static final String TABLE_UUID="tableId";
            public static final String BOARD_NUM = "boardNum";
            public static final String DECLARER = "declarer";
            public static final String CONTRACT = "contract";
            public static final String RESULT = "result";
        }
    }
    public static final class TableInfo
    {
        public static final String NAME = "table_info";

        public static final class Cols
        {
            public static final String TABLE_UUID = "tableId";
            public static final String TABLE_NUM = "tableNum";
            public static final String IS_OPEN_ROOM = "isOpenRoom";
            public static final String TOTAL_HANDS = "totalHands";
            public static final String N_PLAYER = "northPlayer";
            public static final String S_PLAYER = "southPlayer";
            public static final String E_PLAYER = "eastPlayer";
            public static final String W_PLAYER = "westPlayer";
            public static final String NS_TEAM = "nsTeam";
            public static final String EW_TEAM = "ewTeam";
            public static final String DATE = "date";
        }
    }
    public static final class TeamGameInfo
    {
        public static final String NAME = "TeamGame_info";

        public static final class Cols
        {
            public static final String TEAM_GAME_UUID = "teamGameId";
            public static final String OPEN_ROOM_UUID = "openRoomId";
            public static final String ClOSED_ROOM_UUID = "closedRoomId";
        }
    }
    public static final class Settings
    {
        public static final String NAME = "settings";
        public static final class Cols
        {

        }
    }

}
