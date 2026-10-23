package cn.iocoder.yudao.module.dcc.service.filepreview;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.module.dcc.controller.admin.filepreview.DccPreviewReadingSessionController;
import cn.iocoder.yudao.module.dcc.controller.admin.filepreview.vo.*;
import cn.iocoder.yudao.module.dcc.service.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.*;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileAccessEventDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFilePreviewViewSessionDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileAccessEventMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFilePreviewViewSessionMapper;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.authorization.method.PreAuthorizeAuthorizationManager;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DccPreviewReadingSessionControllerTest extends BaseMockitoUnitTest {
    @Mock DccOnlineFilePreviewReadingSessionService service;
    @InjectMocks DccPreviewReadingSessionController controller;
    private static final String BODY = "{\"clientSessionId\":\"client-1\",\"accessEventCode\":\"event\",\"watermarkTraceCode\":\"trace\",\"viewerTokenId\":\"id\",\"viewerTokenNonce\":\"nonce\"}";
    private GenericApplicationContext methodSecurityContext;

    @AfterEach void clearMethodSecurityContext() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
        if (methodSecurityContext != null) methodSecurityContext.close();
    }

    /** Real expression bean, so restoring the old query annotation yields denial, not missing-bean setup errors. */
    public static class PermissionProbe {
        public boolean hasPermission(String permission) {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            return authentication != null && authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals(permission));
        }
    }
    private DccPreviewReadingSessionController secured(DccPreviewReadingSessionController target) {
        methodSecurityContext = new GenericApplicationContext();
        methodSecurityContext.registerBean("ss", PermissionProbe.class);
        methodSecurityContext.refresh();
        var handler = new DefaultMethodSecurityExpressionHandler();
        handler.setApplicationContext(methodSecurityContext);
        var authorization = new PreAuthorizeAuthorizationManager();
        authorization.setExpressionHandler(handler);
        var factory = new ProxyFactory(target);
        factory.setProxyTargetClass(true);
        factory.addAdvisor(AuthorizationManagerBeforeMethodInterceptor.preAuthorize(authorization));
        return (DccPreviewReadingSessionController) factory.getProxy();
    }
    private void authenticatedApprovalActor(String permission) {
        var actor = new LoginUser();
        actor.setId(99L); actor.setTenantId(31L); actor.setUserType(2);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                actor, "test-only", List.of(new SimpleGrantedAuthority(permission))));
        TenantContextHolder.setTenantId(31L);
    }
    private DccControlledFileBodyReadContext taskContext() {
        return new DccControlledFileBodyReadContext(DccControlledFileBodyReadContext.Intent.APPROVAL_TASK,
                "910", "A/1", "UPLOAD", "round-1", "task-1");
    }
    private MockHttpServletRequest auditRequest() {
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1"); request.addHeader("User-Agent", "reading-method-security");
        request.addHeader("X-DCC-Request-Id", "reading-security-request");
        return request;
    }
    @ParameterizedTest
    @CsvSource({"dcc:controlled-file:review,APPROVAL_TASK", "dcc:controlled-file:approve,APPROVAL_TASK",
            "dcc:controlled-file:review,APPROVAL_HISTORY", "dcc:controlled-file:approve,APPROVAL_HISTORY"})
    void actualMethodSecurityAllowsReviewOrApproveOnlyActorIntoAllFourControlledGuards(String permission, String intent) {
        authenticatedApprovalActor(permission);
        var context = new DccControlledFileBodyReadContext(DccControlledFileBodyReadContext.Intent.valueOf(intent),
                "910", "A/1", "UPLOAD", "round-1", "task-1");
        var response = new DccPreviewReadingSessionRespVO(991L,"2026-10-15T08:00:00Z","2026-10-15T08:00:00Z","2026-10-15T08:00:00Z",null,0,10,15);
        when(service.startControlled(eq(99L),eq(910L),eq(context),any(),any())).thenReturn(response);
        when(service.heartbeatControlled(eq(99L),eq(910L),eq(context),any(),any())).thenReturn(response);
        when(service.stopControlled(eq(99L),eq(910L),eq(context),any(),any())).thenReturn(response);
        var summary = new DccPreviewReadingSummaryRespVO(null,null,0,0);
        when(service.summaryControlled(eq(99L),eq(910L),eq(context),any(),any())).thenReturn(summary);
        var proxy = secured(controller);
        assertSame(response, proxy.startControlled(910L, context.wire(), "header-token",
                new DccPreviewReadingSessionStartReqVO("client",null,"event","trace","id","nonce"), auditRequest()).getData());
        assertSame(response, proxy.heartbeatControlled(910L, context.wire(), "header-token",
                new DccPreviewReadingSessionHeartbeatReqVO(991L,null,"event","trace","id","nonce"), auditRequest()).getData());
        assertSame(response, proxy.stopControlled(910L, context.wire(), "header-token",
                new DccPreviewReadingSessionStopReqVO(991L,null,"event","trace","id","nonce"), auditRequest()).getData());
        assertSame(summary, proxy.summaryControlled(910L, context.wire(), "header-token","event","trace","id","nonce",auditRequest()).getData());
        assertFalse(SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("dcc:controlled-file:query")));
        verify(service).startControlled(eq(99L),eq(910L),eq(context),argThat(req -> "header-token".equals(req.viewerToken())),any());
    }
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void actualMethodSecurityRejectsMissingOrAnonymousActorBeforeAllControlledServices(boolean anonymous) {
        if (anonymous) SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "test-key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
        var proxy = secured(controller); var context = taskContext();
        Class<? extends RuntimeException> expected = anonymous ? AccessDeniedException.class : AuthenticationCredentialsNotFoundException.class;
        assertThrows(expected, () -> proxy.startControlled(910L,context.wire(),"header-token",
                new DccPreviewReadingSessionStartReqVO("client",null,"event","trace","id","nonce"),auditRequest()));
        assertThrows(expected, () -> proxy.heartbeatControlled(910L,context.wire(),"header-token",
                new DccPreviewReadingSessionHeartbeatReqVO(991L,null,"event","trace","id","nonce"),auditRequest()));
        assertThrows(expected, () -> proxy.stopControlled(910L,context.wire(),"header-token",
                new DccPreviewReadingSessionStopReqVO(991L,null,"event","trace","id","nonce"),auditRequest()));
        assertThrows(expected, () -> proxy.summaryControlled(910L,context.wire(),"header-token","event","trace","id","nonce",auditRequest()));
        verifyNoInteractions(service);
    }
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void authenticatedControllerStillPropagatesDailyOrRevokedTaskGuardDenialWithZeroSessionWrites(boolean revokedTask) {
        authenticatedApprovalActor("dcc:controlled-file:approve");
        var context = revokedTask ? taskContext() : DccControlledFileBodyReadContext.daily(910L);
        var events = mock(DccControlledFileAccessEventMapper.class);
        var sessions = mock(DccControlledFilePreviewViewSessionMapper.class);
        var query = mock(DccControlledFileQueryService.class);
        var actualService = new DccOnlineFilePreviewReadingSessionService();
        ReflectionTestUtils.setField(actualService,"accessEventMapper",events);
        ReflectionTestUtils.setField(actualService,"sessionMapper",sessions);
        ReflectionTestUtils.setField(actualService,"controlledFileQueryService",query);
        when(sessions.lockControlledFile(910L,31L)).thenReturn(910L);
        when(events.selectOne(any())).thenReturn(DccControlledFileAccessEventDO.builder().id(8801L).tenantId(31L)
                .controlledFileId(910L).userId(99L).fileVersionNo("A/1").accessEventCode("event").accessType("PREVIEW")
                .purpose(context.purpose()).result("SUCCESS").occurredAt(LocalDateTime.of(2026,10,15,0,0))
                .processInstanceId(context.bpmRound()).build());
        var denial = cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(
                cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ACCESS_DENIED);
        doThrow(denial).when(query).assertPreviewReadingAccess(eq(99L),eq(910L),eq(context),
                eq("header-token"),eq("event"),eq("trace"),eq("id"),eq("nonce"),any());
        var actualController = new DccPreviewReadingSessionController();
        ReflectionTestUtils.setField(actualController,"service",actualService);
        var proxy = secured(actualController);
        assertSame(denial, assertThrows(ServiceException.class, () -> proxy.startControlled(910L,context.wire(),"header-token",
                new DccPreviewReadingSessionStartReqVO("client",null,"event","trace","id","nonce"),auditRequest())));
        verify(query).assertPreviewReadingAccess(eq(99L),eq(910L),eq(context),eq("header-token"),eq("event"),eq("trace"),eq("id"),eq("nonce"),any());
        verify(sessions, never()).insert(any(DccControlledFilePreviewViewSessionDO.class));
        verify(sessions).lockControlledFile(910L,31L);
    }
    @Test void onlyControlledRoutesChangeIdentityGateAndUnifiedRoutesKeepOriginalMenuPolicy() {
        int controlled = 0, unified = 0;
        for (var method : DccPreviewReadingSessionController.class.getDeclaredMethods()) {
            var annotation = method.getAnnotation(PreAuthorize.class);
            if (annotation == null) continue;
            if (method.getName().endsWith("Controlled")) {
                controlled++; assertEquals("isAuthenticated()", annotation.value());
            } else {
                unified++; assertEquals("@ss.hasAnyPermissions('dcc:controlled-file:query','mes:pro-edhr-batch-execution:query','mes:pro-edhr-batch-execution:update')", annotation.value());
            }
        }
        assertEquals(4,controlled); assertEquals(4,unified);
    }

    @Test void controlledStartForwardsAuthenticatedActorExactContextAndHeaderToken() throws Exception {
        var context = new DccControlledFileBodyReadContext(DccControlledFileBodyReadContext.Intent.APPROVAL_TASK, "910", "A/1", "UPLOAD", "round-1", "task-1");
        when(service.startControlled(eq(99L), eq(910L), eq(context), any(), any())).thenReturn(new DccPreviewReadingSessionRespVO(991L,"2026-10-15T07:50:00Z","2026-10-15T08:00:00Z","2026-10-15T08:00:00Z",null,0,10,15));
        try (var auth = mockStatic(SecurityFrameworkUtils.class)) {
            auth.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            MockMvcBuilders.standaloneSetup(controller).build().perform(post("/dcc/controlled-files/910/reading-session/start")
                    .header(DccControlledFileBodyReadContext.HEADER, context.wire()).header("X-DCC-Viewer-Token", "secret-header-token")
                    .header("User-Agent", "reading-unit").header("X-DCC-Request-Id", "reading-request")
                    .contentType("application/json").content(BODY)).andExpect(status().isOk());
        }
        verify(service).startControlled(eq(99L), eq(910L), eq(context),
                argThat(req -> req.viewerToken().equals("secret-header-token") && req.clientSessionId().equals("client-1")),
                argThat(audit -> audit.requestId().equals("reading-request")));
    }
    @Test void missingHeaderTokenCannotReachService() throws Exception {
        MockMvcBuilders.standaloneSetup(controller).build().perform(post("/dcc/file-preview/files/7001/reading-session/start")
                .header("User-Agent", "reading-unit").contentType("application/json").content(BODY)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void tokenIsAbsentFromSerializedRequestAndEveryEndpointHasPermissionAndLogProtection() {
        var req = new DccPreviewReadingSessionStartReqVO("client", "secret-value", "event", "trace", "id", "nonce");
        assertFalse(JsonUtils.toJsonString(req).contains("secret-value"));
        assertFalse(req.toString().contains("secret-value"));
        long endpoints = java.util.Arrays.stream(DccPreviewReadingSessionController.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(PreAuthorize.class))
                .peek(method -> assertFalse(method.getAnnotation(ApiAccessLog.class).requestEnable())).count();
        assertEquals(8, endpoints);
    }
}
