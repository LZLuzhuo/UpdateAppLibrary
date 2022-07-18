package me.luzhuo.lib_app_update;

import android.app.Activity;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 应用更新管理
 */
public class AppUpdateManager {
    /**
     * 是否自动检查更新, 自动更新将在任何Activity弹出更新弹窗
     * 如果想在指定的Activity弹出更新弹窗, 可以关闭自动更新, 然后在指定的Activity进行 {@link #checkUpdate(Activity)}
     */
    private boolean autoUpdate = true;
    // 启动后多久开始检查更新, 单位ms
    private static final int checkDelay = 5000;
    private static AppUpdateManager instance = null;
    private final ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
    private AppUpdateEngine updateEngine;
    private AppUpdateManager() { }

    public synchronized static AppUpdateManager getInstance(){
        if(instance == null) instance = new AppUpdateManager();
        return instance;
    }

    /**
     * 初始化, 在Application里进行
     * @param autoUpdate 是否自动更新
     */
    public void init(boolean autoUpdate, @NonNull UpdateAppCheck updateAppCheck, @NonNull UpdateAppDialog updateAppDialog) {
        this.autoUpdate = autoUpdate;
        this.updateEngine = new AppUpdateEngine(updateAppCheck, updateAppDialog);
        if (this.autoUpdate) {
            singleThreadExecutor.execute(autoCheckUpdateAction);
        }
    }

    /**
     * 手动检查更新
     * 调用该函数关闭自动更新
     * @param activity 指定的Activity弹出更新窗口; 如果为null, 则在当前浏览的Activity弹出窗口
     */
    public void checkUpdate(@Nullable Activity activity) {
        try {
            this.autoUpdate = false;
            this.updateEngine.checkAppUpdate(activity);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    /**
     * 在Activity里调用
     */
    public void startDownload(@NonNull File apkFile) {
        this.updateEngine.startDownload(apkFile);
    }

    private final Runnable autoCheckUpdateAction = new Runnable() {
        @Override
        public void run() {
            try {
                Thread.sleep(checkDelay);
                if (autoUpdate) updateEngine.checkAppUpdate();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
    };
}
