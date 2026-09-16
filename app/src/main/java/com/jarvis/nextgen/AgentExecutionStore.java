package com.jarvis.nextgen;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

/** Durable execution state for pause/confirm/resume/recovery. */
public final class AgentExecutionStore {
    public enum Status { RUNNING, WAITING_CONFIRMATION, PAUSED, COMPLETED, FAILED, CANCELLED }
    public record Snapshot(String taskId, String original, String planJson, int nextIndex, Status status, String lastResult) {}
    private final SharedPreferences prefs;
    public AgentExecutionStore(Context c) { prefs = c.getSharedPreferences("agent_execution", Context.MODE_PRIVATE); }

    public synchronized void save(String taskId, String original, Models.TaskPlan plan, int nextIndex, Status status, String lastResult) {
        try {
            JSONObject o = new JSONObject();
            o.put("taskId", taskId); o.put("original", original); o.put("nextIndex", nextIndex);
            o.put("status", status.name()); o.put("lastResult", lastResult == null ? "" : lastResult);
            JSONArray steps = new JSONArray();
            for (Models.PlanStep s : plan.steps()) {
                JSONObject x = new JSONObject(); x.put("id", s.id()); x.put("tool", s.tool()); x.put("input", s.input());
                x.put("confirmationRequired", s.confirmationRequired());
                JSONArray deps = new JSONArray(); for (String d : s.dependsOn()) deps.put(d); x.put("dependsOn", deps); steps.put(x);
            }
            o.put("steps", steps); prefs.edit().putString("snapshot", o.toString()).apply();
        } catch (Exception ignored) {}
    }

    public synchronized Snapshot load() {
        String raw = prefs.getString("snapshot", null); if (raw == null) return null;
        try {
            JSONObject o = new JSONObject(raw); JSONArray a = o.getJSONArray("steps");
            java.util.ArrayList<Models.PlanStep> steps = new java.util.ArrayList<>();
            for (int i=0;i<a.length();i++) { JSONObject x=a.getJSONObject(i); java.util.ArrayList<String> deps=new java.util.ArrayList<>(); JSONArray d=x.optJSONArray("dependsOn"); if(d!=null)for(int j=0;j<d.length();j++)deps.add(d.getString(j)); steps.add(new Models.PlanStep(x.optString("id","step-"+i),x.getString("tool"),x.optString("input",""),x.optBoolean("confirmationRequired"),deps)); }
            Models.TaskPlan plan = new Models.TaskPlan(o.optString("original",""), steps);
            return new Snapshot(o.getString("taskId"),o.optString("original",""),a.toString(),o.optInt("nextIndex",0),Status.valueOf(o.optString("status",Status.PAUSED.name())),o.optString("lastResult",""));
        } catch(Exception e){ return null; }
    }

    private String planToJson(Models.TaskPlan plan) { try { JSONArray a=new JSONArray(); for(Models.PlanStep s:plan.steps()){JSONObject o=new JSONObject();o.put("id",s.id());o.put("tool",s.tool());o.put("input",s.input());a.put(o);} return a.toString(); }catch(Exception e){return "[]";} }
    public synchronized void clear() { prefs.edit().remove("snapshot").apply(); }
    public synchronized boolean hasPending() { Snapshot s=load(); return s!=null && s.status()!=Status.COMPLETED && s.status()!=Status.FAILED && s.status()!=Status.CANCELLED; }
    public synchronized void setStatus(Status status, String lastResult) { Snapshot s=load(); if(s==null)return; Models.TaskPlan plan=parsePlan(s); if(plan!=null)save(s.taskId(),s.original(),plan,s.nextIndex(),status,lastResult); }
    private Models.TaskPlan parsePlan(Snapshot snap) { try { JSONArray a=new JSONArray(snap.planJson()); java.util.ArrayList<Models.PlanStep> steps=new java.util.ArrayList<>(); for(int i=0;i<a.length();i++){JSONObject x=a.getJSONObject(i); java.util.ArrayList<String> deps=new java.util.ArrayList<>(); JSONArray d=x.optJSONArray("dependsOn"); if(d!=null)for(int j=0;j<d.length();j++)deps.add(d.getString(j)); steps.add(new Models.PlanStep(x.optString("id","step-"+i),x.getString("tool"),x.optString("input",""),x.optBoolean("confirmationRequired"),deps));} return new Models.TaskPlan(snap.original(),steps);}catch(Exception e){return null;} }
}
