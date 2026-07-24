package cn.iocoder.yudao.module.dcc.service.file;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccElectronicSignatureAuthorizationDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccElectronicSignatureAuthorizationMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_AUTH_REASON_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_DISABLED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_LOCKED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_NOT_AUTHORIZED;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccElectronicSignatureAuthorizationServiceTest extends BaseMockitoUnitTest {

    @Mock
    private DccElectronicSignatureAuthorizationMapper authorizationMapper;
    @Mock
    private DccElectronicSignatureAuthorizationAuditService authorizationAuditService;

    @InjectMocks
    private DccElectronicSignatureAuthorizationServiceImpl service;

    @Test
    void nullableLockFields_allowExplicitNullClearingThroughMybatisUpdateById() throws NoSuchFieldException {
        assertAlwaysUpdateStrategy("lockedUntil");
        assertAlwaysUpdateStrategy("lockReason");
        assertAlwaysUpdateStrategy("lastFailureAt");
    }

    @Test
    void isElectronicSignatureEnabled_failsClosedWhenMissingConfig() {
        when(authorizationMapper.selectByUserId(99L)).thenReturn(null);

        assertFalse(service.isElectronicSignatureEnabled(99L));
    }

    @Test
    void isElectronicSignatureEnabled_allowsOnlyExplicitEnabledAndUnlockedAuthorization() {
        when(authorizationMapper.selectByUserId(99L)).thenReturn(DccElectronicSignatureAuthorizationDO.builder()
                .userId(99L)
                .electronicSignatureEnabled(Boolean.TRUE)
                .authorizationState("ENABLED")
                .build());

        assertTrue(service.isElectronicSignatureEnabled(99L));
    }

    @Test
    void isElectronicSignatureEnabled_rejectsLockedAuthorizationUntilUnlock() {
        when(authorizationMapper.selectByUserId(99L)).thenReturn(DccElectronicSignatureAuthorizationDO.builder()
                .userId(99L)
                .electronicSignatureEnabled(Boolean.TRUE)
                .authorizationState("LOCKED")
                .lockedUntil(LocalDateTime.now().plusMinutes(10))
                .build());

        assertFalse(service.isElectronicSignatureEnabled(99L));
    }

    @Test
    void isElectronicSignatureEnabled_allowsExpiredLockWhenAuthorizationIsOtherwiseEnabled() {
        when(authorizationMapper.selectByUserId(99L)).thenReturn(DccElectronicSignatureAuthorizationDO.builder()
                .userId(99L)
                .electronicSignatureEnabled(Boolean.TRUE)
                .authorizationState("LOCKED")
                .lockedUntil(LocalDateTime.now().minusMinutes(1))
                .build());

        assertTrue(service.isElectronicSignatureEnabled(99L));
    }

    @Test
    void validateElectronicSignatureEnabled_futureLockFailsWithLockedError() {
        when(authorizationMapper.selectByUserId(99L)).thenReturn(DccElectronicSignatureAuthorizationDO.builder()
                .userId(99L)
                .electronicSignatureEnabled(Boolean.TRUE)
                .authorizationState("LOCKED")
                .lockedUntil(LocalDateTime.now().plusMinutes(10))
                .build());

        assertServiceException(() -> service.validateElectronicSignatureEnabled(99L),
                CONTROLLED_FILE_SIGNATURE_LOCKED);
    }

    @Test
    void validateElectronicSignatureEnabled_expiredLockAllowsSigning() {
        when(authorizationMapper.selectByUserId(99L)).thenReturn(DccElectronicSignatureAuthorizationDO.builder()
                .userId(99L)
                .electronicSignatureEnabled(Boolean.TRUE)
                .authorizationState("LOCKED")
                .lockedUntil(LocalDateTime.now().minusMinutes(1))
                .build());

        assertDoesNotThrow(() -> service.validateElectronicSignatureEnabled(99L));
    }

    @Test
    void validateElectronicSignatureEnabled_missingAuthorizationFailsNotAuthorized() {
        when(authorizationMapper.selectByUserId(99L)).thenReturn(null);

        assertServiceException(() -> service.validateElectronicSignatureEnabled(99L),
                CONTROLLED_FILE_SIGNATURE_NOT_AUTHORIZED);
    }

    @Test
    void validateElectronicSignatureEnabled_disabledStateFailsWithDisabledError() {
        when(authorizationMapper.selectByUserId(99L)).thenReturn(DccElectronicSignatureAuthorizationDO.builder()
                .userId(99L)
                .electronicSignatureEnabled(Boolean.TRUE)
                .authorizationState("DISABLED")
                .build());

        assertServiceException(() -> service.validateElectronicSignatureEnabled(99L),
                CONTROLLED_FILE_SIGNATURE_DISABLED);
    }

    @Test
    void validateElectronicSignatureEnabled_disabledFlagFailsWithDisabledError() {
        when(authorizationMapper.selectByUserId(99L)).thenReturn(DccElectronicSignatureAuthorizationDO.builder()
                .userId(99L)
                .electronicSignatureEnabled(Boolean.FALSE)
                .authorizationState("ENABLED")
                .build());

        assertServiceException(() -> service.validateElectronicSignatureEnabled(99L),
                CONTROLLED_FILE_SIGNATURE_DISABLED);
    }

    @Test
    void getAuthorizationMap_marksMissingUsersUnauthorized() {
        when(authorizationMapper.selectListByUserIds(Set.of(99L, 100L))).thenReturn(java.util.List.of(
                DccElectronicSignatureAuthorizationDO.builder()
                        .userId(99L)
                        .electronicSignatureEnabled(Boolean.TRUE)
                        .authorizationState("ENABLED")
                        .build()));

        Map<Long, Boolean> result = service.getAuthorizationMap(Set.of(99L, 100L));

        assertTrue(result.get(99L));
        assertFalse(result.get(100L));
    }

    @Test
    void updateAuthorization_requiresReasonForAuditableChange() {
        assertServiceException(() -> service.updateAuthorization(99L, true, 1L, "  "),
                CONTROLLED_FILE_SIGNATURE_AUTH_REASON_REQUIRED);
    }

    @Test
    void updateAuthorization_insertsRecordAndAuthorizationAudit() {
        when(authorizationMapper.selectByUserId(99L)).thenReturn(null);
        when(authorizationMapper.insert(any(DccElectronicSignatureAuthorizationDO.class))).thenReturn(1);

        service.updateAuthorization(99L, false, 1L, "岗位授权复核未通过");

        ArgumentCaptor<DccElectronicSignatureAuthorizationDO> captor =
                ArgumentCaptor.forClass(DccElectronicSignatureAuthorizationDO.class);
        verify(authorizationMapper).insert(captor.capture());
        assertTrue(captor.getValue().getUserId().equals(99L));
        assertFalse(Boolean.TRUE.equals(captor.getValue().getElectronicSignatureEnabled()));
        assertTrue("DISABLED".equals(captor.getValue().getAuthorizationState()));
        verify(authorizationAuditService).recordAuthorizationChange(any());
    }

    private static void assertAlwaysUpdateStrategy(String fieldName) throws NoSuchFieldException {
        Field field = DccElectronicSignatureAuthorizationDO.class.getDeclaredField(fieldName);
        TableField tableField = field.getAnnotation(TableField.class);
        assertNotNull(tableField);
        assertEquals(FieldStrategy.ALWAYS, tableField.updateStrategy());
    }
}
