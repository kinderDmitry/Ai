package com.jarvis.nextgen;

import org.json.JSONObject;

public interface Tool {
 String name();
 String description();
 boolean canHandle(String input);
 Models.ToolResult execute(String input);
 default Models.ToolResult verify(Models.ToolResult r){return r;}
 default void cancel(){}
 /** Strict machine-readable contract consumed by the AI planner. */
 default JSONObject schema(){ return ToolSchemaCatalog.schema(this); }
}
