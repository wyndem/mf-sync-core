package cn.wenhaha.mf.sync.http;

import cn.hutool.db.Entity;

import java.util.List;

/**
 * 上传接口的请求类
 * --------
 *
 * @author ：wyndem
 * @Date ：Created in 2022-08-23 20:56
 */
public class RequestParam {

    private  String json;

    private List<Entity> data;

    public RequestParam(String json, List<Entity> data) {
        this.json = json;
        this.data = data;
    }

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }

    public List<Entity> getData() {
        return data;
    }

    public void setData(List<Entity> data) {
        this.data = data;
    }
}
