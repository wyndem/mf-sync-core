package cn.wenhaha.mf.sync;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONNull;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.wenhaha.mf.sync.http.SFQueryApi;
import cn.wenhaha.mf.sync.http.SFRestApi;
import cn.wenhaha.plugin.data.salesforce.SalesUser;
import cn.wenhaha.plugin.data.salesforce.api.ApiContextInfo;
import cn.wenhaha.sync.core.*;
import com.ejlchina.okhttps.HttpResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * SF 同步
 * --------
 *
 * @author ：wyndem
 * @Date ：Created in 2022-08-16 22:56
 */
public class SfSyncCore implements SyncDataCore<SalesUser> {


    private final Logger logger = LoggerFactory.getLogger(SfSyncCore.class);

    /**
     * 请求查询最大限制 5
     **/
    private final Integer maxCompositeRequestLimit = 5;

    private SfSyncCore() {
    }

    public static SyncDataCore<SalesUser> build() {
        SFHook sfHook = new SFHook(new SfSyncCore());
        return (SyncDataCore) Proxy.newProxyInstance(SfSyncCore.class.getClassLoader(), new Class[]{SyncDataCore.class}, sfHook);
    }


    @Override
    public List<Entity> query(DataContext<SalesUser> context, List<Column> fields, List<Query> queries) {

        StringBuffer sql = new StringBuffer("select ");
        List<Column> constantList = new ArrayList<>(fields.size() / 2);
        List<Column> customList = new ArrayList<>(fields.size() / 2);
        List<Column> columnList = new ArrayList<>(fields.size() / 2);
        fields.forEach(f -> {
            if (f.getType() == ColumnType.column) {
                sql.append(f.getColumn()).append(",");
                columnList.add(f);
            } else if (f.getType() == ColumnType.custom) {
                sql.append(f.getValue()).append(",");
                customList.add(f);
            } else if (f.getType() == ColumnType.constant) {
                constantList.add(f);
            }
        });
        sql.deleteCharAt(sql.length() - 1);
        sql.append(" from ")
                .append(context.getObjectName());

        buildCondition(sql, queries);

        logger.debug("生成后的soql：{}", sql);

        HttpResult query = SFQueryApi.query(sql.toString());
        if (query.getStatus() >= 400) {
            throw new RuntimeException(query.getBody().toString());
        }
        String body = query.getBody().toString();
        JSONObject response = JSONUtil.parseObj(body);
        boolean records = response.containsKey("records");
        if (!records) {
            logger.debug("{} 查询到的结果 {}",context.getObjectName(),body );
            return new ArrayList<>();
        }

        JSONArray array = response.getJSONArray("records");
        logger.debug("当前条数{}", array.size());
        List<Entity> arrayList = new ArrayList<>(array.size());

        for (int i = 0; i < array.size(); i++) {
            JSONObject jsonObject = array.getJSONObject(i);
            Entity entity = Entity.create(context.getObjectName());
            // 赋值字段
            columnList.forEach(c -> {
                Object o = jsonObject.get(c.getColumn());
                if(JSONNull.NULL.equals(o)){
                    entity.put(c.getColumn(),null);
                }else if (o instanceof  String && SFDateUtil.isDate(o.toString())){
                    entity.put(c.getColumn(),SFDateUtil.parse(o.toString()));
                }else {
                    entity.put(c.getColumn(),o);
                }
            });
            // 常量
            constantList.forEach(c -> entity.put(c.getColumn(), c.getValue()));
            // 自定义
            customList.forEach(c -> entity.put(c.getColumn(), jsonObject.getByPath(StrUtil.utf8Str(c.getValue()))));

            arrayList.add(entity);
        }
        return arrayList;

    }

    @Override
    public Entity echoQuery(DataContext<SalesUser> context, List<Column> column) {
        int i = 0;
        List<Object> ansList = new ArrayList<>(column.size());
        do {
            List<Column> collect = column.stream().skip(i).limit(maxCompositeRequestLimit).collect(Collectors.toList());

            List<String> soql = new ArrayList<>(maxCompositeRequestLimit);
            for (Column q : collect) {
                StringBuffer sql = new StringBuffer(StrUtil.utf8Str(q.getValue()));
                sql.append(" LIMIT 1");
                logger.debug("生成后的soql：{}", sql);
                soql.add(sql.toString());
            }
            HttpResult query = SFQueryApi.batchQuery(soql);
            // 请求失败了
            if (!ApiContextInfo.isSuccess(query.getStatus())) {
                logger.error("本次批量查询失败，批次{}  {}", i, query.getBody().toString());
                for (int j = 0; j < collect.size(); j++) {
                    ansList.add(null);
                }
            } else {
                JSONObject response = JSONUtil.parseObj(query.getBody().toString());
                JSONArray compositeResponse = response.getJSONArray("compositeResponse");
                for (int j = 0; j < compositeResponse.size(); j++) {
                    JSONObject jsonObject = compositeResponse.getJSONObject(j);
                    if (!ApiContextInfo.isSuccess(jsonObject.getInt("httpStatusCode"))) {
                        Integer referenceId = jsonObject.getInt("referenceId");
                        String s = soql.get(referenceId);
                        logger.error("查询失败，批次{}  条件：{}", i, s);
                        ansList.add(null);
                        continue;
                    }
                    JSONArray records = jsonObject
                            .getJSONObject("body")
                            .getJSONArray("records");

                    if (records == null || records.size() == 0) {
                        ansList.add(null);
                    } else {
                        JSONObject recordBody = records.getJSONObject(0);
                        recordBody.remove("attributes");
                        Set<String> keySet = recordBody.keySet();
                        String key = keySet.iterator().next();
                        ansList.add(recordBody.get(key));
                    }
                }
            }
            i = i + maxCompositeRequestLimit;
        } while (i < column.size());
        Entity entity = new Entity();

        for (int j = 0; j < column.size(); j++) {
            Column c = column.get(j);
            if (j < ansList.size()) {
                entity.put(c.getColumn(), ansList.get(j));
            } else {
                entity.put(c.getColumn(), null);
            }
        }
        return entity;
    }


    /**
     * <p>
     * 复合API最大请求25,查询最多5条子请求<br/>
     * 并发限制：  <br/>
     * 沙盒	5  <br/>
     * 正式	25  <br/>
     * 详情：  <br/>
     * https://developer.salesforce.com/docs/atlas.en-us.salesforce_app_limits_cheatsheet.meta/salesforce_app_limits_cheatsheet/salesforce_app_limits_platform_api.htm
     * </p>
     *
     * @Author: Wyndem
     * @DateTime: 2022-08-21 20:02
     */

    public <K> List<K> batchQuery(DataContext<SalesUser> context, Column field, List<List<Query>> queries) {

        int i = 0;
        List<K> ansList = new ArrayList<>(queries.size());
        do {
            List<List<Query>> collect = queries.stream().skip(i).limit(maxCompositeRequestLimit).collect(Collectors.toList());

            List<String> soql = new ArrayList<>(maxCompositeRequestLimit);
            for (List<Query> q : collect) {
                StringBuffer sql = new StringBuffer("select " + field);
                sql.append(" from ")
                        .append(context.getObjectName());
                buildCondition(sql, q);
                sql.append(" LIMIT 1");
                logger.debug("生成后的soql：{}", sql);
                soql.add(sql.toString());
            }
            HttpResult query = SFQueryApi.batchQuery(soql);
            // 请求失败了
            if (!ApiContextInfo.isSuccess(query.getStatus())) {
                logger.error("本次批量查询失败，批次{}  {}", i, query.getBody().toString());
                for (int j = 0; j < collect.size(); j++) {
                    ansList.add(null);
                }
            } else {
                JSONObject response = JSONUtil.parseObj(query.getBody().toString());
                JSONArray compositeResponse = response.getJSONArray("compositeResponse");
                for (int j = 0; j < compositeResponse.size(); j++) {
                    JSONObject jsonObject = compositeResponse.getJSONObject(j);
                    if (!ApiContextInfo.isSuccess(jsonObject.getInt("httpStatusCode"))) {
                        Integer referenceId = jsonObject.getInt("referenceId");
                        String s = soql.get(referenceId);
                        logger.error("查询失败，批次{}  条件：{}", i, s);
                        ansList.add(null);
                        continue;
                    }
                    JSONArray records = jsonObject
                            .getJSONObject("body")
                            .getJSONArray("records");

                    if (records == null || records.size() == 0) {
                        ansList.add(null);
                    } else {
                        ansList.add((K) records.getJSONObject(0).get(field));
                    }
                }
            }
            i = i + maxCompositeRequestLimit;
        } while (i < queries.size());

        return ansList;
    }


    /**
     * rest API文档
     * https://developer.salesforce.com/docs/atlas.en-us.api_rest.meta/api_rest/resources_composite_sobjects_collections.htm
     * <br/>
     * 因使用的上述api，所以本方法中，只能使用Id作为唯一主键 <br/>
     * Q:为什么不用upsert，而是要分开来调用插入和更新？ <br/>
     * A:upsert接口只能使用外部主键最为唯一键，大多数场景中id还是主流的 <br/>
     *
     * @param context 上下文
     * @param key     主键  id 不能是其他字段了
     * @param data    数据 需要更新的数据
     * @return
     */
    @Override
    public List<SyncReport> upload(DataContext<SalesUser> context, String key, List<Entity> data) {

        // 更新只能通过id
        List<Entity> updateData = data.stream()
                .filter(d -> d.containsKey(key))
                .collect(Collectors.toList());
        data.removeAll(updateData);

        List<SyncReport> reportList = new ArrayList<>();

        try {
            List<SyncReport> update = SFRestApi.update(key, updateData);
            update.forEach(d -> {
                d.setPluginsCode(context.getPluginsCode());
                d.setUserId(context.getUserId());
            });
            reportList.addAll(update);
        } catch (Exception e) {
            e.printStackTrace();
            updateData.forEach(d -> {
                SyncErrorReport error = new SyncErrorReport(context.getPluginsCode(), d.getTableName(),
                        context.getUserId(), d.get(key).toString(), ExceptionUtil.getSimpleMessage(e), e, d);
                reportList.add(error);
            });
        }


        try {
            // 新插入的只能根据id来
            List<SyncReport> insert = SFRestApi.insert(key, data);
            insert.forEach(d -> {
                d.setPluginsCode(context.getPluginsCode());
                d.setUserId(context.getUserId());
            });
            reportList.addAll(insert);
        } catch (Exception e) {
            e.printStackTrace();
            data.forEach(d -> {
                SyncErrorReport error = new SyncErrorReport(context.getPluginsCode(), d.getTableName(),
                        context.getUserId(), null, ExceptionUtil.getSimpleMessage(e), e, d);
                reportList.add(error);
            });
        }

        return reportList;
    }


    private void buildCondition(StringBuffer sql, List<Query> queries) {
        if (queries == null || queries.size() == 0) {
            return;
        }

        sql.append(" where ");


        boolean orStart = false;
        // 上一次的join
        Join preJoin = null;
        for (Query q : queries) {
            String condition = condition(q);
            if (StrUtil.isEmpty(condition)) {
                continue;
            }

            // 拼接 AND OR
            Join join = q.getJoin();

            if (preJoin == null) {
                sql.append(" (").append(condition).append(") ");

            } else if (preJoin == Join.AND && !orStart) {
                sql.append(preJoin.getValue());
                sql.append(" (").append(condition).append(") ");
            } else if (preJoin == Join.AND) {
                // and 在 or 括号中
                sql.append(preJoin.getValue()).append(" ");
                sql.append(condition).append(" ");
            } else if (preJoin == Join.OR && !orStart) {
                // or 一旦开始，后面只会有 or 拼接，不会结束
                sql.append(preJoin.getValue()).append(" ");
                sql.append("( ").append(condition).append(" ");
                orStart = true;
            } else if (preJoin == Join.OR) {
                //开始新的or
                sql.append(") ").append(Join.OR).append(" ( ").append(condition).append(" ");
            }
            if (join != null) {
                preJoin = join;
            }

        }
        if (orStart) {
            sql.append(" )");
        }
        int where = sql.lastIndexOf("where");
        // 没有加条件
        if (sql.length() == where + 5) {
            sql.delete(where, where + 5);
        }
    }


    private String condition(Query query) {
        StringBuilder append = new StringBuilder();
        if (query.getType() != QueryType.Custom) {
            append.append(query.getName());
        }
        switch (query.getType()) {
            case NUILL:
                Signal signal = query.getSignal();
                if (signal == Signal.EQ) {
                    append.append(" is NULL");
                    break;
                } else if (signal == Signal.NQ) {
                    append.append(" is not NULL");
                    break;
                } else {
                    return null;
                }
            case NUMBER:
                append.append(query.getSignal().getId()).append(query.getValue());
                break;
            case STRING:
                append.append(query.getSignal().getId()).append("'").append(query.getValue()).append("'");
                break;
            case Custom:
                append.append(query.getValue());
                break;
            default:
                return null;
        }
        return append.toString();
    }

}
