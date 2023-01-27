package cn.wenhaha;

import cn.hutool.db.ds.simple.SimpleDataSource;
import cn.wenhaha.data.plugin.mysql.bean.MysqlSource;
import cn.wenhaha.datasource.DataUser;
import cn.wenhaha.datasource.IUserContext;

import java.io.Serializable;
import java.util.List;

/**
 * --------
 *
 * @author ：wyndem
 * @Date ：Created in 2022-08-18 19:42
 */
public class UserContextMysql  implements IUserContext<MysqlSource> {


    @Override
    public MysqlSource getUserInfo(Serializable id) {
        MysqlSource source = new MysqlSource();
        source.setDataSource(new SimpleDataSource("jdbc:mysql://127.0.0.1:3306/wuliu?useSSL=false&allowPublicKeyRetrieval=true","wuliu","3qxLt2vkQ7osWo"));
        return source;
    }

    @Override
    public MysqlSource updateUser(Serializable id) {
        return null;
    }

    @Override
    public List<DataUser> list() {
        return null;
    }

    @Override
    public boolean removeUser(Serializable id) {
        return false;
    }
}
