package com.jarvis.nextgen;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Real location/map integration using Android LocationManager and external map apps. */
public final class LocationMapsTool implements Tool {
    private final Context context;
    public LocationMapsTool(Context context) { this.context = context.getApplicationContext(); }

    @Override public String name() { return "location_maps"; }
    @Override public String description() { return "Use real device location and open real map searches or navigation intents."; }

    @Override public boolean canHandle(String input) {
        String x = input == null ? "" : input.toLowerCase(Locale.ROOT);
        return x.contains("где я") || x.contains("местополож") || x.contains("координат") ||
                x.contains("маршрут") || x.contains("построй маршрут") || x.contains("проложи маршрут") ||
                x.contains("ближайш") || x.contains("заправк") || x.contains("аптек") ||
                x.contains("ресторан") || x.contains("магазин") || x.contains("карты") || x.contains("maps") ||
                x.contains("найди место");
    }

    @Override public Models.ToolResult execute(String input) {
        String x = input == null ? "" : input.trim();
        String lower = x.toLowerCase(Locale.ROOT);
        boolean needsLocation = lower.contains("где я") || lower.contains("местополож") || lower.contains("координат") ||
                lower.contains("ближайш") || lower.contains("заправк") || lower.contains("аптек") ||
                lower.contains("ресторан") || lower.contains("магазин") || lower.contains("построй маршрут") || lower.contains("проложи маршрут");
        Location location = needsLocation ? getLastKnownLocation() : null;
        if (needsLocation && location == null) {
            if (!hasLocationPermission()) return Models.ToolResult.fail(Models.ResultCode.PERMISSION_REQUIRED, "Нужен доступ к местоположению, чтобы определить ваше положение.");
            return Models.ToolResult.fail(Models.ResultCode.FAILED, "Текущее местоположение пока недоступно. Включите геолокацию и попробуйте ещё раз.");
        }

        if (lower.contains("где я") || lower.contains("местополож") || lower.contains("координат")) {
            return Models.ToolResult.ok(formatLocation(location));
        }

        String destination = extractDestination(x);
        boolean nearest = lower.contains("ближайш") || lower.contains("заправк") || lower.contains("аптек") || lower.contains("ресторан") || lower.contains("магазин");
        if (nearest) {
            String category = categoryFor(lower);
            String query = category + (destination.isBlank() ? "" : " " + destination);
            return openGeo(location, query, "Поиск «" + query + "» открыт на карте.");
        }

        if (lower.contains("маршрут") || lower.contains("карты") || lower.contains("maps")) {
            if (destination.isBlank()) return Models.ToolResult.fail(Models.ResultCode.FAILED, "Куда построить маршрут?");
            return openDirections(location, destination);
        }

        if (destination.isBlank()) return Models.ToolResult.fail(Models.ResultCode.FAILED, "Не удалось определить место для поиска.");
        return openGeo(location, destination, "Поиск места открыт на карте.");
    }

    private boolean hasLocationPermission() {
        return context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private Location getLastKnownLocation() {
        if (!hasLocationPermission()) return null;
        try {
            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (lm == null) return null;
            Location best = null;
            for (String provider : new String[]{LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER}) {
                try {
                    Location l = lm.getLastKnownLocation(provider);
                    if (l != null && (best == null || l.getTime() > best.getTime())) best = l;
                } catch (SecurityException ignored) { }
            }
            return best;
        } catch (Exception ignored) { return null; }
    }

    private String formatLocation(Location l) {
        String place = reverseGeocode(l);
        return place.isBlank() ? String.format(Locale.ROOT, "Текущие координаты: %.6f, %.6f.", l.getLatitude(), l.getLongitude())
                : place + "\nКоординаты: " + String.format(Locale.ROOT, "%.6f, %.6f", l.getLatitude(), l.getLongitude());
    }

    private String reverseGeocode(Location l) {
        if (!Geocoder.isPresent()) return "";
        try {
            List<Address> a = new Geocoder(context, Locale.getDefault()).getFromLocation(l.getLatitude(), l.getLongitude(), 1);
            if (a != null && !a.isEmpty()) {
                Address x = a.get(0);
                String line = x.getAddressLine(0);
                if (line != null && !line.isBlank()) return line;
                String city = x.getLocality();
                if (city != null && !city.isBlank()) return city;
            }
        } catch (IOException | IllegalArgumentException ignored) { }
        return "";
    }

    private String categoryFor(String lower) {
        if (lower.contains("заправк")) return "заправка";
        if (lower.contains("аптек")) return "аптека";
        if (lower.contains("ресторан")) return "ресторан";
        if (lower.contains("магазин")) return "магазин";
        return "места";
    }

    private String extractDestination(String input) {
        String s = input.replaceFirst("(?is)^.*?(найди|покажи|открой|маршрут|построй маршрут|проложи маршрут|ближайший|ближайшую|ближайшее|карты|maps)\\s*", "").trim();
        s = s.replaceFirst("(?is)\\b(до|в|на)\\s+", "").trim();
        if (s.equalsIgnoreCase("домой")) return "домой";
        Matcher m = Pattern.compile("(?is)\\bдо\\s+(.+)$").matcher(input);
        if (m.find()) return m.group(1).trim();
        return s;
    }

    private Models.ToolResult openGeo(Location l, String query, String ok) {
        String uri;
        if (l != null) uri = "geo:" + l.getLatitude() + "," + l.getLongitude() + "?q=" + Uri.encode(query);
        else uri = "geo:0,0?q=" + Uri.encode(query);
        return open(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)), ok);
    }

    private Models.ToolResult openDirections(Location l, String destination) {
        String origin = l == null ? "" : "&origin=" + Uri.encode(l.getLatitude() + "," + l.getLongitude());
        Uri uri = Uri.parse("https://www.google.com/maps/dir/?api=1" + origin + "&destination=" + Uri.encode(destination) + "&travelmode=driving");
        return open(new Intent(Intent.ACTION_VIEW, uri), "Маршрут открыт в доступном приложении карт.");
    }

    private Models.ToolResult open(Intent intent, String ok) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (intent.resolveActivity(context.getPackageManager()) == null)
            return Models.ToolResult.fail(Models.ResultCode.UNSUPPORTED, "На устройстве нет приложения, которое может открыть карту для этого действия.");
        try { context.startActivity(intent); return Models.ToolResult.ok(ok); }
        catch (Exception e) { return Models.ToolResult.fail(Models.ResultCode.FAILED, "Android не разрешил открыть карту."); }
    }
}
