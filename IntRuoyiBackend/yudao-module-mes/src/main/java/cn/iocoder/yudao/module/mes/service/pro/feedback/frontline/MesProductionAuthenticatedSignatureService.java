package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamEmployeeProfileMapper;
import cn.iocoder.yudao.module.signature.api.dto.*;
import cn.iocoder.yudao.module.signature.service.*;
import cn.iocoder.yudao.module.system.api.signature.dto.AuthenticatedSigner;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.util.Map;
@Service
public class MesProductionAuthenticatedSignatureService {
    @jakarta.annotation.Resource private cn.iocoder.yudao.module.dcc.service.file.DccElectronicSignatureAuthorizationService signatureAuthorization;
    private final ElectronicSignatureTransactionExecutor executor;
    private final ElectronicSignatureVerifiedFactService verifier;
    private final MesProcessPoolTeamEmployeeProfileMapper profiles;
    private final AdminUserApi users;
    public MesProductionAuthenticatedSignatureService(ElectronicSignatureTransactionExecutor executor,ElectronicSignatureVerifiedFactService verifier,MesProcessPoolTeamEmployeeProfileMapper profiles,AdminUserApi users) {this.executor=executor;this.verifier=verifier;this.profiles=profiles;this.users=users;}
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public Long record(AuthenticatedSigner signer,MesProductionSubmitSignatureContext context) {
        String name;
        if("MES_EMPLOYEE_PROFILE".equals(signer.domain())) {
            var profile=profiles.selectSignatureIdentityForUpdate(signer.tenantId(),signer.actorId());
            if(profile==null || profile.getDisplayName()==null || profile.getDisplayName().isBlank()) throw new IllegalStateException("SIGNATURE_DISPLAY_NAME_MISSING");
            name=profile.getDisplayName();
        } else {
            signatureAuthorization.validateElectronicSignatureEnabledLocked(signer.actorId());
            var user=users.getUser(signer.actorId());
            if(user==null || user.getNickname()==null || user.getNickname().isBlank()) throw new IllegalStateException("SIGNATURE_DISPLAY_NAME_MISSING");
            name=user.getNickname();
        }
        String action=MesProBatchRecordExecutionSignatureService.ACTION_PRODUCTION_SUBMIT;
        String subjectId=MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(0L,action,null,null,null,null,null,null,null,MesProductionSubmitSignatureContext.SOURCE_TYPE,context.activeOrderId(),context.sourceName(),null,null,null,null,null);
        String version=MesBatchRecordSignatureSubjectAdapter.subjectVersion(subjectId);
        var command=new ElectronicSignatureCommand("MES",action,MesBatchRecordSignatureSubjectAdapter.SUBJECT_TYPE,subjectId,version,null,"一线生产报工提交","MES|"+signer.domain()+"|"+signer.actorId()+"|PRODUCTION_SUBMIT|"+cn.hutool.crypto.digest.DigestUtil.sha256Hex(subjectId),null,null);
        var subject=new SignatureSubjectSnapshot(command.subjectType(),subjectId,version,MesBatchRecordSignatureSubjectAdapter.canonicalBusinessContent(subjectId,action,command.reason()),null,null,null,null,null,null,null);
        var definition=new MesBatchRecordSignatureSubjectAdapter().supportedActions().stream().filter(a->a.actionCode().equals(action)).findFirst().orElseThrow();
        var image=JsonUtils.parseTree("{\"state\":\"NOT_REQUIRED\",\"snapshot\":null}");
        var result=executor.executeAuthenticated(new AuthenticatedSignatureRequest(command,subject,definition,JsonUtils.parseTree(JsonUtils.toJsonString(Map.of("activeOrderId",context.activeOrderId().toString(),"routeProcessId",context.routeProcessId().toString(),"processId",context.processId().toString(),"submissionIdempotencyKey",context.submissionIdempotencyKey()))),name,image),signer);
        MesSignatureContentProtocol.parseVerifiedContent(verifier.requireVerifiedFact(result.signatureId()),signer.tenantId());
        return result.signatureId();
    }
}
