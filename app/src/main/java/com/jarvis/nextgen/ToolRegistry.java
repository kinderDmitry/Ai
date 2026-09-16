package com.jarvis.nextgen;
import java.util.*;import org.json.*;
public final class ToolRegistry {
 private final List<Tool> tools=new ArrayList<>();
 public void add(Tool t){tools.add(t);}
 public Optional<Tool> find(String input){return tools.stream().filter(t->t.canHandle(input)).findFirst();}
 public Optional<Tool> findByName(String name){return tools.stream().filter(t->t.name().equalsIgnoreCase(name)).findFirst();}
 public List<Tool> all(){return Collections.unmodifiableList(tools);}
 public JSONArray schemas(){JSONArray a=new JSONArray();for(Tool t:tools)a.put(t.schema());return a;}
 public Models.ToolResult dispatch(String input){return find(input).map(t->t.execute(input)).orElse(Models.ToolResult.fail(Models.ResultCode.UNSUPPORTED,"Я не нашёл безопасного инструмента для этой задачи."));}
}
