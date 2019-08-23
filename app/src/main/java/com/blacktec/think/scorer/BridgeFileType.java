package com.blacktec.think.scorer;

/**
 * Created by Think on 2018/11/5.
 */

public enum BridgeFileType {
    SingleSheet,MultiTableSheet,TeamSheet,RoundSheet,EventGameSheet;
    public static final String[] bridgeFileTypeStrings= {
            "单桌计分表",
            "多桌计分表",
            "队式计分表",
            "轮次计分表",
            "赛事计分表"};
    public static String getDescription(BridgeFileType t)
    {
        return bridgeFileTypeStrings[t.ordinal()];
    }
    public static String getDescription(int t)
    {
        return bridgeFileTypeStrings[t];
    }
}
