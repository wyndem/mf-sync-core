package cn.wenhaha.mf.sync;

import cn.wenhaha.mf.sync.http.SFHttpClient;
import cn.wenhaha.plugin.data.salesforce.SalesUser;
import cn.wenhaha.plugin.data.salesforce.api.Oauth2Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @author ：wyndem
 * @Date ：Created in 2022-08-21 17:47
 */
public class SFHook implements InvocationHandler {

    private final SfSyncCore object;

    private final Logger logger = LoggerFactory.getLogger(SfSyncCore.class);


    public SFHook(SfSyncCore object) {
        this.object = object;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //前置
        String name = method.getName();
        boolean setLocal = name.equals("query") || name.equals("echoQuery") || name.equals("upload");

        try {
            if (setLocal){
                DataContext<SalesUser>  dataContext = (DataContext<SalesUser> ) args[0];
                SalesUser dataSource = dataContext.getDataSource(SalesUser.class);
                if (Oauth2Api.checkTokenTimeout(dataSource.getLoginJson(),60)){
                    logger.debug("token失效，正在更新token");
                    dataSource = dataContext.updateDataSource(SalesUser.class);
                }
                SFHttpClient.salesUser.set(dataSource);
            }
            Object invoke = method.invoke(this.object, args);

            return invoke;
        } finally {
            if (setLocal){
                SFHttpClient.salesUser.remove();
            }
        }
    }
}
