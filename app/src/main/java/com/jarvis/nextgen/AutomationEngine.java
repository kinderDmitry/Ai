package com.jarvis.nextgen;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import org.json.JSONObject;
import java.util.*;

/** Durable automation store and scheduler. No simulated execution: AlarmManager is the system scheduler. */
public final class AutomationEngine {
    private final Context c;
    private final android.content.SharedPreferences p;
    private final AlarmManager alarm;
    public AutomationEngine(Context c){this.c=c.getApplicationContext();p=this.c.getSharedPreferences("automations",0);alarm=(AlarmManager)this.c.getSystemService(Context.ALARM_SERVICE);}
    public synchronized void put(String id,String trigger,String condition,String action){
        JSONObject o=new JSONObject(); o.put("id",id).put("trigger",trigger).put("condition",condition).put("action",action).put("enabled",true).put("createdAt",System.currentTimeMillis());
        p.edit().putString(id,o.toString()).apply();
    }
    public synchronized void remove(String id){cancel(id);p.edit().remove(id).apply();}
    public synchronized void setEnabled(String id,boolean enabled){String raw=p.getString(id,null);if(raw==null)return;JSONObject o=new JSONObject(raw).put("enabled",enabled);p.edit().putString(id,o.toString()).apply();if(!enabled)cancel(id);}
    public synchronized Map<String,?> all(){return Collections.unmodifiableMap(p.getAll());}
    public synchronized JSONObject get(String id){String raw=p.getString(id,null);return raw==null?null:new JSONObject(raw);}
    public void scheduleDaily(String id,int hour,int minute){
        if(alarm==null)throw new IllegalStateException("AlarmManager недоступен");
        Calendar n=Calendar.getInstance();n.set(Calendar.HOUR_OF_DAY,hour);n.set(Calendar.MINUTE,minute);n.set(Calendar.SECOND,0);n.set(Calendar.MILLISECOND,0);if(n.getTimeInMillis()<=System.currentTimeMillis())n.add(Calendar.DAY_OF_YEAR,1);
        Intent i=new Intent(c,AutomationReceiver.class).setAction(AutomationReceiver.ACTION_FIRE).putExtra("automation_id",id);
        PendingIntent pi=PendingIntent.getBroadcast(c,stableCode(id),i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP,n.getTimeInMillis(),AlarmManager.INTERVAL_DAY,pi);
    }
    public void cancel(String id){if(alarm==null)return;Intent i=new Intent(c,AutomationReceiver.class).setAction(AutomationReceiver.ACTION_FIRE).putExtra("automation_id",id);PendingIntent pi=PendingIntent.getBroadcast(c,stableCode(id),i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);alarm.cancel(pi);pi.cancel();}
    private int stableCode(String id){return Math.abs(id.hashCode());}
}
