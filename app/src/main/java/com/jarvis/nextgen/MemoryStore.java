package com.jarvis.nextgen;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

/** Durable, user-controlled memory with lightweight lexical retrieval. */
public final class MemoryStore {
    public record Entry(String id, String type, String key, String value, long createdAt, long updatedAt) {}
    private final SharedPreferences p;
    private static final String DATA = "entries";
    public MemoryStore(Context c) { p=c.getSharedPreferences("jarvis_memory_v2", Context.MODE_PRIVATE); }

    public synchronized void remember(String key, String value) { remember("LONG_TERM", key, value); }
    public synchronized void remember(String type, String key, String value) {
        if (key==null || key.isBlank() || value==null || value.isBlank()) return;
        JSONArray a=read(); long now=System.currentTimeMillis(); boolean found=false;
        try {
            for(int i=0;i<a.length();i++) { JSONObject o=a.optJSONObject(i); if(o!=null && key.equalsIgnoreCase(o.optString("key"))) { o.put("type",type);o.put("value",value.trim());o.put("updatedAt",now);found=true;break; } }
            if(!found) { JSONObject o=new JSONObject();o.put("id",UUID.randomUUID().toString());o.put("type",type);o.put("key",key.trim());o.put("value",value.trim());o.put("createdAt",now);o.put("updatedAt",now);a.put(o); }
            write(a);
        } catch (org.json.JSONException e) { throw new IllegalStateException("Не удалось сохранить память JARVIS.", e); }
    }
    public synchronized String get(String key) { for(Entry e:all()) if(e.key().equalsIgnoreCase(key)) return e.value(); return null; }
    public synchronized List<Entry> all() { List<Entry> out=new ArrayList<>(); JSONArray a=read(); for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null)out.add(entry(o));} out.sort((x,y)->Long.compare(y.updatedAt(),x.updatedAt())); return out; }
    public synchronized List<Entry> search(String query,int limit) {
        if(query==null||query.isBlank()) return all().subList(0,Math.min(limit,all().size()));
        String[] q=tokens(query); List<Scored> scored=new ArrayList<>();
        for(Entry e:all()){String hay=(e.key()+" "+e.value()).toLowerCase(Locale.ROOT);int score=0;for(String t:q)if(hay.contains(t))score+=hay.contains(" "+t+" ")?3:1;if(score>0)scored.add(new Scored(e,score));}
        scored.sort((a,b)->Integer.compare(b.score,a.score));List<Entry> out=new ArrayList<>();for(int i=0;i<Math.min(limit,scored.size());i++)out.add(scored.get(i).e);return out;
    }
    public synchronized void forget(String key) { JSONArray a=read();for(int i=a.length()-1;i>=0;i--)if(key.equalsIgnoreCase(a.optJSONObject(i).optString("key")))a.remove(i);write(a); }
    public synchronized void clear() { p.edit().remove(DATA).apply(); }
    public synchronized int size(){return all().size();}
    private JSONArray read(){try{return new JSONArray(p.getString(DATA,"[]"));}catch(Exception e){return new JSONArray();}}
    private void write(JSONArray a){p.edit().putString(DATA,a.toString()).apply();}
    private static Entry entry(JSONObject o){return new Entry(o.optString("id"),o.optString("type","LONG_TERM"),o.optString("key"),o.optString("value"),o.optLong("createdAt"),o.optLong("updatedAt"));}
    private static String[] tokens(String s){return s.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+"," ").trim().split("\\s+");}
    private record Scored(Entry e,int score){}
}
