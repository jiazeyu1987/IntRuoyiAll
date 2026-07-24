package cn.iocoder.yudao.module.erp.dal.mysql.purchase;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpKingdeeSupplierSyncRecordDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ErpKingdeeSupplierSyncRecordMapper extends BaseMapperX<ErpKingdeeSupplierSyncRecordDO> {

    default ErpKingdeeSupplierSyncRecordDO selectBySourceSupplierNumber(String sourceSupplierNumber) {
        return selectOne(ErpKingdeeSupplierSyncRecordDO::getSourceSupplierNumber, sourceSupplierNumber);
    }

}
