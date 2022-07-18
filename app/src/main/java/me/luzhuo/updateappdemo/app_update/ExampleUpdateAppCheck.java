package me.luzhuo.updateappdemo.app_update;

import android.content.Context;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.Nullable;
import me.luzhuo.lib_app_update.UpdateAppCheck;

public class ExampleUpdateAppCheck implements UpdateAppCheck {
    private static final String TAG = ExampleUpdateAppCheck.class.getSimpleName();
    private String apkUrl;
    private int versionCode;
    private boolean isForce; // 是否强更

    @Override
    public void startCheck(Context context) {
        // TODO 同步网络请求
        // TODO 解析数据
        apkUrl = "https://file.expection.cn/admin/app/mingpianwang2.1.43.apk";
        // apkUrl = "http://luzhuo-data.oss-cn-hangzhou.aliyuncs.com/app-release.apk";
        versionCode = 123;
        isForce = false;
    }

    @Nullable
    @Override
    public String getAppUrl() {
        return apkUrl;
    }

    @Override
    public int getVersionCode() {
        return versionCode;
    }

    @Override
    public boolean isForce() {
        return isForce;
    }

    @Override
    public Map<String, Object> getBundleData() {
        HashMap<String, Object> data = new HashMap<>();
        data.put("content", isForce ? "强制更新" : "推荐更新");
        return data;
    }
}
