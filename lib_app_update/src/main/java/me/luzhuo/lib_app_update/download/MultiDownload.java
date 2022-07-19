package me.luzhuo.lib_app_update.download;

import java.io.File;

import androidx.annotation.NonNull;

/**
 * 多线程分段下载
 */
public class MultiDownload extends IDownloadApp {
    private final IDownloadCallback downloadCallback;

    public MultiDownload(@NonNull IDownloadCallback downloadCallback) {
        this.downloadCallback = downloadCallback;
    }

    @Override
    public void download(String url, File apkFile) {

    }
}
