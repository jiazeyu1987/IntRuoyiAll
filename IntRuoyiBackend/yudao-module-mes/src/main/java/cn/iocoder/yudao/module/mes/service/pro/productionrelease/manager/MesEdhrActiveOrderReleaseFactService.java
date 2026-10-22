package cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesActiveOrderSignatureEvidenceService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderDetail;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import cn.iocoder.yudao.module.signature.service.ElectronicSignatureVerifiedFactService;
import org.springframework.stereotype.Service;
import java.util.Objects;

/** Re-reads the real owners on every prepare and finalization; no client-supplied facts. */
@Service
public class MesEdhrActiveOrderReleaseFactService {
    private final MesProcessPoolActiveOrderReleaseApplicationMapper applications;
    private final MesProEdhrBatchExecutionMapper batches;
    private final MesProEdhrBatchExecutionOriginMapper origins;
    private final MesProEdhrWorkTaskMapper tasks;
    private final MesEdhrSourceCreationVerificationService creation;
    private final ElectronicSignatureQueryService signatures;
    private final ElectronicSignatureVerifiedFactService verifiedSignatures;

    public MesEdhrActiveOrderReleaseFactService(MesProcessPoolActiveOrderReleaseApplicationMapper applications,
            MesProEdhrBatchExecutionMapper batches, MesProEdhrBatchExecutionOriginMapper origins,
            MesProEdhrWorkTaskMapper tasks, MesEdhrSourceCreationVerificationService creation,
            ElectronicSignatureQueryService signatures, ElectronicSignatureVerifiedFactService verifiedSignatures) {
        this.applications=applications; this.batches=batches; this.origins=origins; this.tasks=tasks;
        this.creation=creation; this.signatures=signatures; this.verifiedSignatures=verifiedSignatures;
    }

    public MesEdhrVerifiedActiveOrderReleaseFact require(Long applicationId, Long batchId, String frozenHash) {
        Long tenant=TenantContextHolder.getRequiredTenantId();
        var app=applications.selectById(applicationId); var batch=batches.selectById(batchId);
        if (app==null || batch==null || !Objects.equals(app.getTenantId(),tenant)
                || !Objects.equals(batch.getTenantId(),tenant) || app.getActiveOrderId()==null
                || !Objects.equals(app.getBatchExecutionId(),batchId)
                || !Objects.equals(app.getReportSnapshotHash(),frozenHash)
                || !MesProductionReleaseFormalFactSnapshots.isActiveOrderFactsSnapshot(frozenHash)
                || !Objects.equals(MesProductionReleaseFormalFactSnapshots.recomputeActiveOrderFactsSnapshot(app),frozenHash))
            throw new IllegalStateException("ACTIVE_ORDER_RELEASE_FACT_MISMATCH");
        var matches=origins.selectListByBatchExecutionId(batchId);
        if(matches.size()!=1 || matches.get(0).getCompletionBackfillReceiptId()==null)
            throw new IllegalStateException("ACTIVE_ORDER_RELEASE_ORIGIN_REQUIRED");
        var origin=matches.get(0);
        if(!Objects.equals(origin.getTenantId(),tenant) || !Objects.equals(origin.getBatchExecutionId(),batchId)
                || !"ACTIVE_ORDER_COMPLETION".equals(origin.getEntryType())
                || !Objects.equals(origin.getActiveOrderId(),app.getActiveOrderId())
                || !Objects.equals(origin.getWorkOrderId(),app.getWorkOrderId()))
            throw new IllegalStateException("ACTIVE_ORDER_RELEASE_ORIGIN_MISMATCH");
        var completion=creation.requireCompletionCreation(tenant,origin.getCompletionBackfillReceiptId());
        if (!Objects.equals(origin.getCompletionBackfillReceiptHash(),completion.sourceHash()))
            throw new IllegalStateException("ACTIVE_ORDER_RELEASE_COMPLETION_MISMATCH");
        var receipt=JsonUtils.parseTree(app.getDossierSummaryJson());
        var id=receipt==null?null:receipt.get("signatureId");
        Long signatureId=id!=null && id.isIntegralNumber() && id.canConvertToLong()?id.longValue():
                id!=null && id.isTextual() && id.textValue().matches("[1-9][0-9]*")?Long.valueOf(id.textValue()):null;
        if(signatureId==null || signatureId<=0) throw new IllegalStateException("ACTIVE_ORDER_RELEASE_PQC_SIGNATURE_REQUIRED");
        var evidence=signatures.getById(signatureId);
        var task=tasks.selectById(app.getPqcReleaseWorkTaskId());
        var detail=new MesTeamLeaderActiveOrderDetail().setActiveOrderId(app.getActiveOrderId()).setWorkOrderId(app.getWorkOrderId());
        MesActiveOrderSignatureEvidenceService.requirePqcPersistedBinding(detail,app,task,batch,evidence);
        var signature=verifiedSignatures.requireVerifiedFact(signatureId);
        MesActiveOrderSignatureEvidenceService.requirePqcCanonical(evidence,signature);
        return new MesEdhrVerifiedActiveOrderReleaseFact(app.getId(),batchId,frozenHash,origin.getSourceSnapshotHash(),
                completion,signatureId,signature.evidenceHash(),signature.auditEventId());
    }
}
