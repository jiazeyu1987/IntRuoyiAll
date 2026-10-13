package cn.iocoder.yudao.module.mes.approval.content;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrBatchTraceSourcePrecheckRespVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.*;

/** Fresh internal owner graph; callers must not retain it as later authorization. */
public record MesEdhrApprovalFinalizationGraph(Long tenantId, MesProEdhrWorkTaskDO task,
        MesProEdhrReleaseTransactionDO transaction, MesProEdhrBatchExecutionDO batch, boolean managed,
        MesProEdhrBatchExecutionOriginDO origin, MesProEdhrBatchTraceSourcePrecheckRespVO sourceContext,
        MesEdhrVerifiedMaterialGateFact materialFact, MesEdhrSourceCreationEvidence sourceCreationEvidence,
        Long applicationId, Long activeOrderVersion) {
    public MesProEdhrBatchTraceSourcePrecheckRespVO trace() { return sourceContext; }
}
