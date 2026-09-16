package com.jarvis.nextgen;

import android.Manifest;import android.content.Context;import android.content.pm.PackageManager;import java.util.*;

/** User-visible mapping between tools and Android permissions. */
public final class ToolPermissionMatrix {
 private final Context c; public ToolPermissionMatrix(Context c){this.c=c.getApplicationContext();}
 public Map<String,List<String>> mapping(){Map<String,List<String>> m=new LinkedHashMap<>();m.put("VOICE",List.of(Manifest.permission.RECORD_AUDIO));m.put("CONTACTS",List.of(Manifest.permission.READ_CONTACTS));m.put("PHONE",List.of(Manifest.permission.CALL_PHONE));m.put("SMS",List.of(Manifest.permission.SEND_SMS));m.put("CALENDAR",List.of(Manifest.permission.READ_CALENDAR,Manifest.permission.WRITE_CALENDAR));m.put("LOCATION",List.of(Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION));m.put("CAMERA",List.of(Manifest.permission.CAMERA));m.put("NOTIFICATIONS",List.of(Manifest.permission.POST_NOTIFICATIONS));return m;}
 public boolean granted(String permission){return android.os.Build.VERSION.SDK_INT<23||c.checkSelfPermission(permission)==PackageManager.PERMISSION_GRANTED;}
 public Map<String,Boolean> status(){Map<String,Boolean> out=new LinkedHashMap<>();for(Map.Entry<String,List<String>> e:mapping().entrySet()){boolean ok=true;for(String p:e.getValue())if(!granted(p)){ok=false;break;}out.put(e.getKey(),ok);}return out;}
}
