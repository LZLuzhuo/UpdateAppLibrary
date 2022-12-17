package me.luzhuo.updateappdemo.app_update;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import java.io.File;
import java.util.Locale;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;
import me.luzhuo.lib_app_update.AppUpdateManager;
import me.luzhuo.lib_app_update.UpdateAppDialog;
import me.luzhuo.lib_core.app.appinfo.AppManager;
import me.luzhuo.lib_core.ui.dialog.Dialog;
import me.luzhuo.lib_core.ui.toast.ToastManager;
import me.luzhuo.updateappdemo.R;

public class ExampleUpdateAppDialog implements UpdateAppDialog {
    private static final String TAG = ExampleUpdateAppDialog.class.getSimpleName();
    private @Nullable ProgressDialog progressDialog;
    private Activity activity;
    private AlertDialog dialog;
    private @Nullable NotificationManager manager;
    private @Nullable PendingIntent installIntent;

    @Override
    public void showDialog(@Nullable Activity activity, boolean isForce, @NonNull File apkFile, @Nullable Map<String, Object> bundle) {
        Log.e(TAG, "" + apkFile);
        this.activity = activity;
        String content = (String) bundle.get("content");
        String title = "升级提示";
        String okName = "现在升级";
        String cancelName = isForce ? null : "以后再说";
        if (dialog == null) dialog = Dialog.instance().build(activity, title, content, okName, cancelName, false, null, null);
        if (!activity.isFinishing()) {
            try {
                Dialog.show(dialog, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (apkFile.exists()) {
                            new AppManager().installApk(activity, apkFile);
                        } else {
                            if (isForce) {
                                if (progressDialog == null) progressDialog = Dialog.instance().build(activity, title, content, false, 0);
                                progressDialog.setProgress(0);
                                progressDialog.show();
                            }

                            manager = buildNotification(activity);
                            installIntent = installIntent(activity, apkFile);

                            AppUpdateManager.getInstance().startDownload(apkFile);
                        }
                        if (!isForce) dialog.dismiss();
                    }
                }, null);
            } catch (WindowManager.BadTokenException e) {
            } catch (IllegalStateException e) { }
        }
    }

    @Override
    public void progressStart(long total) {
        // don't do anything.
    }

    int oldRate = 0;
    @Override
    public void progress(float percent, long progress, long total) {
        // 限制速度
        if (oldRate == (int) (percent * 30)) return;
            oldRate = (int) (percent * 30);

        if (progressDialog != null) {
            progressDialog.setMax((int) total);
            progressDialog.setProgress((int) progress);
        }

        notification(manager, activity, (int) progress, (int) total, progress == total, installIntent);
    }

    @Override
    public void progressComplete(@NonNull File apkFile) {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
        notification(manager, activity, 0, 0, true, installIntent);
        new AppManager().installApk(activity, apkFile);
    }

    @Override
    public void progressError(@NonNull String errorMsg) {
        Log.e(TAG, "apk下载失败: " + errorMsg);
        if (progressDialog != null) {
            ToastManager.show2(activity, "下载失败, 请稍后重试");
            progressDialog.dismiss();
            progressDialog = null;
        }
        if (manager != null) manager.cancel(1);
    }

    public static final String channelID = "apk_download";
    public static final String channelName = "应用更新";
    public NotificationManager buildNotification(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return manager;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelID, channelName, NotificationManager.IMPORTANCE_LOW);
            manager.createNotificationChannel(channel);
        }
        return manager;
    }

    public PendingIntent installIntent(Context context, File apkFilePath) {
        Intent install = new Intent(Intent.ACTION_VIEW);
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            install.setDataAndType(Uri.parse("file://" + apkFilePath.getAbsolutePath()), "application/vnd.android.package-archive");
        } else {
            install.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            install.setDataAndType(FileProvider.getUriForFile(context, AppManager.AUTHORITY + context.getPackageName(), apkFilePath), "application/vnd.android.package-archive");
        }

        return PendingIntent.getActivity(context, 0, install, flags());
    }

    private int flags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) return PendingIntent.FLAG_IMMUTABLE;
        return 0;
    }

    public void notification(@Nullable NotificationManager manager, @Nullable Context context, int progress, int max, boolean isDownloaded, @Nullable PendingIntent pendingIntent) {
        if (manager == null || installIntent == null || context == null) return;

        NotificationCompat.Builder build = new NotificationCompat.Builder(context, channelID)
                .setContentTitle(context.getResources().getString(R.string.app_name))
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setWhen(System.currentTimeMillis());
        if (isDownloaded) {
            build.setContentText("点击安装")
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setOngoing(false)
                    .setProgress(0, 0, false);
        } else {
            build.setContentText(String.format(Locale.CHINESE, "正在下载 %d%%", (int) ((progress / (float) max) * 100)))
                    .setAutoCancel(false)
                    .setOngoing(true)
                    .setProgress(max, progress, false);
        }
        manager.notify(1, build.build());
    }
}
