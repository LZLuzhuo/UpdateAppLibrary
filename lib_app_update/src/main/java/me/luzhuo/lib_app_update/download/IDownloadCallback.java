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

/**
 * 文件下载回调
 */
public interface IDownloadCallback {

    /**
     * 开始下载
     * @param total 待文件总大小
     */
    @WorkerThread
    public void start(long total);

    /**
     * 下载进度
     * @param percent 进度百分比
     * @param progress 进度
     * @param total 总进度
     */
    @WorkerThread
    public void progress(float percent, long progress, long total);

    /**
     * 下载完成
     * @param apkFile 下载后的文件路径
     */
    @WorkerThread
    public void complete(File apkFile);

    /**
     * 下载异常
     * @param errorMsg 异常信息
     */
    @WorkerThread
    public void err(String errorMsg);
}
