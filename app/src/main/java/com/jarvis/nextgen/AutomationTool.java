package com.jarvis.nextgen;

import com.jarvis.nextgen.Models;import com.jarvis.nextgen.Tool;import com.jarvis.nextgen.AutomationEngine;import java.util.*;import java.util.regex.*;

/** Natural-language daily automation creation. The scheduled event is backed by Android AlarmManager. */
public final class AutomationTool implements Tool {
 private final AutomationEngine engine;
 public AutomationTool(android.content.Context c){engine=new AutomationEngine(c);}
 public String name(){return "automations";}
 public String description(){return "Create, list and cancel persistent daily automations backed by Android AlarmManager.";}
 public boolean canHandle(String s){String x=s.toLowerCase(Locale.ROOT);return x.contains("автоматизац")||x.contains("каждый день")||x.contains("ежедневно")||x.contains("automation");}
 public Models.ToolResult execute(String s){
  String x=s.trim();String l=x.toLowerCase(Locale.ROOT);
  if(l.contains("покажи")||l.contains("список")||l.contains("list")){return Models.ToolResult.ok(engine.all().isEmpty()?"Автоматизаций пока нет.":engine.all().toString());}
  if(l.contains("удали")||l.contains("отмени")||l.contains("cancel")){Matcher m=Pattern.compile("(?i)(?:удали|отмени|cancel)\\s+(?:автоматизац(?:ию|ию)?\\s*)?([A-Za-z0-9_-]+)").matcher(x);if(!m.find())return Models.ToolResult.fail(Models.ResultCode.FAILED,"Укажите идентификатор автоматизации.");engine.remove(m.group(1));return Models.ToolResult.ok("Автоматизация удалена.");}
  Matcher tm=Pattern.compile("(?i)(?:в|at)\\s*(\\d{1,2}):(\\d{2})").matcher(x);if(!tm.find())return Models.ToolResult.fail(Models.ResultCode.FAILED,"Укажите время, например: каждый день в 08:00 сообщай погоду.");
  int h=Integer.parseInt(tm.group(1)),m=Integer.parseInt(tm.group(2));if(h>23||m>59)return Models.ToolResult.fail(Models.ResultCode.FAILED,"Некорректное время.");
  String action=x.substring(tm.end()).trim();action=action.replaceFirst("(?i)^(?:каждый день|ежедневно|каждый)\\s*","").trim();if(action.isEmpty())return Models.ToolResult.fail(Models.ResultCode.FAILED,"Не указано действие автоматизации.");
  String id="auto_"+Math.abs((x+System.currentTimeMillis()).hashCode());String finalAction=action;String finalId=id;
  return Models.ToolResult.confirm("Создать ежедневную автоматизацию на "+String.format(Locale.ROOT,"%02d:%02d",h,m)+": «"+action+"»?",()->{engine.put(finalId,"DAILY "+String.format(Locale.ROOT,"%02d:%02d",h,m),"",finalAction);engine.scheduleDaily(finalId,h,m);});
 }
}
