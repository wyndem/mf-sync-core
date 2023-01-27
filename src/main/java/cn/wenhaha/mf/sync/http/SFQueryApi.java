package cn.wenhaha.mf.sync.http;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.wenhaha.mf.sync.RetryUtil;
import cn.wenhaha.plugin.data.salesforce.api.ApiContextInfo;
import com.ejlchina.okhttps.HttpResult;
import com.ejlchina.okhttps.SHttpTask;

import java.util.List;
import java.util.Optional;

public class SFQueryApi {

    private static final Integer maxRetryCount = 3;

    public static HttpResult query(String soql) {
        Optional<HttpResult> httpResult = RetryUtil.call(() -> {
            SHttpTask task = SFHttpClient.getHttp()
                    .sync(SFHttpClient.getUrl(ApiContextInfo.query))
                    .addUrlPara("q", soql);
            return task.get();
        }, maxRetryCount);

        return httpResult.orElseThrow(() -> new RuntimeException("query响应结果为空"));
    }

    public static HttpResult batchQuery(List<String> soql) {

        JSONObject jsonObject = new JSONObject();
        JSONArray array = new JSONArray(soql.size());
        jsonObject.put("compositeRequest", array);

        for (int i = 0; i < soql.size(); i++) {

            JSONObject request = new JSONObject();
            request.put("method", "GET");
            request.put("referenceId", Integer.toString(i));
            request.put("url", ApiContextInfo.query + "?q=" + soql.get(i));
            array.add(request);
        }

        Optional<HttpResult> httpResult = RetryUtil.call(() -> {
            SHttpTask task = SFHttpClient.getHttp()
                    .sync(SFHttpClient.getUrl(ApiContextInfo.COMPOSITEURL))
                    .setBodyPara(jsonObject.toString());
            return task.post();
        }, maxRetryCount);


        return httpResult.orElseThrow(() -> new RuntimeException("batchQuery响应结果为空"));
    }
}
