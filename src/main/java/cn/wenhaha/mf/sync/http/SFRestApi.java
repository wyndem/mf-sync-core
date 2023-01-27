package cn.wenhaha.mf.sync.http;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.wenhaha.mf.sync.SFDateUtil;
import cn.wenhaha.sync.core.SyncErrorReport;
import cn.wenhaha.sync.core.SyncReport;
import cn.wenhaha.sync.core.SyncSuccessReport;
import com.ejlchina.okhttps.HttpResult;
import com.ejlchina.okhttps.SHttpTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 *
 * rest 更新或者创建
 * --------
 * @link https://developer.salesforce.com/docs/atlas.en-us.api_rest.meta/api_rest/resources_composite_sobjects_collections_update.htm
 * @author ：wyndem
 * @Date ：Created in 2022-08-22 19:37
 */
public class SFRestApi {

    private static String url ="/services/data/v55.0/composite/sobjects";
    private static final Logger logger = LoggerFactory.getLogger(SFRestApi.class);

    public static  List<SyncReport>  update(String key, List<Entity> data){

        List<RequestParam> requestBody = requestBody(data);
        List<SyncReport> syncReports = new ArrayList<>();

        for (RequestParam request:requestBody){

            SHttpTask sHttpTask = SFHttpClient.getHttp()
                    .sync(SFHttpClient.getUrl(url))
                    .setBodyPara(request.getJson());
            HttpResult patch = sHttpTask.patch();
            try {
                syncReports.addAll(handle(request,patch,data,key));
            } catch (Exception e) {
                e.printStackTrace();
                data.forEach(d->{
                    SyncErrorReport error = new SyncErrorReport(null, d.getTableName(),
                            null, StrUtil.utf8Str(d.get(key)), ExceptionUtil.getSimpleMessage(e), e,d);
                    syncReports.add(error);
                });
            }
        }

        return syncReports;
    }






    public static  List<SyncReport>  insert(String key,List<Entity> data){
        List<RequestParam> requestBody = requestBody(data);

        List<SyncReport> syncReports = new ArrayList<>();

        for (RequestParam request:requestBody){
            SHttpTask sHttpTask = SFHttpClient.getHttp()
                    .sync(SFHttpClient.getUrl(url))
                    .setBodyPara(request.getJson());
            HttpResult post = sHttpTask.post();

            try {
                syncReports.addAll(handle(request,post,data,key));
            } catch (Exception e) {
                e.printStackTrace();
                data.forEach(d->{
                    SyncErrorReport error = new SyncErrorReport(null, d.getTableName(),
                            null, StrUtil.utf8Str(d.get(key)), ExceptionUtil.getSimpleMessage(e), e,d);
                    syncReports.add(error);
                });
            }

        }
        return syncReports;
    }


    private static   List<SyncReport>  handle(RequestParam request, HttpResult result, List<Entity> data, String key){

        List<SyncReport> syncReports = new ArrayList<>();

        if (result.getStatus()!=200){
            RuntimeException e = new RuntimeException(result.getBody().toString());
            e.printStackTrace();
            request.getData().forEach(d->{
                // 这里的pluginsCode 会在 后面补上
                SyncErrorReport error = new SyncErrorReport(null, d.getTableName(),
                        null, StrUtil.utf8Str(d.get(key)), ExceptionUtil.getSimpleMessage(e), e,d);
                syncReports.add(error);
            });
            return syncReports;
        }

        JSONArray array = JSONUtil.parseArray(result.getBody().toString());
        for (int i = 0; i < array.size(); i++) {
            JSONObject response = array.getJSONObject(i);
            Entity entity = data.get(i);
            if (!response.getBool("success")){
                String errors = response.getJSONArray("errors").toString();
                RuntimeException runtimeException = new RuntimeException(errors);
                SyncErrorReport error = new SyncErrorReport( null, entity.getTableName(),
                        null, StrUtil.utf8Str(entity.get(key)), errors, runtimeException,entity);
                syncReports.add(error);
            }else {
                entity.set(key,response.getStr("id"));
                SyncSuccessReport syncSuccessReport = new SyncSuccessReport(null,
                        entity.getTableName(), null, response.getStr("id"), entity);
                syncReports.add(syncSuccessReport);
            }
        }
        return syncReports;
    }



    private  static List<RequestParam> requestBody(List<Entity> data){
        int max=200;
        int i = 0;
        List<RequestParam> bodyList = new ArrayList<>();
        JSONObject attributes = new JSONObject();
        do{
            List<Entity> collect = data.stream().skip(i).limit(max).collect(Collectors.toList());
            JSONObject body = new JSONObject();
            JSONArray records = new JSONArray();
            body.put("allOrNone",false);
            body.put("records",records);
            collect.forEach(e->{
                if (!attributes.containsKey("type")){
                    attributes.put("type",e.getTableName());
                }
                handle(e);
                JSONObject request = new JSONObject();
                request.put("attributes",attributes);
                request.putAll(e);
                records.add(request);
            });
            if (records.size()!=0){
                RequestParam requestParam = new RequestParam(body.toString(), collect);
                bodyList.add(requestParam);
            }
            i=i+max;
        }while (i < data.size());

        return bodyList;
    }



    private static void handle(Entity entity){
        Set<String> keySet = entity.keySet();
        for (String key : keySet) {
            Object o = entity.get(key);
            boolean isDate = o instanceof Date;
            if (isDate) {
                logger.debug(key + "为日期类型，正在进行处理");
                entity.put(key, SFDateUtil.fromStr(o));
            }
        }

    }
}
