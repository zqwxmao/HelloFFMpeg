package com.michael.libplayer.util;

/**
 * @Author: zhangqiaowenxiang
 * @Time: 2020/3/30
 * @Description: This is
 */
public class SoundUtils {
    public static double calculateVolume(byte[] buffer){
        double sumVolume = 0.0;
        double avgVolume = 0.0;
        double volume = 0.0;

        for(int i = 0; i < buffer.length; i+=2){
            int v1 = buffer[i] & 0xFF;
            int v2 = buffer[i + 1] & 0xFF;
            int temp = v1 + (v2 << 8);// 小端
            if (temp >= 0x8000) {
                temp = 0xffff - temp;
            }
            sumVolume += Math.abs(temp);
        }
        avgVolume = sumVolume / buffer.length / 2;
        volume = Math.log10(1 + avgVolume) * 10;
        return volume;
    }
}
