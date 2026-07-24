package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordApprovalSnapshotDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MesProBatchRecordApprovalSnapshotMapper extends BaseMapperX<MesProBatchRecordApprovalSnapshotDO> {

    default MesProBatchRecordApprovalSnapshotDO selectByExecutionId(Long executionId) {
        return selectOne(new LambdaQueryWrapperX<MesProBatchRecordApprovalSnapshotDO>()
                .eq(MesProBatchRecordApprovalSnapshotDO::getExecutionId, executionId));
    }

    default MesProBatchRecordApprovalSnapshotDO selectLatestByExecutionId(Long executionId) {
        return selectOne(new LambdaQueryWrapperX<MesProBatchRecordApprovalSnapshotDO>()
                .eq(MesProBatchRecordApprovalSnapshotDO::getExecutionId, executionId)
                .orderByDesc(MesProBatchRecordApprovalSnapshotDO::getCreateTime)
                .orderByDesc(MesProBatchRecordApprovalSnapshotDO::getId)
                .last("LIMIT 1"));
    }

    default MesProBatchRecordApprovalSnapshotDO selectByProcessInstanceId(String processInstanceId) {
        return selectOne(new LambdaQueryWrapperX<MesProBatchRecordApprovalSnapshotDO>()
                .eq(MesProBatchRecordApprovalSnapshotDO::getProcessInstanceId, processInstanceId));
    }

    @Update("""
            UPDATE mes_pro_batch_record_approval_snapshot
            SET approval_status = #{snapshot.approvalStatus},
                approve_signature_id = #{snapshot.approveSignatureId},
                approved_by = #{snapshot.approvedBy},
                approved_at = #{snapshot.approvedAt},
                closed_at = #{snapshot.closedAt},
                current_bpm_task_id = NULL,
                current_task_definition_key = NULL,
                update_time = NOW()
            WHERE id = #{snapshot.id}
            """)
    int approveAndClearCurrentBpmTask(@Param("snapshot") MesProBatchRecordApprovalSnapshotDO snapshot);
}
