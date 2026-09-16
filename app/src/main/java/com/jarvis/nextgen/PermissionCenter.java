package com.jarvis.nextgen;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;
import java.util.LinkedHashMap;
import java.util.Map;

/** Read-only central permission status used by the agent and settings UI. */
public final class PermissionCenter {
    private final Context context;
    public PermissionCenter(Context context){ this.context=context.getApplicationContext(); }
    public Map<String,Boolean> status(){
        Map<String,Boolean> out=new LinkedHashMap<>();
        out.put("microphone", granted(Manifest.permission.RECORD_AUDIO));
        out.put("contacts", granted(Manifest.permission.READ_CONTACTS));
        out.put("phone", granted(Manifest.permission.CALL_PHONE));
        out.put("sms", granted(Manifest.permission.SEND_SMS));
        out.put("calendar_read", granted(Manifest.permission.READ_CALENDAR));
        out.put("calendar_write", granted(Manifest.permission.WRITE_CALENDAR));
        out.put("camera", granted(Manifest.permission.CAMERA));
        out.put("location", granted(Manifest.permission.ACCESS_FINE_LOCATION) || granted(Manifest.permission.ACCESS_COARSE_LOCATION));
        return out;
    }
    private boolean granted(String permission){ return ContextCompat.checkSelfPermission(context,permission)==PackageManager.PERMISSION_GRANTED; }
}
