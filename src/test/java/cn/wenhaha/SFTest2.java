package cn.wenhaha;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.db.Entity;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.wenhaha.mf.sync.*;
import cn.wenhaha.plugin.data.salesforce.SalesUser;
import cn.wenhaha.plugin.data.salesforce.api.ApiContextInfo;
import cn.wenhaha.sync.core.*;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;


public class SFTest2 {


    @Test
    public void query(){
        SFContext sfContext = new SFContext();
        SyncDataCore sfSyncCore = SfSyncCore.build();

        DataContext<SalesUser> dataContext = new DataContext<SalesUser>();
        dataContext.setUserContext(sfContext);
        dataContext.setObjectName("LiveCampaign__c");


        Query query = new Query();
        query.setName("id");
        query.setSignal(Signal.EQ);
        query.setType(QueryType.STRING);
        query.setJoin(Join.AND);
        query.setValue("a0ZN000000IevyuMAB");

        Query query1 = new Query();
        query1.setName("name");
        query1.setSignal(Signal.EQ);
        query1.setType(QueryType.STRING);
        query1.setValue("王鑫");

        Query query2 = new Query();
        query2.setType(QueryType.Custom);
        query2.setValue("``");


        Column name = Column.builder()
                .column("Name")
                .type(ColumnType.column)
                .build();

        Column custom = Column.builder()
                .column("sname")
                .type(ColumnType.custom)
                .value("LiveWeChatId__r.WxOpenServiceId__r.Name")
                .build();

        Column appic = Column.builder()
                .column("appId")
                .type(ColumnType.custom)
                .value("LiveWeChatId__r.WxOpenServiceId__r.AppId__c")
                .build();

        Column custom1 = Column.builder()
                .column("cName")
                .type(ColumnType.custom)
                .value("LiveWeChatId__r.Name")
                .build();

        Column cc = Column.builder()
                .column("tt")
                .type(ColumnType.constant)
                .value("333")
                .build();


        List<Entity> entities = sfSyncCore.query(dataContext, CollUtil.toList(name,appic,custom1,custom,cc), CollUtil.toList(query,query1));
        System.out.println(entities);

    }



    @Test
    public void batch(){
        SFContext sfContext = new SFContext();
        SyncDataCore<SalesUser> sfSyncCore = SfSyncCore.build();

        DataContext<SalesUser> dataContext = new DataContext<SalesUser>();
        dataContext.setUserContext(sfContext);
        dataContext.setObjectName("Account");

        Query query = new Query();
        query.setName("id");
        query.setSignal(Signal.EQ);
        query.setType(QueryType.STRING);
        query.setValue("0012y00000GLo22AAD");


        List<List<Query>> arrayLists = CollUtil.toList(CollUtil.toList(query));
        ArrayList<Query> queries = CollUtil.toList(Query.builder()
                .name("id")
                .signal(Signal.EQ)
                .value("0012y000007IXlxAAG")
                .type(QueryType.STRING)
                .build()
        );

        for (int i = 0; i < 261; i++) {
            arrayLists.add(CollUtil.toList(query));
        }
        arrayLists.add(queries);
        System.out.println(arrayLists.size());
//        List<String> id = sfSyncCore.batchQuery(dataContext, "Name",arrayLists);
//        System.out.println(id);
    }



    @Test
    public void testJson(){
        System.out.println(SFDateUtil.isDate("2021a-03-22T08:05:38.000+0000Z"));
    }

    @Test
    public void testRetry(){
        RetryUtil.call(() -> {
            if ( 1==1){
                 int i = 1/0;
            }
            return null;
        }, 2);
    }


    @Test
    public void  upload(){
        SFContext sfContext = new SFContext();
        SyncDataCore<SalesUser> sfSyncCore = SfSyncCore.build();

        DataContext<SalesUser> dataContext = new DataContext<SalesUser>();
        dataContext.setUserContext(sfContext);
        dataContext.setObjectName("Account");


        Entity set = Entity.create("Sync_Log__c")
                .set("Id", "a0g0l000004Q5yoAAC")
                .set("Sync_ObjectName__c","测试2")
                .set("Sync_Handle__c", "e456");


        Entity set1 = Entity.create("Sync_Log__c")
                .set("Id","a0g0l000004QKjJAAW")
                .set("Sync_ObjectNamae__c","测试3")
                .set("Sync_Handle__c", "e456");

        Entity set2 = Entity.create("Sync_Log__c")
                .set("Sync_ObjectNamae__c","测试3")
                .set("Sync_Handle__c", "e456");


//        List<SyncErrorReport> error = sfSyncCore.upload(dataContext, "Id", CollUtil.toList(set, set1,set2));
//
//        error.forEach(e->{
//            System.out.println(e.update());
//            System.out.println(e.getData().toString());
//            System.out.println(e.getSimpleError());
//        });


    }
}
