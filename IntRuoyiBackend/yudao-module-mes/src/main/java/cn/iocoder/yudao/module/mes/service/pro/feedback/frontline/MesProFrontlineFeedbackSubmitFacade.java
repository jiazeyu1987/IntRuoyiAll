package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.*;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.signature.service.ElectronicSignatureAuthenticationFacade;
import cn.iocoder.yudao.module.system.api.signature.dto.SignatureAuthenticationRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
@Service
public class MesProFrontlineFeedbackSubmitFacade implements MesProFrontlineFeedbackSubmitService {
    private final ElectronicSignatureAuthenticationFacade authentication;
    private final MesProFrontlineFeedbackSubmitServiceImpl executor;
    public MesProFrontlineFeedbackSubmitFacade(ElectronicSignatureAuthenticationFacade authentication,MesProFrontlineFeedbackSubmitServiceImpl executor) {this.authentication=authentication;this.executor=executor;}
    @Override @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public MesProFrontlineFeedbackSubmitRespVO submit(MesProFrontlineFeedbackSubmitReqVO request) {
        if(request==null || request.getSignatureId()!=null) throw new IllegalArgumentException("PRODUCTION_SIGNATURE_CLIENT_ID_FORBIDDEN");
        var replay=executor.findVerifiedReplay(request);
        if(replay!=null) return replay;
        String requestIdentity=cn.hutool.crypto.digest.DigestUtil.sha256Hex(request.getProcessPoolSubmissionIdempotencyKey());
        var signer=authentication.authenticate(new SignatureAuthenticationRequest(TenantContextHolder.getRequiredTenantId(),request.getSignatureIdentityDomain(),request.getSignatureEmployeeId(),SecurityFrameworkUtils.getLoginUserId(),requestIdentity,request.getSignaturePassword()));
        return executor.submitAuthenticated(request,signer);
    }
}
