package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccElectronicSignatureAuthorizationDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccElectronicSignatureFailureAuditDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccElectronicSignaturePolicyDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccElectronicSignatureAuthorizationMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccElectronicSignatureFailureAuditMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccElectronicSignaturePolicyMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_POLICY_MISSING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DccElectronicSignatureFailureAuditServiceTest extends BaseMockitoUnitTest {

    @Mock
    private DccElectronicSignatureFailureAuditMapper failureAuditMapper;
    @Mock
    private DccElectronicSignatureAuthorizationMapper authorizationMapper;
    @Mock
    private DccElectronicSignaturePolicyMapper policyMapper;

    @InjectMocks
    private DccElectronicSignatureFailureAuditServiceImpl service;

    @Test
    void recordPasswordFailure_persistsAuditWithoutPasswordAndLocksAfterFiveFailuresInFifteenMinutes() {
        LocalDateTime failedAt = LocalDateTime.of(2026, 5, 26, 10, 0);
        when(policyMapper.selectEnabledPolicy()).thenReturn(DccElectronicSignaturePolicyDO.builder()
                .passwordFailureWindowMinutes(15)
                .passwordFailureThreshold(5)
                .lockMinutes(30)
                .evidencePayloadVersion("v1")
                .hashAlgorithm("HMAC_SHA256")
                .status(0)
                .build());
        when(authorizationMapper.selectByUserId(99L)).thenReturn(DccElectronicSignatureAuthorizationDO.builder()
                .id(5L)
                .userId(99L)
                .electronicSignatureEnabled(Boolean.TRUE)
                .authorizationState("ENABLED")
                .failureCount(4)
                .lastFailureAt(failedAt.minusMinutes(5))
                .build());
        when(failureAuditMapper.insert(any(DccElectronicSignatureFailureAuditDO.class))).thenReturn(1);
        when(authorizationMapper.updateById(any(DccElectronicSignatureAuthorizationDO.class))).thenReturn(1);

        boolean locked = service.recordPasswordFailure(DccElectronicSignatureFailureAuditCommand.builder()
                .targetUserId(99L)
                .controlledFileId(710088L)
                .revisionId(710088L)
                .taskId("bpm-task-9001")
                .actionType("APPROVED")
                .meaningCode("REVIEW_APPROVE")
                .failureMessage("password verification failed")
                .failedAt(failedAt)
                .remoteIp("127.0.0.1")
                .userAgent("Playwright")
                .build());

        assertTrue(locked);
        ArgumentCaptor<DccElectronicSignatureFailureAuditDO> failureCaptor =
                ArgumentCaptor.forClass(DccElectronicSignatureFailureAuditDO.class);
        verify(failureAuditMapper).insert(failureCaptor.capture());
        assertEquals("PASSWORD_INVALID", failureCaptor.getValue().getFailureType());
        assertEquals("password verification failed", failureCaptor.getValue().getFailureMessage());
        assertFalse(failureCaptor.getValue().toString().contains("wrong-password"));

        ArgumentCaptor<DccElectronicSignatureAuthorizationDO> authorizationCaptor =
                ArgumentCaptor.forClass(DccElectronicSignatureAuthorizationDO.class);
        verify(authorizationMapper).updateById(authorizationCaptor.capture());
        assertEquals(5L, authorizationCaptor.getValue().getId());
        assertEquals("LOCKED", authorizationCaptor.getValue().getAuthorizationState());
        assertEquals(5, authorizationCaptor.getValue().getFailureCount());
        assertEquals(failedAt.plusMinutes(30), authorizationCaptor.getValue().getLockedUntil());
        assertNotNull(authorizationCaptor.getValue().getLockReason());
    }

    @Test
    void recordPasswordFailure_returnsFalseWhenThresholdIsNotReached() {
        LocalDateTime failedAt = LocalDateTime.of(2026, 5, 26, 10, 0);
        when(policyMapper.selectEnabledPolicy()).thenReturn(DccElectronicSignaturePolicyDO.builder()
                .passwordFailureWindowMinutes(15)
                .passwordFailureThreshold(5)
                .lockMinutes(30)
                .evidencePayloadVersion("v1")
                .hashAlgorithm("HMAC_SHA256")
                .status(0)
                .build());
        when(authorizationMapper.selectByUserId(99L)).thenReturn(DccElectronicSignatureAuthorizationDO.builder()
                .id(5L)
                .userId(99L)
                .electronicSignatureEnabled(Boolean.TRUE)
                .authorizationState("ENABLED")
                .failureCount(3)
                .lastFailureAt(failedAt.minusMinutes(5))
                .build());
        when(failureAuditMapper.insert(any(DccElectronicSignatureFailureAuditDO.class))).thenReturn(1);
        when(authorizationMapper.updateById(any(DccElectronicSignatureAuthorizationDO.class))).thenReturn(1);

        boolean locked = service.recordPasswordFailure(DccElectronicSignatureFailureAuditCommand.builder()
                .targetUserId(99L)
                .controlledFileId(710088L)
                .revisionId(710088L)
                .taskId("bpm-task-9001")
                .actionType("APPROVED")
                .meaningCode("REVIEW_APPROVE")
                .failureMessage("password verification failed")
                .failedAt(failedAt)
                .build());

        assertFalse(locked);
        ArgumentCaptor<DccElectronicSignatureAuthorizationDO> authorizationCaptor =
                ArgumentCaptor.forClass(DccElectronicSignatureAuthorizationDO.class);
        verify(authorizationMapper).updateById(authorizationCaptor.capture());
        assertEquals(4, authorizationCaptor.getValue().getFailureCount());
        assertNull(authorizationCaptor.getValue().getAuthorizationState());
        assertNull(authorizationCaptor.getValue().getLockedUntil());
    }

    @Test
    void recordPasswordFailure_failsFastWhenPolicyMissing() {
        when(policyMapper.selectEnabledPolicy()).thenReturn(null);

        assertServiceException(() -> service.recordPasswordFailure(DccElectronicSignatureFailureAuditCommand.builder()
                .targetUserId(99L)
                .failureMessage("password verification failed")
                .failedAt(LocalDateTime.of(2026, 5, 26, 10, 0))
                .build()), CONTROLLED_FILE_SIGNATURE_POLICY_MISSING);
        verifyNoInteractions(authorizationMapper, failureAuditMapper);
    }
}
