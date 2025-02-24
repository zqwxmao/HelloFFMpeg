package com.michael.libplayer.util;

import android.os.Build;

/**
 * @Author: zhangqiaowenxiang
 * @Time: 2019/11/13
 * @Description: This is
 */
public class Utils {
    /*判断是否需要展示在状态栏下 2016.4.28 by wp.nine
     * */
    public static boolean isBelowStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return true;
        }
        return false;
    }

    public static boolean isEmpty(Object[] objects) {
        return  objects==null || objects.length==0;
    }

    public static boolean isEmpty(String string) {
        return  string==null || string.length()==0;
    }
}
