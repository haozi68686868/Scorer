package com.blacktec.think.scorer;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;

import java.util.UUID;

/**
 * Created by Think on 2017/12/8.
 */

public class BridgeTableInfoActivity extends SingleFragmentActivity {
    private static final String EXTRA_TABLE_UUID = "com.blacktec.think.scorer.table_uuid";


    public static Intent newIntent(Context packageContext, UUID tableId)
    {
        Intent intent = new Intent(packageContext,BridgeTableInfoActivity.class);
        intent.putExtra(EXTRA_TABLE_UUID,tableId);
        return intent;
    }
    @Override
    protected Fragment createFragment()
    {
        UUID tableId = (UUID)getIntent().getSerializableExtra(EXTRA_TABLE_UUID);
        return TableInfoFragment.newInstance(tableId);
    }
}
