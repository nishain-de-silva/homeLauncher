package com.ndds.homelauncher;

import android.graphics.drawable.Drawable;

 public class AppInfo {
     String name;
     String packageName;
     String id;
     String activityName;
     Drawable icon;
     boolean isPinned;
     boolean isFresh;

     public AppInfo(String name, String packageName, String activityName, Drawable icon) {
         this.id = name + packageName;
         this.name = name;
         this.activityName = activityName;
         this.packageName = packageName;
         this.icon = icon;
     }
 }