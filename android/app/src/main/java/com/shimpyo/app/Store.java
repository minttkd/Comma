package com.shimpyo.app;

import android.content.Context;
import android.content.SharedPreferences;

import com.shimpyo.app.core.Enums.Intensity;
import com.shimpyo.app.core.Enums.Judgment;
import com.shimpyo.app.core.Enums.Outcome;
import com.shimpyo.app.core.Enums.TargetApp;
import com.shimpyo.app.core.Enums.Trigger;
import com.shimpyo.app.core.Record;
import com.shimpyo.app.core.Settings;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** SharedPreferences + JSON 저장소. 접근성 서비스(작업 스레드)와 화면이 함께 쓰므로 모든 메서드를 동기화한다. */
public final class Store {
    private static Store instance;

    public static synchronized Store get(Context ctx) {
        if (instance == null) instance = new Store(ctx.getApplicationContext());
        return instance;
    }

    private final SharedPreferences prefs;
    private Settings settings;
    private final List<Record> records = new ArrayList<>();
    private final List<String> logs = new ArrayList<>();
    private final Map<TargetApp, String> lastScreen = new EnumMap<>(TargetApp.class);

    private Store(Context ctx) {
        prefs = ctx.getSharedPreferences("shimpyo", Context.MODE_PRIVATE);
        settings = loadSettings();
        loadRecords();
        try {
            JSONArray a = new JSONArray(prefs.getString("logs", "[]"));
            for (int i = 0; i < a.length(); i++) logs.add(a.getString(i));
        } catch (JSONException ignored) {
        }
        for (TargetApp app : TargetApp.values()) {
            String s = prefs.getString("screen_" + app.name(), null);
            if (s != null) lastScreen.put(app, s);
        }
    }

    // ------------------------------------------------------------------ 설정

    public synchronized Settings settings() {
        return copy(settings);
    }

    public synchronized void saveSettings(Settings s) {
        settings = copy(s);
        prefs.edit().putString("settings", toJson(s).toString()).apply();
    }

    private static Settings copy(Settings s) {
        return fromJson(toJson(s));
    }

    private Settings loadSettings() {
        String raw = prefs.getString("settings", null);
        if (raw == null) return new Settings();
        try {
            return fromJson(new JSONObject(raw));
        } catch (JSONException e) {
            return new Settings();
        }
    }

    static JSONObject toJson(Settings s) {
        JSONObject o = new JSONObject();
        try {
            o.put("onboarded", s.onboarded);
            o.put("income", s.income);
            o.put("payday", s.payday);
            o.put("cycleStartOverride", s.cycleStartOverride);
            o.put("rent", s.rent);
            o.put("utilities", s.utilities);
            o.put("phone", s.phone);
            o.put("transport", s.transport);
            o.put("subscriptions", s.subscriptions);
            o.put("martBudget", s.martBudget);
            o.put("coupangFoodBudget", s.coupangFoodBudget);
            o.put("k", s.k);
            o.put("highAbs", s.highAbs);
            o.put("highMultipleOfB", s.highMultipleOfB);
            o.put("bingeWindowMin", s.bingeWindowMin);
            o.put("bingeCount", s.bingeCount);
            o.put("lateEnabled", s.lateEnabled);
            o.put("lateStartHour", s.lateStartHour);
            o.put("lateEndHour", s.lateEndHour);
            o.put("lateWindowDays", s.lateWindowDays);
            o.put("lateCount", s.lateCount);
            o.put("lateResetTs", s.lateResetTs);
            o.put("apiKey", s.apiKey);
            o.put("model", s.model);
            o.put("aiTimeoutMs", s.aiTimeoutMs);
            o.put("simulatedHour", s.simulatedHour);
            o.put("showGuard", s.showGuard);
            o.put("lastSyncTs", s.lastSyncTs);
        } catch (JSONException ignored) {
        }
        return o;
    }

    static Settings fromJson(JSONObject o) {
        Settings d = new Settings();
        Settings s = new Settings();
        s.onboarded = o.optBoolean("onboarded", d.onboarded);
        s.income = o.optLong("income", d.income);
        s.payday = o.optInt("payday", d.payday);
        s.cycleStartOverride = o.optLong("cycleStartOverride", d.cycleStartOverride);
        s.rent = o.optLong("rent", d.rent);
        s.utilities = o.optLong("utilities", d.utilities);
        s.phone = o.optLong("phone", d.phone);
        s.transport = o.optLong("transport", d.transport);
        s.subscriptions = o.optLong("subscriptions", d.subscriptions);
        s.martBudget = o.optLong("martBudget", d.martBudget);
        s.coupangFoodBudget = o.optLong("coupangFoodBudget", d.coupangFoodBudget);
        s.k = o.optDouble("k", d.k);
        s.highAbs = o.optLong("highAbs", d.highAbs);
        s.highMultipleOfB = o.optDouble("highMultipleOfB", d.highMultipleOfB);
        s.bingeWindowMin = o.optInt("bingeWindowMin", d.bingeWindowMin);
        s.bingeCount = o.optInt("bingeCount", d.bingeCount);
        s.lateEnabled = o.optBoolean("lateEnabled", d.lateEnabled);
        s.lateStartHour = o.optInt("lateStartHour", d.lateStartHour);
        s.lateEndHour = o.optInt("lateEndHour", d.lateEndHour);
        s.lateWindowDays = o.optInt("lateWindowDays", d.lateWindowDays);
        s.lateCount = o.optInt("lateCount", d.lateCount);
        s.lateResetTs = o.optLong("lateResetTs", d.lateResetTs);
        s.showGuard = o.optBoolean("showGuard", d.showGuard);
        s.apiKey = o.optString("apiKey", d.apiKey);
        s.model = o.optString("model", d.model);
        // 0.3까지의 기본값(claude-haiku-4-5)이 저장돼 있으면 새 기본값으로 옮긴다
        if ("claude-haiku-4-5".equals(s.model)) s.model = d.model;
        s.aiTimeoutMs = o.optInt("aiTimeoutMs", d.aiTimeoutMs);
        s.simulatedHour = o.optInt("simulatedHour", d.simulatedHour);
        s.lastSyncTs = o.optLong("lastSyncTs", d.lastSyncTs);
        return s;
    }

    // ------------------------------------------------------------------ 기록

    public synchronized List<Record> records() {
        return new ArrayList<>(records);
    }

    public synchronized void addRecord(Record r) {
        records.add(r);
        saveRecords();
    }

    public synchronized void updateRecord(Record r) {
        for (int i = 0; i < records.size(); i++) {
            if (records.get(i).id.equals(r.id)) {
                records.set(i, r);
                break;
            }
        }
        saveRecords();
    }

    public synchronized void deleteRecord(String id) {
        for (int i = records.size() - 1; i >= 0; i--) if (records.get(i).id.equals(id)) records.remove(i);
        saveRecords();
    }

    public synchronized void deleteWhere(Outcome outcome) {
        for (int i = records.size() - 1; i >= 0; i--) if (records.get(i).outcome == outcome) records.remove(i);
        saveRecords();
    }

    public synchronized void clearRecords() {
        records.clear();
        saveRecords();
    }

    private void loadRecords() {
        try {
            JSONArray a = new JSONArray(prefs.getString("records", "[]"));
            for (int i = 0; i < a.length(); i++) records.add(recordFromJson(a.getJSONObject(i)));
        } catch (JSONException ignored) {
        }
    }

    private void saveRecords() {
        JSONArray a = new JSONArray();
        for (Record r : records) a.put(recordToJson(r));
        prefs.edit().putString("records", a.toString()).apply();
    }

    static JSONObject recordToJson(Record r) {
        JSONObject o = new JSONObject();
        try {
            o.put("id", r.id);
            o.put("ts", r.ts);
            o.put("app", r.app == null ? "" : r.app.name());
            o.put("title", r.title);
            o.put("amount", r.amount);
            o.put("discretionary", r.discretionary);
            o.put("food", r.food);
            o.put("judgment", r.judgment == null ? "" : r.judgment.name());
            o.put("isFood", r.isFood);
            JSONArray t = new JSONArray();
            for (Trigger tr : r.triggers) t.put(tr.name());
            o.put("triggers", t);
            o.put("intensity", r.intensity == null ? "" : r.intensity.name());
            o.put("outcome", r.outcome.name());
            o.put("chip", r.chip == null ? "" : r.chip);
            o.put("stoppedAt", r.stoppedAt);
            JSONArray qa = new JSONArray();
            for (String s : r.qa) qa.put(s);
            o.put("qa", qa);
            o.put("lateNight", r.lateNight);
            o.put("review", r.review == null ? "" : r.review);
            o.put("interventionNo", r.interventionNo);
        } catch (JSONException ignored) {
        }
        return o;
    }

    static Record recordFromJson(JSONObject o) {
        Record r = new Record();
        r.id = o.optString("id", r.id);
        r.ts = o.optLong("ts", 0);
        r.app = TargetApp.fromName(o.optString("app", ""));
        r.title = o.optString("title", "");
        r.amount = o.optLong("amount", 0);
        r.discretionary = o.optLong("discretionary", 0);
        r.food = o.optLong("food", 0);
        String j = o.optString("judgment", "");
        r.judgment = j.isEmpty() ? null : Judgment.valueOf(j);
        r.isFood = o.optBoolean("isFood", false);
        JSONArray t = o.optJSONArray("triggers");
        if (t != null) for (int i = 0; i < t.length(); i++) {
            try {
                r.triggers.add(Trigger.valueOf(t.optString(i)));
            } catch (IllegalArgumentException ignored) {
            }
        }
        String it = o.optString("intensity", "");
        r.intensity = it.isEmpty() ? null : Intensity.valueOf(it);
        try {
            r.outcome = Outcome.valueOf(o.optString("outcome", "PASS"));
        } catch (IllegalArgumentException e) {
            r.outcome = Outcome.PASS;
        }
        String chip = o.optString("chip", "");
        r.chip = chip.isEmpty() ? null : chip;
        r.stoppedAt = o.optInt("stoppedAt", 0);
        JSONArray qa = o.optJSONArray("qa");
        if (qa != null) for (int i = 0; i < qa.length(); i++) r.qa.add(qa.optString(i));
        r.lateNight = o.optBoolean("lateNight", false);
        String rv = o.optString("review", "");
        r.review = rv.isEmpty() ? null : rv;
        r.interventionNo = o.optInt("interventionNo", 0);
        return r;
    }

    // ------------------------------------------------------------------ 로그 · 화면 덤프

    public synchronized void log(String msg) {
        String line = new SimpleDateFormat("HH:mm:ss", Locale.KOREA).format(new Date()) + "  " + msg;
        logs.add(line);
        while (logs.size() > 80) logs.remove(0);
        JSONArray a = new JSONArray();
        for (String s : logs) a.put(s);
        prefs.edit().putString("logs", a.toString()).apply();
    }

    public synchronized List<String> logs() {
        return new ArrayList<>(logs);
    }

    public synchronized void setLastScreen(TargetApp app, String text) {
        String old = lastScreen.get(app);
        if (text.equals(old)) return;
        lastScreen.put(app, text);
        prefs.edit().putString("screen_" + app.name(), text).apply();
    }

    public synchronized String lastScreen(TargetApp app) {
        String s = lastScreen.get(app);
        return s == null ? "" : s;
    }
}
