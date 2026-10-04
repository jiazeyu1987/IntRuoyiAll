package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionSignatureMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchRecordSignatureSubjectAdapter;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProductionSubmitSignatureContext;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureEvidenceDTO;
import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.Set;

/** Reads submission signer names from verified, frozen identities, never current personnel names. */
@Component
@RequiredArgsConstructor
public class MesSubmissionSignatureIdentityReader {
    private final ElectronicSignatureQueryService signatureQueryService;
    private final MesProProcessPoolEventMapper eventMapper;
    private final MesProBatchRecordExecutionSignatureMapper simulationSignatureMapper;
    private final MesProductionSubmissionReadBinding productionBinding;

    public MesTeamLeaderActiveOrderDetail.SignatureDetail read(Long signatureId, Long eventId,
                                                               Long activeOrderId, String action) {
        if (signatureId == null) return null;
        var event = eventMapper.selectById(eventId);
        require(event != null && Objects.equals(event.getId(), eventId)
                && Objects.equals(event.getTenantId(), TenantContextHolder.getRequiredTenantId())
                && Objects.equals(event.getSignatureId(), signatureId)
                && Objects.equals(event.getActualEmployeeId(), event.getSignatureUserId()));
        if (Set.of("SIMULATED_PRODUCTION_SUBMIT", "SIMULATED_PQC_INSPECTION").contains(
                event.getTemplateType() == null ? "" : event.getTemplateType())) {
            return readSimulation(event, signatureId, activeOrderId, action);
        }
        var evidence = signatureQueryService.getById(signatureId);
        require(event != null && evidence != null
                && Objects.equals(event.getId(), eventId)
                && Objects.equals(event.getTenantId(), TenantContextHolder.getRequiredTenantId())
                && Objects.equals(event.getSignatureId(), signatureId)
                && Objects.equals(event.getSignatureUserId(), evidence.actorId())
                && Objects.equals(event.getActualEmployeeId(), event.getSignatureUserId())
                && Objects.equals(evidence.id(), signatureId)
                && MesBatchRecordSignatureSubjectAdapter.MODULE_CODE.equals(evidence.moduleCode())
                && MesBatchRecordSignatureSubjectAdapter.SUBJECT_TYPE.equals(evidence.subjectType())
                && action.equals(evidence.actionCode()) && evidence.signedAt() != null
                && "VALID".equals(evidence.verificationStatus()));
        var content = JSON.parseObject(evidence.canonicalContentJson());
        require(content != null);
        var payload = JSON.parseObject(event.getRawPayload());
        String domain = "PQC_SUBMIT".equals(action) ? "SYSTEM_USER"
                : payload == null ? null : payload.getString("signatureIdentityDomain");
        var identity = content.getJSONObject("signatureIdentity");
        require(identity != null && Objects.equals(domain, identity.getString("domain"))
                && Objects.equals(event.getDeviceAccountId(), identity.getLong("operatorId")));
        String name = frozenName(evidence);
        String[] subject = new String(Base64.getUrlDecoder().decode(evidence.subjectId()), StandardCharsets.UTF_8)
                .split("\n", -1);
        require(subject.length == 17 && action.equals(subject[1])
                && action.equals(content.getString("actionType"))
                && Objects.equals(subject[0], content.getString("executionId"))
                && Objects.equals(subject[9], content.getString("reviewSourceType"))
                && Objects.equals(subject[10], content.getString("reviewSourceId")));
        if ("PRODUCTION_SUBMIT".equals(action)) {
            var context = productionBinding.require(event, activeOrderId);
            require("PRODUCTION_SUBMIT".equals(event.getEventType())
                    && MesProductionSubmitSignatureContext.SOURCE_TYPE.equals(subject[9])
                    && Objects.equals(context.activeOrderId(), content.getLong("reviewSourceId"))
                    && Objects.equals(context.sourceName(), subject[11])
                    && Objects.equals(subject[11], content.getString("reviewSourceName")));
        } else {
            require("PQC_SUBMIT".equals(action) && "PQC_INSPECTION".equals(event.getEventType())
                    && "MES_PQC_INSPECTION_TASK".equals(event.getFeedbackSourceType())
                    && "MES_PQC_INSPECTION_TASK".equals(subject[9])
                    && Objects.equals(event.getFeedbackSourceId(), content.getLong("reviewSourceId")));
        }
        var verification = signatureQueryService.verifyEvidence(signatureId);
        require(verification != null && Objects.equals(verification.signatureId(), signatureId)
                && "VALID".equals(verification.verificationStatus())
                && Objects.equals(evidence.contentHash(), verification.storedContentHash())
                && Objects.equals(evidence.contentHash(), verification.calculatedContentHash())
                && Objects.equals(evidence.evidenceHash(), verification.storedEvidenceHash())
                && Objects.equals(evidence.evidenceHash(), verification.calculatedEvidenceHash()));
        return new MesTeamLeaderActiveOrderDetail.SignatureDetail().setSignatureId(signatureId)
                .setSignerName(name).setSignedAt(evidence.signedAt()).setRole(action);
    }

    private MesTeamLeaderActiveOrderDetail.SignatureDetail readSimulation(MesProProcessPoolEventDO event,
                                                                         Long signatureId, Long activeOrderId,
                                                                         String action) {
        var snapshot = JSON.parseObject(event.getSignatureSnapshot());
        var payload = JSON.parseObject(event.getRawPayload());
        var signature = simulationSignatureMapper.selectById(signatureId);
        require(snapshot != null && payload != null && signature != null
                && Boolean.TRUE.equals(snapshot.getBoolean("simulated"))
                && Boolean.TRUE.equals(payload.getBoolean("simulated"))
                && Objects.equals(snapshot.getLong("signatureId"), signatureId)
                && Objects.equals(snapshot.getLong("actorId"), event.getSignatureUserId())
                && Objects.equals(snapshot.getLong("objectId"), activeOrderId)
                && action.equals(snapshot.getString("actionType"))
                && Objects.equals(signature.getId(), signatureId)
                && Objects.equals(signature.getActorId(), event.getSignatureUserId())
                && action.equals(signature.getActionType())
                && "SIMULATION_SESSION".equals(signature.getSignatureMode())
                && Boolean.FALSE.equals(signature.getPasswordVerified())
                && "MES_ACTIVE_ORDER_SIMULATION".equals(signature.getReviewSourceType())
                && Objects.equals(signature.getReviewSourceId(), activeOrderId)
                && signature.getSignedAt() != null
                && signature.getActorName() != null && !signature.getActorName().isBlank()
                && ("PRODUCTION_SUBMIT".equals(action)
                    ? "PRODUCTION_SUBMIT".equals(event.getEventType()) && "SIMULATED_PRODUCTION_SUBMIT".equals(event.getTemplateType())
                    : "PQC_SUBMIT".equals(action) && "PQC_INSPECTION".equals(event.getEventType()) && "SIMULATED_PQC_INSPECTION".equals(event.getTemplateType())));
        // A projection ID is not a unified formal signature ID and must not become a formal evidence link.
        return new MesTeamLeaderActiveOrderDetail.SignatureDetail()
                .setSignerName(signature.getActorName() + "（模拟）")
                .setSignedAt(signature.getSignedAt()).setRole("SIMULATION_SESSION");
    }

    /** Call after the enclosing reader has checked the formal binding and signature hashes. */
    static String frozenName(ElectronicSignatureEvidenceDTO evidence) {
        var content = JSON.parseObject(evidence.canonicalContentJson());
        var identity = content == null ? null : content.getJSONObject("signatureIdentity");
        require(identity != null && Set.of("SYSTEM_USER", "MES_EMPLOYEE_PROFILE").contains(identity.getString("domain") == null ? "" : identity.getString("domain"))
                && Objects.equals(identity.getLong("tenantId"), TenantContextHolder.getRequiredTenantId())
                && Objects.equals(identity.getLong("signerId"), evidence.actorId())
                && identity.getString("displayName") != null && !identity.getString("displayName").isBlank());
        return identity.getString("displayName");
    }

    private static void require(boolean valid) {
        if (!valid) throw new IllegalStateException("MES_SUBMISSION_SIGNATURE_IDENTITY_INVALID");
    }
}
