package com.fadhilmarzuki.myqiblat;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.SystemClock;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MyqWidget extends AppWidgetProvider {
    static final String PREF = "myq_widget";
    static final String TICK = "com.fadhilmarzuki.myqiblat.WIDGET_TICK";
    static final String[] NM = {"Subuh", "Zohor", "Asar", "Maghrib", "Isyak"};
    static final int[] COL = {R.id.w_c0, R.id.w_c1, R.id.w_c2, R.id.w_c3, R.id.w_c4};
    static final int[] NAMES = {R.id.w_n0, R.id.w_n1, R.id.w_n2, R.id.w_n3, R.id.w_n4};
    static final int[] TIMES = {R.id.w_t0, R.id.w_t1, R.id.w_t2, R.id.w_t3, R.id.w_t4};
    static final int GOLD = Color.parseColor("#E8C468");
    static final int DARK = Color.parseColor("#1D1A0C");
    static final int WHITE = Color.parseColor("#F3F1E8");

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int id : ids) m.updateAppWidget(id, build(c));
        schedule(c);
    }

    @Override
    public void onReceive(Context c, Intent i) {
        super.onReceive(c, i);
        String a = i.getAction();
        if (TICK.equals(a) || Intent.ACTION_TIME_CHANGED.equals(a) || Intent.ACTION_TIMEZONE_CHANGED.equals(a)) refreshAll(c);
    }

    static void refreshAll(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        int[] ids = m.getAppWidgetIds(new ComponentName(c, MyqWidget.class));
        if (ids == null || ids.length == 0) return;
        for (int id : ids) m.updateAppWidget(id, build(c));
        schedule(c);
    }

    static String key(long ms) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(ms));
    }

    static String fmt(long ms) {
        Calendar k = Calendar.getInstance();
        k.setTimeInMillis(ms);
        int h = k.get(Calendar.HOUR_OF_DAY), mi = k.get(Calendar.MINUTE);
        int h12 = h % 12 == 0 ? 12 : h % 12;
        return h12 + ":" + (mi < 10 ? "0" : "") + mi + (h < 12 ? " pg" : " ptg");
    }

    static JSONObject data(Context c) {
        try {
            String s = c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString("data", null);
            return s == null ? null : new JSONObject(s);
        } catch (Exception e) {
            return null;
        }
    }

    /* waktu seterusnya: {masa, indeks} ; indeks 0..4 */
    static long[] next(JSONObject days, long now) {
        try {
            for (int d = 0; d < 2; d++) {
                JSONObject o = days.optJSONObject(key(now + d * 86400000L));
                if (o == null) continue;
                JSONArray t = o.getJSONArray("t");
                for (int i = 0; i < 5; i++) if (t.getLong(i) > now) return new long[]{t.getLong(i), i, d};
            }
        } catch (Exception e) { }
        return null;
    }

    static RemoteViews build(Context c) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_myq);
        Intent open = c.getPackageManager().getLaunchIntentForPackage(c.getPackageName());
        if (open != null) {
            PendingIntent pi = PendingIntent.getActivity(c, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            v.setOnClickPendingIntent(R.id.w_root, pi);
        }
        long now = System.currentTimeMillis();
        JSONObject o = data(c);
        JSONObject d = null, days = null;
        if (o != null) {
            days = o.optJSONObject("days");
            if (days != null) d = days.optJSONObject(key(now));
        }
        if (d == null) {
            v.setTextViewText(R.id.w_next, "Buka myQiblat sekali untuk memaparkan waktu solat");
            v.setTextViewText(R.id.w_cd, "");
            return v;
        }
        try {
            v.setTextViewText(R.id.w_date, d.optString("g"));
            v.setTextViewText(R.id.w_hijri, d.optString("h"));
            String zone = o.optString("zone", "");
            String qib = o.optString("qibla", "");
            v.setTextViewText(R.id.w_zone, "📍 " + zone + (qib.length() > 0 ? "   ·   🧭 Qiblat " + qib : ""));
            JSONArray t = d.getJSONArray("t");
            long[] nx = next(days, now);
            int hi = (nx != null && nx[2] == 0) ? (int) nx[1] : -1;
            for (int i = 0; i < 5; i++) {
                v.setTextViewText(TIMES[i], fmt(t.getLong(i)));
                boolean on = i == hi;
                v.setInt(COL[i], "setBackgroundResource", on ? R.drawable.widget_pill : 0);
                v.setTextColor(NAMES[i], on ? DARK : GOLD);
                v.setTextColor(TIMES[i], on ? DARK : WHITE);
            }
            if (nx != null) {
                String nm = NM[(int) nx[1]];
                v.setTextViewText(R.id.w_next, "Menjelang " + nm + (nx[2] == 1 ? " (esok)" : "") + " · " + fmt(nx[0]));
                long base = SystemClock.elapsedRealtime() + (nx[0] - now);
                v.setChronometer(R.id.w_cd, base, null, true);
                if (Build.VERSION.SDK_INT >= 24) v.setChronometerCountDown(R.id.w_cd, true);
            } else {
                v.setTextViewText(R.id.w_next, "Buka myQiblat untuk kemas kini waktu");
            }
        } catch (Exception e) {
            v.setTextViewText(R.id.w_next, "Buka myQiblat untuk kemas kini");
        }
        return v;
    }

    static void schedule(Context c) {
        long now = System.currentTimeMillis();
        long at = now + 30 * 60000L;
        JSONObject o = data(c);
        if (o != null && o.optJSONObject("days") != null) {
            long[] nx = next(o.optJSONObject("days"), now);
            if (nx != null && nx[0] + 1000 < at) at = nx[0] + 1000;
        }
        Calendar mid = Calendar.getInstance();
        mid.setTimeInMillis(now);
        mid.add(Calendar.DAY_OF_YEAR, 1);
        mid.set(Calendar.HOUR_OF_DAY, 0); mid.set(Calendar.MINUTE, 0); mid.set(Calendar.SECOND, 5); mid.set(Calendar.MILLISECOND, 0);
        if (mid.getTimeInMillis() < at) at = mid.getTimeInMillis();
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent i = new Intent(c, MyqWidget.class).setAction(TICK);
        PendingIntent p = PendingIntent.getBroadcast(c, 77, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        try {
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) am.set(AlarmManager.RTC, at, p);
            else am.setExact(AlarmManager.RTC, at, p);
        } catch (Exception e) {
            am.set(AlarmManager.RTC, at, p);
        }
    }
}
