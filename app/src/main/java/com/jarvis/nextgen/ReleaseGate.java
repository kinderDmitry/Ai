package com.jarvis.nextgen;

import java.io.*;import java.util.*;

/** Local release checklist; external signing/device evidence is never fabricated. */
public final class ReleaseGate {
 public record Check(String name,boolean passed,String detail){}
 public static List<Check> sourceChecks(File project){List<Check> r=new ArrayList<>();r.add(new Check("project exists",project!=null&&project.exists(),String.valueOf(project)));r.add(new Check("manifest exists",new File(project,"app/src/main/AndroidManifest.xml").isFile(),"Manifest"));r.add(new Check("gradle config exists",new File(project,"app/build.gradle.kts").isFile(),"Gradle"));return r;}
 private ReleaseGate(){}
}
