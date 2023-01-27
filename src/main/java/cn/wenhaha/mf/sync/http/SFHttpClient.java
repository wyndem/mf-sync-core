package cn.wenhaha.mf.sync.http;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.wenhaha.mf.sync.RetryUtil;
import cn.wenhaha.plugin.data.salesforce.SalesUser;
import cn.wenhaha.plugin.data.salesforce.http.OkHttpUtil;
import cn.wenhaha.plugin.data.salesforce.http.interceptor.LogInterceptor;
import com.ejlchina.okhttps.HTTP;
import okhttp3.ConnectionPool;
import okhttp3.Request;

import java.util.concurrent.TimeUnit;


/**
 * sf客户端
 * --------
 *
 * @author ：wyndem
 * @Date ：Created in 2022-08-21 17:00
 */
public class SFHttpClient {

    private static  HTTP http;

    public static final ThreadLocal<SalesUser> salesUser =  new ThreadLocal();


    public static String getUrl(String path){
        SalesUser salesUser = SFHttpClient.salesUser.get();
        if (salesUser!=null){
            JSONObject jsonObject = JSONUtil.parseObj(salesUser.getLoginJson());
            return  jsonObject.getStr("instance_url")+path;
        }
        return path;
    }

    public static String getToken(){
        SalesUser salesUser = SFHttpClient.salesUser.get();
        if (salesUser!=null ){
            return  salesUser.getToken();
        }
        return    "";
    }



    public static synchronized HTTP getHttp() {
        if (http==null){
            buildHttp();
        }
        return http;
    }


    private static void buildHttp() {
        http =  HTTP.builder().bodyType("json").config((b)->{
            OkHttpUtil.build(b);
            b.connectionPool(new ConnectionPool(32, 5L, TimeUnit.SECONDS));
            b.addInterceptor(new LogInterceptor());
            b.addInterceptor(chain -> {
                String s =getToken();
                Request request = chain.request();
                if (StrUtil.isNotEmpty(s)){
                    request =request.newBuilder()
                            .addHeader("Authorization","Bearer "+ s).build();
                }
                return chain.proceed(request);
            });
        }).build();
    }




}
