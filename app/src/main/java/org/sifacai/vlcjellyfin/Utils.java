package org.sifacai.vlcjellyfin;

import android.app.Activity;
import android.util.DisplayMetrics;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.AbsCallback;
import com.lzy.okgo.callback.Callback;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.HttpHeaders;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Utils {

//    public static <T> T JsonToObj(String jsonStr, Class<T> tClass) {
//        if (jsonStr != null && jsonStr.length() > 0) {
//            try {
//                return new Gson().fromJson(jsonStr, tClass);
//            } catch (Exception e) {
//                return null;
//            }
//        }
//        return null;
//    }

    /**
     * 获取Json项
     *
     * @param jo
     * @param key
     * @return
     */
//    public static JsonElement getJsonString(JsonObject jo, String key) {
//        JsonElement je = new Gson().toJsonTree("", String.class);
//        if (jo.has(key)) {
//            je = jo.get(key);
//        }
//        return je;
//    }

//    public static int getPixelsFromDp(Activity context, int i) {
//        DisplayMetrics metrics = new DisplayMetrics();
//        context.getWindowManager().getDefaultDisplay().getMetrics(metrics);
//        return (i * metrics.densityDpi) / DisplayMetrics.DENSITY_DEFAULT;
//    }


}
