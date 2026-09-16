package com.jarvis.nextgen;

import java.util.*;

/** Central UI language contract; product strings are kept outside business logic. */
public final class LocalizationAudit {
 public static final String RU="ru", EN="en"; public static boolean supported(String x){return RU.equals(x)||EN.equals(x);}
 public static String normalize(String x){return supported(x)?x:RU;}
 public static final Set<String> KEEP_ENGLISH=Set.of("JARVIS","LONG","SHORT","TP","SL","AI","OCR","PDF","SMS","Telegram");
 private LocalizationAudit(){}
}
