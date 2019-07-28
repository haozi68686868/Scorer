package com.blacktec.think.scorer.Utils;


import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/*
* 该类是用来对文件进行基本的操作，包括复制，移动，创建，删除等操作。
*/

public class FileUtils {

    public final static int DEFAULT_FILE_OPERATE_MODE = 0;     //创建文件时File对象是绝对路径，如果存在文件则覆盖

    public final static int IGNORE_NOT_RECREATE_MODE = 1;    //创建文件时File对象是相对储存器的路径，如果存在文件则表示已经存在，不进行操作

    public final static int IGNORE_AND_RECREATE_MODE = 2;    //创建文件时File对象是相对储存器的路径，如果存在文件则表示已经存在，不进行操作

    public final static int NOT_IGNORE_RECREATE_MODE = 3;   //创建文件时File对象是绝对路径，如果存在文件则覆盖

    private final static boolean DEFAULT_IGNORE_STYLE = false;

    private final static boolean DEFAULT_AUTO_CREATE_DIRECTORY = true;   //如果目录不存在是否自动创建


    /**

     *以File对象为参数创建文件
     * @param file
     * @param mode
     * 如果mode等于IGNORE_NOT_RECREATE_MODE或 IGNORE_AND_RECREATE_MODE则表示输入的file是相对路径,如果是其他两种模式则表示输入的是绝对路径。
     * added sdcard dirctory.
     * @throws IOException
     */
    public static void createFile(File file, int mode) throws IOException
    {
        if (file == null || StringUtils.isEmpty(file.getAbsolutePath())) {
            return;
        }

        if (mode == IGNORE_NOT_RECREATE_MODE
                || mode == IGNORE_AND_RECREATE_MODE) {
            file = new File(Environment.getExternalStorageDirectory(),
                   file.getAbsolutePath());
        }
        if (mode == DEFAULT_FILE_OPERATE_MODE
                || mode == IGNORE_AND_RECREATE_MODE) {
            deleteFile(file);
        }
        file.createNewFile();
    }

    /**

     *以String对象为参数创建文件
     * @param filePath
     * @param mode
     * @throws IOException
     */
    public static void createFile(String filePath, int mode) throws IOException
    {

        createFile(new File(filePath), mode);
    }

    /**

     *以默认的方式创建文件
     * @param filePath
     * @throws IOException
     */
    public static void createFile(File filePath) throws IOException
    {

        createFile(filePath, DEFAULT_FILE_OPERATE_MODE);
    }

    /**
     * @param filePath
     * @throws IOException
     */
    public static void createFile(String filePath) throws IOException
    {

        createFile(new File(filePath));
    }

    /**
     * 创建文件夹
     *
     * @param folder
     * @param mode
     */
    public static void createFolder(File folder, int mode)
    {

        if (folder == null || StringUtils.isEmpty(folder.getAbsolutePath())) {
            return;
        }
        if (!folder.isDirectory()) {
            return;
        }
        if (mode == IGNORE_NOT_RECREATE_MODE
                || mode == IGNORE_AND_RECREATE_MODE) {
            folder = new File(Environment.getExternalStorageDirectory(),//被修改
                    folder.getAbsolutePath());
        }
        if (mode == DEFAULT_FILE_OPERATE_MODE
                || mode == IGNORE_AND_RECREATE_MODE) {
            deleteFolder(folder);
        }
        folder.mkdirs();

    }

    /**
     * @param folder
     * @param mode
     */
    public static void createFolder(String folder, int mode)
    {

        createFolder(new File(folder), mode);
    }

    /**
     * @param folder
     */
    public static void createFolder(File folder)
    {

        createFolder(folder, DEFAULT_FILE_OPERATE_MODE);
    }

    /**
     * @param folder
     */
    public static void createFolder(String folder)
    {

        createFolder(new File(folder));
    }


    /**
     * 删除文件
     *
     * @param file
     */
    public static void deleteFile(File file)
    {

        if (file == null || StringUtils.isEmpty(file.getAbsolutePath())) {
            return;
        }
        if (file.exists()) {
            if (!file.isDirectory()) {
                file.delete();
            }
        }
    }

    /**
     * @param filePath
     */
    public static void deleteFile(String filePath)
    {

        if (!StringUtils.isEmpty(filePath)) {
            deleteFile(new File(filePath));
        }
    }

    /**
     * 删除一个目录
     *
     * @param folder
     */
    public static void deleteFolder(File folder)
    {

        if (folder == null || StringUtils.isEmpty(folder.getAbsolutePath())) {
            return;
        }
        if (folder.exists()) {
            if (folder.isDirectory()) {
                File[] files = folder.listFiles();
                if(files!=null)
                {
                    for (File file : files) {
                        deleteFolder(file);
                    }
                }
            }
            else {
                deleteFile(folder);
            }
        }
    }

    /**
     * @param folderPath
     */
    public static void deleteFolder(String folderPath)
    {

        if (!StringUtils.isEmpty(folderPath)) {
            deleteFile(new File(folderPath));
        }
    }


    /**
     * 查找指定目录下以extensions结尾的文件
     *
     * @param
     * @return
     */

    public static List<File> getAllWithEnd(File file, boolean ignore,
                                           String... extensions)
    {

        if (StringUtils.isEmpty(file.getAbsolutePath())) {
            return null;
        }
        for (String extension : extensions) {
            if (StringUtils.isEmpty(extension)) {
                return null;
            }
        }
        if (ignore) {
            file = new File(Environment.getExternalStorageDirectory(),
                    file.getAbsolutePath());
        }
        if ((!file.exists()) && file.isDirectory()) {
            return null;
        }
        List<File> files = new ArrayList<File>();
        fileFilter(file, files, extensions);
        return files;

    }

    /**
     * @param path
     * @param extensions
     * @param ignore
     * @return
     */
    public static List<File> getAllWithEnd(String path, boolean ignore,
                                           String... extensions)
    {

        return getAllWithEnd(new File(path), ignore, extensions);
    }

    /**
     * @param file
     * @param extensions
     * @return
     */
    public static List<File> getAllWithEnd(File file, String... extensions)
    {

        return getAllWithEnd(file, DEFAULT_IGNORE_STYLE, extensions);
    }

    /**
     * @param file
     * @param extensions
     * @return
     */
    public static List<File> getAllWithEnd(String file, String... extensions)
    {

        return getAllWithEnd(new File(file), DEFAULT_IGNORE_STYLE, extensions);
    }

    /**
     * 过滤文件
     *
     * @param file
     * @param extensions
     * @param files
     */

    public static void fileFilter(File file, List<File> files,
                                  String... extensions)
    {

        if (!file.isDirectory()) {
            return;
        }
        File[] allFiles = file.listFiles();
        File[] allExtensionFiles = file.listFiles(new CustomFileFilter(extensions));
        if (allExtensionFiles != null) {
            for (File single : allExtensionFiles) {
                files.add(single);
            }
        }
        if (allFiles != null) {
            for (File single : allFiles) {
                if (single.isDirectory()) {
                    fileFilter(single, files, extensions);
                }
            }
        }
    }



    /**
     * 复制Assets目录下的文件到指定位置
     *
     * @param strAssetsFilePath
     * @param strDesFilePath
     * @return
     */
    /*
    public boolean assetsCopyData(String strAssetsFilePath,
                                  String strDesFilePath)
    {

        boolean bIsSuc = true;
        InputStream inputStream = null;
        OutputStream outputStream = null;

        File file = new File(strDesFilePath);
        if (!file.exists()) {
            try {
                file.createNewFile();
                Runtime.getRuntime().exec("chmod 766 " + file);
            }
            catch (IOException e) {
                bIsSuc = false;
            }

        }
        else {// 存在
            return true;
        }

        try {
            inputStream = ApplicationContext.mContext.getAssets().open(
                    strAssetsFilePath);
            outputStream = new FileOutputStream(file);

            int nLen = 0;

            byte[] buff = new byte[1024 * 1];
            while ((nLen = inputStream.read(buff)) > 0) {
                outputStream.write(buff, 0, nLen);
            }

        }
        catch (IOException e) {
            bIsSuc = false;
        }
        finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }

                if (inputStream != null) {
                    inputStream.close();
                }
            }
            catch (IOException e) {
                bIsSuc = false;
            }

        }

        return bIsSuc;
    }
*/

    /**
     * 复制文件
     *
     * @param src
     * @param dst
     * @return
     * @throws IOException
     */
    public static boolean copyFile(File src, File dst) throws IOException
    {

        if ((!src.exists()) || src.isDirectory() || dst.isDirectory()) {
            return false;
        }
        if (!dst.exists()) {
            if(!dst.createNewFile())return false;
        }
        FileInputStream inputStream = null;
        FileOutputStream outputStream = null;
        inputStream = new FileInputStream(src);
        outputStream = new FileOutputStream(dst);
        int readLen = 0;
        byte[] buf = new byte[1024];
        while ((readLen = inputStream.read(buf)) != -1) {
            outputStream.write(buf, 0, readLen);
        }
        outputStream.flush();
        inputStream.close();
        outputStream.close();
        return true;
    }

    /**
     * @param src
     * @param dst
     * @return
     * @throws IOException
     */
    public static boolean copyFile(String src, String dst) throws IOException
    {

        return copyFile(new File(src), new File(dst));
    }


    /**
     * 复制整个目录
     * @param srcDir
     * @param destDir
     * @param auto
     * @return
     * @throws IOException
     */
    public static boolean copyFolder(File srcDir, File destDir, boolean auto)
            throws IOException
    {
        if ((!srcDir.exists())) {
            return false;
        }
        if (srcDir.isFile() || destDir.isFile())
            return false;
        if (!destDir.exists()) {
            if (auto) {
                destDir.mkdirs();
            }
            else {
                return false;
            }
        }
        File[] srcFiles = srcDir.listFiles();
        int len = srcFiles.length;
        for (int i = 0; i < len; i++) {
            if (srcFiles[i].isFile()) {
                File destFile = new File(destDir.getPath() + "//"
                        + srcFiles[i].getName());
                copyFile(srcFiles[i], destFile);
            }
            else if (srcFiles[i].isDirectory()) {
                File theDestDir = new File(destDir.getPath() + "//"
                        + srcFiles[i].getName());
                copyFolder(srcFiles[i], theDestDir,auto);
            }
        }
        return true;
    }

    /**
     * @param srcDir
     * @param desDir
     * @param auto
     * @return
     * @throws IOException
     */
    public static boolean copyFolder(String srcDir, String desDir, boolean auto)
            throws IOException
    {

        return copyFolder(new File(srcDir), new File(desDir), auto);
    }

    /**
     * @param srcDir
     * @param desDir
     * @return
     * @throws IOException
     */
    public static boolean copyFolder(File srcDir, File desDir)
            throws IOException
    {

        return copyFolder(srcDir, desDir, DEFAULT_AUTO_CREATE_DIRECTORY);
    }

    /**
     * @param srcDir
     * @param desDir
     * @return
     * @throws IOException
     */
    public static boolean copyFolder(String srcDir, String desDir)
            throws IOException
    {

        return copyFolder(srcDir, desDir, DEFAULT_AUTO_CREATE_DIRECTORY);
    }


    /**
     * 移动单个文件
     * @param src
     * @param dst
     * @return
     */
    public static boolean moveFile(File src, File dst)
    {
        boolean isCopy = false;
        try {
            isCopy = copyFile(src, dst);
        }
        catch (IOException e) {
// TODO Auto-generated catch block
            e.printStackTrace();
        }
        if(!isCopy)
        {
            return false;
        }
        deleteFile(src);
        return true;
    }

    /**
     * @param src
     * @param dst
     * @return
     */
    public static boolean moveFile(String src, String dst)
    {

        return moveFile(new File(src), new File(dst));
    }

    /**
     * 移动整个目录
     * @param srcDir
     * @param destDir
     * @param auto
     * @return
     */
    public static boolean moveFolder(File srcDir,File destDir,boolean auto)
    {
        if (!srcDir.exists()|| !srcDir.isDirectory() ) {
            return false;
        }
        if(!destDir.exists())
        {
            if(auto)
            {
                destDir.mkdirs();
            }
            else{
                return false;
            }
        }
        else if(!destDir.isDirectory())
        {
            return false;
        }
        File[] srcDirFiles = srcDir.listFiles();
        int len = srcDirFiles.length;
        if(len<=0)
        {
            srcDir.delete();
        }
        for (int i = 0; i < len; i++) {
            if (srcDirFiles[i].isFile()) {
                File oneDestFile = new File(destDir.getPath() + "//"
                        + srcDirFiles[i].getName());
                if(!moveFile(srcDirFiles[i], oneDestFile))return false;
            } else if (srcDirFiles[i].isDirectory()) {
                File oneDestFile = new File(destDir.getPath() + "//"
                        + srcDirFiles[i].getName());
                if(!moveFolder(srcDirFiles[i], oneDestFile,auto))return false;
                deleteFolder(srcDirFiles[i]);
            }

        }
        return true;
    }
    /**
     * @param src
     * @param dst
     * @param auto
     * @return
     */
    public static boolean moveFolder(String src,String dst,boolean auto)
    {
        return moveFolder(new File(src), new File(dst));
    }
    /**
     * @param src
     * @param dst
     * @return
     */
    public static boolean moveFolder(File src,File dst)
    {
        return moveFolder(src, dst, DEFAULT_AUTO_CREATE_DIRECTORY);
    }
    /**
     * @param src
     * @param dst
     * @return
     */
    public static boolean moveFolder(String src,String dst)
    {
        return moveFolder(new File(src), new File(dst), DEFAULT_AUTO_CREATE_DIRECTORY);
    }

}