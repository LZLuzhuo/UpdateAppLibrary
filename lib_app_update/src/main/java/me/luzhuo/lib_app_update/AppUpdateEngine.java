/* Copyright 2022 Luzhuo. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.luzhuo.lib_app_update;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import me.luzhuo.lib_app_update.download.DownloadType;
import me.luzhuo.lib_app_update.download.IDownloadApp;
import me.luzhuo.lib_app_update.download.IDownloadCallback;
import me.luzhuo.lib_app_update.download.MultiDownload;
import me.luzhuo.lib_app_update.download.SingleDownload;
import me.luzhuo.lib_core.app.appinfo.AppManager;
import me.luzhuo.lib_core.app.base.CoreBaseApplication;
import me.luzhuo.lib_core.data.hashcode.HashManager;
import me.luzhuo.lib_file.FileManager;

/**
 * 具体的App更新检查
 */
class AppUpdateEngine implements IDownloadCallback {
    private final ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainThread = new Handler(Looper.getMainLooper());
    // 如果为null, 则在当前浏览的Activity弹出
    private Activity currentActivity = null;
    private final UpdateAppCheck updateAppCheck;
    private final UpdateAppDialog updateAppDialog;
    private final IDownloadApp downloadApp;

    public AppUpdateEngine(@NonNull UpdateAppCheck updateAppCheck, @NonNull UpdateAppDialog updateAppDialog, @DownloadType int downloadType) {
        this.updateAppCheck = updateAppCheck;
        this.updateAppDialog = updateAppDialog;
        if (downloadType == IDownloadApp.Single) downloadApp = new SingleDownload(this);
        else downloadApp = new MultiDownload(this);
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
            int currentVersionCode = new AppManager().getAppInfo().versionCode;
            String appUrl = updateAppCheck.getAppUrl();
            if (versionCode <= currentVersionCode || TextUtils.isEmpty(appUrl) || !appUrl.startsWith("http")) return;

            // 更新应用
            updateAppDialog();
        }
    };

    private void updateAppDialog() {
        // 弹窗
        String appUrl = updateAppCheck.getAppUrl();
        File apkFile = new File(new FileManager(CoreBaseApplication.appContext).getCacheDirectory() + File.separator + "apkCache", HashManager.getInstance().getMD5(appUrl) + ".apk");
        Activity activity = currentActivity == null ? me.luzhuo.lib_core.app.base.AppManager.currentActivity() : currentActivity;
        if (activity == null || activity.isFinishing()) return; // 当前没有存活的Activity
        mainThread.post(new ShowDialogAction(activity, updateAppCheck.isForce(), apkFile));
    }

    /**
     * 开始下载apk
     */
    public void startDownload(@NonNull File apkFile) {
        // 下载apk
        if (!apkFile.exists()) singleThreadExecutor.execute(new DownloadApk(apkFile));
    }

    private final class DownloadApk implements Runnable {
        private final File apkFile;

        public DownloadApk(File apkFile) {
            this.apkFile = apkFile;
        }

        @Override
        public void run() {
            downloadApp.download(updateAppCheck.getAppUrl(), apkFile);
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

    @Override
    public void start(long total) {
        mainThread.post(new Runnable() {
            @Override
            public void run() {
                updateAppDialog.progressStart(total);
            }
        });
    }

    @Override
    public void progress(float percent, long progress, long total) {
        mainThread.post(new Runnable() {
            @Override
            public void run() {
                updateAppDialog.progress(percent, progress, total);
            }
        });
    }

    @Override
    public void complete(File apkFile) {
        mainThread.post(new Runnable() {
            @Override
            public void run() {
                updateAppDialog.progressComplete(apkFile);
            }
        });
    }

    @Override
    public void err(String errorMsg) {
        mainThread.post(new Runnable() {
            @Override
            public void run() {
                updateAppDialog.progressError(errorMsg);
            }
        });
    }
}
