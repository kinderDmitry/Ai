package com.jarvis.nextgen;

import java.util.*;

/** Validates trigger/condition/action definitions before they reach AlarmManager. */
public final class AutomationEditorModel {
 public record Definition(String id,String trigger,String condition,String action,boolean enabled){}
 public static List<String> validate(Definition d){List<String> e=new ArrayList<>();if(d==null)e.add("Определение отсутствует.");else{if(d.id()==null||d.id().isBlank())e.add("Нужен идентификатор.");if(d.trigger()==null||d.trigger().isBlank())e.add("Нужен триггер.");if(d.action()==null||d.action().isBlank())e.add("Нужно действие.");}return e;}
}
