package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordDomainTraceSnapshotDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface MesProBatchRecordDomainTraceSnapshotMapper extends BaseMapperX<MesProBatchRecordDomainTraceSnapshotDO> {

    default MesProBatchRecordDomainTraceSnapshotDO selectByExecutionIdAndSnapshotHash(Long executionId,
                                                                                      String snapshotHash) {
        return selectOne(new LambdaQueryWrapperX<MesProBatchRecordDomainTraceSnapshotDO>()
                .eq(MesProBatchRecordDomainTraceSnapshotDO::getExecutionId, executionId)
                .eq(MesProBatchRecordDomainTraceSnapshotDO::getSnapshotHash, snapshotHash));
    }

    default MesProBatchRecordDomainTraceSnapshotDO selectLatestByExecutionId(Long executionId) {
        return selectOne(new LambdaQueryWrapperX<MesProBatchRecordDomainTraceSnapshotDO>()
                .eq(MesProBatchRecordDomainTraceSnapshotDO::getExecutionId, executionId)
                .orderByDesc(MesProBatchRecordDomainTraceSnapshotDO::getVerifiedAt)
                .orderByDesc(MesProBatchRecordDomainTraceSnapshotDO::getId)
                .last("LIMIT 1"));
    }

    default List<MesProBatchRecordDomainTraceSnapshotDO> selectListByExecutionIds(Collection<Long> executionIds) {
        return selectList(new LambdaQueryWrapperX<MesProBatchRecordDomainTraceSnapshotDO>()
                .in(MesProBatchRecordDomainTraceSnapshotDO::getExecutionId, executionIds)
                .orderByAsc(MesProBatchRecordDomainTraceSnapshotDO::getExecutionId)
                .orderByDesc(MesProBatchRecordDomainTraceSnapshotDO::getVerifiedAt)
                .orderByDesc(MesProBatchRecordDomainTraceSnapshotDO::getId));
    }
}
