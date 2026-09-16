package com.jarvis.nextgen;

import android.app.*;import android.content.*;import android.os.Build;import java.util.*;

/** User-controlled briefing schedule with explicit enable/disable and no invented data. */
public final class ProactiveScheduler {
 private final Context c; private final AlarmManager alarm; private final android.content.SharedPreferences p; private static final int ID=81422;
 public ProactiveScheduler(Context c){this.c=c.getApplicationContext();alarm=(AlarmManager)this.c.getSystemService(Context.ALARM_SERVICE);p=this.c.getSharedPreferences("proactive_schedule",0);}
 public void setMorningTime(int hour,int minute){p.edit().putInt("hour",Math.max(0,Math.min(23,hour))).putInt("minute",Math.max(0,Math.min(59,minute))).apply();if(p.getBoolean("enabled",false))schedule();}
 public void setEnabled(boolean enabled){p.edit().putBoolean("enabled",enabled).apply();if(enabled)schedule();else cancel();}
 public boolean enabled(){return p.getBoolean("enabled",false);} public String time(){return String.format(Locale.ROOT,"%02d:%02d",p.getInt("hour",8),p.getInt("minute",0));}
 public void schedule(){if(alarm==null||!enabled())return;Calendar n=Calendar.getInstance();n.set(Calendar.HOUR_OF_DAY,p.getInt("hour",8));n.set(Calendar.MINUTE,p.getInt("minute",0));n.set(Calendar.SECOND,0);n.set(Calendar.MILLISECOND,0);if(n.getTimeInMillis()<=System.currentTimeMillis())n.add(Calendar.DAY_OF_YEAR,1);Intent i=new Intent(c,ProactiveReceiver.class).setAction(ProactiveReceiver.ACTION).putExtra("type","MORNING");PendingIntent pi=PendingIntent.getBroadcast(c,ID,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP,n.getTimeInMillis(),AlarmManager.INTERVAL_DAY,pi);}
 public void cancel(){if(alarm==null)return;Intent i=new Intent(c,ProactiveReceiver.class).setAction(ProactiveReceiver.ACTION);PendingIntent pi=PendingIntent.getBroadcast(c,ID,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);alarm.cancel(pi);pi.cancel();}
}
