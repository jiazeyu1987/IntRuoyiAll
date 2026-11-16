package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionArchiveMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrRecordChangeEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Objects;

/** A status flag alone never grants edit access to a signed record. */
@Service
public class MesEdhrRecordChangeEditGuard {
    @Resource private MesProEdhrRecordChangeEventMapper events;
    @Resource private MesProEdhrWorkTaskMapper tasks;
    @Resource private MesProBatchRecordExecutionArchiveMapper archives;
    @Resource private MesEdhrRecordChangeApprovalEvidence evidence;

    public void requireReopened(MesProBatchRecordExecutionDO execution, Long workTaskId) {
        if (execution == null || !Integer.valueOf(5).equals(execution.getStatus())
                || execution.getReopenedByChangeEventId() == null) fail("执行记录没有正式重开批准");
        var event = events.selectById(execution.getReopenedByChangeEventId());
        if (event == null || !"REOPEN".equals(event.getChangeType()) || !"EXECUTION".equals(event.getTargetScope())
                || !Objects.equals(event.getExecutionId(), execution.getId())) fail("重开事件不属于当前执行记录");
        evidence.requireApproved(event);
        var task = workTaskId == null ? null : tasks.selectById(workTaskId);
        if (task == null || !"REWORK".equals(task.getTaskType())
                || !"RECORD_CHANGE".equals(task.getBusinessScopeType())
                || !Objects.equals(task.getBusinessScopeId(), event.getId())
                || !Objects.equals(task.getExecutionId(), execution.getId())) fail("缺少本次获批重开的修改待办");
        var archive = archives.selectLatestByExecutionId(execution.getId());
        if (archive != null && (Boolean.TRUE.equals(archive.getArchiveValidFlag())
                || !Objects.equals(archive.getInvalidatedByChangeEventId(), event.getId()))) {
            fail("当前执行记录已有后续有效归档或重开归档关系不一致");
        }
    }

    private void fail(String reason) { throw new IllegalStateException(reason); }
}
