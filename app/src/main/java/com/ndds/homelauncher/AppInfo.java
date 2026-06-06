package com.ndds.homelauncher;

import android.graphics.drawable.Drawable;

 public class AppInfo {
     public String name;
     public String packageName;
     public String id;
     public String activityName;
     public Drawable icon;
     public boolean isPinned;
     public boolean isLastUsed;
     public boolean isFresh;
     public int usedCount;

     public AppInfo(String name, String packageName, String activityName, Drawable icon) {
         this.id = name + packageName;
         this.name = name;
         this.activityName = activityName;
         this.packageName = packageName;
         this.icon = icon;
     }
 }