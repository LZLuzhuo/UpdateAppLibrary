package me.luzhuo.lib_app_update.download;

import android.accounts.NetworkErrorException;
import android.text.TextUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import me.luzhuo.lib_core.data.hashcode.HashManager;
import me.luzhuo.lib_file.FileManager;
import me.luzhuo.lib_okhttp.OKHttpManager;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.Util;

/**
 * 多线程分段下载
 */
public class MultiDownload extends IDownloadApp {
    private static final String TAG = MultiDownload.class.getSimpleName();
    private final IDownloadCallback downloadCallback;
    private final ExecutorService executorService = new ThreadPoolExecutor(0/*核心线程*/, Integer.MAX_VALUE/*最大线程*/, 60, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), Util.threadFactory("download slice apk", false));
    private final static int SliceFileSize = 1000000; // 切片文件大小, 默认1M
    private final ArrayList<Slice> slices = new ArrayList<>();
    private File apkFile;
    private int mSlice = 0; // 切片数量

    private int mProgress;

    public MultiDownload(@NonNull IDownloadCallback downloadCallback) {
        this.downloadCallback = downloadCallback;
    }

    @Override
    @WorkerThread
    public void download(String apkUrl, File apkFile) {
        if (isDowning) return;
        isDowning = true;

        if (TextUtils.isEmpty(apkUrl)) return;
        this.slices.clear();
        this.apkFile = apkFile;

        try {
            // 1. 获取网络文件大小; 文件名: uuid(url + file size + 切片索引)
            long fileSize = getNetFileSize(apkUrl);
            int slice = (int) (fileSize / SliceFileSize); // 切成多少片
            boolean supplementSlice = (fileSize % SliceFileSize) != 0;
            this.mSlice = slice;
            this.mProgress = 0;

            for (int i = 0; i < slice; i++) {
                File sliceFile = new File(new FileManager().getCacheDirectory() + File.separator + "downloadSliceCache", HashManager.getInstance().getUuid(apkUrl + fileSize + i));
                slices.add(new Slice(i, sliceFile, SliceFileSize * i, SliceFileSize * (i + 1) - 1));
            }
            if (supplementSlice) {
                int i = slice;
                File sliceFile = new File(new FileManager().getCacheDirectory() + File.separator + "downloadSliceCache", HashManager.getInstance().getUuid(apkUrl + fileSize + i));
                slices.add(new Slice(i, sliceFile, SliceFileSize * i, fileSize - 1));
            }

            // 2. 检查本地切片
            boolean isComplete = checkLocalSlice();
            mProgress = getLocalSliceSize();

            // 3. 合并文件
            if (isComplete) {
                boolean merged = mergeSliceFile(apkFile);
                if (merged) downloadCallback.complete(apkFile);
                return;
            }

            // 4. 下载剩余未下载的切片文件
            downloadCallback.start(fileSize);
            for (int i = 0; i < slices.size(); i++) {
                if (!slices.get(i).isDownloaded) downloadSliceFile(apkUrl, slices.get(i));
            }
        } catch (Exception e) {
            isDowning = false;
            downloadCallback.err("初始化异常: " + e.getMessage());
        }
    }

    /**
     * 下载碎片文件
     */
    private void downloadSliceFile(String apkUrl, Slice slice) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Request request = new Request.Builder().url(apkUrl).addHeader("Range", "bytes=" + slice.startIndex + "-" + slice.endIndex).build();
                    Response response = new OKHttpManager().getClient().newCall(request).execute();
                    if (response.isSuccessful() || response.isRedirect()) {

                        File tempFile = new File(new FileManager().getCacheDirectory() + File.separator + "downloadCache", HashManager.getInstance().getUuid());
                        if (!slice.sliceFile.getParentFile().exists()) { slice.sliceFile.getParentFile().mkdirs(); }
                        if (!tempFile.getParentFile().exists()) { tempFile.getParentFile().mkdirs(); }
                        if (!tempFile.exists()) { tempFile.createNewFile(); }

                        InputStream inputStream = response.body().byteStream();
                        BufferedInputStream bis = new BufferedInputStream(inputStream);
                        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(tempFile));

                        long total = response.body().contentLength();
                        if (total <= 0) {
                            downloadCallback.err("文件异常: 文件大小为" + total);
                            return;
                        }

                        byte[] bys = new byte[10240];
                        int len;
                        while((len = bis.read(bys)) != -1) {
                            bos.write(bys, 0, len);
                            bos.flush();
                        }

                        bis.close();
                        bos.close();
                        tempFile.renameTo(slice.sliceFile);
                        downloadCallback.progress(mProgress * 1f / mSlice, mProgress, mSlice);
                        mProgress++; // 滞后一步

                        slice.isDownloaded = true;
                        checkDownloadComplete();
                    } else {
                        isDowning = false;
                        downloadCallback.err("服务器异常: " + response.code());
                    }
                } catch (Exception e) {
                    executorService.execute(this);
                }
            }
        });
    }

    /**
     * 合并碎片文件
     */
    private boolean mergeSliceFile(File apkFile) throws IOException {
        try {
            File tempFile = new File(new FileManager().getCacheDirectory() + File.separator + "downloadCache", HashManager.getInstance().getUuid());
            if (!apkFile.getParentFile().exists()) apkFile.getParentFile().mkdirs();
            if (!tempFile.getParentFile().exists()) tempFile.getParentFile().mkdirs();
            if (!tempFile.exists()) tempFile.createNewFile();

            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(tempFile));
            byte[] bys = new byte[10240];
            for (int i = 0; i < slices.size(); i++) {
                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(slices.get(i).sliceFile));
                int len;
                while((len = bis.read(bys)) != -1) {
                    bos.write(bys, 0, len);
                    bos.flush();
                }
                bis.close();
            }
            bos.close();

            tempFile.renameTo(apkFile);
            return true;
        } catch (Exception e) {
            throw new IOException("切片文件合并异常");
        }
    }

    @WorkerThread
    private long getNetFileSize(String apkUrl) throws NetworkErrorException {
        try {
            Request request = new Request.Builder().url(apkUrl).build();
            Response response = new OKHttpManager().getClient().newCall(request).execute();
            if (response.isSuccessful() || response.isRedirect()) {
                long total = response.body().contentLength();
                if (total <= 0) {
                    throw new NetworkErrorException("文件异常: 文件大小为" + total);
                }
                return total;
            } else {
                throw new NetworkErrorException("服务器异常: " + response.code() + " - " + apkUrl);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new NetworkErrorException("获取网络文件大小失败!");
        }
    }

    /**
     * 1. 检查本地切片文件
     * 2. 标记已经下载的切片
     * 3. 如果全部下载完成, 返回true, 否则返回false
     */
    private synchronized boolean checkLocalSlice() {
        boolean isFill = true;
        for (int i = 0; i < slices.size(); i++) {
            boolean exists = slices.get(i).sliceFile.exists();
            if (exists) slices.get(i).isDownloaded = true;
            else isFill = false;
        }
        return isFill;
    }

    private synchronized int getLocalSliceSize() {
        int size = 0;
        for (int i = 0; i < slices.size(); i++) {
            if (slices.get(i).isDownloaded) size++;
        }
        return size;
    }

    /**
     * 检查是否下载完成
     * 每下载一个切片都会检查下
     */
    private synchronized boolean checkDownloadComplete() {
        // 检查内存数据
        for (int i = 0; i < slices.size(); i++) {
            if (!slices.get(i).isDownloaded) return false;
        }

        // 检查本地文件
        boolean isComplete = checkLocalSlice();
        if (!isComplete) return false;

        // 合并文件
        try {
            boolean merged = mergeSliceFile(apkFile);
            if (merged) downloadCallback.complete(apkFile);
        } catch (Exception e) {
            downloadCallback.err(e.getMessage());
        }

        isDowning = false;
        return false;
    }

    /**
     * 切片
     */
    public static class Slice {
        public int index = -1;
        public boolean isDownloaded = false;
        public File sliceFile;
        public long startIndex;
        public long endIndex;
        public Slice(int index, File sliceFile, long startIndex, long endIndex) {
            this.index = index;
            this.isDownloaded = false;
            this.sliceFile = sliceFile;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }
    }
}
