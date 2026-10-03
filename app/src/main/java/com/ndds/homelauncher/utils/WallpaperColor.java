package com.ndds.homelauncher.utils;

import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Color;

import com.ndds.homelauncher.R;

public class WallpaperColor {
    public static int getPrimaryColor(Context context) {
        WallpaperColors colors = WallpaperManager.getInstance(context).getWallpaperColors(WallpaperManager.FLAG_SYSTEM);
        if (colors != null) {
            Color primaryColor = colors.getPrimaryColor();
            int p = primaryColor.toArgb();
            int r = (p >> 16) & 0xff;
            int g = (p >> 8) & 0xff;
            int b = p & 0xff;
            float ratio = 255f / Math.max(r, Math.max(g,b));
            r = (int) (r * ratio) & 0xff;
            g = (int) (g * ratio) & 0xff;
            b = (int) (b * ratio) & 0xff;
            return 0xff000000 | (r << 16) | (g << 8) | b;
        }
        return Color.WHITE;
    }
}
