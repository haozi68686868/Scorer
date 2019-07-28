package com.blacktec.think.scorer;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

/**
 * Created by Think on 2017/10/12.
 */

public class BridgeResultSheetActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment()
    {
        return new ResultListFragment();
    }
}
