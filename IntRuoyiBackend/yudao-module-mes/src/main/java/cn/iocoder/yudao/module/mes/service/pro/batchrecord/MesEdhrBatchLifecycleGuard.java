package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionArchiveDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrRecordChangeEventDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrNonconformanceReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrRecordChangeEventMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import java.util.Objects;
import java.util.Set;

/** Shared lifecycle exclusion. Call while holding the GXP ledger lock before any business write. */
@Service
public class MesEdhrBatchLifecycleGuard {
    public static final ErrorCode BLOCKED = new ErrorCode(1_040_760_456, "批次生命周期冲突：{}");
    @Resource private MesProEdhrNonconformanceReviewMapper reviewMapper;
    @Resource private MesProEdhrRecordChangeEventMapper changeMapper;
    @Resource private MesProEdhrBatchExecutionMapper batchMapper;

    public void requireIndependentVoidAllowed(MesProEdhrBatchExecutionDO batch) {
        require(batch != null && batch.getId() != null, "缺少正式批次");
        var pending = reviewMapper.selectPendingByBatchExecutionId(batch.getId());
        require(pending == null, "存在待审NCR，请通过QA不合格评审处置，不得独立作废");
        require(batch.getWorkOrderId() != null, "批次缺少正式工单");
        Long count = reviewMapper.selectPendingCountByWorkOrderId(batch.getWorkOrderId());
        require(count != null && count == 0, "工单存在待审NCR，请通过QA不合格评审处置");
    }

    public void requireReleaseAllowed(Long batchId) {
        require(batchId != null && batchId > 0, "缺少正式批次");
        Long count = changeMapper.selectCount(new LambdaQueryWrapperX<MesProEdhrRecordChangeEventDO>()
                .eq(MesProEdhrRecordChangeEventDO::getBatchExecutionId, batchId)
                .eq(MesProEdhrRecordChangeEventDO::getTargetScope, "BATCH")
                .eq(MesProEdhrRecordChangeEventDO::getChangeType, "VOID")
                .in(MesProEdhrRecordChangeEventDO::getChangeStatus, Set.of("DRAFT", "SUBMITTED", "APPROVED")));
        require(count != null && count == 0, "存在待处理正式作废申请，不能继续放行");
    }

    public void requireDossierMutable(Long batchId) {
        var batch = batchMapper.selectById(batchId);
        require(batch != null && batch.getStatus() != null, "正式批次不存在或缺少状态");
        require(!Set.of(15, 40, 50, 60).contains(batch.getStatus()), "批次已冻结、上市放行、关闭或作废，资料仅可查看");
        requireIndependentVoidAllowed(batch);
        requireReleaseAllowed(batchId);
    }

    public void requireVoidEffectState(MesProEdhrBatchExecutionDO batch, MesProEdhrRecordChangeEventDO event,
                                       MesProEdhrBatchExecutionArchiveDO archive) {
        requireIndependentVoidAllowed(batch);
        require(Objects.equals(event.getPreviousStatus(), String.valueOf(batch.getStatus())),
                "批次状态已在作废申请后改变，请重新申请，旧审批不得覆盖新状态");
        require(Objects.equals(event.getPreviousHeadHash(), batch.getAggregateHash()), "批次内容已在申请后改变");
        require(Objects.equals(event.getSourceArchiveId(), archive == null ? null : archive.getId())
                && Objects.equals(event.getPreviousArchiveHash(), archive == null ? null : archive.getContentHash()),
                "归档已在作废申请后改变");
        require(!Integer.valueOf(40).equals(batch.getStatus()) || archive != null,
                "已上市放行批次缺少正式归档，不能作废归档");
    }

    private static void require(boolean ok, String reason) {
        if (!ok) throw ServiceExceptionUtil.exception(BLOCKED, reason);
    }
}
