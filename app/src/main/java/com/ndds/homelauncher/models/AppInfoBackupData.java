package com.ndds.homelauncher.models;

import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

public class AppInfoBackupData implements Parcelable {
    public String name, activityName, packageName;
    public AppInfoBackupData(String name, String packageName, String activityName) {
        this.name = name;
        this.activityName = activityName;
        this.packageName = packageName;
    }

    protected AppInfoBackupData(Parcel in) {
        name = in.readString();
        activityName = in.readString();
        packageName = in.readString();
    }
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", name);
            jsonObject.put("packageName", packageName);
            jsonObject.put("activityName", activityName);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return jsonObject;
    }

    public static final Creator<AppInfoBackupData> CREATOR = new Creator<>() {
        @Override
        public AppInfoBackupData createFromParcel(Parcel in) {
            return new AppInfoBackupData(in);
        }

        @Override
        public AppInfoBackupData[] newArray(int size) {
            return new AppInfoBackupData[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeString(name);
        parcel.writeString(activityName);
        parcel.writeString(packageName);
    }
}
