package com.jarvis.nextgen;

import android.app.*;import android.os.Bundle;import android.graphics.Color;import android.view.*;import android.widget.*;import java.util.*;

/** Minimal real automation editor backed by AutomationEngine. */
public final class AutomationEditorActivity extends Activity {
 private LinearLayout root; private EditText id,trigger,condition,action; private AutomationEngine engine;
 private TextView t(String s){TextView v=new TextView(this);v.setText(s);v.setTextColor(Color.WHITE);v.setTextSize(14);v.setPadding(12,8,12,8);return v;}
 private EditText e(String hint){EditText x=new EditText(this);x.setHint(hint);x.setTextColor(Color.WHITE);x.setHintTextColor(Color.GRAY);x.setSingleLine(true);x.setPadding(12,6,12,6);return x;}
 public void onCreate(Bundle b){super.onCreate(b);engine=new AutomationEngine(this);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(18,18,18,18);root.setBackgroundColor(Color.rgb(2,5,10));root.addView(t("JARVIS • AUTOMATION EDITOR"));id=e("ID");trigger=e("Триггер, например DAILY_08_00");condition=e("Условие (необязательно)");action=e("Действие");root.addView(id);root.addView(trigger);root.addView(condition);root.addView(action);Button save=new Button(this);save.setText("СОХРАНИТЬ");save.setOnClickListener(v->save());root.addView(save);setContentView(root);}
 private void save(){AutomationEditorModel.Definition d=new AutomationEditorModel.Definition(id.getText().toString().trim(),trigger.getText().toString().trim(),condition.getText().toString().trim(),action.getText().toString().trim(),true);List<String> errors=AutomationEditorModel.validate(d);if(!errors.isEmpty()){Toast.makeText(this,String.join(" ",errors),Toast.LENGTH_LONG).show();return;}engine.put(d.id(),d.trigger(),d.condition(),d.action());if(d.trigger().startsWith("DAILY_")){String[] x=d.trigger().substring(6).split("_");if(x.length==2)try{engine.scheduleDaily(d.id(),Integer.parseInt(x[0]),Integer.parseInt(x[1]));}catch(Exception ex){Toast.makeText(this,"Неверный формат DAILY_HH_MM",Toast.LENGTH_LONG).show();return;}}Toast.makeText(this,"Автоматизация сохранена.",Toast.LENGTH_SHORT).show();}
}
