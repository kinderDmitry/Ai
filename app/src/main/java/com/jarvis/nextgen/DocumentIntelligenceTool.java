package com.jarvis.nextgen;

import android.content.Context;
import android.net.Uri;
import org.json.JSONObject;

/** Real local document pipeline: text files are decoded; PDFs are rendered and OCR'd page-by-page. */
public final class DocumentIntelligenceTool implements Tool {
    private final Context context;
    private volatile Uri pending;
    public DocumentIntelligenceTool(Context context) { this.context=context.getApplicationContext(); }
    public void setDocument(Uri uri) { pending=uri; }
    @Override public String name(){return "document_intelligence";}
    @Override public String description(){return "Inspect local text documents and PDFs; PDF pages are rendered and processed with on-device OCR.";}
    @Override public boolean canHandle(String input){String s=input==null?"":input.toLowerCase();return s.contains("pdf")||s.contains("документ")||s.contains("файл")||s.contains("проанализируй")||s.contains("прочитай");}
    @Override public Models.ToolResult execute(String input){
        Uri uri=pending;
        if(uri==null)return Models.ToolResult.fail(Models.ResultCode.FAILED,"Сначала выберите документ.");
        DocumentWorkspace.Result inspected=new DocumentWorkspace(context).inspect(uri);
        String mime=context.getContentResolver().getType(uri);
        String name=uri.getLastPathSegment()==null?"":uri.getLastPathSegment().toLowerCase();
        if("application/pdf".equals(mime)||name.endsWith(".pdf")){
            PdfOcrPipeline.Result r=new PdfOcrPipeline(context).process(uri);
            if(!r.success)return Models.ToolResult.fail(Models.ResultCode.FAILED,r.message);
            return Models.ToolResult.ok("PDF OCR\nСтраниц: "+r.pages+"\n\n"+r.text);
        }
        if(inspected.success)return Models.ToolResult.ok(inspected.message);
        if(!inspected.supported)return Models.ToolResult.fail(Models.ResultCode.UNSUPPORTED,inspected.message);
        return Models.ToolResult.fail(Models.ResultCode.FAILED,inspected.message);
    }
    @Override public JSONObject schema(){return new JSONObject().put("name",name()).put("description",description()).put("type","object").put("additionalProperties",false).put("properties",new JSONObject().put("request",new JSONObject().put("type","string").put("description","Что сделать с выбранным документом"))).put("required",new org.json.JSONArray().put("request"));}
}
