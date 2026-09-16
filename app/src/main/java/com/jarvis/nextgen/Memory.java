package com.jarvis.nextgen;

import android.content.Context;
import java.util.List;

/** Compatibility facade over the durable MemoryStore v2. */
public final class Memory {
    private final MemoryStore store;
    public Memory(Context context) { store = new MemoryStore(context); }
    public void put(String key,String value){store.remember(key,value);}
    public String get(String key){return store.get(key);}
    public List<MemoryStore.Entry> search(String query,int limit){return store.search(query,limit);}
    public List<MemoryStore.Entry> all(){return store.all();}
    public void remove(String key){store.forget(key);}
    public void clear(){store.clear();}
}
