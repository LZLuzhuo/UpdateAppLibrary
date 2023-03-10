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

import android.content.Context;

import java.util.Map;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

public interface UpdateAppCheck {
    /**
     * 开始检查之前的回调, 可以在里面执行 从服务器拉取版本数据
     */
    @WorkerThread
    public void startCheck(@Nullable Context context);

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
