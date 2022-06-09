package me.luzhuo.updateappdemo;

import me.luzhuo.lib_app_update.AppUpdateManager;
import me.luzhuo.lib_core.app.base.CoreBaseApplication;
import me.luzhuo.updateappdemo.app_update.ExampleUpdateAppCheck;
import me.luzhuo.updateappdemo.app_update.ExampleUpdateAppDialog;

public class MyApplication extends CoreBaseApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        AppUpdateManager.getInstance().init(true, new ExampleUpdateAppCheck(), new ExampleUpdateAppDialog());
    }
}
