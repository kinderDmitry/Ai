package com.jarvis.nextgen;

import android.content.Context;import java.util.*;

/** User-controlled retrieval policy: scopes memory by explicit categories and recency. */
public final class SemanticMemoryPolicy {
 private final android.content.SharedPreferences p; public SemanticMemoryPolicy(Context c){p=c.getSharedPreferences("jarvis_memory_policy",0);}
 public boolean allowLongTerm(){return p.getBoolean("long_term",true);} public void setAllowLongTerm(boolean v){p.edit().putBoolean("long_term",v).apply();}
 public Set<String> allowedTypes(){String raw=p.getString("types","LONG_TERM,USER_PREFERENCE,TASK");return new HashSet<>(Arrays.asList(raw.split(",")));}
 public List<MemoryStore.Entry> retrieve(MemoryStore store,String query,int limit){if(!allowLongTerm())return List.of();List<MemoryStore.Entry> all=store.search(query,Math.max(limit,20));List<MemoryStore.Entry> out=new ArrayList<>();for(MemoryStore.Entry e:all)if(allowedTypes().contains(e.type())){out.add(e);if(out.size()>=limit)break;}return out;}
}
