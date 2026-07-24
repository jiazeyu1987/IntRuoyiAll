package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpKingdeeWarehouseSyncRecordDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ErpKingdeeWarehouseSyncRecordMapper extends BaseMapperX<ErpKingdeeWarehouseSyncRecordDO> {

    default ErpKingdeeWarehouseSyncRecordDO selectBySourceKey(String sourceStockOrgNumber,
                                                              String sourceWarehouseNumber) {
        return selectOne(ErpKingdeeWarehouseSyncRecordDO::getSourceStockOrgNumber, sourceStockOrgNumber,
                ErpKingdeeWarehouseSyncRecordDO::getSourceWarehouseNumber, sourceWarehouseNumber);
    }

}
