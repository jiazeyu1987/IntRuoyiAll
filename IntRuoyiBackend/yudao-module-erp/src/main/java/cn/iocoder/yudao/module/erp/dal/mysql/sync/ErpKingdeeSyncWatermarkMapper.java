package cn.iocoder.yudao.module.erp.dal.mysql.sync;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.sync.ErpKingdeeSyncWatermarkDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ErpKingdeeSyncWatermarkMapper extends BaseMapperX<ErpKingdeeSyncWatermarkDO> {

    default ErpKingdeeSyncWatermarkDO selectBySyncType(String syncType) {
        return selectOne(new LambdaQueryWrapperX<ErpKingdeeSyncWatermarkDO>()
                .eq(ErpKingdeeSyncWatermarkDO::getSyncType, syncType));
    }

    default List<ErpKingdeeSyncWatermarkDO> selectListOrderBySyncType() {
        return selectList(new LambdaQueryWrapperX<ErpKingdeeSyncWatermarkDO>()
                .orderByAsc(ErpKingdeeSyncWatermarkDO::getSyncType));
    }

}
