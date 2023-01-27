package cn.wenhaha.mf.sync;

import cn.hutool.core.bean.BeanUtil;
import cn.wenhaha.datasource.IUserContext;

/**
 * 数据上下文
 * --------
 *
 * @author ：wyndem
 * @Date ：Created in 2022-08-16 23:04
 */
public class DataContext<T> {

    private IUserContext<T> userContext;

    private Integer userId;

    private String objectName;

    private  String pluginsCode;


    public T getDataSource(Class<T> t) {
        return userContext.getUserInfo(getUserId());
    }

    public T updateDataSource(Class<T> t) {
        return  userContext.updateUser(getUserId());
    }

    public IUserContext<T> getUserContext() {
        return userContext;
    }

    public void setUserContext(IUserContext<T> userContext) {
        this.userContext = userContext;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public String getPluginsCode() {
        return pluginsCode;
    }

    public void setPluginsCode(String pluginsCode) {
        this.pluginsCode = pluginsCode;
    }
}
