package com.blacktec.think.scorer;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;

import com.blacktec.think.scorer.Utils.BackHandlerHelper;

/**
 * Created by Think on 2018/2/3.
 */

public class FileExplorerActivity extends SingleFragmentActivity {
    @Override
    protected Fragment createFragment()
    {
        Bundle bundle = getIntent().getExtras();
        FileExplorerTask task= (FileExplorerTask) bundle.getSerializable(FileExplorerFragment.ARG_EXPLORE_TASK);
        String fileFilter = bundle.getString(FileExplorerFragment.ARG_PARAM);
        if(task == null)
            return new FileExplorerFragment();
        else if(fileFilter==null)
            return FileExplorerFragment.newInstance(task);
        else
            return FileExplorerFragment.newInstance(task,fileFilter);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,@NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    @Override
    public void onBackPressed() {
        if (!BackHandlerHelper.handleBackPress(this)) {
            super.onBackPressed();
        }
    }

}
