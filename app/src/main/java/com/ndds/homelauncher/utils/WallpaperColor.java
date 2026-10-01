package com.ndds.homelauncher.utils;

import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Color;

import com.ndds.homelauncher.R;

public class WallpaperColor {
    public static int getPrimaryColor(Context context) {
        WallpaperColors colors = WallpaperManager.getInstance(context).getWallpaperColors(WallpaperManager.FLAG_SYSTEM);
        if (colors != null)
            return context.getColor(R.color.accent_brown_test);
        return Color.WHITE;
    }
}
