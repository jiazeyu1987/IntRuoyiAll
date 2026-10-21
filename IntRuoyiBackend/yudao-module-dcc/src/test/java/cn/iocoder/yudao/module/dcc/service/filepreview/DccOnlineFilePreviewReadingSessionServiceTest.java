package cn.iocoder.yudao.module.dcc.service.filepreview;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.controller.admin.filepreview.vo.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFilePreviewViewSessionDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFilePreviewViewSessionMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.*;
import cn.iocoder.yudao.module.dcc.service.file.*;
import cn.iocoder.yudao.module.dcc.service.token.*;
import cn.iocoder.yudao.module.infra.service.file.access.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DccOnlineFilePreviewReadingSessionServiceTest extends BaseMockitoUnitTest {
    private static final Long TENANT = 31L, USER = 99L, FILE = 7001L, EVENT = 88001L;
    private static final String VERSION = "INFRA_FILE:7001", EVENT_CODE = "AE-1", TRACE = "WM-1", TOKEN_ID = "VT-1", NONCE = "VN-1", TOKEN = "viewer-token";
    private static final LocalDateTime NOW = LocalDateTime.parse("2026-10-15T08:00:00");
    private static final DccRequestAuditContext AUDIT = new DccRequestAuditContext("10.8.0.31", "JUnit", "REQ-READING-1");

    @Mock private DccControlledFilePreviewViewSessionMapper sessionMapper;
    @Mock private DccControlledFileAccessEventMapper accessEventMapper;
    @Mock private DccControlledFileWatermarkTraceMapper watermarkTraceMapper;
    @Mock private DccViewerTokenService viewerTokenService;
    @Mock private BusinessFileAccessService businessFileAccessService;
    @Mock private DccControlledFileQueryService controlledFileQueryService;
    @InjectMocks private DccOnlineFilePreviewReadingSessionService service;

    @BeforeEach void context() {
        TenantContextHolder.setTenantId(TENANT);
        ReflectionTestUtils.setField(service, "clock", Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
    }
    @AfterEach void clearTenant() { TenantContextHolder.clear(); }

    @ParameterizedTest(name = "controlled={0}")
    @ValueSource(booleans = {false, true})
    void startAcquiresResourceLockBeforeAuthorizationEstablishesItsSnapshot(boolean controlled) {
        insert();
        var ctx = DccControlledFileBodyReadContext.daily(FILE);
        var calls = new ArrayList<String>();
        var authorizedEvent = controlled ? event().fileVersionNo("A/1").purpose(ctx.purpose()).build() : event().build();
        when(accessEventMapper.selectOne(any())).thenAnswer(call -> {
            calls.add("authorize");
            return authorizedEvent;
        });
        if (controlled) {
            when(sessionMapper.lockControlledFile(FILE, TENANT)).thenAnswer(call -> {
                calls.add("resourceLock");
                return FILE;
            });
            service.startControlled(USER, FILE, ctx, start(), AUDIT);
        } else {
            when(watermarkTraceMapper.selectOne(any())).thenReturn(trace().build());
            when(sessionMapper.lockInfraFile(FILE)).thenAnswer(call -> {
                calls.add("resourceLock");
                return FILE;
            });
            service.start(USER, FILE, start(), AUDIT);
        }
        assertEquals(List.of("resourceLock", "authorize"), calls);
    }

    @Test void startPersistsServerIdentityAndNoClientDuration() {
        evidence();
        when(sessionMapper.selectActiveAfterResourceLock(FILE, "ONLINE_FILE", USER, TENANT)).thenReturn(List.of());
        insert();
        var started = service.start(USER, FILE, start(), AUDIT);
        assertEquals(991L, started.sessionId());
        assertEquals("2026-10-15T08:00Z", started.startedAt());
        assertEquals(0, started.accumulatedSeconds());
        var captor = ArgumentCaptor.forClass(DccControlledFilePreviewViewSessionDO.class);
        verify(sessionMapper).insert(captor.capture());
        assertEquals(TENANT, captor.getValue().getTenantId());
        assertEquals(EVENT, captor.getValue().getAccessEventId());
        assertEquals(TOKEN_ID, captor.getValue().getViewerTokenId());
        assertEquals(AUDIT.requestId(), captor.getValue().getRequestId());
        verify(viewerTokenService, times(1)).verify(TOKEN, new DccViewerTokenExpectedContext(TENANT, USER, FILE, VERSION, EVENT, "ONLINE_FILE_PREVIEW", 900L, NONCE, TOKEN_ID));
    }
    @Test void continuousHeartbeatAddsOnlyServerElapsedSeconds() {
        evidence(); current(row().lastHeartbeatAt(NOW.minusSeconds(10)).accumulatedSeconds(4).build()); update();
        assertEquals(14, service.heartbeat(USER, FILE, heart(), AUDIT).accumulatedSeconds());
    }
    @Test void heartbeatBeyondFifteenSecondsAddsNothing() {
        evidence(); current(row().lastHeartbeatAt(NOW.minusSeconds(60)).accumulatedSeconds(4).build()); update();
        assertEquals(4, service.heartbeat(USER, FILE, heart(), AUDIT).accumulatedSeconds());
    }
    @Test void exactlyFifteenSecondsIsAccepted() {
        evidence(); current(row().lastHeartbeatAt(NOW.minusSeconds(15)).build()); update();
        assertEquals(15, service.heartbeat(USER, FILE, heart(), AUDIT).accumulatedSeconds());
    }
    @ParameterizedTest(name = "gap={0}us, increment={1}s, stop={2}")
    @CsvSource({"15000000,15,false", "15000001,0,false", "16000000,0,false",
            "15000000,15,true", "15000001,0,true", "16000000,0,true"})
    void clockMicrosecondLivenessBoundary(long gapMicroseconds, int expectedIncrement, boolean stopRequest) {
        LocalDateTime exactNow = NOW.plusNanos(Math.multiplyExact(gapMicroseconds, 1000L));
        ReflectionTestUtils.setField(service, "clock", Clock.fixed(exactNow.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
        evidence();
        var session = row().lastHeartbeatAt(NOW).accumulatedSeconds(4).build();
        current(session);
        update();
        var result = stopRequest ? service.stop(USER, FILE, stop(), AUDIT)
                : service.heartbeat(USER, FILE, heart(), AUDIT);
        assertEquals(4 + expectedIncrement, result.accumulatedSeconds());
        assertEquals(exactNow, session.getLastHeartbeatAt());
        if (stopRequest) assertEquals(exactNow, session.getEndedAt());
        verify(sessionMapper).updateById(session);
    }
    @Test void repeatedHeartbeatAtSameServerInstantDoesNotDoubleCount() {
        evidence(); current(row().lastHeartbeatAt(NOW).accumulatedSeconds(4).build()); update();
        assertEquals(4, service.heartbeat(USER, FILE, heart(), AUDIT).accumulatedSeconds());
    }
    @Test void newSessionClosesPreviousAcrossEventsWithoutUnconfirmedTail() {
        evidence(); var old = row().lastHeartbeatAt(NOW.minusSeconds(20)).accumulatedSeconds(3).build();
        when(sessionMapper.selectActiveAfterResourceLock(FILE, "ONLINE_FILE", USER, TENANT)).thenReturn(List.of(old)); update(); insert();
        service.start(USER, FILE, start(), AUDIT);
        verify(sessionMapper).updateById(argThat((DccControlledFilePreviewViewSessionDO s) -> s.getId().equals(991L)
                && s.getEndedAt().equals(NOW.minusSeconds(20)) && s.getAccumulatedSeconds() == 3 && "REPLACED".equals(s.getEndedReason())));
    }
    @Test void replayOfStoppedStartReturnsOriginalWithoutReopeningOrReplacingNewer() {
        evidence(); var old = row().endedAt(NOW.minusSeconds(2)).accumulatedSeconds(3).build();
        when(sessionMapper.selectClientAfterResourceLock(EVENT, USER, TENANT, "client-1")).thenReturn(old);
        assertEquals(3, service.start(USER, FILE, start(), AUDIT).accumulatedSeconds());
        verify(sessionMapper, never()).insert(any(DccControlledFilePreviewViewSessionDO.class));
        verify(sessionMapper, never()).selectActiveAfterResourceLock(any(), any(), any(), any());
    }
    @Test void stoppedSessionRejectsLateHeartbeatWithoutWriting() {
        evidence(); current(row().endedAt(NOW).build());
        assertThrows(ServiceException.class, () -> service.heartbeat(USER, FILE, heart(), AUDIT)); noUpdate();
    }
    @Test void stopIsIdempotentAndDoesNotAddTailOnReplay() {
        evidence(); current(row().lastHeartbeatAt(NOW.minusSeconds(5)).build()); update();
        var stopped = service.stop(USER, FILE, stop(), AUDIT);
        assertEquals(5, stopped.accumulatedSeconds()); assertNotNull(stopped.endedAt());
        assertEquals(5, service.stop(USER, FILE, stop(), AUDIT).accumulatedSeconds());
        verify(sessionMapper, times(1)).updateById(any(DccControlledFilePreviewViewSessionDO.class));
    }
    @Test void wrongEventVersionIsRejectedBeforeTokenAndSessionWrite() {
        when(sessionMapper.lockInfraFile(FILE)).thenReturn(FILE);
        when(accessEventMapper.selectOne(any())).thenReturn(event().fileVersionNo("INFRA_FILE:other").build());
        assertThrows(ServiceException.class, () -> service.start(USER, FILE, start(), AUDIT));
        verify(viewerTokenService, never()).verify(any(), any()); noInsert();
    }
    @Test void wrongEventTenantIsRejectedEvenIfMapperReturnsRow() {
        when(sessionMapper.lockInfraFile(FILE)).thenReturn(FILE);
        when(accessEventMapper.selectOne(any())).thenReturn(event().tenantId(44L).build());
        assertThrows(ServiceException.class, () -> service.start(USER, FILE, start(), AUDIT)); noInsert();
    }
    @Test void wrongEventUserIsRejected() {
        when(sessionMapper.lockInfraFile(FILE)).thenReturn(FILE);
        when(accessEventMapper.selectOne(any())).thenReturn(event().userId(101L).build());
        assertThrows(ServiceException.class, () -> service.start(USER, FILE, start(), AUDIT)); noInsert();
    }
    @Test void ignoredTenantContextIsRejectedBeforeQueries() {
        TenantContextHolder.setIgnore(true);
        assertThrows(ServiceException.class, () -> service.start(USER, FILE, start(), AUDIT));
        verifyNoInteractions(accessEventMapper, sessionMapper);
    }
    @Test void revokedBusinessPermissionPreventsAllSessionWrites() {
        evidence();
        when(businessFileAccessService.assertAllowed(any())).thenThrow(new BusinessFileAccessDeniedException("revoked", BusinessFileAccessOperation.PREVIEW, FILE, "dcc"));
        assertThrows(ServiceException.class, () -> service.start(USER, FILE, start(), AUDIT)); noInsert(); noUpdate();
    }
    @Test void expiredTokenIsRejectedAfterLockWithoutSessionWrite() {
        evidence();
        when(viewerTokenService.verify(any(), any())).thenThrow(new IllegalStateException("expired token"));
        assertThrows(IllegalStateException.class, () -> service.start(USER, FILE, start(), AUDIT));
        verify(sessionMapper).lockInfraFile(FILE); noInsert(); noUpdate();
    }
    @Test void wrongSessionEventOrTokenRejectsLateVersionRequest() {
        evidence(); current(row().accessEventId(990L).build());
        assertThrows(ServiceException.class, () -> service.heartbeat(USER, FILE, heart(), AUDIT)); noUpdate();
    }
    @Test void mismatchedWatermarkRejectsBeforeTokenVerification() {
        when(sessionMapper.lockInfraFile(FILE)).thenReturn(FILE);
        when(accessEventMapper.selectOne(any())).thenReturn(event().build());
        when(watermarkTraceMapper.selectOne(any())).thenReturn(trace().accessEventId(777L).build());
        assertThrows(ServiceException.class, () -> service.start(USER, FILE, start(), AUDIT));
        verify(viewerTokenService, never()).verify(any(), any()); noInsert();
    }
    @Test void persistenceFailureIsNotReturnedAsSuccess() {
        evidence(); when(sessionMapper.selectActiveAfterResourceLock(FILE, "ONLINE_FILE", USER, TENANT)).thenReturn(List.of());
        when(sessionMapper.insert(any(DccControlledFilePreviewViewSessionDO.class))).thenReturn(0);
        assertThrows(IllegalStateException.class, () -> service.start(USER, FILE, start(), AUDIT));
    }
    @Test void clockGoingBackwardsCannotCreateNegativeDuration() {
        evidence(); current(row().lastHeartbeatAt(NOW.plusSeconds(2)).build());
        assertThrows(IllegalStateException.class, () -> service.heartbeat(USER, FILE, heart(), AUDIT)); noUpdate();
    }
    @Test void controlledBodyRequiresExactContextAndUsesExistingAuthorizationPort() {
        var ctx = new DccControlledFileBodyReadContext(DccControlledFileBodyReadContext.Intent.APPROVAL_TASK,
                FILE.toString(), "A/1", "UPLOAD", "round-1", "task-1");
        when(accessEventMapper.selectOne(any())).thenReturn(event().fileVersionNo("A/1").purpose(ctx.purpose()).processInstanceId("round-1").build());
        when(sessionMapper.lockControlledFile(FILE, TENANT)).thenReturn(FILE);
        when(sessionMapper.selectActiveAfterResourceLock(FILE, "CONTROLLED_FILE", USER, TENANT)).thenReturn(List.of()); insert();
        service.startControlled(USER, FILE, ctx, start(), AUDIT);
        verify(controlledFileQueryService, times(1)).assertPreviewReadingAccess(USER, FILE, ctx, TOKEN, EVENT_CODE, TRACE, TOKEN_ID, NONCE, AUDIT);
        verifyNoInteractions(businessFileAccessService, viewerTokenService);
        verify(sessionMapper).insert(argThat((DccControlledFilePreviewViewSessionDO s) -> ctx.wire().equals(s.getBodyReadContext()) && "A/1".equals(s.getFileVersionNo())));
    }
    @ParameterizedTest
    @ValueSource(strings = {"valid", "wrong-user", "wrong-tenant", "wrong-task", "wrong-version", "wrong-event",
            "wrong-watermark", "missing-session", "wrong-session-user", "wrong-session-context", "expired-token", "tampered-token", "legacy-token", "heartbeat"})
    void completedTaskCanEndItsExistingSourceBoundReadingSessionWithoutRenewingBodyAccess(String scenario) {
        var ctx = new DccControlledFileBodyReadContext(DccControlledFileBodyReadContext.Intent.APPROVAL_TASK,
                FILE.toString(), "A/1", "REVISION", "round-1", "task-1");
        var tokens = new DccViewerTokenService();
        ReflectionTestUtils.setField(tokens, "hmacSecret", "task-owned-explicit-hmac-key-20261015-reading");
        ReflectionTestUtils.setField(tokens, "clock", Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
        ReflectionTestUtils.setField(service, "viewerTokenService", tokens);
        var source = new DccPreviewSourceIdentity(801L, 1L, "task-owned/body.pdf", "body.pdf", "application/pdf", 123L);
        var issued = tokens.issueSourceBound(new DccViewerTokenIssueCommand(TENANT, USER, FILE, "A/1", EVENT, ctx.purpose(), 900L), source);
        var req = new DccPreviewReadingSessionStopReqVO(991L, issued.token(), EVENT_CODE, TRACE,
                issued.payload().getTokenId(), issued.payload().getNonce());
        // Negative parameters intentionally stop at different validation boundaries.
        lenient().when(accessEventMapper.selectOne(any())).thenReturn(event().fileVersionNo("A/1").purpose(ctx.purpose()).processInstanceId("round-1").build());
        lenient().when(watermarkTraceMapper.selectOne(any())).thenReturn(trace().fileVersionNo("A/1").build());
        lenient().when(sessionMapper.lockControlledFile(FILE, TENANT)).thenReturn(FILE);
        var session = row().resourceType("CONTROLLED_FILE").fileVersionNo("A/1").bodyReadContext(ctx.wire())
                .viewerTokenId(req.viewerTokenId()).viewerTokenNonce(req.viewerTokenNonce()).lastHeartbeatAt(NOW.minusSeconds(5)).build();
        lenient().when(sessionMapper.selectOwnedByIdForUpdate(991L, TENANT)).thenReturn(session);
        lenient().when(sessionMapper.updateById(any(DccControlledFilePreviewViewSessionDO.class))).thenReturn(1);
        switch (scenario) {
            case "wrong-user" -> { var actorContext = ctx; var actorRequest = req; assertThrows(ServiceException.class, () -> service.stopControlled(USER + 1, FILE, actorContext, actorRequest, AUDIT)); noUpdate(); return; }
            case "wrong-tenant" -> TenantContextHolder.setTenantId(TENANT + 1);
            case "wrong-task" -> ctx = new DccControlledFileBodyReadContext(DccControlledFileBodyReadContext.Intent.APPROVAL_TASK, FILE.toString(), "A/1", "REVISION", "round-1", "other-task");
            case "wrong-version" -> ctx = new DccControlledFileBodyReadContext(DccControlledFileBodyReadContext.Intent.APPROVAL_TASK, FILE.toString(), "B/1", "REVISION", "round-1", "task-1");
            case "wrong-event" -> when(accessEventMapper.selectOne(any())).thenReturn(event().id(EVENT + 1).fileVersionNo("A/1").purpose(ctx.purpose()).processInstanceId("round-1").build());
            case "wrong-watermark" -> when(watermarkTraceMapper.selectOne(any())).thenReturn(trace().fileVersionNo("A/1").userId(USER + 1).build());
            case "missing-session" -> current(null);
            case "wrong-session-user" -> session.setUserId(USER + 1);
            case "wrong-session-context" -> session.setBodyReadContext("different-context");
            case "expired-token" -> ReflectionTestUtils.setField(tokens, "clock", Clock.fixed(NOW.plusSeconds(900).toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
            case "tampered-token" -> req = new DccPreviewReadingSessionStopReqVO(991L, "changed" + issued.token(), EVENT_CODE, TRACE, req.viewerTokenId(), req.viewerTokenNonce());
            case "legacy-token" -> {
                var legacy = tokens.issue(new DccViewerTokenIssueCommand(TENANT, USER, FILE, "A/1", EVENT, ctx.purpose(), 900L));
                req = new DccPreviewReadingSessionStopReqVO(991L, legacy.token(), EVENT_CODE, TRACE, legacy.payload().getTokenId(), legacy.payload().getNonce());
            }
            case "heartbeat" -> {
                doThrow(new IllegalStateException("task completed; no current body access"))
                        .when(controlledFileQueryService).assertPreviewReadingAccess(any(), any(), any(), any(), any(), any(), any(), any(), any());
                var heartbeat = new DccPreviewReadingSessionHeartbeatReqVO(991L, req.viewerToken(), EVENT_CODE, TRACE, req.viewerTokenId(), req.viewerTokenNonce());
                var exactContext = ctx;
                assertThrows(IllegalStateException.class, () -> service.heartbeatControlled(USER, FILE, exactContext, heartbeat, AUDIT)); noUpdate(); return;
            }
        }
        var exactContext = ctx;
        var exactRequest = req;
        if (!"valid".equals(scenario)) {
            Class<? extends RuntimeException> rejection = "wrong-version".equals(scenario) ? IllegalArgumentException.class : ServiceException.class;
            assertThrows(rejection, () -> service.stopControlled(USER, FILE, exactContext, exactRequest, AUDIT));
            noUpdate(); return;
        }
        var ended = assertDoesNotThrow(() -> service.stopControlled(USER, FILE, exactContext, exactRequest, AUDIT));
        assertEquals(5, ended.accumulatedSeconds()); assertNotNull(ended.endedAt());
        assertEquals("STOPPED", session.getEndedReason());
        verifyNoInteractions(controlledFileQueryService);
        verify(sessionMapper).updateById(session);
    }
    @Test void taskReassignmentDenialIsPropagatedBeforeControlledSessionWrite() {
        var ctx = DccControlledFileBodyReadContext.daily(FILE);
        when(sessionMapper.lockControlledFile(FILE, TENANT)).thenReturn(FILE);
        when(accessEventMapper.selectOne(any())).thenReturn(event().fileVersionNo("A/1").purpose(ctx.purpose()).build());
        doThrow(new IllegalStateException("task revoked")).when(controlledFileQueryService).assertPreviewReadingAccess(USER, FILE, ctx, TOKEN, EVENT_CODE, TRACE, TOKEN_ID, NONCE, AUDIT);
        assertThrows(IllegalStateException.class, () -> service.startControlled(USER, FILE, ctx, start(), AUDIT)); noInsert();
    }
    @Test void summaryUsesExactTenantUserAndServerVersionAndDoesNotCountMetadataAsOpened() {
        eventAndTrace();
        var sum = new cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPreviewReadingSummary();
        sum.setFirstOpenedAt(NOW.minusMinutes(10)); sum.setLastOpenedAt(NOW.minusSeconds(10));
        sum.setAccumulatedSeconds(12L); sum.setOpenedSessionCount(2L);
        when(sessionMapper.selectSummary(TENANT, "ONLINE_FILE", FILE, VERSION, USER)).thenReturn(sum);
        var result = service.summary(USER, FILE, start(), AUDIT);
        assertEquals("2026-10-15T07:50Z", result.firstOpenedAt());
        assertEquals("2026-10-15T07:59:50Z", result.lastOpenedAt());
        assertEquals(12, result.accumulatedSeconds()); assertEquals(2, result.openedSessionCount());
        noInsert(); noUpdate();
    }
    @Test void lifecycleVerifiesRealConfiguredHmacTokenAndRejectsChangedNonce() {
        eventAndTrace();
        var tokens = new DccViewerTokenService();
        ReflectionTestUtils.setField(tokens, "hmacSecret", "task-owned-explicit-hmac-key-20261015-reading");
        ReflectionTestUtils.setField(tokens, "clock", Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
        ReflectionTestUtils.setField(service, "viewerTokenService", tokens);
        var issued = tokens.issue(new DccViewerTokenIssueCommand(TENANT, USER, FILE, VERSION, EVENT, "ONLINE_FILE_PREVIEW", 900L));
        var req = new DccPreviewReadingSessionStartReqVO("signed-client", issued.token(), EVENT_CODE, TRACE, issued.payload().getTokenId(), issued.payload().getNonce());
        when(sessionMapper.lockInfraFile(FILE)).thenReturn(FILE);
        when(sessionMapper.selectActiveAfterResourceLock(FILE, "ONLINE_FILE", USER, TENANT)).thenReturn(List.of()); insert();
        assertEquals(991L, service.start(USER, FILE, req, AUDIT).sessionId());
        var changed = new DccPreviewReadingSessionStartReqVO("other-client", issued.token(), EVENT_CODE, TRACE, issued.payload().getTokenId(), "changed-nonce");
        assertThrows(ServiceException.class, () -> service.start(USER, FILE, changed, AUDIT));
        verify(sessionMapper, times(1)).insert(any(DccControlledFilePreviewViewSessionDO.class));
    }
    private void eventAndTrace() {
        when(accessEventMapper.selectOne(any())).thenReturn(event().build());
        when(watermarkTraceMapper.selectOne(any())).thenReturn(trace().build());
    }
    private void evidence() { eventAndTrace(); when(sessionMapper.lockInfraFile(FILE)).thenReturn(FILE); }
    private void current(DccControlledFilePreviewViewSessionDO row) { when(sessionMapper.selectOwnedByIdForUpdate(991L, TENANT)).thenReturn(row); }
    private void update() { when(sessionMapper.updateById(any(DccControlledFilePreviewViewSessionDO.class))).thenReturn(1); }
    private void insert() { when(sessionMapper.insert(any(DccControlledFilePreviewViewSessionDO.class))).thenAnswer(i -> { ((DccControlledFilePreviewViewSessionDO)i.getArgument(0)).setId(991L); return 1; }); }
    private void noInsert() { verify(sessionMapper, never()).insert(any(DccControlledFilePreviewViewSessionDO.class)); }
    private void noUpdate() { verify(sessionMapper, never()).updateById(any(DccControlledFilePreviewViewSessionDO.class)); }
    private DccPreviewReadingSessionStartReqVO start() { return new DccPreviewReadingSessionStartReqVO("client-1", TOKEN, EVENT_CODE, TRACE, TOKEN_ID, NONCE); }
    private DccPreviewReadingSessionHeartbeatReqVO heart() { return new DccPreviewReadingSessionHeartbeatReqVO(991L, TOKEN, EVENT_CODE, TRACE, TOKEN_ID, NONCE); }
    private DccPreviewReadingSessionStopReqVO stop() { return new DccPreviewReadingSessionStopReqVO(991L, TOKEN, EVENT_CODE, TRACE, TOKEN_ID, NONCE); }
    private DccControlledFileAccessEventDO.DccControlledFileAccessEventDOBuilder event() {
        return DccControlledFileAccessEventDO.builder().id(EVENT).tenantId(TENANT).controlledFileId(FILE).fileVersionNo(VERSION)
                .accessEventCode(EVENT_CODE).userId(USER).accessType("PREVIEW").purpose("ONLINE_FILE_PREVIEW").result("SUCCESS").occurredAt(NOW.minusSeconds(30));
    }
    private DccControlledFileWatermarkTraceDO.DccControlledFileWatermarkTraceDOBuilder trace() {
        return DccControlledFileWatermarkTraceDO.builder().id(9001L).traceCode(TRACE).accessEventId(EVENT).accessEventCode(EVENT_CODE).controlledFileId(FILE).fileVersionNo(VERSION).userId(USER);
    }
    private DccControlledFilePreviewViewSessionDO.DccControlledFilePreviewViewSessionDOBuilder row() {
        return DccControlledFilePreviewViewSessionDO.builder().id(991L).tenantId(TENANT).resourceType("ONLINE_FILE").controlledFileId(FILE)
                .fileVersionNo(VERSION).accessEventId(EVENT).accessEventCode(EVENT_CODE).watermarkTraceCode(TRACE).userId(USER)
                .viewerTokenId(TOKEN_ID).viewerTokenNonce(NONCE).clientSessionId("client-1").openedAt(NOW.minusSeconds(30)).startedAt(NOW.minusSeconds(10))
                .lastHeartbeatAt(NOW.minusSeconds(10)).accumulatedSeconds(0);
    }
}
