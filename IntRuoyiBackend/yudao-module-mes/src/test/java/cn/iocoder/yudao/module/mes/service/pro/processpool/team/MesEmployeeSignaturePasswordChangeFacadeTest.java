package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesEmployeeSignaturePasswordChangeReqVO;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineEmployeeSwitchService;
import cn.iocoder.yudao.module.system.api.signature.dto.SignatureAuthenticationOutcome;
import org.junit.jupiter.api.Test;

import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.ESIGN_IDENTITY_BAD_CREDENTIALS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesEmployeeSignaturePasswordChangeFacadeTest {
    @Test void badCredentialReturnsBusinessErrorAndDoesNotUpdateSettings() {
        var employees=mock(MesFrontlineEmployeeSwitchService.class);
        var authentication=mock(MesEmployeeSignatureAuthenticationService.class);
        var credentials=mock(MesEmployeeSignatureCredentialService.class);
        when(authentication.authenticatePasswordChange(any())).thenReturn(new SignatureAuthenticationOutcome(null,"BAD_CREDENTIALS"));
        var request=new MesEmployeeSignaturePasswordChangeReqVO();
        request.setIdentityDomain("MES_EMPLOYEE_PROFILE");
        request.setEmployeeProfileId(7L);
        request.setCurrentPassword("wrong");
        TenantContextHolder.setTenantId(1L);
        try(var security=mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(2L);
            var facade=new MesEmployeeSignaturePasswordChangeFacade(employees,authentication,credentials);
            var error=assertThrows(ServiceException.class,()->facade.change(request));
            assertEquals(ESIGN_IDENTITY_BAD_CREDENTIALS.getCode(),error.getCode());
            assertEquals(ESIGN_IDENTITY_BAD_CREDENTIALS.getMsg(),error.getMessage());
            verify(employees).switchActualEmployee(any());
            verifyNoInteractions(credentials);
        } finally { TenantContextHolder.clear(); }
    }
}
