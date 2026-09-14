package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionAttachmentDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MesProBatchRecordExecutionAttachmentMapper
        extends BaseMapperX<MesProBatchRecordExecutionAttachmentDO> {

    default List<MesProBatchRecordExecutionAttachmentDO> selectListByExecutionId(Long executionId) {
        return selectList(new LambdaQueryWrapperX<MesProBatchRecordExecutionAttachmentDO>()
                .eq(MesProBatchRecordExecutionAttachmentDO::getExecutionId, executionId)
                .orderByAsc(MesProBatchRecordExecutionAttachmentDO::getFieldPath)
                .orderByAsc(MesProBatchRecordExecutionAttachmentDO::getAttachmentGroupKey)
                .orderByAsc(MesProBatchRecordExecutionAttachmentDO::getVersionNo)
                .orderByAsc(MesProBatchRecordExecutionAttachmentDO::getId));
    }

    default List<MesProBatchRecordExecutionAttachmentDO> selectListByAuditBatchId(Long auditBatchId) {
        return selectList(new LambdaQueryWrapperX<MesProBatchRecordExecutionAttachmentDO>()
                .eq(MesProBatchRecordExecutionAttachmentDO::getAuditBatchId, auditBatchId)
                .orderByAsc(MesProBatchRecordExecutionAttachmentDO::getFieldPath)
                .orderByAsc(MesProBatchRecordExecutionAttachmentDO::getAttachmentGroupKey)
                .orderByAsc(MesProBatchRecordExecutionAttachmentDO::getVersionNo)
                .orderByAsc(MesProBatchRecordExecutionAttachmentDO::getId));
    }

    default List<MesProBatchRecordExecutionAttachmentDO> selectListByBatchTaskId(Long batchTaskId) {
        return selectList(new LambdaQueryWrapperX<MesProBatchRecordExecutionAttachmentDO>()
                .eq(MesProBatchRecordExecutionAttachmentDO::getBatchTaskId, batchTaskId)
                .orderByAsc(MesProBatchRecordExecutionAttachmentDO::getFieldPath)
                .orderByAsc(MesProBatchRecordExecutionAttachmentDO::getAttachmentGroupKey)
                .orderByAsc(MesProBatchRecordExecutionAttachmentDO::getVersionNo)
                .orderByAsc(MesProBatchRecordExecutionAttachmentDO::getId));
    }

    default List<MesProBatchRecordExecutionAttachmentDO> selectListByBatchExecutionId(Long batchExecutionId) {
        return selectList(new LambdaQueryWrapperX<MesProBatchRecordExecutionAttachmentDO>()
                .eq(MesProBatchRecordExecutionAttachmentDO::getBatchExecutionId, batchExecutionId)
                .orderByAsc(MesProBatchRecordExecutionAttachmentDO::getFieldKey)
                .orderByAsc(MesProBatchRecordExecutionAttachmentDO::getVersionNo)
                .orderByAsc(MesProBatchRecordExecutionAttachmentDO::getId));
    }

    default List<MesProBatchRecordExecutionAttachmentDO> selectListByExecutionField(
            Long executionId, String fieldPath, String fieldKey) {
        return selectList(new LambdaQueryWrapperX<MesProBatchRecordExecutionAttachmentDO>()
                .eq(MesProBatchRecordExecutionAttachmentDO::getExecutionId, executionId)
                .eq(MesProBatchRecordExecutionAttachmentDO::getFieldPath, fieldPath)
                .eq(MesProBatchRecordExecutionAttachmentDO::getFieldKey, fieldKey)
                .orderByAsc(MesProBatchRecordExecutionAttachmentDO::getAttachmentGroupKey)
                .orderByAsc(MesProBatchRecordExecutionAttachmentDO::getVersionNo)
                .orderByAsc(MesProBatchRecordExecutionAttachmentDO::getId));
    }

    default MesProBatchRecordExecutionAttachmentDO selectLatestByExecutionFieldGroup(
            Long executionId, String fieldPath, String fieldKey, String attachmentGroupKey) {
        return selectOne(new LambdaQueryWrapperX<MesProBatchRecordExecutionAttachmentDO>()
                .eq(MesProBatchRecordExecutionAttachmentDO::getExecutionId, executionId)
                .eq(MesProBatchRecordExecutionAttachmentDO::getFieldPath, fieldPath)
                .eq(MesProBatchRecordExecutionAttachmentDO::getFieldKey, fieldKey)
                .eq(MesProBatchRecordExecutionAttachmentDO::getAttachmentGroupKey, attachmentGroupKey)
                .orderByDesc(MesProBatchRecordExecutionAttachmentDO::getVersionNo)
                .orderByDesc(MesProBatchRecordExecutionAttachmentDO::getId)
                .last("LIMIT 1"));
    }
}
