package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpKingdeeSaleOrderSyncRecordDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ErpKingdeeSaleOrderSyncRecordMapper extends BaseMapperX<ErpKingdeeSaleOrderSyncRecordDO> {

    default ErpKingdeeSaleOrderSyncRecordDO selectBySourceKey(String sourceFormId, String sourceFid) {
        return selectOne(ErpKingdeeSaleOrderSyncRecordDO::getSourceFormId, sourceFormId,
                ErpKingdeeSaleOrderSyncRecordDO::getSourceFid, sourceFid);
    }

}
