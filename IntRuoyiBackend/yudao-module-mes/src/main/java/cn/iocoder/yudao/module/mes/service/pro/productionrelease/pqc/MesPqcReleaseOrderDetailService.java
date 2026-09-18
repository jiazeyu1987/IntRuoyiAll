package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowStatus;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchRecordSignatureSubjectAdapter;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionSignatureService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderDetail;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderDetailService;
import cn.iocoder.yudao.module.mes.service.pro.workorder.kingdee.MesKingdeeProductionMaterialListQueryService;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.kingdee.MesKingdeeProductionMaterialListPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.kingdee.MesKingdeeProductionMaterialListRespVO;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureEvidenceDTO;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureVerificationDTO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import cn.hutool.core.util.StrUtil;
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

    @Transactional(readOnly = true)
    public Result get(Long viewerId, Long applicationId) {
        MesPqcProductionReleaseDecisionResult decision = authorization.get(viewerId, applicationId);
        var application = applicationMapper.selectById(applicationId);
        if (application == null || application.getActiveOrderId() == null) {
            throw new IllegalStateException("PQC_RELEASE_ACTIVE_ORDER_SOURCE_MISSING");
        }
        var order = orderMapper.selectById(application.getActiveOrderId());
        if (order == null || order.getLeaderUserId() == null
                || !Objects.equals(order.getWorkOrderId(), application.getWorkOrderId())) {
            throw new IllegalStateException("PQC_RELEASE_ACTIVE_ORDER_SOURCE_INVALID");
        }
        // PQC authorization above governs the viewer; reuse the owner's identical read projection.
        var detail = detailService.getDetail(order.getLeaderUserId(), order.getId());
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
        if (decision.getSignatureId() == null) {
            throw new IllegalStateException("PQC_RELEASE_SIGNATURE_ID_MISSING");
        }
        ElectronicSignatureEvidenceDTO signature = requireUnifiedPqcReleaseSignature(
                decision.getSignatureId(), application.getBatchExecutionId());
        AdminUserDO signer = adminUserService.getUser(signature.actorId());
        if (signer == null || StrUtil.isBlank(signer.getNickname())) {
            throw new IllegalStateException("PQC_RELEASE_SIGNATURE_ACTOR_MISSING");
        }
        detail.setPqcProductionRelease(new MesTeamLeaderActiveOrderDetail.PqcProductionReleaseSummary()
                .setStatus(decision.getStatus())
                .setStatusLabel("已生产放行")
                .setSignature(new MesTeamLeaderActiveOrderDetail.SignatureDetail()
                        .setSignatureId(signature.id())
                        .setSignerName(signer.getNickname())
                        .setSignedAt(signature.signedAt())
                        .setRole(MesProBatchRecordExecutionSignatureService.ACTION_PQC_RELEASE)));
    }

    private ElectronicSignatureEvidenceDTO requireUnifiedPqcReleaseSignature(Long signatureId, Long batchExecutionId) {
        if (signatureId == null) {
            throw new IllegalStateException("PQC_RELEASE_SIGNATURE_ID_MISSING");
        }
        if (batchExecutionId == null || batchExecutionId <= 0) {
            throw new IllegalStateException("PQC_RELEASE_SIGNATURE_EXECUTION_MISMATCH");
        }
        String subjectId = MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(batchExecutionId,
                MesProBatchRecordExecutionSignatureService.ACTION_PQC_RELEASE,
                null, null, null, null, null, null, null,
                "PQC_RELEASE_APPLICATION", null, "PQC生产放行",
                MesProBatchRecordExecutionSignatureService.ACTION_PQC_RELEASE,
                null, null, null, null);
        ElectronicSignatureEvidenceDTO signature = signatureQueryService
                .listBySubject(MesBatchRecordSignatureSubjectAdapter.MODULE_CODE,
                        MesBatchRecordSignatureSubjectAdapter.SUBJECT_TYPE, subjectId)
                .stream()
                .filter(item -> Objects.equals(item.id(), signatureId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("PQC_RELEASE_SIGNATURE_RECORD_MISSING"));
        if (!Objects.equals(signature.actionCode(), MesProBatchRecordExecutionSignatureService.ACTION_PQC_RELEASE)
                || !Objects.equals(signature.moduleCode(), MesBatchRecordSignatureSubjectAdapter.MODULE_CODE)
                || !Objects.equals(signature.subjectType(), MesBatchRecordSignatureSubjectAdapter.SUBJECT_TYPE)
                || !Objects.equals(signature.subjectId(), subjectId)
                || !Objects.equals(signature.verificationStatus(), "VALID")
                || !Objects.equals(signature.authenticationMethod(), "SESSION_PLUS_PASSWORD")
                || signature.actorId() == null
                || signature.signedAt() == null) {
            throw new IllegalStateException("PQC_RELEASE_SIGNATURE_ACTION_MISMATCH");
        }
        ElectronicSignatureVerificationDTO verification = signatureQueryService.verifyEvidence(signatureId);
        if (verification == null
                || !Objects.equals(verification.signatureId(), signatureId)
                || !Objects.equals(verification.verificationStatus(), "VALID")
                || !Objects.equals(verification.storedEvidenceHash(), signature.evidenceHash())
                || !Objects.equals(verification.calculatedEvidenceHash(), signature.evidenceHash())) {
            throw new IllegalStateException("PQC_RELEASE_SIGNATURE_RECORD_MISSING");
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
