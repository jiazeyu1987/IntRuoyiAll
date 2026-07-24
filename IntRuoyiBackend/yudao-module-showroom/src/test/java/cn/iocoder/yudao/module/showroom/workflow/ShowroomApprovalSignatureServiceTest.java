package cn.iocoder.yudao.module.showroom.workflow;

import cn.iocoder.yudao.module.dcc.service.file.DccElectronicSignatureAuthorizationService;
import cn.iocoder.yudao.module.showroom.dal.dataobject.workflow.ShowroomChangeRequestSignatureDO;
import cn.iocoder.yudao.module.showroom.dal.mysql.workflow.ShowroomChangeRequestSignatureMapper;
import cn.iocoder.yudao.module.showroom.workflow.service.ShowroomApprovalSignatureService;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShowroomApprovalSignatureServiceTest {

    @Mock
    private AdminUserService adminUserService;
    @Mock
    private DccElectronicSignatureAuthorizationService authorizationService;
    @Mock
    private ShowroomChangeRequestSignatureMapper signatureMapper;

    @InjectMocks
    private ShowroomApprovalSignatureService signatureService;

    @Test
    void signShouldFailWhenElectronicSignatureNotAuthorized() {
        when(authorizationService.isElectronicSignatureEnabled(200L)).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> signatureService.recordSignedDecision(
                900L, "SUPERVISOR", "APPROVE", 200L, "111111", "主管通过"));
    }

    @Test
    void signShouldFailWhenPasswordDoesNotMatch() {
        AdminUserDO user = new AdminUserDO();
        user.setId(200L);
        user.setPassword("encoded-password");
        when(authorizationService.isElectronicSignatureEnabled(200L)).thenReturn(true);
        when(adminUserService.getUser(200L)).thenReturn(user);
        when(adminUserService.isPasswordMatch("bad-password", "encoded-password")).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> signatureService.recordSignedDecision(
                901L, "SUPERVISOR", "REJECT", 200L, "bad-password", "驳回"));
    }

    @Test
    void signShouldPersistSignatureRecordAfterAuthorizationAndPasswordVerification() {
        AdminUserDO user = new AdminUserDO();
        user.setId(300L);
        user.setPassword("encoded-password");
        when(authorizationService.isElectronicSignatureEnabled(300L)).thenReturn(true);
        when(adminUserService.getUser(300L)).thenReturn(user);
        when(adminUserService.isPasswordMatch("111111", "encoded-password")).thenReturn(true);
        when(signatureMapper.insert(any(ShowroomChangeRequestSignatureDO.class))).thenReturn(1);

        signatureService.recordSignedDecision(902L, "PUBLICITY", "APPROVE", 300L, "111111", "企宣通过");

        ArgumentCaptor<ShowroomChangeRequestSignatureDO> captor =
                ArgumentCaptor.forClass(ShowroomChangeRequestSignatureDO.class);
        verify(signatureMapper).insert(captor.capture());
        assertEquals(902L, captor.getValue().getChangeRequestId());
        assertEquals("PUBLICITY", captor.getValue().getApprovalStage());
        assertEquals("APPROVE", captor.getValue().getActionType());
        assertEquals(300L, captor.getValue().getActorId());
        assertEquals("企宣通过", captor.getValue().getComment());
        assertEquals(Boolean.TRUE, captor.getValue().getPasswordVerified());
        assertEquals("PASSWORD", captor.getValue().getSignatureMode());
    }
}
