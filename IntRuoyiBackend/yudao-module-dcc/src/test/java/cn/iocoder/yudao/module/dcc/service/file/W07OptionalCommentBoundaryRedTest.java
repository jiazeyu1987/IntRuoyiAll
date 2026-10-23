package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureCommand;
import cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureRecordMapper;
import cn.iocoder.yudao.module.signature.service.ElectronicSignatureServiceImpl;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real shared guard RED/GREEN probes. Authentication deliberately throws; no fake successful persistence. */
public class W07OptionalCommentBoundaryRedTest {
    static class AuthenticationReached extends RuntimeException {}
    @AfterEach void cleanup(){TenantContextHolder.clear();}

    @Test void dccNullReceiptCommentMustReachActualSignatureAuthorizationBoundary() { probeDcc(null); }
    @Test void dccEmptyReceiptCommentMustReachActualSignatureAuthorizationBoundary() { probeDcc(""); }
    @Test void dccWhitespaceReceiptCommentMustReachActualSignatureAuthorizationBoundary() { probeDcc("  "); }
    private void probeDcc(String comment) {
        var authorization=mock(DccElectronicSignatureAuthorizationService.class);
        doThrow(new AuthenticationReached()).when(authorization).validateElectronicSignatureEnabled(99L);
        var actual=new DccSignatureVerificationServiceImpl();
        ReflectionTestUtils.setField(actual,"electronicSignatureAuthorizationService",authorization);
        assertThrows(AuthenticationReached.class,()->actual.verifyPasswordAndCreateSignature(
                99L,42L,"DISTRIBUTION:51:71","DISTRIBUTION","DISTRIBUTION_ACK","nonempty-test-credential",comment),
                "Optional comment must not be rejected before actual signature authorization");
        verify(authorization).validateElectronicSignatureEnabled(99L);
    }
    @Test void dccApprovalAndAssignmentReasonsRemainRequired() {
        var authorization=mock(DccElectronicSignatureAuthorizationService.class);
        var actual=new DccSignatureVerificationServiceImpl();
        ReflectionTestUtils.setField(actual,"electronicSignatureAuthorizationService",authorization);
        for(var action:List.of("APPROVE","ASSIGN"))
            assertThrows(ServiceException.class,()->actual.verifyPasswordAndCreateSignature(99L,42L,"task-42","MATRIX_REVIEW",action,"nonempty-test-credential",null));
        verifyNoInteractions(authorization);
    }

    @Test void kernelNullReceiptReasonMustReachActualReauthenticationBoundary(){probeKernel(null);}
    @Test void kernelEmptyReceiptReasonMustReachActualReauthenticationBoundary(){probeKernel("");}
    @Test void kernelWhitespaceReceiptReasonMustReachActualReauthenticationBoundary(){probeKernel("  ");}
    private void probeKernel(String comment) {
        TenantContextHolder.setTenantId(1L);
        var actual=new ElectronicSignatureServiceImpl();
        var users=mock(AdminUserApi.class);
        doThrow(new AuthenticationReached()).when(users).reauthenticateForSignature(99L,"nonempty-test-credential");
        ReflectionTestUtils.setField(actual,"adminUserApi",users);
        ReflectionTestUtils.setField(actual,"signatureRecordMapper",mock(ElectronicSignatureRecordMapper.class));
        ReflectionTestUtils.setField(actual,"subjectAdapters",List.of(new DccControlledFileSignatureSubjectAdapter()));
        var command=new ElectronicSignatureCommand("DCC","DISTRIBUTION_ACK","DCC_CONTROLLED_FILE",
                "exact-receipt-subject","exact-version","nonempty-test-credential",comment,"receipt-idempotency",null,null);
        try(var security=mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            assertThrows(AuthenticationReached.class,()->actual.sign(command),
                    "Optional DCC receipt reason must not be rejected before reauthentication");
        }
        verify(users).reauthenticateForSignature(99L,"nonempty-test-credential");
    }
    @Test void kernelOtherActionReasonRemainsRequired() {
        var actual=new ElectronicSignatureServiceImpl();var users=mock(AdminUserApi.class);
        ReflectionTestUtils.setField(actual,"adminUserApi",users);
        var command=new ElectronicSignatureCommand("DCC","APPROVE","DCC_CONTROLLED_FILE","exact-subject","exact-version",
                "nonempty-test-credential",null,"approval-idempotency",null,null);
        assertThrows(ServiceException.class,()->actual.sign(command));verifyNoInteractions(users);
    }
}
