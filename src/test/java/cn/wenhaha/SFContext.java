package cn.wenhaha;

import cn.wenhaha.datasource.DataUser;
import cn.wenhaha.datasource.IUserContext;
import cn.wenhaha.plugin.data.salesforce.SalesUser;

import java.io.Serializable;
import java.util.List;

public class SFContext implements IUserContext<SalesUser> {


    @Override
    public SalesUser getUserInfo(Serializable id) {

        SalesUser salesUser = new SalesUser();
        salesUser.setLoginJson("{\"access_token\":\"00DN000000AAADAQkAQH2rXkBRGAUzENXFeYmsCd5Yq3UuQ7E.7Cl0iJjlSJiMP7V0_zqYKlPD9geG7MQdyVeP_4e2Ahlaask9itl9VEqSbUsY\",\"instance_url\":\"https://frensworkz--campaign.sandbox.my.salesforce.com\",\"id\":\"https://test.salesforce.com/id/00DN0000000VM2VMAW/00590000000gtjtAAA\",\"token_type\":\"Bearer\",\"issued_at\":\"1668314543800\",\"signature\":\"wObreTdSXT+C4vkZWST/G8fEsUo94xg/xYYUsd4Lclk=\"}");
        salesUser.setToken("00DN0000000VM2V!AQkAQFFFFF3UuQ7E.7Cl0iJjlSJiMP7V0_zqYKlPD9geG7MQdyVeP_4e2Ahlaask9itl9VEqSbUsY");
        salesUser.setUrl("https://test.my.salesforce.com");
        return salesUser;
    }

    @Override
    public SalesUser updateUser(Serializable id) {
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
