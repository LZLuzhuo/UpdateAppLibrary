package me.luzhuo.lib_app_update;

import android.content.Context;

import java.util.Map;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

public interface UpdateAppCheck {
    /**
     * 开始检查之前的回调, 可以在里面执行 从服务器拉取版本数据
     */
    @WorkerThread
    public void startCheck(Context context);

    /**
     * 设置app的网络下载地址
     * @return app下载地址
     */
    @WorkerThread
    @Nullable
    public String getAppUrl();

    /**
     * 设置app的versionCode
     * 当前用户App的版本 > 此版本号时, 才会进行更新
     * @return app的versionCode
     */
    @WorkerThread
    public int getVersionCode();

    /**
     * 是否是强更
     * @return true强更, false非强更
     */
    @WorkerThread
    public boolean isForce();

    /**
     * 提供给Dialog需要的一些数据
     * @return 拓展数据
     */
    @WorkerThread
    @Nullable
    public Map<String, Object> getBundleData();
}
