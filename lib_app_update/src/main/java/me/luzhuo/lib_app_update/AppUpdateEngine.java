package me.luzhuo.lib_app_update;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.Nullable;
import me.luzhuo.lib_core.app.appinfo.AppManager;
import me.luzhuo.lib_core.app.base.CoreBaseApplication;
import me.luzhuo.lib_core.data.hashcode.HashManager;
import me.luzhuo.lib_file.FileManager;
import me.luzhuo.lib_okhttp.OKHttpManager;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 具体的App更新检查
 */
class AppUpdateEngine {
    private final ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainThread = new Handler(Looper.getMainLooper());
    // 如果为null, 则在当前浏览的Activity弹出
    private Activity currentActivity = null;
    private final UpdateAppCheck updateAppCheck;
    private final UpdateAppDialog updateAppDialog;

    public AppUpdateEngine(UpdateAppCheck updateAppCheck, UpdateAppDialog updateAppDialog) {
        this.updateAppCheck = updateAppCheck;
        this.updateAppDialog = updateAppDialog;
    }

    public synchronized void checkAppUpdate() {
        this.checkAppUpdate(null);
    }

    /**
     * 检查版本更新
     * @param activity 在指定的Activity弹出更新窗口; 如果为null, 则在当前浏览的Activity弹出
     */
    public synchronized void checkAppUpdate(@Nullable Activity activity) {
        this.currentActivity = activity;
        singleThreadExecutor.execute(checkVersionAction);
    }

    private final Runnable checkVersionAction = new Runnable() {
        @Override
        public void run() {
            if (updateAppCheck == null) return;
            // 检查版本, 是否需要更新
            updateAppCheck.startCheck(CoreBaseApplication.appContext);
            int versionCode = updateAppCheck.getVersionCode();
            int currentVersionCoe = new AppManager().getAppInfo().versionCode;
            if (versionCode <= currentVersionCoe) return;

            // 更新应用
            updateAppDialog();
        }
    };

    private void updateAppDialog() {
        // 弹窗
        String appUrl = updateAppCheck.getAppUrl();
        File apkFile = new File(new FileManager().getCacheDirectory() + File.separator + "apkCache", HashManager.getInstance().getMD5(appUrl) + ".apk");
        Activity activity = currentActivity == null ? me.luzhuo.lib_core.app.base.AppManager.currentActivity() : currentActivity;
        if (activity == null || activity.isFinishing()) return; // 当前没有存活的Activity
        mainThread.post(new ShowDialogAction(activity, updateAppCheck.isForce(), apkFile));
    }

    /**
     * 开始下载apk
     */
    public void startDownload(File apkFile) {
        // 下载apk
        if (!apkFile.exists()) singleThreadExecutor.execute(new DownloadApk(apkFile));
    }

    private void downloadApk(String apkUrl, File apkFile) {
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
                    mainThread.post(new ProgressError("文件异常: 文件大小为" + total));
                    return;
                }
                mainThread.post(new ProgressAction(0f, 0, total));

                int len;
                while((len = bis.read(bys)) != -1) {
                    bos.write(bys, 0, len);
                    bos.flush();
                    progress += len;
                    mainThread.post(new ProgressAction(progress * 1f / total, progress, total));
                }

                bis.close();
                bos.close();
                tempFile.renameTo(apkFile);
                mainThread.post(new ProgressComplete(apkFile));
            } else {
                mainThread.post(new ProgressError("服务器异常: " + response.code()));
            }
        } catch (Exception e) {
            mainThread.post(new ProgressError("本地异常: " + e.getMessage()));
        }
    }

    private final class DownloadApk implements Runnable {
        private final File apkFile;

        public DownloadApk(File apkFile) {
            this.apkFile = apkFile;
        }

        @Override
        public void run() {
            downloadApk(updateAppCheck.getAppUrl(), apkFile);
        }
    }

    private final class ShowDialogAction implements Runnable {
        private final Activity activity;
        private final boolean isForce;
        private final File apkFile;

        public ShowDialogAction(Activity activity, boolean isForce, File apkFile) {
            this.activity = activity;
            this.isForce = isForce;
            this.apkFile = apkFile;
        }

        @Override
        public void run() {
            updateAppDialog.showDialog(activity, isForce, apkFile, updateAppCheck.getBundleData());
        }
    }

    private final class ProgressError implements Runnable {
        private final String errorMsg;

        public ProgressError(String errorMsg) {
            this.errorMsg = errorMsg;
        }

        @Override
        public void run() {
            updateAppDialog.progressError(errorMsg);
        }
    }

    private final class ProgressAction implements Runnable {
        private final float percent;
        private final long progress;
        private final long total;

        public ProgressAction(float percent, long progress, long total) {
            this.percent = percent;
            this.progress = progress;
            this.total = total;
        }

        @Override
        public void run() {
            updateAppDialog.progress(percent, progress, total);
        }
    }

    private final class ProgressComplete implements Runnable {
        private final File apkFile;

        public ProgressComplete(File apkFile) {
            this.apkFile = apkFile;
        }

        @Override
        public void run() {
            updateAppDialog.progressComplete(apkFile);
        }
    }
}
