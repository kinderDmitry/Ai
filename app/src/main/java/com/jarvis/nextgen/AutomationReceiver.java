package com.jarvis.nextgen;

import android.app.*;import android.content.*;import androidx.core.app.NotificationCompat;import org.json.JSONObject;

public final class AutomationReceiver extends BroadcastReceiver {
    public static final String ACTION_FIRE="com.jarvis.nextgen.AUTOMATION_FIRE";
    @Override public void onReceive(Context context,Intent intent){
        if(!ACTION_FIRE.equals(intent.getAction()))return;
        String id=intent.getStringExtra("automation_id"); if(id==null)return;
        AutomationEngine e=new AutomationEngine(context); JSONObject a=e.get(id); if(a==null||!a.optBoolean("enabled",false))return;
        String action=a.optString("action","").trim(); if(action.isEmpty())return;
        NotificationManager nm=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE); String channel="jarvis_automation";
        if(android.os.Build.VERSION.SDK_INT>=26)nm.createNotificationChannel(new NotificationChannel(channel,"JARVIS Automations",NotificationManager.IMPORTANCE_DEFAULT));
        Notification n=new NotificationCompat.Builder(context,channel).setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("JARVIS").setContentText(action).setAutoCancel(true).build();
        nm.notify(Math.abs(id.hashCode()),n);
    }
}
