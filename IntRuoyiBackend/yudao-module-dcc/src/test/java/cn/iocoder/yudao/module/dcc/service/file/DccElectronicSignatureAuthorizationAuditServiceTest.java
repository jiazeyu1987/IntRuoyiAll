package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccElectronicSignatureAuthorizationAuditDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccElectronicSignatureAuthorizationAuditMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_AUTH_REASON_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_PERSIST_FAILED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccElectronicSignatureAuthorizationAuditServiceTest extends BaseMockitoUnitTest {

    @Mock
    private DccElectronicSignatureAuthorizationAuditMapper authorizationAuditMapper;

    @InjectMocks
    private DccElectronicSignatureAuthorizationAuditServiceImpl service;

    @Test
    void recordAuthorizationChange_requiresReason() {
        DccElectronicSignatureAuthorizationAuditDO audit = DccElectronicSignatureAuthorizationAuditDO.builder()
                .targetUserId(99L)
                .operatorId(1L)
                .beforeState("DISABLED")
                .afterState("ENABLED")
                .reason(" ")
                .operatedAt(LocalDateTime.of(2026, 5, 26, 10, 0))
                .build();

        assertServiceException(() -> service.recordAuthorizationChange(audit),
                CONTROLLED_FILE_SIGNATURE_AUTH_REASON_REQUIRED);
    }

    @Test
    void recordAuthorizationChange_persistsStateChangeReasonAndOperator() {
        when(authorizationAuditMapper.insert(any(DccElectronicSignatureAuthorizationAuditDO.class))).thenReturn(1);
        DccElectronicSignatureAuthorizationAuditDO audit = DccElectronicSignatureAuthorizationAuditDO.builder()
                .targetUserId(99L)
                .operatorId(1L)
                .beforeState("UNAUTHORIZED")
                .beforeEnabled(Boolean.FALSE)
                .afterState("ENABLED")
                .afterEnabled(Boolean.TRUE)
                .reason("完成岗位电子签名授权")
                .operatedAt(LocalDateTime.of(2026, 5, 26, 10, 0))
                .build();

        service.recordAuthorizationChange(audit);

        ArgumentCaptor<DccElectronicSignatureAuthorizationAuditDO> captor =
                ArgumentCaptor.forClass(DccElectronicSignatureAuthorizationAuditDO.class);
        verify(authorizationAuditMapper).insert(captor.capture());
        assertEquals(99L, captor.getValue().getTargetUserId());
        assertEquals(1L, captor.getValue().getOperatorId());
        assertEquals("UNAUTHORIZED", captor.getValue().getBeforeState());
        assertEquals("ENABLED", captor.getValue().getAfterState());
        assertEquals("完成岗位电子签名授权", captor.getValue().getReason());
        assertEquals(LocalDateTime.of(2026, 5, 26, 10, 0), captor.getValue().getOperatedAt());
    }

    @Test
    void recordAuthorizationChange_failsFastWhenAuditInsertFails() {
        when(authorizationAuditMapper.insert(any(DccElectronicSignatureAuthorizationAuditDO.class))).thenReturn(0);
        DccElectronicSignatureAuthorizationAuditDO audit = DccElectronicSignatureAuthorizationAuditDO.builder()
                .targetUserId(99L)
                .operatorId(1L)
                .beforeState("ENABLED")
                .afterState("DISABLED")
                .reason("人员岗位调整")
                .operatedAt(LocalDateTime.of(2026, 5, 26, 10, 0))
                .build();

        assertServiceException(() -> service.recordAuthorizationChange(audit),
                CONTROLLED_FILE_SIGNATURE_PERSIST_FAILED);
    }
}
