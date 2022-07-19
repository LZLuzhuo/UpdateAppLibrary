package me.luzhuo.lib_app_update.download;

import java.io.File;

import androidx.annotation.WorkerThread;

public abstract class IDownloadApp {
    /**
     * 直接下载文件
     */
    public static final int Single = 1 << 1;
    /**
     * 多线程分段下载文件
     */
    public static final int Multi = 1 << 2;

    public boolean isDowning = false; // 是否在下载中

    /**
     * 下载文件
     * @param apkUrl apk文件下载链接
     * @param apkFile apk文件保存的路径
     */
    @WorkerThread
    public abstract void download(String apkUrl, File apkFile);
}
