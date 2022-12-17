package me.luzhuo.updateappdemo.app_update;

import android.content.Context;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.Nullable;
import me.luzhuo.lib_app_update.UpdateAppCheck;

public class ExampleUpdateAppCheck implements UpdateAppCheck {
    private String apkUrl;
    private int versionCode;
    private boolean isForce; // 是否强更

    @Override
    public void startCheck(Context context) {
        // TODO 同步网络请求
        // TODO 解析数据
        apkUrl = "https://652a3b7a97c2aa061b59601f56ff9948.rdt.tfogc.com:49156/dd.myapp.com/sjy.00004/16891/apk/9702011C8E5D6F5019372701B7206C7F.apk?mkey=639da2595c7416850f37aefcea6188d2&arrive_key=78392160294&fsname=com.tencent.mobileqq_8.9.25_3640.apk&cip=117.147.91.111&proto=https";
        versionCode = 123;
        isForce = true;
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
