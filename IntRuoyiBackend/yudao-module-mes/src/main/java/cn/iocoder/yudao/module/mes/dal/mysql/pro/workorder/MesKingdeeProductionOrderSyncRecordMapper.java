package cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesKingdeeProductionOrderSyncRecordDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesKingdeeProductionOrderSyncRecordMapper extends BaseMapperX<MesKingdeeProductionOrderSyncRecordDO> {

    default MesKingdeeProductionOrderSyncRecordDO selectBySourceKey(String sourceFid, String sourceMaterialNumber) {
        return selectOne(MesKingdeeProductionOrderSyncRecordDO::getSourceFid, sourceFid,
                MesKingdeeProductionOrderSyncRecordDO::getSourceMaterialNumber, sourceMaterialNumber);
    }

    default MesKingdeeProductionOrderSyncRecordDO selectByWorkOrderId(Long workOrderId) {
        return selectOne(MesKingdeeProductionOrderSyncRecordDO::getWorkOrderId, workOrderId);
    }

}
