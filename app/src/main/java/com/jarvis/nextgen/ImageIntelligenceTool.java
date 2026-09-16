package com.jarvis.nextgen;

import android.content.Context;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Real OCR tool. It never fabricates recognition results. */
public final class ImageIntelligenceTool implements Tool {
 private final Context context;
 private volatile InputImage pendingImage;
 public ImageIntelligenceTool(Context context){this.context=context.getApplicationContext();}
 public void setImage(InputImage image){pendingImage=image;}
 @Override public String name(){return "image_ocr";}
 @Override public String description(){return "Extract text from a real camera/gallery image using on-device ML Kit OCR.";}
 @Override public boolean canHandle(String input){String s=input==null?"":input.toLowerCase();return s.contains("ocr")||s.contains("текст с фото")||s.contains("распознай текст")||s.contains("прочитай фото")||s.contains("изображение");}
 @Override public Models.ToolResult execute(String input){
  InputImage image=pendingImage;
  if(image==null)return Models.ToolResult.fail(Models.ResultCode.FAILED,"Для OCR сначала выберите или снимите изображение.");
  final CountDownLatch latch=new CountDownLatch(1); final AtomicReference<String> text=new AtomicReference<>(); final AtomicReference<Exception> error=new AtomicReference<>();
  var recognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
  recognizer.process(image).addOnSuccessListener(r->{text.set(r.getText());latch.countDown();}).addOnFailureListener(e->{error.set(e);latch.countDown();});
  try{if(!latch.await(12,TimeUnit.SECONDS))return Models.ToolResult.fail(Models.ResultCode.TIMEOUT,"OCR превысил лимит времени.");}
  catch(InterruptedException e){Thread.currentThread().interrupt();return Models.ToolResult.fail(Models.ResultCode.CANCELLED,"OCR отменён.");}
  finally{recognizer.close();}
  if(error.get()!=null)return Models.ToolResult.fail(Models.ResultCode.FAILED,"Не удалось распознать текст: "+error.get().getMessage());
  String value=text.get(); if(value==null||value.trim().isEmpty())return Models.ToolResult.fail(Models.ResultCode.FAILED,"Текст на изображении не обнаружен.");
  return Models.ToolResult.ok(value.trim());
 }
}
