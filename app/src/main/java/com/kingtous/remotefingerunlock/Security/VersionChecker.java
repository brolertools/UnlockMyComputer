package com.kingtous.remotefingerunlock.Security;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Author：Kingtous
 * Date：2019/4/28
 * Description:版本检测
 */
public class VersionChecker {

    public static float versionRequirement=2;

    public static boolean versionAvaliable(String readline) throws JSONException {
        JSONObject array=new JSONObject(readline);
        if (Float.valueOf(array.getString("version"))>=versionRequirement)
            return true;
        else return false;
    }
}
