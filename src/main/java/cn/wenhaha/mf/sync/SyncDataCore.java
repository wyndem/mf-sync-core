package cn.wenhaha.mf.sync;

import cn.hutool.db.Entity;
import cn.wenhaha.sync.core.Column;
import cn.wenhaha.sync.core.Query;
import cn.wenhaha.sync.core.SyncReport;

import java.util.List;

/**
 * --------
 *
 * @author ：wyndem
 * @Date ：Created in 2022-08-16 22:58
 */
public interface SyncDataCore<T> {

    List<Entity> query(DataContext<T> context, List<Column> fields, List<Query> queries);

//    <K> List<K> batchQuery(DataContext<T> context,Column field, List<List<Query>> queries);

    Entity echoQuery(DataContext<T> context,List<Column> column);

    List<SyncReport> upload(DataContext<T> context, String key, List<Entity> data);
}
