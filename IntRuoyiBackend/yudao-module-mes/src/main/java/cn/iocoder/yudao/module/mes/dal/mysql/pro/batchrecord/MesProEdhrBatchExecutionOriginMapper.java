package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionOriginDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MesProEdhrBatchExecutionOriginMapper extends BaseMapperX<MesProEdhrBatchExecutionOriginDO> {

    default MesProEdhrBatchExecutionOriginDO selectByBatchAndOriginKey(Long batchExecutionId, String originKey) {
        return selectOne(new LambdaQueryWrapperX<MesProEdhrBatchExecutionOriginDO>()
                .eq(MesProEdhrBatchExecutionOriginDO::getBatchExecutionId, batchExecutionId)
                .eq(MesProEdhrBatchExecutionOriginDO::getOriginKey, originKey));
    }

    default List<MesProEdhrBatchExecutionOriginDO> selectListByBatchExecutionId(Long batchExecutionId) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrBatchExecutionOriginDO>()
                .eq(MesProEdhrBatchExecutionOriginDO::getBatchExecutionId, batchExecutionId)
                .orderByAsc(MesProEdhrBatchExecutionOriginDO::getId));
    }

    default List<MesProEdhrBatchExecutionOriginDO> selectListByTraceFilter(Long activeOrderId,
                                                                             Long workOrderId,
                                                                             Long pickListId,
                                                                             String entryType) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrBatchExecutionOriginDO>()
                .eqIfPresent(MesProEdhrBatchExecutionOriginDO::getActiveOrderId, activeOrderId)
                .eqIfPresent(MesProEdhrBatchExecutionOriginDO::getWorkOrderId, workOrderId)
                .eqIfPresent(MesProEdhrBatchExecutionOriginDO::getPickListId, pickListId)
                .eqIfPresent(MesProEdhrBatchExecutionOriginDO::getEntryType, entryType)
                .orderByDesc(MesProEdhrBatchExecutionOriginDO::getId));
    }
}
