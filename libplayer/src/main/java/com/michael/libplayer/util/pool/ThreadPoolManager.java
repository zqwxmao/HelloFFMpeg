package com.michael.libplayer.util.pool;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolManager {

    private volatile static ThreadPoolManager instance;
    private ThreadPoolExecutor executor;
    HandlerThread mHandlerThread;
    Handler mHandler;

    public static ThreadPoolManager getInstance() {
        if (instance == null) {
            synchronized (ThreadPoolManager.class) {
                if (instance == null) {
                    instance = new ThreadPoolManager();
                }
            }
        }
        return instance;
    }

    private ThreadPoolManager() {
        int num = Runtime.getRuntime().availableProcessors();
        int corePoolNum = num + 1;
        int maxPoolNum = 2 * num + 1;

        executor = new ThreadPoolExecutor(
                corePoolNum,
                maxPoolNum,
                60,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(50),
                new CustomThreadFactory("ThreadPoolManager"),
                new CustomRejectedExecutionHandler());

        mHandlerThread = new HandlerThread("ThreadPoolManager");
        mHandlerThread.start();
        Looper loop = mHandlerThread.getLooper();
        if (loop != null) {
            mHandler = new Handler(loop);
        } else {
            mHandlerThread.quit();
        }
    }

    public void start(Runnable runnable) {
        try {
            executor.execute(runnable);
        } catch (Throwable e) {
        }
    }

    public void startDelayed(final Runnable runnable, long delayMillis) {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    executor.execute(runnable);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }, delayMillis);
    }

    public void release() {
        executor.shutdownNow();
        instance = null;
    }

}
