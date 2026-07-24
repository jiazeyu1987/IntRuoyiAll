package cn.iocoder.yudao.module.erp.dal.mysql.purchase;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpKingdeePurchaseOrderSyncRecordDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ErpKingdeePurchaseOrderSyncRecordMapper extends BaseMapperX<ErpKingdeePurchaseOrderSyncRecordDO> {

    default ErpKingdeePurchaseOrderSyncRecordDO selectBySourceKey(String sourceFormId, String sourceFid) {
        return selectOne(new LambdaQueryWrapperX<ErpKingdeePurchaseOrderSyncRecordDO>()
                .eq(ErpKingdeePurchaseOrderSyncRecordDO::getSourceFormId, sourceFormId)
                .eq(ErpKingdeePurchaseOrderSyncRecordDO::getSourceFid, sourceFid));
    }

}
