package cn.iocoder.yudao.module.dcc.service.preview;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileAccessEventDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileWatermarkTraceDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileAccessEventMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileWatermarkTraceMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileAccessLogMapper;
import cn.iocoder.yudao.module.dcc.service.audit.DccAccessEventCreateCommand;
import cn.iocoder.yudao.module.dcc.service.audit.DccControlledFileAccessAuditService;
import cn.iocoder.yudao.module.dcc.service.token.DccViewerTokenService;
import cn.iocoder.yudao.module.dcc.service.token.DccViewerTokenExpectedContext;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.*;
import java.util.List;
import java.util.function.Consumer;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Current services + real H2 mappers/transactions/HMAC; mapper doubles exercise write/readback failures only. */
@Import({DccControlledPreviewAccessService.class, DccControlledFileAccessAuditService.class, DccViewerTokenService.class})
class DccG09PreviewRegisteredAtTest extends BaseDbUnitTest {
    @Resource DccControlledPreviewAccessService producer;
    @Resource DccControlledFileAccessAuditService audit;
    @Resource DccControlledFileAccessEventMapper events;
    @Resource DccControlledFileWatermarkTraceMapper traces;
    @Resource DccControlledFileAccessLogMapper logs;
    @Resource DccViewerTokenService tokens;
    private static final LocalDateTime SECOND = LocalDateTime.of(2026, 10, 15, 0, 30, 10);

    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);
        Clock clock = Clock.fixed(Instant.parse("2026-10-15T00:30:10.987654321Z"), ZoneOffset.UTC);
        ReflectionTestUtils.setField(producer, "clock", clock);
        ReflectionTestUtils.setField(tokens, "clock", clock);
        ReflectionTestUtils.setField(tokens, "hmacSecret", "g09-registered-at-real-hmac-over-32-bytes");
    }
    @AfterEach void clear() { TenantContextHolder.clear(); }
    private DccPreviewAccessRequest request(String process) {
        return new DccPreviewAccessRequest(1L, 99L, 20L, "A/1", "DOC-20", "PREVIEW", "CONTROLLED_PREVIEW",
                300L, "user99", "Reader", null, null, "Test", "TRACE_CODE_ONLY", "127.0.0.1", "JUnit", "g09", process);
    }
    private DccAccessEventCreateCommand command(String process) {
        return new DccAccessEventCreateCommand("AE-G09", 20L, "A/1", 99L, "PREVIEW", "CONTROLLED_PREVIEW", "SUCCESS",
                null, null, "127.0.0.1", "JUnit", "g09", SECOND, process);
    }
    private DccControlledFileAccessEventDO row(String process) {
        return DccControlledFileAccessEventDO.builder().id(123L).tenantId(1L).accessEventCode("AE-G09")
                .controlledFileId(20L).fileVersionNo("A/1").userId(99L).accessType("PREVIEW").purpose("CONTROLLED_PREVIEW")
                .result("SUCCESS").sourceIp("127.0.0.1").userAgent("JUnit").requestId("g09").occurredAt(SECOND)
                .processInstanceId(process).build();
    }

    @Test void ordinaryAndRoundPreviewReturnTheSameVerifiedPersistedUtcSecondAndOnlyOneOriginalEvent() {
        for (String process : new String[]{null, "formal-round"}) {
            var result = producer.prepareAccess(request(process));
            var saved = events.selectById(result.accessEventId());
            assertEquals(SECOND, result.issuedAt());
            assertEquals(saved.getOccurredAt(), result.issuedAt());
            assertEquals(process, saved.getProcessInstanceId());
            assertEquals(1L, saved.getTenantId());
            assertEquals(99L, saved.getUserId());
            assertEquals(20L, saved.getControlledFileId());
            assertEquals("A/1", saved.getFileVersionNo());
            assertEquals(result.accessEventCode(), saved.getAccessEventCode());
            assertEquals("PREVIEW", saved.getAccessType());
            assertEquals("CONTROLLED_PREVIEW", saved.getPurpose());
            assertEquals("SUCCESS", saved.getResult());
            assertEquals(saved.getId(), tokens.verify(result.viewerToken(), new DccViewerTokenExpectedContext(
                    1L, 99L, 20L, "A/1", saved.getId(), "CONTROLLED_PREVIEW", 300L,
                    result.viewerTokenNonce(), result.viewerTokenId())).getAccessEventId());
        }
        assertEquals(2L, events.selectCount(null));
        assertEquals(2L, traces.selectCount(null));
        assertEquals(2L, logs.selectCount(null));
    }

    @Test void ordinaryPreviewRejectsZeroInsertMissingIdOrMissingReadback() {
        var broken = mock(DccControlledFileAccessEventMapper.class);
        ReflectionTestUtils.setField(audit, "accessEventMapper", broken);
        try {
            assertThrows(IllegalStateException.class, () -> audit.createAccessEvent(command(null)));
            when(broken.insert(any(DccControlledFileAccessEventDO.class))).thenReturn(1);
            assertThrows(IllegalStateException.class, () -> audit.createAccessEvent(command(null)));
            when(broken.insert(any(DccControlledFileAccessEventDO.class))).thenAnswer(call -> {
                ((DccControlledFileAccessEventDO) call.getArgument(0)).setId(123L); return 1;
            });
            assertThrows(IllegalStateException.class, () -> audit.createAccessEvent(command(null)));
        } finally { ReflectionTestUtils.setField(audit, "accessEventMapper", events); }
    }

    @Test void ordinaryAndRoundPreviewRejectEveryReadbackIdentityAndTimeMismatch() {
        List<Consumer<DccControlledFileAccessEventDO>> corruptions = List.of(
                r -> r.setId(124L), r -> r.setTenantId(2L), r -> r.setUserId(100L), r -> r.setControlledFileId(21L),
                r -> r.setFileVersionNo("A/2"), r -> r.setAccessEventCode("AE-OTHER"), r -> r.setPurpose("OTHER"),
                r -> r.setAccessType("DOWNLOAD"), r -> r.setResult("DENIED"), r -> r.setOccurredAt(SECOND.plusSeconds(1)),
                r -> r.setOccurredAt(null), r -> r.setProcessInstanceId("other-round"));
        var broken = mock(DccControlledFileAccessEventMapper.class);
        when(broken.insert(any(DccControlledFileAccessEventDO.class))).thenAnswer(call -> {
            ((DccControlledFileAccessEventDO) call.getArgument(0)).setId(123L); return 1;
        });
        ReflectionTestUtils.setField(audit, "accessEventMapper", broken);
        try {
            for (String process : new String[]{null, "formal-round"}) {
                for (var corrupt : corruptions) {
                    var saved = row(process); corrupt.accept(saved);
                    when(broken.selectById(123L)).thenReturn(saved);
                    assertThrows(IllegalStateException.class, () -> audit.createAccessEvent(command(process)));
                }
            }
        } finally { ReflectionTestUtils.setField(audit, "accessEventMapper", events); }
    }

    @Test void producerRejectsAnUntrustedAuditResultBeforeTraceLogOrToken() {
        var broken = mock(DccControlledFileAccessAuditService.class);
        when(broken.createAccessEvent(any())).thenAnswer(call -> {
            DccAccessEventCreateCommand c = call.getArgument(0);
            var saved = row(null); saved.setAccessEventCode(c.accessEventCode()); saved.setOccurredAt(SECOND.plusSeconds(1));
            return saved;
        });
        ReflectionTestUtils.setField(producer, "accessAuditService", broken);
        try {
            assertThrows(IllegalStateException.class, () -> producer.prepareAccess(request(null)));
            verify(broken, never()).recordWatermarkTrace(any());
            verify(broken, never()).recordAccessLog(any());
        } finally { ReflectionTestUtils.setField(producer, "accessAuditService", audit); }
    }

    @Test void lateOriginalTraceWriteFailureRollsBackOriginalEventWithoutNewRegistrationEvent() {
        var broken = mock(DccControlledFileWatermarkTraceMapper.class);
        when(broken.insert(any(DccControlledFileWatermarkTraceDO.class))).thenThrow(new IllegalStateException("trace insert failed"));
        ReflectionTestUtils.setField(audit, "watermarkTraceMapper", broken);
        try {
            assertThrows(RuntimeException.class, () -> producer.prepareAccess(request(null)));
            assertEquals(0L, events.selectCount(null));
            assertEquals(0L, logs.selectCount(null));
        } finally { ReflectionTestUtils.setField(audit, "watermarkTraceMapper", traces); }
    }

    @Test void previewRequestCannotIssueAnEventInAnotherTenant() {
        TenantContextHolder.setTenantId(2L);
        assertThrows(IllegalArgumentException.class, () -> producer.prepareAccess(request(null)));
        assertEquals(0L, events.selectCount(null));
    }
}
