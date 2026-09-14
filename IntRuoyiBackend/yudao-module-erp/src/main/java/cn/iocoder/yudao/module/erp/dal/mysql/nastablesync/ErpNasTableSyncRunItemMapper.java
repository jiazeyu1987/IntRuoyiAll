package cn.iocoder.yudao.module.erp.dal.mysql.nastablesync;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.nastablesync.ErpNasTableSyncRunItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ErpNasTableSyncRunItemMapper extends BaseMapperX<ErpNasTableSyncRunItemDO> {

    default List<ErpNasTableSyncRunItemDO> selectListByRunId(Long runId) {
        return selectList(new LambdaQueryWrapperX<ErpNasTableSyncRunItemDO>()
                .eq(ErpNasTableSyncRunItemDO::getRunId, runId)
                .orderByAsc(ErpNasTableSyncRunItemDO::getId));
    }
}
