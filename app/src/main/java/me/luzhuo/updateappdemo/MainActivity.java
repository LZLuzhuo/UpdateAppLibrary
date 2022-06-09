package me.luzhuo.updateappdemo;

import me.luzhuo.lib_app_update.AppUpdateManager;
import me.luzhuo.lib_core.app.base.CoreBaseActivity;

import android.os.Bundle;

public class MainActivity extends CoreBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //AppUpdateManager.getInstance().checkUpdate(this);
    }
}