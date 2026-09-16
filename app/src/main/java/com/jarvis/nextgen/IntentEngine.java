package com.jarvis.nextgen;
import java.util.*;
public final class IntentEngine {
 public String normalize(String s){String x=s.trim();x=x.replaceFirst("(?i)^(джарвис|jarvis)[, ]*","").trim();return x;}
 public boolean isConfirmation(String s){String x=normalize(s).toLowerCase(Locale.ROOT);return Set.of("да","ага","подтверждаю","отправляй","отправить","yes","confirm").contains(x);}
 public boolean isCancellation(String s){String x=normalize(s).toLowerCase(Locale.ROOT);return Set.of("нет","отмена","отменить","cancel","no").contains(x);}
}
