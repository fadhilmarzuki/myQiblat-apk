package com.fadhilmarzuki.myqiblat;

import android.content.Context;

import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "MyqWidget")
public class WidgetBridge extends Plugin {
    @PluginMethod
    public void setData(PluginCall call) {
        String d = call.getString("data");
        if (d == null) { call.reject("tiada data"); return; }
        Context c = getContext();
        c.getSharedPreferences(MyqWidget.PREF, Context.MODE_PRIVATE).edit().putString("data", d).apply();
        MyqWidget.refreshAll(c);
        call.resolve();
    }
}
