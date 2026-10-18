package com.ndds.homelauncher.utils;

import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Color;

public class ColorTool {
    public static int getLightPrimaryColorFromWallpaper(Context context) {
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
    public static int getDarkPrimaryColorFromWallpaper(Context context) {
        WallpaperColors colors = WallpaperManager.getInstance(context).getWallpaperColors(WallpaperManager.FLAG_SYSTEM);
        if (colors != null) {
            Color primaryColor = colors.getPrimaryColor();
            int p = primaryColor.toArgb();
            int r = (p >> 16) & 0xff;
            int g = (p >> 8) & 0xff;
            int b = p & 0xff;
            r = (int) (r * 0.5) & 0xff;
            g = (int) (g * 0.5) & 0xff;
            b = (int) (b * 0.5) & 0xff;
            return 0xff000000 | (r << 16) | (g << 8) | b;
        }
        return Color.WHITE;
    }
}
