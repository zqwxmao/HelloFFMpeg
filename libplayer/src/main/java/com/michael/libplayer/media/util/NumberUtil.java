package com.michael.libplayer.media.util;

import android.util.Log;

public class NumberUtil {
    static String TAG = "PlayerCameraRecordMuxerActivity/ MediaRtmpEncoder";
    public static String encodeHex(byte[] bytes) {
        if (bytes == null) return "";
        StringBuilder stringBuilder = new StringBuilder();
        StringBuilder stringBuilder1 = new StringBuilder();
        stringBuilder1.append("0x ");
        stringBuilder.append("0x ");
        for (byte b : bytes) {
            stringBuilder1.append(b+" ");
            stringBuilder.append(Integer.toHexString(b)+" ");
        }
        Log.e(TAG, stringBuilder1.toString());
        return stringBuilder.toString();
    }
}
