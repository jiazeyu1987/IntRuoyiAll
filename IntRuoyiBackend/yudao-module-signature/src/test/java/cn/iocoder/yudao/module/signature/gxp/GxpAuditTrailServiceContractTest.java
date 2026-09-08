package cn.iocoder.yudao.module.signature.gxp;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.signature.gxp.api.GxpAuditAction;
import cn.iocoder.yudao.module.signature.gxp.api.GxpAuditActor;
import cn.iocoder.yudao.module.signature.gxp.api.GxpAuditStateEnvelope;
import cn.iocoder.yudao.module.signature.gxp.api.GxpAuditSubject;
import cn.iocoder.yudao.module.signature.gxp.api.GxpAuditTrailAppendCommand;
import cn.iocoder.yudao.module.signature.gxp.dal.dataobject.GxpAuditEventDO;
import cn.iocoder.yudao.module.signature.gxp.dal.mysql.GxpAuditEventMapper;
import cn.iocoder.yudao.module.signature.gxp.service.GxpAuditTrailServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Constructor;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GxpAuditTrailServiceContractTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-09-08T01:23:45Z"),
            ZoneId.of("Asia/Shanghai"));

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void appendRequiresCompleteServerSideEvidenceAndStoresCanonicalHash() {
        TenantContextHolder.setTenantId(1L);
        GxpAuditEventMapper mapper = mock(GxpAuditEventMapper.class);
        when(mapper.insert(any(GxpAuditEventDO.class))).thenAnswer(invocation -> {
            GxpAuditEventDO event = invocation.getArgument(0);
            event.setId(1001L);
            return 1;
        });
        when(mapper.selectMaxLedgerSequence(1L)).thenReturn(41L);

        GxpAuditTrailServiceImpl service = new GxpAuditTrailServiceImpl(mapper, FIXED_CLOCK);
        Long eventId = service.append(validCommand());

        assertEquals(1001L, eventId);
        ArgumentCaptor<GxpAuditEventDO> captor = ArgumentCaptor.forClass(GxpAuditEventDO.class);
        verify(mapper).insert(captor.capture());
        GxpAuditEventDO event = captor.getValue();
        assertEquals(1L, event.getTenantId());
        assertEquals(42L, event.getLedgerSequence());
        assertEquals("MES.BATCH_RECORD.UPDATE", event.getOperationId());
        assertEquals("MES", event.getDomain());
        assertEquals("BATCH_RECORD", event.getSubjectType());
        assertEquals("BR-001", event.getSubjectId());
        assertEquals("UPDATE", event.getAction());
        assertEquals(101L, event.getActorId());
        assertEquals("PRESENT", event.getBeforeState());
        assertEquals("PRESENT", event.getAfterState());
        assertEquals("policy-20260908", event.getPolicyVersion());
        assertEquals("idem-20260908-001", event.getIdempotencyKey());
        assertNotNull(event.getCanonicalEventJson());
        assertEquals(64, event.getEventHash().length());
        assertEquals("SHA-256", event.getAlgorithm());
    }

    @Test
    void appendRejectsMissingReasonAndDoesNotReturnDefaultSuccess() {
        TenantContextHolder.setTenantId(1L);
        GxpAuditTrailServiceImpl service = new GxpAuditTrailServiceImpl(mock(GxpAuditEventMapper.class), FIXED_CLOCK);

        GxpAuditTrailAppendCommand command = validCommand().withReason("");

        assertThrows(ServiceException.class, () -> service.append(command));
    }

    @Test
    void appendPropagatesMapperFailureForTransactionalRollback() {
        TenantContextHolder.setTenantId(1L);
        GxpAuditEventMapper mapper = mock(GxpAuditEventMapper.class);
        when(mapper.insert(any(GxpAuditEventDO.class))).thenThrow(new IllegalStateException("audit insert failed"));

        GxpAuditTrailServiceImpl service = new GxpAuditTrailServiceImpl(mapper, FIXED_CLOCK);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> service.append(validCommand()));
        assertEquals("audit insert failed", exception.getMessage());
    }

    @Test
    void appendRequiresStateEnvelopeInsteadOfNullUnavailablePayload() {
        TenantContextHolder.setTenantId(1L);
        GxpAuditTrailServiceImpl service = new GxpAuditTrailServiceImpl(mock(GxpAuditEventMapper.class), FIXED_CLOCK);

        GxpAuditTrailAppendCommand command = validCommand().withAfterState(new GxpAuditStateEnvelope("UNAVAILABLE", null,
                null));

        assertThrows(ServiceException.class, () -> service.append(command));
    }

    @Test
    void appendReturnsExistingEventForSameIdempotencyAndCanonicalPayload() {
        TenantContextHolder.setTenantId(1L);
        GxpAuditEventMapper mapper = mock(GxpAuditEventMapper.class);
        GxpAuditTrailServiceImpl service = new GxpAuditTrailServiceImpl(mapper, FIXED_CLOCK);
        GxpAuditTrailAppendCommand command = validCommand();
        when(mapper.insert(any(GxpAuditEventDO.class))).thenAnswer(invocation -> {
            GxpAuditEventDO event = invocation.getArgument(0);
            event.setId(2002L);
            return 1;
        });
        when(mapper.selectMaxLedgerSequence(1L)).thenReturn(0L);

        Long firstEventId = service.append(command);
        assertEquals(2002L, firstEventId);

        ArgumentCaptor<GxpAuditEventDO> captor = ArgumentCaptor.forClass(GxpAuditEventDO.class);
        verify(mapper).insert(captor.capture());
        GxpAuditEventDO existing = captor.getValue();
        reset(mapper);
        when(mapper.selectByTenantIdAndIdempotencyKey(1L, command.idempotencyKey())).thenReturn(existing);

        Long eventId = service.append(command);

        assertEquals(2002L, eventId);
        verify(mapper, never()).insert(any(GxpAuditEventDO.class));
    }

    @Test
    void appendRejectsSameIdempotencyWithDifferentCanonicalPayload() {
        TenantContextHolder.setTenantId(1L);
        GxpAuditEventMapper mapper = mock(GxpAuditEventMapper.class);
        GxpAuditTrailServiceImpl service = new GxpAuditTrailServiceImpl(mapper, FIXED_CLOCK);
        GxpAuditTrailAppendCommand command = validCommand();
        GxpAuditEventDO existing = GxpAuditEventDO.builder()
                .id(2003L)
                .tenantId(1L)
                .idempotencyKey(command.idempotencyKey())
                .canonicalEventJson("{\"different\":true}")
                .build();
        when(mapper.selectByTenantIdAndIdempotencyKey(1L, command.idempotencyKey())).thenReturn(existing);

        assertThrows(ServiceException.class, () -> service.append(command));
        verify(mapper, never()).insert(any(GxpAuditEventDO.class));
    }

    @Test
    void springRuntimeUsesExplicitMapperConstructor() throws NoSuchMethodException {
        Constructor<GxpAuditTrailServiceImpl> runtimeConstructor =
                GxpAuditTrailServiceImpl.class.getConstructor(GxpAuditEventMapper.class);
        Constructor<GxpAuditTrailServiceImpl> testConstructor =
                GxpAuditTrailServiceImpl.class.getConstructor(GxpAuditEventMapper.class, Clock.class);

        assertTrue(runtimeConstructor.isAnnotationPresent(Autowired.class));
        assertFalse(testConstructor.isAnnotationPresent(Autowired.class));
    }

    private GxpAuditTrailAppendCommand validCommand() {
        return new GxpAuditTrailAppendCommand(
                "MES.BATCH_RECORD.UPDATE",
                new GxpAuditSubject("MES", "BATCH_RECORD", "BR-001", "V1"),
                GxpAuditAction.UPDATE,
                "修正批记录关键字段",
                new GxpAuditActor(101L, "zhangsan", "张三"),
                new GxpAuditStateEnvelope("PRESENT", "V1", "{\"temperature\":\"36.4\"}"),
                new GxpAuditStateEnvelope("PRESENT", "V2", "{\"temperature\":\"36.5\"}"),
                "policy-20260908",
                "idem-20260908-001",
                "REQ-001",
                "SIG-001",
                "content-hash-001"
        );
    }

}
