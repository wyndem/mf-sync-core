package cn.wenhaha;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.db.Entity;
import cn.wenhaha.data.plugin.mysql.bean.MysqlSource;
import cn.wenhaha.datasource.IUserContext;
import cn.wenhaha.mf.sync.DataContext;
import cn.wenhaha.mf.sync.MySqlSyncCore;
import cn.wenhaha.sync.core.*;
import org.junit.Before;
import org.junit.Test;
import java.util.List;

/**
 * Unit test for simple App.
 */
public class MysqlTest {

    @Before
    public void init() {

    }

    private IUserContext<MysqlSource> getUserContext() {
        return new UserContextMysql();
    }


    @Test
    public void query() {

        DataContext<MysqlSource> dataContext = new DataContext<MysqlSource>();
        dataContext.setObjectName("user");
        dataContext.setUserContext(getUserContext());
        MySqlSyncCore mySqlSyncCore = new MySqlSyncCore();


        Query query = new Query();
        query.setName("name");
        query.setSignal(Signal.EQ);
        query.setType(QueryType.STRING);
        query.setValue("333");
        query.setJoin(Join.AND);

        Query query1 = Query.builder()
                .name("online")
                .type(QueryType.NUMBER)
                .join(Join.AND)
                .value(1)
                .signal(Signal.GEQ).build();

        Query query2 = Query.builder().type(QueryType.Custom)
                .value("last_login_model='MI 8'")
//                .join(Join.AND)
                .build();

        Query aa = Query.builder().name("login_out_time")
                .join(Join.AND)
                .type(QueryType.NUILL).signal(Signal.EQ).build();


        Column name = Column.builder()
                .column("name")
                .type(ColumnType.column)
                .build();

        Column c1 = Column.builder()
                .column("Name__c")
                .value(2)
                .type(ColumnType.constant)
                .build();
        List<Entity> query3 = mySqlSyncCore.query(dataContext, CollUtil.toList(name,c1),
                CollUtil.toList(query, query1, aa, query2));
        System.out.println(query3);

    }


    @Test
    public void upload() {

        DataContext<MysqlSource> dataContext = new DataContext<MysqlSource>();
        dataContext.setObjectName("user");
        dataContext.setUserContext(getUserContext());
        MySqlSyncCore mySqlSyncCore = new MySqlSyncCore();


        Entity en = Entity.create("user")
                .set("id",10)
                .set("last_login_ip", "23");

        Entity en1 = Entity.create("user")
                .set("id",7)
                .set("last_login_ip", "23");


        Entity entity = mySqlSyncCore.echoQuery(dataContext, CollUtil.toList());
//        List<SyncErrorReport> reports = mySqlSyncCore.upload(dataContext, "id", CollUtil.toList(en,en1));
//        reports.forEach(e-> System.out.println(e.getSimpleError()));

    }

    @Test
    public void batch(){
        DataContext<MysqlSource> dataContext = new DataContext<MysqlSource>();
        dataContext.setObjectName("user");
        dataContext.setUserContext(getUserContext());
        MySqlSyncCore mySqlSyncCore = new MySqlSyncCore();
        Query online = Query.builder()
                .name("online")
                .type(QueryType.NUMBER)
                .value(1)
                .signal(Signal.GEQ).build();
//        List<Integer> id = mySqlSyncCore.batchQuery(dataContext, "id", CollUtil.toList(CollUtil.toList(online),CollUtil.toList(online)));
//        System.out.println(id);
        Entity entity = mySqlSyncCore.echoQuery(dataContext, CollUtil.toList());


    }


}
