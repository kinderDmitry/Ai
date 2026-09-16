package com.jarvis.nextgen;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.AlarmClock;
import android.provider.CalendarContract;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Real Android Calendar Provider + AlarmClock integration. No fabricated success. */
public final class CalendarRemindersTool implements Tool {
    private final Context c;
    private static final Pattern TIME = Pattern.compile("(?<!\\d)(\\d{1,2}):(\\d{2})(?!\\d)");

    public CalendarRemindersTool(Context context) { c = context.getApplicationContext(); }

    @Override public String name() { return "calendar_reminders"; }
    @Override public String description() {
        return "Read, search, create, update and delete real Android calendar events; create system reminders with explicit confirmation and verification where Android exposes it.";
    }

    @Override public boolean canHandle(String s) {
        String x = s.toLowerCase(Locale.ROOT);
        return x.contains("календар") || x.contains("событи") || x.contains("встреч") ||
                x.contains("напомни") || x.contains("напоминание") || x.contains("calendar") ||
                x.contains("reminder") || x.contains("event");
    }

    @Override public Models.ToolResult execute(String input) {
        String x = input.toLowerCase(Locale.ROOT).trim();
        if (isDelete(x)) return deleteEvent(input);
        if (isUpdate(x)) return updateEvent(input);
        if (isSearch(x)) return searchEvents(input);
        if (isList(x)) return listEvents(x.contains("завтра") || x.contains("tomorrow"));
        if (x.contains("напомни") || x.contains("напоминание") || x.contains("reminder")) return createReminder(input);
        if (x.contains("создай") || x.contains("добавь") || x.contains("создать") || x.contains("встреч")) return createEvent(input);
        return openCalendar();
    }

    private boolean isDelete(String x) {
        return x.contains("удали событие") || x.contains("удалить событие") || x.contains("удали встреч") ||
                x.contains("удалить встреч") || x.contains("delete event") || x.contains("remove event");
    }
    private boolean isUpdate(String x) {
        return x.contains("измени событие") || x.contains("изменить событие") || x.contains("перенеси встреч") ||
                x.contains("перенести встреч") || x.contains("измени встреч") || x.contains("update event") ||
                x.contains("reschedule event");
    }
    private boolean isSearch(String x) {
        return x.startsWith("найди событие") || x.startsWith("найди встреч") || x.startsWith("поиск события") ||
                x.startsWith("найти событие") || x.startsWith("search event") || x.startsWith("find event");
    }
    private boolean isList(String x) {
        return x.contains("что у меня") || x.contains("какие события") || x.contains("план") ||
                x.contains("сегодня") || x.contains("завтра") || x.contains("today") || x.contains("tomorrow");
    }

    private Models.ToolResult listEvents(boolean tomorrow) {
        if (!hasRead()) return Models.ToolResult.fail(Models.ResultCode.PERMISSION_REQUIRED, "Нужно разрешение на чтение календаря.");
        Calendar start = dayStart(tomorrow ? 1 : 0), end = (Calendar) start.clone(); end.add(Calendar.DAY_OF_YEAR, 1);
        String[] p = {CalendarContract.Instances.EVENT_ID, CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END, CalendarContract.Instances.ALL_DAY, CalendarContract.Instances.EVENT_LOCATION};
        StringBuilder out = new StringBuilder(); int n = 0;
        try (android.database.Cursor cur = CalendarContract.Instances.query(c.getContentResolver(), p, start.getTimeInMillis(), end.getTimeInMillis())) {
            while (cur != null && cur.moveToNext() && n < 30) {
                long begin = cur.getLong(2); boolean allDay = cur.getInt(4) != 0;
                String title = safe(cur.getString(1), "Без названия");
                String time = allDay ? "весь день" : new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(begin));
                out.append("• ").append(time).append(" — ").append(title);
                String location = cur.getString(5); if (location != null && !location.isBlank()) out.append(" (" + location + ")");
                out.append('\n'); n++;
            }
        } catch (Exception e) { return Models.ToolResult.fail(Models.ResultCode.FAILED, "Не удалось прочитать календарь."); }
        String day = tomorrow ? "Завтра" : "Сегодня";
        return Models.ToolResult.ok(n == 0 ? day + " событий не найдено." : day + " в календаре:\n" + out.toString().trim());
    }

    private Models.ToolResult searchEvents(String input) {
        if (!hasRead()) return Models.ToolResult.fail(Models.ResultCode.PERMISSION_REQUIRED, "Нужно разрешение на чтение календаря.");
        String q = input.replaceFirst("(?is)^(найди|найти|поиск|search|find)\\s+(событие|события|встречу|встреч|event)\\s*", "").trim();
        if (q.isBlank()) return Models.ToolResult.fail(Models.ResultCode.FAILED, "Укажите название события для поиска.");
        Calendar from = Calendar.getInstance(); from.add(Calendar.DAY_OF_YEAR, -365);
        Calendar to = Calendar.getInstance(); to.add(Calendar.DAY_OF_YEAR, 365);
        String[] p = {CalendarContract.Instances.EVENT_ID, CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END, CalendarContract.Instances.EVENT_LOCATION};
        StringBuilder out = new StringBuilder(); int n = 0;
        try (android.database.Cursor cur = CalendarContract.Instances.query(c.getContentResolver(), p, from.getTimeInMillis(), to.getTimeInMillis())) {
            while (cur != null && cur.moveToNext() && n < 20) {
                String title = safe(cur.getString(1), "Без названия");
                if (!title.toLowerCase(Locale.ROOT).contains(q.toLowerCase(Locale.ROOT))) continue;
                String when = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(new Date(cur.getLong(2)));
                out.append("• ").append(when).append(" — ").append(title);
                String loc = cur.getString(4); if (loc != null && !loc.isBlank()) out.append(" (" + loc + ")");
                out.append('\n'); n++;
            }
        } catch (Exception e) { return Models.ToolResult.fail(Models.ResultCode.FAILED, "Не удалось выполнить поиск в календаре."); }
        return Models.ToolResult.ok(n == 0 ? "Совпадений не найдено." : "Найдено:\n" + out.toString().trim());
    }

    private Models.ToolResult createEvent(String input) {
        if (!hasWrite()) return Models.ToolResult.fail(Models.ResultCode.PERMISSION_REQUIRED, "Нужно разрешение на изменение календаря.");
        Matcher tm = TIME.matcher(input); if (!tm.find()) return Models.ToolResult.fail(Models.ResultCode.FAILED, "Укажите время, например 18:30.");
        int h = Integer.parseInt(tm.group(1)), min = Integer.parseInt(tm.group(2)); if (!validTime(h,min)) return badTime();
        boolean tomorrow = hasAny(input, "завтра", "tomorrow");
        Calendar start = dayStart(tomorrow ? 1 : 0); start.set(Calendar.HOUR_OF_DAY,h); start.set(Calendar.MINUTE,min);
        if (!tomorrow && start.getTimeInMillis() <= System.currentTimeMillis()) start.add(Calendar.DAY_OF_YEAR,1);
        final long begin = start.getTimeInMillis(); final String title = cleanCreateTitle(input);
        if (title.isBlank()) return Models.ToolResult.fail(Models.ResultCode.FAILED, "Не указано название события.");
        final String when = (tomorrow ? "завтра " : "") + String.format(Locale.ROOT,"%02d:%02d",h,min);
        return Models.ToolResult.confirm("Создать событие «" + title + "» на " + when + "?", () -> {
            long calendarId = findWritableCalendar(); if (calendarId < 0) throw new IllegalStateException("Нет доступного для записи календаря.");
            long end = begin + 60*60*1000L; if (hasConflict(begin,end)) throw new IllegalStateException("На это время уже есть событие в календаре.");
            ContentValues v = new ContentValues(); v.put(CalendarContract.Events.DTSTART,begin); v.put(CalendarContract.Events.DTEND,end);
            v.put(CalendarContract.Events.TITLE,title); v.put(CalendarContract.Events.CALENDAR_ID,calendarId); v.put(CalendarContract.Events.EVENT_TIMEZONE,TimeZone.getDefault().getID());
            android.net.Uri uri = c.getContentResolver().insert(CalendarContract.Events.CONTENT_URI,v); if(uri==null) throw new IllegalStateException("Android не создал событие.");
            long id=Long.parseLong(uri.getLastPathSegment()); verifyEventExists(id);
        });
    }

    private Models.ToolResult updateEvent(String input) {
        if (!hasWrite() || !hasRead()) return Models.ToolResult.fail(Models.ResultCode.PERMISSION_REQUIRED, "Нужно разрешение на чтение и изменение календаря.");
        Matcher tm = TIME.matcher(input); if (!tm.find()) return Models.ToolResult.fail(Models.ResultCode.FAILED,"Укажите новое время, например 18:30.");
        int h=Integer.parseInt(tm.group(1)), min=Integer.parseInt(tm.group(2)); if(!validTime(h,min)) return badTime();
        String title = extractTargetTitle(input); if(title.isBlank()) return Models.ToolResult.fail(Models.ResultCode.FAILED,"Укажите название события.");
        List<EventRef> matches=findByTitle(title);
        if(matches.isEmpty()) return Models.ToolResult.fail(Models.ResultCode.FAILED,"Событие «"+title+"» не найдено.");
        if(matches.size()>1) return Models.ToolResult.fail(Models.ResultCode.FAILED, "Найдено несколько событий «"+title+"». Уточните дату.");
        EventRef event=matches.get(0); boolean tomorrow=hasAny(input,"завтра","tomorrow"); Calendar target=dayStart(tomorrow?1:0); target.set(Calendar.HOUR_OF_DAY,h); target.set(Calendar.MINUTE,min);
        if(!tomorrow && target.getTimeInMillis()<=System.currentTimeMillis()) target.add(Calendar.DAY_OF_YEAR,1);
        final long newBegin=target.getTimeInMillis(), id=event.id, newEnd=newBegin+(event.end-event.begin);
        return Models.ToolResult.confirm("Перенести «"+event.title+"» на "+(tomorrow?"завтра ":"")+String.format(Locale.ROOT,"%02d:%02d",h,min)+"?", () -> {
            if(hasConflictExcluding(id,newBegin,newEnd)) throw new IllegalStateException("На новое время уже есть событие в календаре.");
            ContentValues v=new ContentValues(); v.put(CalendarContract.Events.DTSTART,newBegin); v.put(CalendarContract.Events.DTEND,newEnd); v.put(CalendarContract.Events.EVENT_TIMEZONE,TimeZone.getDefault().getID());
            int changed=c.getContentResolver().update(CalendarContract.Events.CONTENT_URI,v,CalendarContract.Events._ID+"=?",new String[]{String.valueOf(id)}); if(changed!=1) throw new IllegalStateException("Событие не было изменено.");
            verifyEventExists(id);
        });
    }

    private Models.ToolResult deleteEvent(String input) {
        if (!hasWrite() || !hasRead()) return Models.ToolResult.fail(Models.ResultCode.PERMISSION_REQUIRED, "Нужно разрешение на чтение и изменение календаря.");
        String title=extractTargetTitle(input); if(title.isBlank()) return Models.ToolResult.fail(Models.ResultCode.FAILED,"Укажите название события.");
        List<EventRef> matches=findByTitle(title); if(matches.isEmpty()) return Models.ToolResult.fail(Models.ResultCode.FAILED,"Событие «"+title+"» не найдено.");
        if(matches.size()>1) return Models.ToolResult.fail(Models.ResultCode.FAILED,"Найдено несколько событий «"+title+"». Уточните дату.");
        EventRef e=matches.get(0);
        return Models.ToolResult.confirm("Удалить событие «"+e.title+"» ("+formatDate(e.begin)+")?", () -> {
            int deleted=c.getContentResolver().delete(CalendarContract.Events.CONTENT_URI,CalendarContract.Events._ID+"=?",new String[]{String.valueOf(e.id)});
            if(deleted!=1) throw new IllegalStateException("Событие не было удалено.");
            try(android.database.Cursor cur=c.getContentResolver().query(CalendarContract.Events.CONTENT_URI,new String[]{CalendarContract.Events._ID},CalendarContract.Events._ID+"=?",new String[]{String.valueOf(e.id)},null)){
                if(cur!=null && cur.moveToFirst()) throw new IllegalStateException("Событие удалено, но проверка показала, что оно всё ещё существует.");
            }catch(android.database.SQLException ex){throw new IllegalStateException("Не удалось проверить удаление события.");}
        });
    }

    private String extractTargetTitle(String input) {
        String s=input.trim();
        s=s.replaceFirst("(?is)^(измени|изменить|перенеси|перенести|удали|удалить|update|reschedule|delete|remove)\\s+","");
        s=s.replaceFirst("(?is)^(событие|события|встречу|встреча|event)\\s*","");
        s=s.replaceFirst("(?is)\\s+(?:на|at)\\s+\\d{1,2}:\\d{2}.*$","");
        s=s.replaceFirst("(?is)\\s+(?:завтра|tomorrow).*$","");
        s=s.replaceFirst("(?is)\\s+(?:в|at)\\s+\\d{1,2}:\\d{2}.*$","");
        return s.trim().replaceAll("[«»\"']","");
    }

    private List<EventRef> findByTitle(String title) {
        List<EventRef> result=new ArrayList<>(); Calendar from=Calendar.getInstance(); from.add(Calendar.DAY_OF_YEAR,-365); Calendar to=Calendar.getInstance(); to.add(Calendar.DAY_OF_YEAR,365);
        String[] p={CalendarContract.Instances.EVENT_ID,CalendarContract.Instances.TITLE,CalendarContract.Instances.BEGIN,CalendarContract.Instances.END};
        try(android.database.Cursor cur=CalendarContract.Instances.query(c.getContentResolver(),p,from.getTimeInMillis(),to.getTimeInMillis())){
            while(cur!=null&&cur.moveToNext()&&result.size()<10){String t=safe(cur.getString(1),""); if(t.equalsIgnoreCase(title)||t.toLowerCase(Locale.ROOT).contains(title.toLowerCase(Locale.ROOT))) result.add(new EventRef(cur.getLong(0),t,cur.getLong(2),cur.getLong(3)));}
        }catch(Exception ignored){}
        LinkedHashMap<Long,EventRef> unique=new LinkedHashMap<>(); for(EventRef e:result)unique.put(e.id,e); return new ArrayList<>(unique.values());
    }

    private long findWritableCalendar() {
        String[] p={CalendarContract.Calendars._ID,CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,CalendarContract.Calendars.VISIBLE};
        try(android.database.Cursor cur=c.getContentResolver().query(CalendarContract.Calendars.CONTENT_URI,p,CalendarContract.Calendars.VISIBLE+"=1",null,CalendarContract.Calendars._ID+" ASC")){
            while(cur!=null&&cur.moveToNext()) if(cur.getInt(1)>=CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR)return cur.getLong(0);
        }catch(Exception ignored){} return -1;
    }
    private boolean hasConflict(long begin,long end){return hasConflictExcluding(-1,begin,end);}
    private boolean hasConflictExcluding(long excluded,long begin,long end){
        String[] p={CalendarContract.Instances.EVENT_ID,CalendarContract.Instances.BEGIN,CalendarContract.Instances.END};
        try(android.database.Cursor cur=CalendarContract.Instances.query(c.getContentResolver(),p,begin,end)){
            while(cur!=null&&cur.moveToNext()) if(cur.getLong(0)!=excluded)return true;
        }catch(Exception ignored){} return false;
    }
    private void verifyEventExists(long id){
        try(android.database.Cursor cur=c.getContentResolver().query(CalendarContract.Events.CONTENT_URI,new String[]{CalendarContract.Events._ID},CalendarContract.Events._ID+"=?",new String[]{String.valueOf(id)},null)){
            if(cur==null||!cur.moveToFirst())throw new IllegalStateException("Изменение создано, но не прошло проверку.");
        }catch(android.database.SQLException e){throw new IllegalStateException("Не удалось проверить событие.");}
    }

    private Models.ToolResult createReminder(String input){
        Matcher m=Pattern.compile("(?i)(?:в|на|at)\\s*(\\d{1,2})(?::(\\d{2}))?").matcher(input);
        if(!m.find())return Models.ToolResult.fail(Models.ResultCode.FAILED,"Укажите время напоминания, например: «напомни в 19:30 позвонить».");
        int h=Integer.parseInt(m.group(1)),min=m.group(2)==null?0:Integer.parseInt(m.group(2)); if(!validTime(h,min))return badTime();
        String text=input.replaceFirst("(?is).*?(?:напомни|напоминание|reminder)\\s*","").replaceFirst("(?is)(?:в|на|at)\\s*\\d{1,2}(?::\\d{2})?","").trim();
        if(text.isBlank())return Models.ToolResult.fail(Models.ResultCode.FAILED,"Что именно нужно напомнить?"); final String message=text;
        if(c.getPackageManager().resolveActivity(new Intent(AlarmClock.ACTION_SET_ALARM),0)==null)return Models.ToolResult.fail(Models.ResultCode.UNSUPPORTED,"На этом устройстве нет приложения, которое принимает системные будильники.");
        return Models.ToolResult.confirm("Поставить напоминание на "+String.format(Locale.ROOT,"%02d:%02d",h,min)+": «"+message+"»?",()->{
            Intent in=new Intent(AlarmClock.ACTION_SET_ALARM).putExtra(AlarmClock.EXTRA_HOUR,h).putExtra(AlarmClock.EXTRA_MINUTES,min).putExtra(AlarmClock.EXTRA_MESSAGE,message).putExtra(AlarmClock.EXTRA_SKIP_UI,false).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); c.startActivity(in);
        });
    }

    private Models.ToolResult openCalendar(){Intent in=new Intent(Intent.ACTION_VIEW,CalendarContract.CONTENT_URI).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);try{c.startActivity(in);return Models.ToolResult.ok("Открыл календарь.");}catch(Exception e){return Models.ToolResult.fail(Models.ResultCode.UNSUPPORTED,"Не найдено приложение календаря.");}}
    private boolean hasRead(){return ContextCompat.checkSelfPermission(c,Manifest.permission.READ_CALENDAR)==PackageManager.PERMISSION_GRANTED;}
    private boolean hasWrite(){return ContextCompat.checkSelfPermission(c,Manifest.permission.WRITE_CALENDAR)==PackageManager.PERMISSION_GRANTED;}
    private Calendar dayStart(int add){Calendar x=Calendar.getInstance();x.add(Calendar.DAY_OF_YEAR,add);x.set(Calendar.HOUR_OF_DAY,0);x.set(Calendar.MINUTE,0);x.set(Calendar.SECOND,0);x.set(Calendar.MILLISECOND,0);return x;}
    private boolean validTime(int h,int m){return h>=0&&h<=23&&m>=0&&m<=59;}
    private Models.ToolResult badTime(){return Models.ToolResult.fail(Models.ResultCode.FAILED,"Время указано некорректно.");}
    private boolean hasAny(String s,String... values){String x=s.toLowerCase(Locale.ROOT);for(String v:values)if(x.contains(v))return true;return false;}
    private String safe(String s,String fallback){return s==null||s.isBlank()?fallback:s;}
    private String formatDate(long ms){return new SimpleDateFormat("dd.MM.yyyy HH:mm",Locale.getDefault()).format(new Date(ms));}
    private String cleanCreateTitle(String input){String s=input.replaceFirst("(?is).*?(создай|добавь|создать)\\s*(событие|встречу)?\\s*","");s=s.replaceFirst("(?i)\\bзавтра\\b","").replaceFirst("(?i)\\btomorrow\\b","").replaceFirst("(?i)\\b(?:в|at)\\s*\\d{1,2}:\\d{2}","");return s.trim();}
    private record EventRef(long id,String title,long begin,long end){}
}
