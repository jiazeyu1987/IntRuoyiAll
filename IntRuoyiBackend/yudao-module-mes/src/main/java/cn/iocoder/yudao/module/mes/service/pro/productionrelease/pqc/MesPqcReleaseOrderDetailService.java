package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowStatus;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchRecordSignatureSubjectAdapter;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionSignatureService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderDetail;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderDetailService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesActiveOrderSignatureEvidenceService;
import cn.iocoder.yudao.module.mes.service.pro.workorder.kingdee.MesKingdeeProductionMaterialListQueryService;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.kingdee.MesKingdeeProductionMaterialListPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.kingdee.MesKingdeeProductionMaterialListRespVO;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureEvidenceDTO;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureVerificationDTO;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MesPqcReleaseOrderDetailService {
    @jakarta.annotation.Resource private cn.iocoder.yudao.module.signature.service.ElectronicSignatureVerifiedFactService verifiedSignatureFacts;
    private static final Set<String> PQC_RELEASED_STATUSES = Set.of(
            MesReleaseFlowStatus.REPORT_UPLOAD_PENDING,
            MesReleaseFlowStatus.MANAGER_RELEASE_PENDING,
            MesReleaseFlowStatus.RELEASED);

    private final MesPqcProductionReleaseService authorization;
    private final MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper;
    private final MesProcessPoolActiveOrderMapper orderMapper;
    private final ElectronicSignatureQueryService signatureQueryService;
    private final AdminUserService adminUserService;
    private final MesTeamLeaderActiveOrderDetailService detailService;
    private final MesKingdeeProductionMaterialListQueryService materialService;
    @jakarta.annotation.Resource
    private cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper workTaskMapper;
    @jakarta.annotation.Resource
    private cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper batchMapper;

    @Transactional(readOnly = true)
    public Result get(Long viewerId, Long applicationId) {
        MesPqcProductionReleaseDecisionResult decision = authorization.get(viewerId, applicationId);
        var application = applicationMapper.selectById(applicationId);
        if (application == null || !Objects.equals(application.getId(), applicationId) || application.getActiveOrderId() == null) {
            throw new IllegalStateException("PQC_RELEASE_ACTIVE_ORDER_SOURCE_MISSING");
        }
        var order = orderMapper.selectByIdIgnoreDeleted(application.getActiveOrderId());
        if (order == null || order.getLeaderUserId() == null
                || !Objects.equals(order.getWorkOrderId(), application.getWorkOrderId())) {
            throw new IllegalStateException("PQC_RELEASE_ACTIVE_ORDER_SOURCE_INVALID");
        }
        var detail = detailService.getArchivedFormalDetail(application.getActiveOrderId());
        if (detail == null || detail.getWorkOrderCode() == null || detail.getWorkOrderCode().isBlank()) {
            throw new IllegalStateException("PQC_RELEASE_WORK_ORDER_CODE_MISSING");
        }
        attachPqcProductionReleaseSummary(detail, decision, application);
        List<MesKingdeeProductionMaterialListRespVO> materials = new ArrayList<>();
        var query = new MesKingdeeProductionMaterialListPageReqVO();
        query.setProductionOrderNo(detail.getWorkOrderCode());
        query.setPageSize(100);
        query.setPageNo(1);
        while (true) {
            var page = materialService.getPage(query);
            if (page == null || page.getList() == null || page.getTotal() == null) {
                throw new IllegalStateException("PQC_RELEASE_MATERIAL_PAGE_INVALID");
            }
            materials.addAll(page.getList());
            if (materials.size() >= page.getTotal()) break;
            if (page.getList().isEmpty()) throw new IllegalStateException("PQC_RELEASE_MATERIAL_PAGE_INCOMPLETE");
            query.setPageNo(query.getPageNo() + 1);
        }
        return new Result(detail, List.copyOf(materials));
    }

    private void attachPqcProductionReleaseSummary(
            MesTeamLeaderActiveOrderDetail detail,
            MesPqcProductionReleaseDecisionResult decision,
            MesProcessPoolActiveOrderReleaseApplicationDO application) {
        if (!isPqcProductionReleased(decision)) {
            return;
        }
        if (!Objects.equals(decision.getApplicationId(), application.getId())) {
            throw new IllegalStateException("PQC_RELEASE_SIGNATURE_APPLICATION_MISMATCH");
        }
        if (decision.getSignatureId() == null) {
            throw new IllegalStateException("PQC_RELEASE_SIGNATURE_ID_MISSING");
        }
        ElectronicSignatureEvidenceDTO signature = requireUnifiedPqcReleaseSignature(
                decision.getSignatureId(), application.getBatchExecutionId(), application.getId());
        var task = MesActiveOrderSignatureEvidenceService.readPqcTask(workTaskMapper, application.getPqcReleaseWorkTaskId());
        var batch = MesActiveOrderSignatureEvidenceService.readPqcBatch(batchMapper, application.getBatchExecutionId());
        MesActiveOrderSignatureEvidenceService.requirePqcPersistedBinding(detail, application, task, batch, signature);
        ElectronicSignatureVerificationDTO verification = signatureQueryService.verifyEvidence(signature.id());
        MesActiveOrderSignatureEvidenceService.requirePqcReadBinding(detail, application, task, batch, signature, verification);
        detail.setPqcProductionRelease(new MesTeamLeaderActiveOrderDetail.PqcProductionReleaseSummary()
                .setStatus(decision.getStatus())
                .setStatusLabel("已生产放行")
                .setSignature(MesActiveOrderSignatureEvidenceService.pqcReadProjection(signature, verification)));
    }

    private ElectronicSignatureEvidenceDTO requireUnifiedPqcReleaseSignature(
            Long signatureId, Long batchExecutionId, Long applicationId) {
        if (signatureId == null) {
            throw new IllegalStateException("PQC_RELEASE_SIGNATURE_ID_MISSING");
        }
        if (batchExecutionId == null || batchExecutionId <= 0) {
            throw new IllegalStateException("PQC_RELEASE_SIGNATURE_EXECUTION_MISMATCH");
        }
        if (applicationId == null || applicationId <= 0) {
            throw new IllegalStateException("PQC_RELEASE_SIGNATURE_APPLICATION_MISSING");
        }
        String subjectId = MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(batchExecutionId,
                MesProBatchRecordExecutionSignatureService.ACTION_PQC_RELEASE,
                null, null, null, null, null, null, null,
                "PQC_RELEASE_APPLICATION", applicationId, "PQC生产放行",
                MesProBatchRecordExecutionSignatureService.ACTION_PQC_RELEASE,
                null, null, null, null);
        ElectronicSignatureEvidenceDTO signature = signatureQueryService.getById(signatureId);
        if (signature == null || !Objects.equals(signature.id(), signatureId)
                || !Objects.equals(MesBatchRecordSignatureSubjectAdapter.subjectWithoutOpinionSource(signature.subjectId()), subjectId)) {
            throw new IllegalStateException("PQC_RELEASE_SIGNATURE_RECORD_MISSING");
        }
        if (!Objects.equals(signature.actionCode(), MesProBatchRecordExecutionSignatureService.ACTION_PQC_RELEASE)
                || !Objects.equals(signature.moduleCode(), MesBatchRecordSignatureSubjectAdapter.MODULE_CODE)
                || !Objects.equals(signature.subjectType(), MesBatchRecordSignatureSubjectAdapter.SUBJECT_TYPE)
                || !Objects.equals(signature.subjectVersion(), MesBatchRecordSignatureSubjectAdapter.subjectVersion(signature.subjectId()))
                || signature.authenticationMethod() == null
                || !Set.of("SESSION_PLUS_PASSWORD", "SESSION_PLUS_SELECTED_USER_PASSWORD").contains(signature.authenticationMethod())
                || signature.actorId() == null || signature.actorId() <= 0) {
            throw new IllegalStateException("PQC_RELEASE_SIGNATURE_ACTION_MISMATCH");
        }
        return signature;
    }

    private boolean isPqcProductionReleased(MesPqcProductionReleaseDecisionResult decision) {
        return decision != null
                && Objects.equals(decision.getDecision(), "APPROVE")
                && PQC_RELEASED_STATUSES.contains(decision.getStatus());
    }

    public record Result(MesTeamLeaderActiveOrderDetail detail,
                         List<MesKingdeeProductionMaterialListRespVO> productionMaterialLists) {}
}
