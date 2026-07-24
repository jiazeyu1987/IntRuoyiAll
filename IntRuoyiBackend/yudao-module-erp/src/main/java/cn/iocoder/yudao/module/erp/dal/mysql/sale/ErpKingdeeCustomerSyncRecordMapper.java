package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpKingdeeCustomerSyncRecordDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ErpKingdeeCustomerSyncRecordMapper extends BaseMapperX<ErpKingdeeCustomerSyncRecordDO> {

    default ErpKingdeeCustomerSyncRecordDO selectBySourceCustomerNumber(String sourceCustomerNumber) {
        return selectOne(ErpKingdeeCustomerSyncRecordDO::getSourceCustomerNumber, sourceCustomerNumber);
    }

}
