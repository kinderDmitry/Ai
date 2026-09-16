package com.jarvis.nextgen;

import android.Manifest;
import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Real settings/privacy surface. Values are persisted locally via SharedPreferences. */
public final class SettingsActivity extends Activity {
    private LinearLayout root; private final int BG=Color.rgb(2,5,10), FG=Color.WHITE, MUTED=Color.rgb(145,170,195), ACC=Color.rgb(75,180,255);
    private android.content.SharedPreferences prefs;
    @Override public void onCreate(Bundle b){super.onCreate(b); prefs=getSharedPreferences("jarvis_settings",MODE_PRIVATE); build();}
    private TextView text(String s,float size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(18,10,18,10);return t;}
    private void build(){ root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(18,18,18,18);root.setBackgroundColor(BG); ScrollView scroll=new ScrollView(this); LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);
        TextView title=text("JARVIS  •  НАСТРОЙКИ",24,FG);title.setTypeface(null,1);content.addView(title);
        content.addView(text("Управление поведением помощника, доступами и приватностью",11,MUTED));
        section(content,"AI"); toggle(content,"Онлайн AI", "ai_online", true); toggle(content,"Подтверждать действия", "confirm_actions", true); aiProvider(content);
        section(content,"ГОЛОС"); toggle(content,"Непрерывный разговор", "continuous_voice", false); seek(content,"Скорость речи", "speech_rate", 50, 100);
        section(content,"ПАМЯТЬ"); toggle(content,"Долговременная память", "long_memory", true); button(content,"Управление памятью",v->toast("Управление памятью доступно через команды: «Что ты обо мне помнишь?» и «Забудь всё»."));
        section(content,"АВТОМАТИЗАЦИЯ"); toggle(content,"Автоматизации", "automations", false); toggle(content,"Проактивные уведомления", "proactive", false); button(content,"Открыть редактор автоматизаций",v->startActivity(new Intent(this,com.jarvis.nextgen.automation.AutomationEditorActivity.class)));
        section(content,"УВЕДОМЛЕНИЯ"); toggle(content,"Уведомления JARVIS", "notifications", true); button(content,"Доступ к уведомлениям Android",v->startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        section(content,"PRIVACY / PERMISSIONS"); showPermissions(content);
        section(content,"ВНЕШНИЙ ВИД"); toggle(content,"Анимации", "animations", true); toggle(content,"Звуковые ответы", "sound", true);
        section(content,"ЯЗЫК"); language(content);
        section(content,"СИСТЕМА"); button(content,"Выбрать JARVIS системным Assistant",v->requestAssistant()); button(content,"Очистить локальные настройки",v->{prefs.edit().clear().apply();build();});
        content.addView(text("Все настройки этого экрана сохраняются локально на устройстве. Отдельные возможности зависят от Android, установленных приложений и выданных разрешений.",10,MUTED));
        scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root); }
    private void aiProvider(LinearLayout c){
        OnlineBrain brain=new OnlineBrain(this);
        c.addView(text("Провайдер: "+brain.providerName(),13,MUTED));
        EditText endpoint=field("HTTPS endpoint",brain.endpoint()==null?"https://api.openai.com/v1/chat/completions":brain.endpoint(),false);
        EditText model=field("Model",brain.model()==null?"gpt-4o-mini":brain.model(),false);
        EditText key=field("API key", "", true);
        c.addView(endpoint); c.addView(model); c.addView(key);
        button(c,"Сохранить AI provider",v->{try{brain.save(endpoint.getText().toString(),key.getText().toString(),model.getText().toString());key.setText("");toast("Настройки AI сохранены. Ключ хранится в Android Keystore.");}catch(Exception e){toast(e.getMessage()==null?"Не удалось сохранить настройки AI.":e.getMessage());}});
        button(c,"Проверить соединение",v->{if(!brain.configured()){toast("Сначала сохраните endpoint, model и API key.");return;}toast("Проверяю соединение...");brain.ask("Ответь одним словом: OK",new OnlineBrain.Callback(){public void done(String t){runOnUiThread(()->toast("AI доступен. Ответ получен."));}public void failed(String r){runOnUiThread(()->toast(r));}});});
        button(c,"Удалить сохранённые данные AI",v->{brain.clear();key.setText("");toast("Данные AI удалены.");});
        c.addView(text("API key не отображается после сохранения и не записывается в обычные SharedPreferences. Endpoint принимается только по HTTPS.",10,MUTED));
    }
    private EditText field(String hint,String value,boolean password){EditText e=new EditText(this);e.setHint(hint);e.setText(value);e.setTextColor(FG);e.setHintTextColor(MUTED);e.setSingleLine(true);e.setPadding(18,6,18,6);if(password)e.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);else e.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);return e;}
    private void section(LinearLayout c,String s){TextView t=text(s,12,ACC);t.setTypeface(null,1);t.setPadding(18,24,18,8);c.addView(t);}
    private void toggle(LinearLayout c,String label,String key,boolean def){Switch sw=new Switch(this);sw.setText(label);sw.setTextColor(FG);sw.setTextSize(15);sw.setPadding(18,6,18,6);sw.setChecked(prefs.getBoolean(key,def));sw.setOnCheckedChangeListener((b,v)->prefs.edit().putBoolean(key,v).apply());c.addView(sw,new LinearLayout.LayoutParams(-1,58));}
    private void seek(LinearLayout c,String label,String key,int min,int max){TextView v=text(label+": "+prefs.getInt(key,75)+"%",14,FG);SeekBar bar=new SeekBar(this);bar.setMax(max-min);bar.setProgress(prefs.getInt(key,75)-min);bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int p,boolean f){int x=p+min;v.setText(label+": "+x+"%");prefs.edit().putInt(key,x).apply();}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});c.addView(v);c.addView(bar);}
    private void button(LinearLayout c,String label,View.OnClickListener l){Button b=new Button(this);b.setText(label);b.setTextColor(FG);b.setAllCaps(false);b.setOnClickListener(l);c.addView(b,new LinearLayout.LayoutParams(-1,56));}
    private void language(LinearLayout c){String[] vals={"Русский","English"};Spinner sp=new Spinner(this);ArrayAdapter<String>a=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,vals);sp.setAdapter(a);sp.setSelection(prefs.getString("language","ru").equals("en")?1:0);sp.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){prefs.edit().putString("language",pos==1?"en":"ru").apply();}});c.addView(sp,new LinearLayout.LayoutParams(-1,55));}
    private void showPermissions(LinearLayout c){PermissionCenter pc=new PermissionCenter(this);for(Map.Entry<String,Boolean> e:pc.status().entrySet())c.addView(text(e.getKey()+": "+(e.getValue()?"разрешено":"не разрешено"),13,e.getValue()?Color.rgb(100,220,150):Color.rgb(255,145,130)));button(c,"Запросить необходимые разрешения",v->requestPermissionsNow());}
    private void requestPermissionsNow(){ArrayList<String> p=new ArrayList<>();String[] all={Manifest.permission.RECORD_AUDIO,Manifest.permission.READ_CONTACTS,Manifest.permission.CALL_PHONE,Manifest.permission.SEND_SMS,Manifest.permission.READ_CALENDAR,Manifest.permission.WRITE_CALENDAR,Manifest.permission.CAMERA,Manifest.permission.ACCESS_FINE_LOCATION};for(String x:all)if(android.os.Build.VERSION.SDK_INT<23||checkSelfPermission(x)!=PackageManager.PERMISSION_GRANTED)p.add(x);if(!p.isEmpty())requestPermissions(p.toArray(new String[0]),42);else toast("Необходимые разрешения уже выданы.");}
    private void requestAssistant(){if(android.os.Build.VERSION.SDK_INT>=29){android.app.role.RoleManager rm=getSystemService(android.app.role.RoleManager.class);if(rm!=null&&rm.isRoleAvailable(android.app.role.RoleManager.ROLE_ASSISTANT)){startActivityForResult(rm.createRequestRoleIntent(android.app.role.RoleManager.ROLE_ASSISTANT),77);}else toast("Роль Assistant недоступна на этом устройстве.");}else toast("Роль Assistant требует Android 10+.");}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}
