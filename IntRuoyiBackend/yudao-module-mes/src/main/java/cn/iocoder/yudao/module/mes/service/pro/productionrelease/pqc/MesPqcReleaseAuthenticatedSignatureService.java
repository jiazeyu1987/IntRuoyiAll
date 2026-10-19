package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import cn.iocoder.yudao.module.mes.service.pro.batchrecord.*;
import cn.iocoder.yudao.module.signature.api.dto.*;
import cn.iocoder.yudao.module.signature.service.*;
import cn.iocoder.yudao.module.system.api.signature.dto.AuthenticatedSigner;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MesPqcReleaseAuthenticatedSignatureService {
    private final ElectronicSignatureTransactionExecutor executor;
    private final ElectronicSignatureVerifiedFactService verifier;
    private final AdminUserApi users;
    private final cn.iocoder.yudao.module.dcc.service.file.DccElectronicSignatureAuthorizationService authorization;
    @Transactional(propagation=Propagation.MANDATORY, rollbackFor=Exception.class)
    public Long record(AuthenticatedSigner signer, Long batchId, Long applicationId, String opinion, String payloadHash) {
        authorization.validateElectronicSignatureEnabledLocked(signer.actorId());
        var user = users.getUser(signer.actorId());
        if (user == null || user.getNickname() == null || user.getNickname().isBlank()) throw new IllegalStateException("SIGNATURE_DISPLAY_NAME_MISSING");
        String action = MesProBatchRecordExecutionSignatureService.ACTION_PQC_RELEASE;
        String subjectId = MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(batchId, action,
                null,null,null,null,null,null,null,"PQC_RELEASE_APPLICATION",applicationId,"PQC生产放行",action,null,null,null,null);
        subjectId = MesBatchRecordSignatureSubjectAdapter.withOpinionSource(subjectId, opinion);
        String version = MesBatchRecordSignatureSubjectAdapter.subjectVersion(subjectId);
        String reason = opinion == null || opinion.isBlank() ? action : opinion;
        var command = new ElectronicSignatureCommand("MES", action, MesBatchRecordSignatureSubjectAdapter.SUBJECT_TYPE,
                subjectId, version, null, reason, "MES_PQC_RELEASE|" + signer.actorId() + "|" + applicationId + "|" + payloadHash, null, null);
        var snapshot = new SignatureSubjectSnapshot(command.subjectType(),subjectId,version,
                MesBatchRecordSignatureSubjectAdapter.canonicalBusinessContent(subjectId,action,reason),null,null,null,null,null,null,null);
        var definition = new MesBatchRecordSignatureSubjectAdapter().supportedActions().stream()
                .filter(item -> item.actionCode().equals(action)).findFirst().orElseThrow();
        var request = new AuthenticatedSignatureRequest(command,snapshot,definition,
                JsonUtils.parseTree(JsonUtils.toJsonString(Map.of("applicationId",applicationId.toString(),"batchExecutionId",batchId.toString(),"decisionPayloadHash",payloadHash))),
                user.getNickname(),JsonUtils.parseTree("{\"state\":\"NOT_REQUIRED\",\"snapshot\":null}"));
        var result = executor.executeAuthenticated(request,signer);
        MesSignatureContentProtocol.parseVerifiedContent(verifier.requireVerifiedFact(result.signatureId()),signer.tenantId());
        return result.signatureId();
    }
}
