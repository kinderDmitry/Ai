package com.jarvis.nextgen;

import android.app.*;import android.app.role.RoleManager;import android.content.*;import android.os.Build;import android.provider.Settings;

/** Reports only verifiable lock-screen/assistant capabilities. */
public final class LockScreenAssistantVerifier {
 public record Status(boolean showWhenLocked,boolean assistantRoleAvailable,boolean assistantRoleHeld,boolean keyguardLocked){}
 private final Activity a; public LockScreenAssistantVerifier(Activity a){this.a=a;}
 public Status status(){boolean role=false,held=false;if(Build.VERSION.SDK_INT>=29){RoleManager rm=a.getSystemService(RoleManager.class);if(rm!=null){role=rm.isRoleAvailable(RoleManager.ROLE_ASSISTANT);held=role&&rm.isRoleHeld(RoleManager.ROLE_ASSISTANT);}}android.app.KeyguardManager k=a.getSystemService(KeyguardManager.class);return new Status(Build.VERSION.SDK_INT>=27,role,held,k!=null&&k.isKeyguardLocked());}
 public void requestAssistantRole(){if(Build.VERSION.SDK_INT<29)throw new IllegalStateException("Роль Assistant требует Android 10+.");RoleManager rm=a.getSystemService(RoleManager.class);if(rm==null||!rm.isRoleAvailable(RoleManager.ROLE_ASSISTANT))throw new IllegalStateException("Роль Assistant недоступна на этом устройстве.");a.startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT),1901);}
 public Intent notificationAccessIntent(){return new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);}
}
