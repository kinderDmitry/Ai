package com.jarvis.nextgen;

import android.os.Build;import java.util.*;

/** Declares the physical test matrix; PASS is recorded only by an external device runner. */
public final class DeviceTestMatrix {
 public static List<String> cases(){return List.of("microphone-permission","speech-recognition","tts","assistant-role","lock-screen","notifications","contacts","phone-intent","sms-handoff","calendar-crud","location","camera-ocr","pdf-ocr","web-research","automation-alarm","memory","adaptive-fonts","offline-mode");}
 public static Map<String,String> environment(){Map<String,String> m=new LinkedHashMap<>();m.put("android",String.valueOf(Build.VERSION.SDK_INT));m.put("abi",Build.SUPPORTED_ABIS.length>0?Build.SUPPORTED_ABIS[0]:"unknown");m.put("status","NOT_RUN_ON_PHYSICAL_DEVICE");return m;}
}
