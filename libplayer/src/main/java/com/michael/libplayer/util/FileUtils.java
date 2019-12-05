package com.michael.libplayer.util;

import android.text.TextUtils;

import java.net.URLConnection;

/**
 * @Author: zhangqiaowenxiang
 * @Time: 2019/12/5
 * @Description: This is
 */
public class FileUtils {
    private final static String PREFIX_VIDEO="video/";
    private final static String SUFFIX_VIDEO="flv";

    public static boolean isVideo(String fileName) {
        if(!TextUtils.isEmpty(fileName)) {
            if (fileName.endsWith(SUFFIX_VIDEO)) return true;
            String contentType = URLConnection.getFileNameMap().getContentTypeFor(fileName);
            if (TextUtils.isEmpty(contentType)) return false;
            return contentType.contains(PREFIX_VIDEO);
        } else {
            return false;
        }
    }
}
