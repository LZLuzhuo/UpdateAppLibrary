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
package me.luzhuo.lib_app_update.download;

import java.io.File;

import androidx.annotation.WorkerThread;

public abstract class IDownloadApp {
    /**
     * 直接下载文件
     */
    public static final int Single = 1 << 1;
    /**
     * 多线程分段下载文件
     */
    public static final int Multi = 1 << 2;

    public boolean isDowning = false; // 是否在下载中

    /**
     * 下载文件
     * @param apkUrl apk文件下载链接
     * @param apkFile apk文件保存的路径
     */
    @WorkerThread
    public abstract void download(String apkUrl, File apkFile);
}
