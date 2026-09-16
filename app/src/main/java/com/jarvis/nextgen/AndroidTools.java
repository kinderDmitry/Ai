package com.jarvis.nextgen;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.AlarmClock;
import android.provider.CalendarContract;
import android.provider.Settings;
import android.telephony.PhoneNumberUtils;
import com.jarvis.nextgen.*;
import java.util.*;
import java.util.regex.*;

public final class AndroidTools {
    private final Context c;
    public AndroidTools(Context c) { this.c = c.getApplicationContext(); }

    public ToolRegistry registry() {
        ToolRegistry r = new ToolRegistry();
        r.add(new AppResolverTool(c)); r.add(new ContactIntelligenceTool(c));
        r.add(new LocationMapsTool(c));
        r.add(new ImageIntelligenceTool(c));
        r.add(new DocumentIntelligenceTool(c));
        r.add(new WebResearchTool(c)); r.add(new AutomationTool(c)); r.add(new CalendarRemindersTool(c)); r.add(new ProactiveTool(c)); r.add(apps()); r.add(contacts()); r.add(timer()); r.add(web()); r.add(settings());
        r.add(new TelegramCommunicationTool(c)); r.add(new SmsCommunicationTool(c)); r.add(communication()); r.add(phone()); r.add(sms()); r.add(telegram()); r.add(weather()); r.add(memory()); r.add(calculator()); r.add(camera());
        r.add(filePicker()); r.add(documentPicker()); r.add(imagePicker()); r.add(notificationAccess()); r.add(wifiSettings()); r.add(bluetoothSettings()); r.add(volumeSettings()); r.add(flashlight());
        return r;
    }

    private Tool apps() { return new Tool() {
        public String name(){return "apps";} public String description(){return "Launch installed applications";}
        public boolean canHandle(String s){return s.matches("(?is).*(открой|запусти|open|launch).*\\b(telegram|телеграм|youtube|ютуб|whatsapp|ватсап|chrome|браузер|maps|карты)\\b.*");}
        public Models.ToolResult execute(String s){
            String x=s.toLowerCase(Locale.ROOT); String[][] apps={{"telegram","org.telegram.messenger"},{"телеграм","org.telegram.messenger"},{"youtube","com.google.android.youtube"},{"ютуб","com.google.android.youtube"},{"whatsapp","com.whatsapp"},{"ватсап","com.whatsapp"},{"chrome","com.android.chrome"}};
            for(String[] a:apps) if(x.contains(a[0])) { Intent in=c.getPackageManager().getLaunchIntentForPackage(a[1]); if(in==null)return Models.ToolResult.fail(Models.ResultCode.FAILED,"Приложение не установлено или недоступно."); in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); try{c.startActivity(in);return Models.ToolResult.ok("Открыл "+a[0]+".");}catch(Exception e){return Models.ToolResult.fail(Models.ResultCode.FAILED,"Android не разрешил запуск приложения.");}}
            return Models.ToolResult.fail(Models.ResultCode.UNSUPPORTED,"Не удалось однозначно определить приложение.");
        }
    };}

    private Tool timer() { return new Tool() {
        public String name(){return "timer";} public String description(){return "Create Android timer";}
        public boolean canHandle(String s){String x=s.toLowerCase(Locale.ROOT);return x.contains("таймер")||x.contains("timer");}
        public Models.ToolResult execute(String s){
            Matcher m=Pattern.compile("(\\d+)\\s*(секунд|сек|минут|мин|час|ч)",Pattern.CASE_INSENSITIVE).matcher(s.toLowerCase(Locale.ROOT));
            if(!m.find())return Models.ToolResult.fail(Models.ResultCode.FAILED,"Сколько времени поставить на таймер?");
            long n=Long.parseLong(m.group(1)); String u=m.group(2); long sec=n*(u.startsWith("час")||u.equals("ч")?3600:u.startsWith("мин")?60:1);
            if(sec>Integer.MAX_VALUE)return Models.ToolResult.fail(Models.ResultCode.FAILED,"Слишком большой интервал.");
            Intent in=new Intent(AlarmClock.ACTION_SET_TIMER).putExtra(AlarmClock.EXTRA_LENGTH,(int)sec).putExtra(AlarmClock.EXTRA_SKIP_UI,false).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try{c.startActivity(in);return Models.ToolResult.ok("Открыл системный таймер на "+n+" "+u+".");}catch(Exception e){return Models.ToolResult.fail(Models.ResultCode.UNSUPPORTED,"На этом устройстве нет приложения, которое может установить таймер.");}
        }
    };}

    private Tool web() { return new Tool() {
        public String name(){return "web_search";} public String description(){return "Open a real web search";}
        public boolean canHandle(String s){String x=s.toLowerCase(Locale.ROOT);return x.contains("найди в интернете")||x.startsWith("поищи")||x.contains("в интернете")||x.contains("search web");}
        public Models.ToolResult execute(String s){String q=s.replaceFirst("(?is).*?(найди в интернете|поищи|search web)","").trim();if(q.isEmpty())return Models.ToolResult.fail(Models.ResultCode.FAILED,"Что именно искать?");return open(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/search?q="+Uri.encode(q))),"Поиск открыт.");}
    };}

    private Tool settings(){return simple("settings","настройки","Открыл настройки.",new Intent(Settings.ACTION_SETTINGS));}

    private Tool map(){return new Tool(){
        public String name(){return "maps";} public String description(){return "Open a real map search";}
        public boolean canHandle(String s){String x=s.toLowerCase(Locale.ROOT);return x.contains("маршрут")||x.contains("карты")||x.contains("заправк")||x.contains("maps")||x.contains("ближайш");}
        public Models.ToolResult execute(String s){String q=s.replaceFirst("(?is).*?(маршрут|найди|заправк|карты|maps|ближайш)","").trim();if(q.isBlank())q="заправка";return open(new Intent(Intent.ACTION_VIEW,Uri.parse("geo:0,0?q="+Uri.encode(q))),"Карты открыты.");}
    };}


    private Tool contacts(){return new Tool(){
        public String name(){return "contacts";}
        public String description(){return "Search real Android contacts by name and return matching names and phone numbers.";}
        public boolean canHandle(String s){String x=s.toLowerCase(Locale.ROOT);return x.contains("найди контакт")||x.contains("найди человека")||x.contains("контакт ");}
        public Models.ToolResult execute(String s){
            if(c.checkSelfPermission(Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED)return Models.ToolResult.fail(Models.ResultCode.PERMISSION_REQUIRED,"Нужен доступ к контактам.");
            String q=s.replaceFirst("(?is).*?(найди контакт|найди человека|контакт)\\s*","").trim();
            if(q.isBlank())return Models.ToolResult.fail(Models.ResultCode.FAILED,"Кого найти в контактах?");
            StringBuilder out=new StringBuilder();int n=0;
            try(android.database.Cursor cur=c.getContentResolver().query(android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,new String[]{android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER},android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" LIKE ?",new String[]{"%"+q+"%"},android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" ASC")){
                if(cur!=null)while(cur.moveToNext()&&n<10){out.append("• ").append(cur.getString(0)).append(" — ").append(cur.getString(1)).append('\n');n++;}
            }catch(Exception e){return Models.ToolResult.fail(Models.ResultCode.FAILED,"Не удалось прочитать контакты.");}
            return Models.ToolResult.ok(n==0?"Контакт «"+q+"» не найден.":out.toString().trim());
        }
    };}

    private Tool calendarCreate(){return new Tool(){
        public String name(){return "calendar_create";}
        public String description(){return "Create a real Android calendar event after explicit confirmation.";}
        public boolean canHandle(String s){String x=s.toLowerCase(Locale.ROOT);return (x.contains("создай событие")||x.contains("добавь событие")||x.contains("создай встречу"))&&x.matches(".*\\d{1,2}:\\d{2}.*");}
        public Models.ToolResult execute(String s){
            if(c.checkSelfPermission(Manifest.permission.WRITE_CALENDAR)!=PackageManager.PERMISSION_GRANTED)return Models.ToolResult.fail(Models.ResultCode.PERMISSION_REQUIRED,"Нужен доступ к календарю для создания события.");
            Matcher tm=Pattern.compile("(\\d{1,2}):(\\d{2})").matcher(s);if(!tm.find())return Models.ToolResult.fail(Models.ResultCode.FAILED,"Укажите время события.");
            int h=Integer.parseInt(tm.group(1)),m=Integer.parseInt(tm.group(2));if(h>23||m>59)return Models.ToolResult.fail(Models.ResultCode.FAILED,"Время указано некорректно.");
            String title=s.replaceFirst("(?is).*?(создай событие|добавь событие|создай встречу)\\s*","").replaceFirst("(?i)\\d{1,2}:\\d{2}","").trim();if(title.isBlank())title="Встреча JARVIS";
            java.util.Calendar start=java.util.Calendar.getInstance();start.set(java.util.Calendar.HOUR_OF_DAY,h);start.set(java.util.Calendar.MINUTE,m);start.set(java.util.Calendar.SECOND,0);start.set(java.util.Calendar.MILLISECOND,0);if(start.getTimeInMillis()<=System.currentTimeMillis())start.add(java.util.Calendar.DAY_OF_YEAR,1);final long begin=start.getTimeInMillis();final String finalTitle=title;
            return Models.ToolResult.confirm("Создать событие «"+finalTitle+"» в "+String.format(Locale.ROOT,"%02d:%02d",h,m)+"?",()->{
                long calendarId=-1;
                try(android.database.Cursor cur=c.getContentResolver().query(CalendarContract.Calendars.CONTENT_URI,new String[]{CalendarContract.Calendars._ID,CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL},CalendarContract.Calendars.VISIBLE+"=1",null,CalendarContract.Calendars._ID+" ASC")){if(cur!=null)while(cur.moveToNext()){int level=cur.getInt(1);if(level>=CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR){calendarId=cur.getLong(0);break;}}}
                if(calendarId<0)throw new IllegalStateException("Не найден доступный для записи календарь.");
                android.content.ContentValues v=new android.content.ContentValues();v.put(CalendarContract.Events.DTSTART,begin);v.put(CalendarContract.Events.DTEND,begin+60L*60L*1000L);v.put(CalendarContract.Events.TITLE,finalTitle);v.put(CalendarContract.Events.CALENDAR_ID,calendarId);v.put(CalendarContract.Events.EVENT_TIMEZONE,java.util.TimeZone.getDefault().getID());Uri u=c.getContentResolver().insert(CalendarContract.Events.CONTENT_URI,v);if(u==null)throw new IllegalStateException("Android не создал событие.");
            });
        }
    };}

    private Tool communication(){return new Tool(){
        public String name(){return "communication";}
        public String description(){return "Resolve a real Android contact and execute phone calls or prepare SMS/Telegram messages with explicit confirmation.";}
        public boolean canHandle(String s){
            String x=s.toLowerCase(Locale.ROOT);
            return x.contains("позвони")||x.contains("набери")||x.contains("смс")||x.contains("sms")||x.contains("напиши")||x.contains("telegram")||x.contains("телеграм");
        }
        public Models.ToolResult execute(String s){
            String x=s.toLowerCase(Locale.ROOT);
            boolean call=x.contains("позвони")||x.contains("набери");
            boolean telegram=x.contains("telegram")||x.contains("телеграм");
            boolean sms=x.contains("смс")||x.contains("sms")||(!call&&!telegram&&(x.startsWith("напиши")||x.startsWith("отправь сообщение")));
            String number=findNumber(s);
            String person=extractPerson(s,call,telegram,sms);
            if(number.isBlank()&&!person.isBlank()){
                if(c.checkSelfPermission(Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED)
                    return Models.ToolResult.fail(Models.ResultCode.PERMISSION_REQUIRED,"Нужен доступ к контактам, чтобы найти получателя.");
                List<String[]> matches=findContacts(person);
                if(matches.isEmpty()) return Models.ToolResult.fail(Models.ResultCode.FAILED,"Контакт «"+person+"» не найден.");
                if(matches.size()>1){StringBuilder b=new StringBuilder("Нашёл несколько контактов: ");for(int i=0;i<matches.size();i++){if(i>0)b.append(", ");b.append(matches.get(i)[0]);}return Models.ToolResult.fail(Models.ResultCode.FAILED,b+". Уточните, кому выполнить действие.");}
                number=PhoneNumberUtils.normalizeNumber(matches.get(0)[1]); person=matches.get(0)[0];
            }
            if(number.isBlank()) return Models.ToolResult.fail(Models.ResultCode.FAILED,"Не удалось определить получателя. Назовите имя контакта или номер.");
            if(call) return call(number,person);
            String text=extractMessage(s,person,number);
            if(text.isBlank()) return Models.ToolResult.fail(Models.ResultCode.FAILED,"Какой текст отправить?");
            if(telegram) return telegram(number,person,text);
            return sms(number,person,text);
        }
        private String findNumber(String s){Matcher m=Pattern.compile("(?:\\+?\\d[\\d ()-]{6,})").matcher(s);return m.find()?PhoneNumberUtils.normalizeNumber(m.group()):"";}
        private List<String[]> findContacts(String q){List<String[]> out=new ArrayList<>();try(android.database.Cursor cur=c.getContentResolver().query(android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,new String[]{android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER},android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" LIKE ?",new String[]{"%"+q+"%"},android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" ASC")){if(cur!=null)while(cur.moveToNext()&&out.size()<5)out.add(new String[]{cur.getString(0),cur.getString(1)});}catch(Exception ignored){}return out;}
        private String extractPerson(String s,boolean call,boolean telegram,boolean sms){String x=s.trim();x=x.replaceFirst("(?is).*?(позвони|набери|напиши|отправь сообщение|смс|sms|telegram|телеграм)\\s*","");x=x.replaceFirst("(?is)^(?:на номер|номер)\\s*(?:\\+?\\d[\\d ()-]{6,})\\s*","");if(call)return x.replaceFirst("(?is)\\s*(?:пожалуйста|срочно)$","").trim();int colon=x.indexOf(':');if(colon>0)return x.substring(0,colon).trim();Matcher m=Pattern.compile("(?is)^(.+?)\\s+(?:что|—|-)\\s+(.+)$").matcher(x);return m.find()?m.group(1).trim():"";}
        private String extractMessage(String s,String person,String number){String x=s.trim();x=x.replaceFirst("(?is).*?(напиши|отправь сообщение|смс|sms|telegram|телеграм)\\s*","");x=x.replaceFirst("(?is)^(?:[A-Za-zА-Яа-яЁё]+\\s+)?(?:на номер|номер)\\s*(?:\\+?\\d[\\d ()-]{6,})\\s*","");if(!person.isBlank())x=x.replaceFirst("(?is)^"+Pattern.quote(person)+"\\s*[:,-]?\\s*","");return x.trim();}
        private Models.ToolResult call(String number,String person){return Models.ToolResult.confirm("Позвонить контакту «"+(person.isBlank()?number:person)+"»?",()->{if(c.checkSelfPermission(Manifest.permission.CALL_PHONE)!=PackageManager.PERMISSION_GRANTED)throw new SecurityException("Нужен доступ к звонкам.");Intent in=new Intent(Intent.ACTION_CALL,Uri.parse("tel:"+number)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);c.startActivity(in);});}
        private Models.ToolResult sms(String number,String person,String text){return Models.ToolResult.confirm("Открыть SMS для «"+(person.isBlank()?number:person)+"»: «"+text+"»?",()->{Intent in=new Intent(Intent.ACTION_SENDTO,Uri.parse("smsto:"+Uri.encode(number)));in.putExtra("sms_body",text).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);try{c.startActivity(in);}catch(Exception e){throw new IllegalStateException("На устройстве нет приложения SMS, принимающего это действие.");}});}
        private Models.ToolResult telegram(String number,String person,String text){return Models.ToolResult.confirm("Открыть Telegram с подготовленным сообщением для «"+(person.isBlank()?number:person)+"»?",()->{Intent in=new Intent(Intent.ACTION_SEND);in.setType("text/plain");in.setPackage("org.telegram.messenger");in.putExtra(Intent.EXTRA_TEXT,text).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);try{c.startActivity(in);}catch(Exception e){throw new IllegalStateException("Telegram не установлен или не принимает это действие.");}});}
    };}

    private Tool phone(){return new Tool(){
        public String name(){return "phone";}
        public String description(){return "Find a contact and prepare a real phone call";}
        public boolean canHandle(String s){return s.toLowerCase(Locale.ROOT).contains("позвони") || s.toLowerCase(Locale.ROOT).contains("набери");}
        public Models.ToolResult execute(String s){
            Matcher m=Pattern.compile("(?:\\+?\\d[\\d ()-]{6,})").matcher(s);
            if(m.find()) return callAfterConfirmation(PhoneNumberUtils.normalizeNumber(m.group()), "номер "+m.group());
            if(c.checkSelfPermission(Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED) return Models.ToolResult.fail(Models.ResultCode.PERMISSION_REQUIRED,"Нужен доступ к контактам, чтобы найти человека по имени.");
            String query=s.replaceFirst("(?is).*?(позвони|набери)","").trim();
            if(query.isBlank()) return Models.ToolResult.fail(Models.ResultCode.FAILED,"Кому позвонить?");
            List<String[]> matches=new ArrayList<>();
            try(android.database.Cursor cur=c.getContentResolver().query(android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,new String[]{android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER},android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" LIKE ?",new String[]{"%"+query+"%"},android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" ASC")){
                if(cur!=null) while(cur.moveToNext() && matches.size()<5) matches.add(new String[]{cur.getString(0),cur.getString(1)});
            } catch(Exception e){return Models.ToolResult.fail(Models.ResultCode.FAILED,"Не удалось прочитать контакты.");}
            if(matches.isEmpty()) return Models.ToolResult.fail(Models.ResultCode.FAILED,"Контакт «"+query+"» не найден.");
            if(matches.size()>1){StringBuilder b=new StringBuilder("Нашёл несколько контактов: ");for(int i=0;i<matches.size();i++){if(i>0)b.append(", ");b.append(matches.get(i)[0]);}return Models.ToolResult.fail(Models.ResultCode.FAILED,b+". Уточните имя.");}
            return callAfterConfirmation(PhoneNumberUtils.normalizeNumber(matches.get(0)[1]), matches.get(0)[0]);
        }
        private Models.ToolResult callAfterConfirmation(String number,String label){
            if(number==null||number.isBlank()) return Models.ToolResult.fail(Models.ResultCode.FAILED,"У контакта нет корректного номера.");
            return Models.ToolResult.confirm("Позвонить: "+label+"?",()->{Intent in=new Intent(Intent.ACTION_CALL,Uri.parse("tel:"+number)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);if(c.checkSelfPermission(Manifest.permission.CALL_PHONE)!=PackageManager.PERMISSION_GRANTED)throw new SecurityException("Нужен доступ к звонкам.");c.startActivity(in);});
        }
    };}

    private Tool sms(){return new Tool(){
        public String name(){return "sms";} public String description(){return "Prepare an SMS and require explicit confirmation";}
        public boolean canHandle(String s){String x=s.toLowerCase(Locale.ROOT);return x.contains("смс")||x.contains("sms")||x.startsWith("отправь сообщение")||x.startsWith("напиши");}
        public Models.ToolResult execute(String s){
            Matcher n=Pattern.compile("(?:\\+?\\d[\\d ()-]{6,})").matcher(s);
            String text=s.replaceFirst("(?is).*?(?:смс|sms|сообщение|напиши)","").trim();
            String number=n.find()?PhoneNumberUtils.normalizeNumber(n.group()):"";
            if(number.isBlank()) return Models.ToolResult.fail(Models.ResultCode.FAILED,"Для SMS пока нужен номер телефона. Поиск контактов для сообщений будет использовать отдельный разрешённый провайдер.");
            text=text.replaceFirst("(?is)^(?:на номер|номер)\\s*(?:\\+?\\d[\\d ()-]{6,})\\s*","").trim();
            if(text.isBlank()) return Models.ToolResult.fail(Models.ResultCode.FAILED,"Какой текст отправить?");
            String finalNumber=number, finalText=text;
            return Models.ToolResult.confirm("Подготовить SMS на "+finalNumber+": «"+finalText+"»?",()->{Intent in=new Intent(Intent.ACTION_SENDTO,Uri.parse("smsto:"+Uri.encode(finalNumber)));in.putExtra("sms_body",finalText).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);c.startActivity(in);});
        }
    };}

    private Tool reminder(){return new Tool(){
        public String name(){return "reminder";} public String description(){return "Create a real Android alarm reminder";}
        public boolean canHandle(String s){String x=s.toLowerCase(Locale.ROOT);return x.contains("напомни")||x.contains("напоминание");}
        public Models.ToolResult execute(String s){
            Matcher m=Pattern.compile("(?:в|на)\\s*(\\d{1,2})(?::(\\d{2}))?").matcher(s.toLowerCase(Locale.ROOT));
            if(!m.find()) return Models.ToolResult.fail(Models.ResultCode.FAILED,"Укажите время напоминания, например: «напомни в 19:30 позвонить».");
            int hour=Integer.parseInt(m.group(1)); int minute=m.group(2)==null?0:Integer.parseInt(m.group(2));
            if(hour>23||minute>59) return Models.ToolResult.fail(Models.ResultCode.FAILED,"Время указано некорректно.");
            String text=s.replaceFirst("(?is).*?(?:напомни|напоминание)","").replaceFirst("(?is)(?:в|на)\\s*\\d{1,2}(?::\\d{2})?","").trim();
            if(text.isBlank()) text="Напоминание JARVIS"; final String message=text;
            return Models.ToolResult.confirm("Создать напоминание на "+String.format(Locale.ROOT,"%02d:%02d",hour,minute)+": «"+message+"»?",()->{Intent in=new Intent(AlarmClock.ACTION_SET_ALARM).putExtra(AlarmClock.EXTRA_HOUR,hour).putExtra(AlarmClock.EXTRA_MINUTES,minute).putExtra(AlarmClock.EXTRA_MESSAGE,message).putExtra(AlarmClock.EXTRA_SKIP_UI,false).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);c.startActivity(in);});
        }
    };}

    private Tool calendar(){return new Tool(){
        public String name(){return "calendar";} public String description(){return "Open Android calendar";}
        public boolean canHandle(String s){String x=s.toLowerCase(Locale.ROOT);return x.contains("календар")||x.contains("событи")||x.contains("план");}
        public Models.ToolResult execute(String s){return open(new Intent(Intent.ACTION_VIEW,CalendarContract.CONTENT_URI),"Календарь открыт.");}
    };}

    private Tool memory(){return new Tool(){
        public String name(){return "memory";}
        public String description(){return "Explicit long-term memory management";}
        public boolean canHandle(String s){String x=s.toLowerCase(Locale.ROOT);return x.startsWith("запомни")||x.startsWith("забудь")||x.contains("что ты обо мне помнишь")||x.contains("удалить всю память");}
        public Models.ToolResult execute(String s){
            Memory memory=new Memory(c);
            String x=s.trim();
            String low=x.toLowerCase(Locale.ROOT);
            if(low.contains("удалить всю память")){
                return Models.ToolResult.confirm("Удалить всю сохранённую память JARVIS?", () -> memory.clear());
            }
            if(low.contains("что ты обо мне помнишь")){
                Map<String,?> all=memory.all();
                if(all.isEmpty()) return Models.ToolResult.ok("Сохранённой памяти пока нет.");
                StringBuilder out=new StringBuilder("Я помню:\n");
                for(Map.Entry<String,?> e:all.entrySet()) out.append("• ").append(e.getKey()).append(": ").append(e.getValue()).append('\n');
                return Models.ToolResult.ok(out.toString().trim());
            }
            if(low.startsWith("забудь")){
                String key=x.substring("забудь".length()).trim();
                if(key.isBlank()) return Models.ToolResult.fail(Models.ResultCode.FAILED,"Что именно забыть?");
                return Models.ToolResult.confirm("Удалить из памяти: «"+key+"»?", () -> memory.remove(key));
            }
            String payload=x.substring("запомни".length()).trim();
            if(payload.isBlank()) return Models.ToolResult.fail(Models.ResultCode.FAILED,"Что именно запомнить?");
            String[] pair=payload.split("\\s*=\\s*|\\s*:\\s*",2);
            String key=pair.length==2?pair[0].trim():"note_"+System.currentTimeMillis();
            String value=pair.length==2?pair[1].trim():payload;
            return Models.ToolResult.confirm("Сохранить в память: «"+value+"»?", () -> memory.put(key,value));
        }
    };}


    private Tool telegram() { return new Tool() {
        public String name(){return "telegram_message";}
        public String description(){return "Prepare a real Telegram message using Android intent; final sending remains controlled by Telegram/Android.";}
        public boolean canHandle(String s){String x=s.toLowerCase(Locale.ROOT);return x.contains("telegram")||x.contains("телеграм");}
        public Models.ToolResult execute(String s){
            String text=s.replaceFirst("(?is).*?(?:telegram|телеграм)","").replaceFirst("(?is)^(?:напиши|отправь|сообщение)\\s*","").trim();
            if(text.isBlank()) return Models.ToolResult.fail(Models.ResultCode.FAILED,"Укажите текст сообщения для Telegram.");
            return Models.ToolResult.confirm("Подготовить сообщение Telegram: «"+text+"»?",()->{
                Intent in=new Intent(Intent.ACTION_SEND); in.setType("text/plain"); in.setPackage("org.telegram.messenger"); in.putExtra(Intent.EXTRA_TEXT,text); in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try{c.startActivity(in);}catch(Exception e){throw new IllegalStateException("Telegram не установлен или не принимает это действие.");}
            });
        }
    };}

    private Tool calendarEvents() { return new Tool() {
        public String name(){return "calendar_events";}
        public String description(){return "Read upcoming Android calendar events.";}
        public boolean canHandle(String s){String x=s.toLowerCase(Locale.ROOT);return (x.contains("что у меня")||x.contains("событи")||x.contains("встреч"))&&x.contains("сегодня");}
        public Models.ToolResult execute(String s){
            if(androidx.core.content.ContextCompat.checkSelfPermission(c, Manifest.permission.READ_CALENDAR)!=PackageManager.PERMISSION_GRANTED) return Models.ToolResult.fail(Models.ResultCode.PERMISSION_REQUIRED,"Нужно разрешение на чтение календаря.");
            long now=System.currentTimeMillis(); long end=now+24L*60L*60L*1000L;
            String[] proj={CalendarContract.Instances.EVENT_ID,CalendarContract.Instances.TITLE,CalendarContract.Instances.BEGIN,CalendarContract.Instances.END,CalendarContract.Instances.ALL_DAY};
            StringBuilder out=new StringBuilder();
            try(android.database.Cursor cur=CalendarContract.Instances.query(c.getContentResolver(),proj,now,end)){int n=0;while(cur.moveToNext()&&n<10){String title=cur.getString(1);long begin=cur.getLong(2);boolean allDay=cur.getInt(4)!=0;java.text.DateFormat f=new java.text.SimpleDateFormat(allDay?"dd.MM":"HH:mm",Locale.getDefault());out.append("• ").append(f.format(new java.util.Date(begin))).append(" — ").append(title==null?"Без названия":title).append('\n');n++;}}
            catch(Exception e){return Models.ToolResult.fail(Models.ResultCode.FAILED,"Не удалось прочитать календарь: "+e.getMessage());}
            return Models.ToolResult.ok(out.length()==0?"На сегодня событий не найдено.":"Сегодня в календаре:\n"+out.toString().trim());
        }
    };}

    private Tool weather() { return new Tool() {
        public String name(){return "weather";}
        public String description(){return "Get current or next-day weather from Open-Meteo using a real geocoding request.";}
        public boolean canHandle(String s){String x=s.toLowerCase(Locale.ROOT);return x.contains("погод")||x.contains("weather");}
        public Models.ToolResult execute(String s){
            final String query=extractCity(s);
            if(query.isBlank()) return Models.ToolResult.fail(Models.ResultCode.FAILED,"Укажите город для прогноза погоды.");
            try{
                String geo=readUrl("https://geocoding-api.open-meteo.com/v1/search?name="+java.net.URLEncoder.encode(query,"UTF-8")+"&count=1&language=ru&format=json");
                org.json.JSONObject g=new org.json.JSONObject(geo);org.json.JSONArray r=g.optJSONArray("results");if(r==null||r.length()==0)return Models.ToolResult.fail(Models.ResultCode.FAILED,"Город не найден: "+query);
                org.json.JSONObject place=r.getJSONObject(0);double lat=place.getDouble("latitude"),lon=place.getDouble("longitude");String city=place.optString("name",query);
                String forecast=readUrl("https://api.open-meteo.com/v1/forecast?latitude="+lat+"&longitude="+lon+"&current=temperature_2m,apparent_temperature,weather_code,wind_speed_10m&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max&timezone=auto");
                org.json.JSONObject f=new org.json.JSONObject(forecast);org.json.JSONObject cur=f.getJSONObject("current");org.json.JSONObject daily=f.getJSONObject("daily");
                boolean tomorrow=s.toLowerCase(Locale.ROOT).contains("завтра")||s.toLowerCase(Locale.ROOT).contains("tomorrow"); int idx=tomorrow?1:0;
                String result=tomorrow?"Завтра в "+city+": ":"Сейчас в "+city+": ";
                if(tomorrow) result+=daily.getJSONArray("temperature_2m_min").getDouble(idx)+"…"+daily.getJSONArray("temperature_2m_max").getDouble(idx)+" °C, вероятность осадков "+daily.getJSONArray("precipitation_probability_max").getInt(idx)+"%.";
                else result+=cur.getDouble("temperature_2m")+" °C, ощущается как "+cur.getDouble("apparent_temperature")+" °C, ветер "+cur.getDouble("wind_speed_10m")+" км/ч.";
                return Models.ToolResult.ok(result+" Источник: Open-Meteo.");
            }catch(Exception e){return Models.ToolResult.fail(Models.ResultCode.OFFLINE,"Не удалось получить реальный прогноз погоды. Проверьте интернет-соединение.");}
        }
        private String extractCity(String s){String x=s.trim();java.util.regex.Matcher m=Pattern.compile("(?i)(?:в|для|in)\\s+([А-ЯЁа-яёA-Za-z\\- ]+?)(?:\\s+(?:сегодня|завтра|сейчас|today|tomorrow)|$)").matcher(x);if(m.find())return m.group(1).trim();return "";}
        private String readUrl(String u)throws Exception{java.net.HttpURLConnection h=(java.net.HttpURLConnection)new java.net.URL(u).openConnection();h.setConnectTimeout(8000);h.setReadTimeout(10000);h.setRequestMethod("GET");int code=h.getResponseCode();java.io.InputStream in=code>=200&&code<300?h.getInputStream():h.getErrorStream();if(in==null)throw new java.io.IOException("HTTP "+code);try(in){return new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);}}
    };}

    private Tool calculator(){return new Tool(){
        public String name(){return "calculator";} public String description(){return "Evaluate simple arithmetic";}
        public boolean canHandle(String s){String x=s.toLowerCase(Locale.ROOT);return x.startsWith("посчитай")||x.startsWith("сколько будет");}
        public Models.ToolResult execute(String s){String q=s.replaceFirst("(?is)^(посчитай|сколько будет)\\s*","").replace(',','.').replaceAll("[^0-9+\\-*/().]","");if(q.isBlank())return Models.ToolResult.fail(Models.ResultCode.FAILED,"Не вижу выражения.");try{return Models.ToolResult.ok("Результат: "+eval(q));}catch(Exception e){return Models.ToolResult.fail(Models.ResultCode.FAILED,"Не удалось вычислить выражение.");}}
        double eval(String s){return new Object(){int p=0;double parse(){double x=term();while(p<s.length()){char c=s.charAt(p);if(c=='+'){p++;x+=term();}else if(c=='-'){p++;x-=term();}else break;}return x;}double term(){double x=factor();while(p<s.length()){char c=s.charAt(p);if(c=='*'){p++;x*=factor();}else if(c=='/'){p++;x/=factor();}else break;}return x;}double factor(){if(p>=s.length())throw new IllegalArgumentException();if(s.charAt(p)=='('){p++;double x=parse();if(p>=s.length()||s.charAt(p)!=')')throw new IllegalArgumentException();p++;return x;}int st=p;while(p<s.length()&&(Character.isDigit(s.charAt(p))||s.charAt(p)=='.'))p++;if(st==p)throw new IllegalArgumentException();return Double.parseDouble(s.substring(st,p));}}.parse();}
    };}


    private Tool filePicker(){return new Tool(){
        public String name(){return "file_picker";}
        public String description(){return "Open the Android document picker for a real local file.";}
        public boolean canHandle(String s){String x=s.toLowerCase(Locale.ROOT);return x.contains("выбери файл")||x.contains("открой файл")||x.contains("файл для анализа")||x.contains("choose file");}
        public Models.ToolResult execute(String s){Intent in=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);return open(in,"Открыл выбор файла.");}
    };}

    private Tool documentPicker(){return new Tool(){
        public String name(){return "documents";}
        public String description(){return "Open Android Storage Access Framework for a real document; supports local text extraction and PDF page inspection.";}
        public boolean canHandle(String s){String x=s.toLowerCase(Locale.ROOT);return x.contains("проанализируй файл")||x.contains("прочитай файл")||x.contains("проанализируй документ")||x.contains("прочитай документ")||x.contains("pdf")||x.contains("документ");}
        public Models.ToolResult execute(String s){
            Intent in=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            return open(in,"Открыл выбор документа. После выбора JARVIS сможет прочитать поддерживаемый формат.");
        }
    };}

    private Tool imagePicker(){return new Tool(){
        public String name(){return "image_picker";}
        public String description(){return "Open the Android photo picker for a real image.";}
        public boolean canHandle(String s){String x=s.toLowerCase(Locale.ROOT);return x.contains("выбери фото")||x.contains("выбери изображение")||x.contains("открой галерею")||x.contains("photo picker");}
        public Models.ToolResult execute(String s){Intent in=new Intent("android.provider.action.PICK_IMAGES").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);return open(in,"Открыл выбор изображения.");}
    };}

    private Tool notificationAccess(){return new Tool(){
        public String name(){return "notification_access";}
        public String description(){return "Check whether JARVIS has Android notification-listener access.";}
        public boolean canHandle(String s){String x=s.toLowerCase(Locale.ROOT);return x.contains("уведомлен")&&(x.contains("доступ")||x.contains("разрешен"));}
        public Models.ToolResult execute(String s){String enabled=android.provider.Settings.Secure.getString(c.getContentResolver(),"enabled_notification_listeners");boolean ok=enabled!=null&&enabled.contains(c.getPackageName());return Models.ToolResult.ok(ok?"Доступ к уведомлениям включён.":"Доступ к уведомлениям пока не включён.");}
    };}

    private Tool wifiSettings(){return simple("wifi_settings","wi-fi","Открыл настройки Wi-Fi.",new Intent(Settings.ACTION_WIFI_SETTINGS));}
    private Tool bluetoothSettings(){return simple("bluetooth_settings","bluetooth","Открыл настройки Bluetooth.",new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));}
    private Tool volumeSettings(){return new Tool(){public String name(){return "volume_settings";}public String description(){return "Open Android sound and volume settings.";}public boolean canHandle(String s){String x=s.toLowerCase(Locale.ROOT);return x.contains("громк")||x.contains("звук")||x.contains("volume");}public Models.ToolResult execute(String s){return open(new Intent(Settings.ACTION_SOUND_SETTINGS),"Открыл настройки звука.");}};}
    private Tool flashlight(){return new Tool(){
        public String name(){return "flashlight";} public String description(){return "Turn the camera torch on or off using Android CameraManager.";}
        public boolean canHandle(String s){String x=s.toLowerCase(Locale.ROOT);return x.contains("фонар")||x.contains("flashlight");}
        public Models.ToolResult execute(String s){try{android.hardware.camera2.CameraManager cm=(android.hardware.camera2.CameraManager)c.getSystemService(Context.CAMERA_SERVICE);String id=cm.getCameraIdList()[0];boolean on=!(s.toLowerCase(Locale.ROOT).contains("выключ")||s.toLowerCase(Locale.ROOT).contains("off"));cm.setTorchMode(id,on);return Models.ToolResult.ok(on?"Фонарик включён.":"Фонарик выключен.");}catch(Exception e){return Models.ToolResult.fail(Models.ResultCode.FAILED,"Не удалось управлять фонариком на этом устройстве.");}}
    };}

    private Tool camera(){return new Tool(){public String name(){return "camera";}public String description(){return "Open camera";}public boolean canHandle(String s){return s.toLowerCase(Locale.ROOT).contains("камер");}public Models.ToolResult execute(String s){return open(new Intent("android.media.action.IMAGE_CAPTURE"),"Камера открыта.");}};}
    private Tool simple(String name,String key,String ok,Intent intent){return new Tool(){public String name(){return name;}public String description(){return key;}public boolean canHandle(String s){return s.toLowerCase(Locale.ROOT).contains(key);}public Models.ToolResult execute(String s){return open(intent,ok);}};}
    private Models.ToolResult open(Intent in,String ok){in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);try{c.startActivity(in);return Models.ToolResult.ok(ok);}catch(Exception e){return Models.ToolResult.fail(Models.ResultCode.FAILED,"Android не нашёл подходящее приложение для этого действия.");}}
}
