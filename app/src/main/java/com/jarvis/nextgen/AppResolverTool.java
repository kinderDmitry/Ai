package com.jarvis.nextgen;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import java.util.*;

/** Resolves installed apps by their real Android application label and launches the selected package. */
public final class AppResolverTool implements Tool {
    private final Context context;
    public AppResolverTool(Context context) { this.context = context.getApplicationContext(); }
    @Override public String name() { return "app_resolver"; }
    @Override public String description() { return "Find an installed Android app by its real label/package and launch it."; }
    @Override public boolean canHandle(String input) {
        String x = input == null ? "" : input.toLowerCase(Locale.ROOT);
        return (x.contains("открой") || x.contains("запусти") || x.contains("open") || x.contains("launch"))
                && !x.contains("сайт") && !x.contains("браузер");
    }
    @Override public Models.ToolResult execute(String input) {
        String query = input.replaceFirst("(?is).*?(открой|запусти|open|launch)\\s*", "").trim();
        if (query.isBlank()) return Models.ToolResult.fail(Models.ResultCode.FAILED, "Какое приложение открыть?");
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        List<ApplicationInfo> matches = new ArrayList<>();
        String q = query.toLowerCase(Locale.ROOT);
        for (ApplicationInfo app : apps) {
            CharSequence label = pm.getApplicationLabel(app);
            String labelText = label == null ? "" : label.toString();
            if (labelText.toLowerCase(Locale.ROOT).contains(q) || app.packageName.toLowerCase(Locale.ROOT).contains(q)) matches.add(app);
            if (matches.size() >= 5) break;
        }
        if (matches.isEmpty()) return Models.ToolResult.fail(Models.ResultCode.FAILED, "Установленное приложение «" + query + "» не найдено.");
        if (matches.size() > 1) {
            StringBuilder b = new StringBuilder("Нашёл несколько приложений: ");
            for (int i = 0; i < matches.size(); i++) {
                if (i > 0) b.append(", ");
                b.append(pm.getApplicationLabel(matches.get(i)));
            }
            return Models.ToolResult.fail(Models.ResultCode.FAILED, b + ". Уточните название.");
        }
        ApplicationInfo target = matches.get(0);
        Intent launch = pm.getLaunchIntentForPackage(target.packageName);
        if (launch == null) return Models.ToolResult.fail(Models.ResultCode.UNSUPPORTED, "У приложения нет доступного экрана запуска.");
        String label = String.valueOf(pm.getApplicationLabel(target));
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return Models.ToolResult.confirm("Открыть «" + label + "»?", () -> {
            try { context.startActivity(launch); }
            catch (Exception e) { throw new IllegalStateException("Android не разрешил запуск «" + label + "»."); }
        });
    }
}
