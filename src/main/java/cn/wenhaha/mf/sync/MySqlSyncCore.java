package cn.wenhaha.mf.sync;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import cn.wenhaha.data.plugin.mysql.bean.MysqlSource;
import cn.wenhaha.sync.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * mysql 同步类
 * --------
 *
 * @author ：wyndem
 * @Date ：Created in 2022-08-16 22:56
 */

public class MySqlSyncCore implements SyncDataCore<MysqlSource> {

    private final Logger logger = LoggerFactory.getLogger(MySqlSyncCore.class);


    @Override
    public List<Entity> query(DataContext<MysqlSource> context, List<Column> fields, List<Query> queries) {

        MysqlSource dataSource1 = context.getDataSource(MysqlSource.class);

        DataSource dataSource = dataSource1.getDataSource();
        StringBuffer sql = new StringBuffer("select ");
        sql.append(buildField(context.getObjectName(),fields.toArray(new Column[0])));
        sql.append(" from ")
                .append(context.getObjectName())
                .append(" AS ").append(context.getObjectName());

        buildCondition(sql, queries,context.getObjectName());
        try {
            Object[] params = getValue(queries).toArray();
            logger.debug("生成后的sql：{}  ,参数： {}", sql, params);
            return Db.use(dataSource).query(sql.toString(), params);
        } catch (SQLException e) {
            logger.error("查询出现问题-> {}", ExceptionUtil.stacktraceToString(e));
            throw new RuntimeException(e);
        }

    }


    @Override
    public Entity echoQuery(DataContext<MysqlSource> context, List<Column> column) {
        DataSource dataSource = context.getDataSource(MysqlSource.class).getDataSource();
        Db db = Db.use(dataSource);
        Entity entity = new Entity();
        for (Column c : column) {

            try {
                Entity e = db.queryOne(StrUtil.utf8Str(c.getValue()));
                if (e == null || e.get(c.getColumn()) == null) {
                    entity.put(c.getColumn(), null);
                }
                assert e != null;
                entity.put(c.getColumn(), e.get(c.getColumn()));
            } catch (Exception e) {
                e.printStackTrace();
                entity.put(c.getColumn(), null);
            }
        }

        return entity;
    }


    public <K> List<K> batchQuery(DataContext<MysqlSource> context, Column field, List<List<Query>> queries) {
        DataSource dataSource = context.getDataSource(MysqlSource.class).getDataSource();
        Db db = Db.use(dataSource);

        List<K> ansList = new ArrayList<>(queries.size());
        // 确保顺序执行
        for (List<Query> qs : queries) {
            StringBuffer sql = new StringBuffer("select ");
            sql.append(buildField(context.getObjectName(),field))
                    .append(" ").append(" from ")
                    .append(context.getObjectName())
                    .append(" AS ").append(context.getObjectName());


            buildCondition(sql, qs,context.getObjectName());
            Object[] params = getValue(qs).toArray();
            try {
                logger.debug("生成后的sql：{}  ,参数： {}", sql, params);
                Entity entity = db.queryOne(sql.toString(), params);
                if (entity != null && entity.get(field.getColumn()) != null) {
                    ansList.add((K) entity.get(field.getColumn()));
                } else {
                    ansList.add(null);
                }
            } catch (Exception e) {
                logger.error("查询出现问题-> {}", ExceptionUtil.stacktraceToString(e));
                ansList.add(null);
            }
        }

        return ansList;

    }

    /**
     * 上传数据
     *
     * @param context
     * @param key
     * @param data
     * @return
     */
    @Override
    public List<SyncReport> upload(DataContext<MysqlSource> context, String key, List<Entity> data) {

        DataSource dataSource = context.getDataSource(MysqlSource.class).getDataSource();

        // 新增数据
        List<Entity> newData = data.stream().filter(e -> e.get(key) == null)
                .collect(Collectors.toList());

        List<SyncReport> reportList = new ArrayList<>();

        data.removeAll(newData);
        newData.forEach(d -> {
            try {
                Db.use(dataSource).insertForGeneratedKey(d);
                SyncSuccessReport syncSuccessReport = new SyncSuccessReport(context.getPluginsCode(),
                        d.getTableName(), context.getUserId(), (Serializable) d.get(key), d);
                reportList.add(syncSuccessReport);
            } catch (Exception e) {
                logger.error("数据保存出现问题-> {}", ExceptionUtil.stacktraceToString(e));
                SyncErrorReport syncErrorReport = new SyncErrorReport(context.getPluginsCode(),
                        d.getTableName(), context.getUserId(), null, e.getMessage(), e, d);
                reportList.add(syncErrorReport);
            }
        });

        data.forEach(d -> {
            try {
                Db.use(dataSource).insertOrUpdate(d, key);
                SyncSuccessReport syncSuccessReport = new SyncSuccessReport(context.getPluginsCode(),
                        d.getTableName(), context.getUserId(), (Serializable) d.get(key), d);
                reportList.add(syncSuccessReport);
            } catch (Exception e) {
                e.printStackTrace();
                SyncErrorReport syncErrorReport = new SyncErrorReport(context.getPluginsCode(),
                        d.getTableName(), context.getUserId(), d.get(key).toString(), e.getMessage(), e, d);
                reportList.add(syncErrorReport);
            }
        });

        return reportList;
    }


    private String buildField(String objectName,Column... c) {
        StringBuilder sql = new StringBuilder(" ");
        for (Column f : c) {
            if (f.getType() == ColumnType.column) {
                sql.append(objectName).append(".").append(f.getColumn()).append(",");
            } else if (f.getType() == ColumnType.custom) {
                sql.append("(").append(f.getValue()).append(") as ").append(f.getColumn()).append(",");
            } else if (f.getType() == ColumnType.constant) {
                sql.append("'").append(f.getValue()).append("' as ").append(f.getColumn()).append(",");
            } else if (f.getType() == ColumnType.sourceLookup) {
                QueryLookUp value = (QueryLookUp) f.getValue();
                sql.append("(  select ")
                        .append(value.getColumn())
                        .append(" FROM ").append(value.getObjName()).append(" as ").append(value.getObjName())
                        .append(" WHERE ")
                        .append(value.getObjName()).append(".")
                        .append(value.getLinkColumn())
                        .append(" = ")
                        .append(objectName).append(".")
                        .append(value.getCurrentColumn())
                        .append(" ) as `").append(f.getColumn()).append("`")
                        .append(",");

            }
        }
        sql.deleteCharAt(sql.length() - 1);
        return sql.toString();
    }


    /**
     * 构建条件查询
     *
     * @param sql
     * @param queries
     */
    private void buildCondition(StringBuffer sql, List<Query> queries,String objectName) {
        if (queries == null || queries.size() == 0) {
            return;
        }

        sql.append(" where ");

        boolean orStart = false;
        // 上一次的join
        Join preJoin = null;
        for (Query q : queries) {
            String condition = condition(q,objectName);
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


    private String condition(Query query,String objectName) {
        StringBuilder append = new StringBuilder();

        if (query.getType() != QueryType.Custom) {
            append.append(objectName).append(".").append(query.getName());
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
            case STRING:
                append.append(query.getSignal().getId()).append("?");
                break;
            case Custom:
                append.append(query.getValue());
                break;
            default:
                return null;
        }
        return append.toString();
    }


    private List<Object> getValue(List<Query> query) {
        return query.stream()
                .filter(q -> q.getType() == QueryType.NUMBER || q.getType() == QueryType.STRING)
                .map(Query::getValue).collect(Collectors.toList());
    }

}
