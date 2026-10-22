package cn.iocoder.yudao.module.mes.approval.content;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrBatchTraceSourcePrecheckRespVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.*;

/** Fresh internal owner graph; callers must not retain it as later authorization. */
public record MesEdhrApprovalFinalizationGraph(Long tenantId, MesProEdhrWorkTaskDO task,
        MesProEdhrReleaseTransactionDO transaction, MesProEdhrBatchExecutionDO batch, boolean managed,
        MesProEdhrBatchExecutionOriginDO origin, MesProEdhrBatchTraceSourcePrecheckRespVO sourceContext,
        MesEdhrVerifiedMaterialGateFact materialFact, MesEdhrSourceCreationEvidence sourceCreationEvidence,
        Long applicationId, Long activeOrderVersion,
        cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesEdhrVerifiedActiveOrderReleaseFact activeOrderFact) {
    public MesEdhrApprovalFinalizationGraph(Long tenantId, MesProEdhrWorkTaskDO task,
            MesProEdhrReleaseTransactionDO transaction, MesProEdhrBatchExecutionDO batch, boolean managed,
            MesProEdhrBatchExecutionOriginDO origin, MesProEdhrBatchTraceSourcePrecheckRespVO sourceContext,
            MesEdhrVerifiedMaterialGateFact materialFact, MesEdhrSourceCreationEvidence sourceCreationEvidence,
            Long applicationId, Long activeOrderVersion) {
        this(tenantId,task,transaction,batch,managed,origin,sourceContext,materialFact,sourceCreationEvidence,applicationId,activeOrderVersion,null);
    }
    public MesEdhrApprovalFinalizationGraph {
        if((materialFact==null)==(activeOrderFact==null)) throw new IllegalArgumentException("APPROVAL_EXACT_FACT_BRANCH_REQUIRED");
        if(activeOrderFact!=null && (!managed || !"ACTIVE_ORDER_COMPLETION".equals(origin.getEntryType())
                || !java.util.Objects.equals(applicationId,activeOrderFact.applicationId())))
            throw new IllegalArgumentException("APPROVAL_ACTIVE_FACT_BINDING_INVALID");
    }
    public MesProEdhrBatchTraceSourcePrecheckRespVO trace() { return sourceContext; }
}
