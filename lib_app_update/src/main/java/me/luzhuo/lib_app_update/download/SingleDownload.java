package me.luzhuo.lib_app_update.download;

import android.text.TextUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import androidx.annotation.NonNull;
import me.luzhuo.lib_core.data.hashcode.HashManager;
import me.luzhuo.lib_file.FileManager;
import me.luzhuo.lib_okhttp.OKHttpManager;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 直接下载
 * 对于不支持分片下载的服务器来说, 或许是个无奈的选择
 */
public class SingleDownload extends IDownloadApp {
    private final IDownloadCallback downloadCallback;

    public SingleDownload(@NonNull IDownloadCallback downloadCallback) {
        this.downloadCallback = downloadCallback;
    }

    @Override
    public void download(String apkUrl, File apkFile) {
        if (isDowning) return;
        isDowning = true;

        if (TextUtils.isEmpty(apkUrl)) return;

        try {
            Request request = new Request.Builder().url(apkUrl).build();
            Response response = new OKHttpManager().getClient().newCall(request).execute();
            if (response.isSuccessful() || response.isRedirect()) {

                File tempFile = new File(new FileManager().getCacheDirectory() + File.separator + "downloadCache", HashManager.getInstance().getUuid());
                if (!apkFile.getParentFile().exists()) { apkFile.getParentFile().mkdirs(); }
                if (!tempFile.getParentFile().exists()) { tempFile.getParentFile().mkdirs(); }
                if (!tempFile.exists()) { tempFile.createNewFile(); }

                InputStream inputStream = response.body().byteStream();
                BufferedInputStream bis = new BufferedInputStream(inputStream);
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(tempFile));
                byte[] bys = new byte[10240];
                long progress = 0;

                // start download
                long total = response.body().contentLength();
                if (total <= 0) {
                    downloadCallback.err("文件异常: 文件大小为" + total);
                    return;
                }
                downloadCallback.start(total);
                downloadCallback.progress(0f, 0, total);

                int len;
                while((len = bis.read(bys)) != -1) {
                    bos.write(bys, 0, len);
                    bos.flush();
                    progress += len;
                    downloadCallback.progress(progress * 1f / total, progress, total);
                }

                bis.close();
                bos.close();
                tempFile.renameTo(apkFile);
                isDowning = false;
                downloadCallback.complete(apkFile);
            } else {
                isDowning = false;
                downloadCallback.err("服务器异常: " + response.code());
            }
        } catch (Exception e) {
            isDowning = false;
            downloadCallback.err("本地异常: " + e.getMessage());
        }
    }
}