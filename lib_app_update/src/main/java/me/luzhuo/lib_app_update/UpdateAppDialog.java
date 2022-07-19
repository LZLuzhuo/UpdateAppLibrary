package me.luzhuo.lib_app_update;

import android.app.Activity;

import java.io.File;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

public interface UpdateAppDialog {
    /**
     * 显示弹窗
     * @param isForce 是否是强更, true强更, false非强更
     * @param apkFile Apk的保存路径
     * @param bundle 相关捆绑的数据
     */
    @UiThread
    public void showDialog(@NonNull Activity activity, boolean isForce, @NonNull File apkFile, @Nullable Map<String, Object> bundle);

    /**
     * 开始下载
     * @param total 文件总大小
     */
    @UiThread
    public void progressStart(long total);

    /**
     * 应用更新进度
     * @param percent 进度百分比 = 当前进度 / 总大小
     * @param progress 当前进度
     * @param total 总大小
     */
    @UiThread
    public void progress(float percent, long progress, long total);

    /**
     * 进度完成
     */
    @UiThread
    public void progressComplete(@NonNull File apkFile);

    /**
     * 进度发生错误
     */
    @UiThread
    public void progressError(@NonNull String errorMsg);
}
