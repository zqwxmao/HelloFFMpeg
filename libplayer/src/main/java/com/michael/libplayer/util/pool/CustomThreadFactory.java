package com.michael.libplayer.util.pool;

import android.text.TextUtils;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomThreadFactory implements ThreadFactory {
    public static final AtomicInteger poolNumber = new AtomicInteger(1);
    public final ThreadGroup group;
    public final AtomicInteger threadNumber = new AtomicInteger(1);
    public final String namePrefix;

    public CustomThreadFactory(String threadNamePrix) {
        SecurityManager s = System.getSecurityManager();
        group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
        namePrefix = "pool-" + poolNumber.getAndIncrement() + (TextUtils.isEmpty(threadNamePrix) ? "" : ("-" + threadNamePrix)) + "-thread-";
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
        if (t.isDaemon())
            t.setDaemon(false);
        if (t.getPriority() != Thread.MIN_PRIORITY)
            t.setPriority(Thread.MIN_PRIORITY);
        return t;
    }
}
