package com.jarvis.nextgen;

import android.content.Context;import java.util.*;

public final class PrivacyCenterModel {
 private final ToolPermissionMatrix matrix; public PrivacyCenterModel(Context c){matrix=new ToolPermissionMatrix(c);}
 public Map<String,Boolean> toolStatus(){return matrix.status();}
 public Map<String,List<String>> toolPermissions(){return matrix.mapping();}
 public List<String> missing(String tool){List<String> out=new ArrayList<>();List<String> p=matrix.mapping().get(tool);if(p!=null)for(String x:p)if(!matrix.granted(x))out.add(x);return out;}
}
