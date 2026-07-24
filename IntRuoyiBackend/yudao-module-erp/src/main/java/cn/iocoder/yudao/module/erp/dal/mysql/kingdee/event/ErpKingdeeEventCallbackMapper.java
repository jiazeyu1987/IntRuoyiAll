package cn.iocoder.yudao.module.erp.dal.mysql.kingdee.event;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.kingdee.event.ErpKingdeeEventCallbackDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ErpKingdeeEventCallbackMapper extends BaseMapperX<ErpKingdeeEventCallbackDO> {

    default ErpKingdeeEventCallbackDO selectByEventKey(String eventKey) {
        return selectOne(ErpKingdeeEventCallbackDO::getEventKey, eventKey);
    }

}
