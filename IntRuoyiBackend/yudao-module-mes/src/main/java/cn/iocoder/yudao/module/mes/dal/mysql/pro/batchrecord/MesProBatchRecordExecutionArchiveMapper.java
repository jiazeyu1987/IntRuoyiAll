package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionArchiveDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProBatchRecordExecutionArchiveMapper extends BaseMapperX<MesProBatchRecordExecutionArchiveDO> {

    default MesProBatchRecordExecutionArchiveDO selectLatestByExecutionAndType(Long executionId, String artifactType) {
        return selectOne(new LambdaQueryWrapperX<MesProBatchRecordExecutionArchiveDO>()
                .eq(MesProBatchRecordExecutionArchiveDO::getExecutionId, executionId)
                .eq(MesProBatchRecordExecutionArchiveDO::getArtifactType, artifactType)
                .orderByDesc(MesProBatchRecordExecutionArchiveDO::getArchiveVersion)
                .orderByDesc(MesProBatchRecordExecutionArchiveDO::getId)
                .last("LIMIT 1"));
    }

    default MesProBatchRecordExecutionArchiveDO selectLatestByExecutionId(Long executionId) {
        return selectOne(new LambdaQueryWrapperX<MesProBatchRecordExecutionArchiveDO>()
                .eq(MesProBatchRecordExecutionArchiveDO::getExecutionId, executionId)
                .orderByDesc(MesProBatchRecordExecutionArchiveDO::getGeneratedAt)
                .orderByDesc(MesProBatchRecordExecutionArchiveDO::getId)
                .last("LIMIT 1"));
    }

    default MesProBatchRecordExecutionArchiveDO selectSealedBySourceHashes(Long executionId, String artifactType,
                                                                           String executionSnapshotHash,
                                                                           String cellValuesHash,
                                                                           String signatureHash,
                                                                           String approvalSnapshotHash) {
        return selectOne(new LambdaQueryWrapperX<MesProBatchRecordExecutionArchiveDO>()
                .eq(MesProBatchRecordExecutionArchiveDO::getExecutionId, executionId)
                .eq(MesProBatchRecordExecutionArchiveDO::getArtifactType, artifactType)
                .eq(MesProBatchRecordExecutionArchiveDO::getArchiveStatus, "SEALED")
                .eq(MesProBatchRecordExecutionArchiveDO::getExecutionSnapshotHash, executionSnapshotHash)
                .eq(MesProBatchRecordExecutionArchiveDO::getCellValuesHash, cellValuesHash)
                .eq(MesProBatchRecordExecutionArchiveDO::getSignatureHash, signatureHash)
                .eq(MesProBatchRecordExecutionArchiveDO::getApprovalSnapshotHash, approvalSnapshotHash)
                .orderByDesc(MesProBatchRecordExecutionArchiveDO::getArchiveVersion)
                .orderByDesc(MesProBatchRecordExecutionArchiveDO::getId)
                .last("LIMIT 1"));
    }
}
