package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.service.file.DccElectronicSignatureAuthorizationService;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionSignatureDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionSignatureMapper;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFrontlineFeedbackErrorCodeConstants.PRO_FRONTLINE_FEEDBACK_SIGNATURE_NOT_AUTHORIZED;
import static cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFrontlineFeedbackErrorCodeConstants.PRO_FRONTLINE_FEEDBACK_SIGNATURE_PASSWORD_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFrontlineFeedbackErrorCodeConstants.PRO_FRONTLINE_FEEDBACK_SIGNATURE_PERSIST_FAILED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MesFrontlineSubmitSignatureServiceImplTest extends BaseMockitoUnitTest {

    @Mock
    private AdminUserService adminUserService;
    @Mock
    private DccElectronicSignatureAuthorizationService authorizationService;
    @Mock
    private MesProBatchRecordExecutionSignatureMapper signatureMapper;

    @InjectMocks
    private MesFrontlineSubmitSignatureServiceImpl signatureService;

    @Test
    void recordSubmitSignature_persistsActualEmployeePasswordSignature() {
        AdminUserDO actualEmployee = AdminUserDO.builder()
                .id(3001L)
                .username("operator01")
                .nickname("一线员工")
                .deptId(101L)
                .password("encoded-password")
                .build();
        when(authorizationService.isElectronicSignatureEnabled(3001L)).thenReturn(true);
        when(adminUserService.getUser(3001L)).thenReturn(actualEmployee);
        when(adminUserService.isPasswordMatch("secret", "encoded-password")).thenReturn(true);
        when(signatureMapper.insert(any(MesProBatchRecordExecutionSignatureDO.class))).thenAnswer(invocation -> {
            MesProBatchRecordExecutionSignatureDO signature = invocation.getArgument(0);
            signature.setId(7001L);
            return 1;
        });

        Long signatureId = signatureService.recordSubmitSignature(3001L, "secret", "本班次报工");

        assertEquals(7001L, signatureId);
        ArgumentCaptor<MesProBatchRecordExecutionSignatureDO> captor =
                ArgumentCaptor.forClass(MesProBatchRecordExecutionSignatureDO.class);
        verify(signatureMapper).insert(captor.capture());
        MesProBatchRecordExecutionSignatureDO signature = captor.getValue();
        assertEquals(3001L, signature.getActorId());
        assertEquals("operator01", signature.getActorUsernameSnapshot());
        assertEquals("一线员工", signature.getActorNicknameSnapshot());
        assertEquals("SUBMIT", signature.getActionType());
        assertEquals("PASSWORD", signature.getSignatureMode());
        assertEquals("本班次报工", signature.getComment());
        assertEquals("SERVER_TIME", signature.getSignatureTimeMode());
        assertEquals(signature.getSignedAt(), signature.getSignatureDisplayAt());
        assertEquals(0, signature.getSignedAt().getNano());
        assertNotNull(signature.getSelectedTimeAuditHash());
        assertTrue(signature.getSelectedTimeAuditHash().matches("[0-9a-f]{64}"));
        assertTrue(Boolean.TRUE.equals(signature.getPasswordVerified()));
    }

    @Test
    void recordSubmitSignature_rejectsDisabledAuthorizationBeforePasswordLookup() {
        when(authorizationService.isElectronicSignatureEnabled(3001L)).thenReturn(false);

        assertServiceException(() -> signatureService.recordSubmitSignature(3001L, "secret", null),
                PRO_FRONTLINE_FEEDBACK_SIGNATURE_NOT_AUTHORIZED, 3001L);

        verify(adminUserService, never()).getUser(3001L);
        verify(signatureMapper, never()).insert(any(MesProBatchRecordExecutionSignatureDO.class));
    }

    @Test
    void recordSubmitSignature_rejectsInvalidActualEmployeePassword() {
        when(authorizationService.isElectronicSignatureEnabled(3001L)).thenReturn(true);
        when(adminUserService.getUser(3001L)).thenReturn(AdminUserDO.builder()
                .id(3001L)
                .password("encoded-password")
                .build());
        when(adminUserService.isPasswordMatch("wrong", "encoded-password")).thenReturn(false);

        assertServiceException(() -> signatureService.recordSubmitSignature(3001L, "wrong", null),
                PRO_FRONTLINE_FEEDBACK_SIGNATURE_PASSWORD_INVALID, 3001L);

        verify(signatureMapper, never()).insert(any(MesProBatchRecordExecutionSignatureDO.class));
    }

    @Test
    void recordSubmitSignature_rejectsInsertWithoutGeneratedId() {
        when(authorizationService.isElectronicSignatureEnabled(3001L)).thenReturn(true);
        when(adminUserService.getUser(3001L)).thenReturn(AdminUserDO.builder()
                .id(3001L)
                .username("operator01")
                .nickname("一线员工")
                .password("encoded-password")
                .build());
        when(adminUserService.isPasswordMatch("secret", "encoded-password")).thenReturn(true);
        when(signatureMapper.insert(any(MesProBatchRecordExecutionSignatureDO.class))).thenReturn(1);

        assertServiceException(() -> signatureService.recordSubmitSignature(3001L, "secret", "报工"),
                PRO_FRONTLINE_FEEDBACK_SIGNATURE_PERSIST_FAILED, 3001L);
    }
}
