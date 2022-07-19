package me.luzhuo.lib_app_update.download;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import androidx.annotation.IntDef;

@IntDef({IDownloadApp.Single, IDownloadApp.Multi})
@Retention(RetentionPolicy.SOURCE)
public @interface DownloadType {
}
