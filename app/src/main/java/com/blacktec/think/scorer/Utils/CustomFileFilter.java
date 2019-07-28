package com.blacktec.think.scorer.Utils;

import android.database.Cursor;

import java.io.File;
import java.io.FileFilter;

/**
 * Created by Think on 2018/12/8.
 */

public class CustomFileFilter implements FileFilter {

    private String[] extensions;
    private boolean folderAcceptable;

    /**
     * 为文件的过滤提供过滤数组
     * @param extensions
     */
    public CustomFileFilter(String[] extensions) {
        this.extensions = extensions;
        folderAcceptable =false;
    }
    public CustomFileFilter(String[] extensions,boolean folderAcceptable)
    {
        this.extensions = extensions ;
        this.folderAcceptable = folderAcceptable;
    }
    @Override
    public boolean accept(File pathname) {
        if(pathname.isDirectory())
            return folderAcceptable;
        String fileName = pathname.getName().toLowerCase();
            for(int i=0;i<extensions.length;i++) {
                if(fileName.endsWith(extensions[i])) {
                    return true;
                }
            }
            return false;
        }

}
