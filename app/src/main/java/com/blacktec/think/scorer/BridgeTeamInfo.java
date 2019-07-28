package com.blacktec.think.scorer;

import java.util.UUID;

/**
 * Created by Think on 2018/3/31.
 */

public class BridgeTeamInfo {
    private UUID mOpenRoomId;
    private UUID mClosedRoomId;
    BridgeTeamInfo()
    {
    }
    public void randomGenerateId()
    {
        mOpenRoomId=UUID.randomUUID();
        mClosedRoomId=UUID.randomUUID();
    }
    public UUID getClosedRoomId() {
        return mClosedRoomId;
    }

    public void setClosedRoomId(UUID closedRoomId) {
        mClosedRoomId = closedRoomId;
    }

    public UUID getOpenRoomId() {
        return mOpenRoomId;
    }

    public void setOpenRoomId(UUID openRoomId) {
        mOpenRoomId = openRoomId;
    }
}
