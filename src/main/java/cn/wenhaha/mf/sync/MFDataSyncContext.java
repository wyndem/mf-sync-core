package cn.wenhaha.mf.sync;

import cn.hutool.db.Entity;
import cn.wenhaha.data.plugin.mysql.bean.MysqlSource;
import cn.wenhaha.plugin.data.salesforce.SalesUser;
import cn.wenhaha.sync.core.*;
import org.pf4j.Extension;

import java.util.List;

/**
 * 同步中间库
 * --------
 *
 * @author ：wyndem
 * @Date ：Created in 2022-08-16 20:53
 */
@Extension
public class MFDataSyncContext extends BaseSyncCore {

    private final SyncDataCore<SalesUser> sf = SfSyncCore.build();
    private final SyncDataCore<MysqlSource> mysql = new MySqlSyncCore();

    @Override
    public List<Entity> query(List<Column> list, List<Query> list1) {
        if ("SFRestApi".equals(getPluginCode())) {
            DataContext<SalesUser> salesUserDataContext = new DataContext<>();
            initDataContext(salesUserDataContext);
            return sf.query(salesUserDataContext, list, list1);
        } else if ("mysqlHikariC".equals(getPluginCode())) {
            DataContext<MysqlSource> mysqlSyncContext = new DataContext<>();
            initDataContext(mysqlSyncContext);
            return mysql.query(mysqlSyncContext, list, list1);
        }
        return null;
    }

    @Override
    public Boolean isEchoQuery(String type) {
        if ("SFRestApi".equals(getPluginCode()) && "soql".equals(type)) {
            return true;
        }

        return "mysqlHikariC".equals(getPluginCode()) && "sql".equals(type);
    }

    @Override
    public Entity echoQuery(List<Column> list) {
        // 本插件中，sf只支持soql,mysql 只支持 sql
        list.forEach((column) -> {
            if (column.getValue()!=null){
                column.setValue(CustomKit.rmPrefix(column.getValue().toString()));
            }
        });

        if ("SFRestApi".equals(getPluginCode())) {
            DataContext<SalesUser> salesUserDataContext = new DataContext<>();
            initDataContext(salesUserDataContext);
            return sf.echoQuery(salesUserDataContext, list);
        } else if ("mysqlHikariC".equals(getPluginCode())) {
            DataContext<MysqlSource> mysqlSyncContext = new DataContext<>();
            initDataContext(mysqlSyncContext);
            return mysql.echoQuery(mysqlSyncContext, list);
        }
        return null;
    }


    @Override
    public List<SyncReport> upload(String s, List<Entity> list) {

        if ("SFRestApi".equals(getPluginCode())) {
            DataContext<SalesUser> salesUserDataContext = new DataContext<>();
            initDataContext(salesUserDataContext);
            return sf.upload(salesUserDataContext, s, list);
        } else if ("mysqlHikariC".equals(getPluginCode())) {
            DataContext<MysqlSource> mysqlSyncContext = new DataContext<>();
            initDataContext(mysqlSyncContext);
            return mysql.upload(mysqlSyncContext, s, list);
        }
        return null;
    }


    private <T> void initDataContext(DataContext<T> context) {
        context.setUserId(getUserId());
        context.setUserContext(getUserContext());
        context.setObjectName(getObjectName());
        context.setPluginsCode(getPluginCode());
    }

}
