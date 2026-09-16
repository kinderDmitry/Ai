package com.jarvis.nextgen;

import android.content.Context;import android.view.*;import android.widget.TextView;

public final class AdaptiveUi {
 public static void apply(TextView v,float sp){v.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP,sp);v.setIncludeFontPadding(true);v.setMinHeight(dp(v.getContext(),44));v.setContentDescription(v.getText());}
 public static int dp(Context c,int x){return Math.round(x*c.getResources().getDisplayMetrics().density);}
 public static void enableTouchTarget(View v){v.setMinimumHeight(dp(v.getContext(),48));v.setMinimumWidth(dp(v.getContext(),48));}
 private AdaptiveUi(){}
}
