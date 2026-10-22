package cn.iocoder.yudao.module.mes.service.pro.processpool.team;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesEmployeeSignaturePasswordChangeReqVO;
import cn.iocoder.yudao.module.mes.service.pro.frontline.*;
import cn.iocoder.yudao.module.system.api.signature.dto.SignatureAuthenticationRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
@Service
public class MesEmployeeSignaturePasswordChangeFacade {
    private final MesFrontlineEmployeeSwitchService employees;
    private final MesEmployeeSignatureAuthenticationService authentication;
    private final MesEmployeeSignatureCredentialService credentials;
    public MesEmployeeSignaturePasswordChangeFacade(MesFrontlineEmployeeSwitchService employees,MesEmployeeSignatureAuthenticationService authentication,MesEmployeeSignatureCredentialService credentials) {this.employees=employees;this.authentication=authentication;this.credentials=credentials;}
    @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public void change(MesEmployeeSignaturePasswordChangeReqVO r) {
        employees.switchActualEmployee(new MesFrontlineEmployeeSwitchCommand(SecurityFrameworkUtils.getLoginUserId(),r.getActiveOrderId(),r.getRouteId(),r.getRouteProcessId(),r.getProcessId(),r.getEmployeeProfileId(),r.getIdentityDomain()));
        if(!"MES_EMPLOYEE_PROFILE".equals(r.getIdentityDomain())) throw new IllegalArgumentException("SIGNATURE_IDENTITY_DOMAIN_UNSUPPORTED");
        var result=authentication.authenticatePasswordChange(new SignatureAuthenticationRequest(TenantContextHolder.getRequiredTenantId(),r.getIdentityDomain(),r.getEmployeeProfileId(),SecurityFrameworkUtils.getLoginUserId(),java.util.UUID.randomUUID().toString(),r.getCurrentPassword()));
        if(!result.succeeded()) throw new IllegalArgumentException(result.rejectionCode());
        credentials.change(result.signer(),r.getNewPassword());
    }
    @Transactional(propagation=Propagation.NOT_SUPPORTED,readOnly=true)
    public MesEmployeeSignatureCredentialService.Qualification qualification(Long activeOrderId,Long routeId,Long routeProcessId,Long processId,Long employeeProfileId,String domain) {
        employees.switchActualEmployee(new MesFrontlineEmployeeSwitchCommand(SecurityFrameworkUtils.getLoginUserId(),activeOrderId,routeId,routeProcessId,processId,employeeProfileId,domain));
        if(!"MES_EMPLOYEE_PROFILE".equals(domain)) throw new IllegalArgumentException("SIGNATURE_IDENTITY_DOMAIN_UNSUPPORTED");
        return credentials.qualification(employeeProfileId);
    }
}
