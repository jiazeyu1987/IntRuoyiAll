package cn.iocoder.yudao.module.system.service.notify;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.system.dal.dataobject.notify.NotifyMessageDO;
import cn.iocoder.yudao.module.system.dal.dataobject.notify.NotifyTemplateDO;
import cn.iocoder.yudao.module.system.dal.mysql.notify.NotifyMessageMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import({NotifySendServiceImpl.class, NotifyMessageServiceImpl.class})
class NotifyMessageBusinessKeyIdempotencyTest extends BaseDbUnitTest {

    private static final long TENANT_A = 11L;
    private static final long TENANT_B = 12L;
    private static final int TEMPLATE_NOT_EXISTS = 1_002_026_000;
    private static final int TEMPLATE_PARAM_MISS = 1_002_028_000;
    private static final int TEMPLATE_DISABLED = 1_002_028_001;
    private static final int BUSINESS_KEY_INVALID = 1_002_028_002;
    private static final int BUSINESS_KEY_CONFLICT = 1_002_028_003;
    private static final int MESSAGE_ID_EMPTY = 1_002_028_004;

    @Resource
    private NotifySendService notifySendService;
    @Resource
    private NotifyMessageMapper notifyMessageMapper;
    @Resource
    private NotifyMessageService notifyMessageService;
    @MockitoSpyBean
    private NotifyMessageServiceImpl notifyMessageServiceSpy;
    @MockitoBean
    private NotifyTemplateService notifyTemplateService;

    private NotifyTemplateDO template;

    @BeforeEach
    void setUpNotifyTemplate() {
        TenantContextHolder.setTenantId(TENANT_A);
        template = template(71L, "REG_CERT_READY");
        when(notifyTemplateService.getNotifyTemplateByCodeFromCache("REG_CERT_READY")).thenReturn(template);
        when(notifyTemplateService.getNotifyTemplateByCodeFromCache("OTHER_TEMPLATE"))
                .thenReturn(template(72L, "OTHER_TEMPLATE"));
        when(notifyTemplateService.formatNotifyTemplateContent(anyString(), anyMap()))
                .thenReturn("registration certificate is ready");
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void firstIdempotentAdminSendPersistsTrimmedBusinessKey() {
        Long messageId = sendAdminIdempotently(31L, "REG_CERT_READY",
                Map.of("certificateId", "RC-1"), "  REG_CERT:RC-1:READY  ");

        List<NotifyMessageDO> messages = notifyMessageMapper.selectList();
        assertNotNull(messageId);
        assertEquals(1, messages.size());
        assertEquals(messageId, messages.get(0).getId());
        assertEquals("REG_CERT:RC-1:READY", ReflectionTestUtils.getField(messages.get(0), "businessKey"));
    }

    @Test
    void serialReplayWithEquivalentParameterOrderReturnsOriginalMessage() {
        Map<String, Object> firstParams = new LinkedHashMap<>();
        firstParams.put("certificateId", "RC-1");
        firstParams.put("stage", "READY");
        Map<String, Object> replayParams = new LinkedHashMap<>();
        replayParams.put("stage", "READY");
        replayParams.put("certificateId", "RC-1");

        Long firstId = sendAdminIdempotently(31L, "REG_CERT_READY", firstParams, "REG_CERT:RC-1:READY");
        Long replayId = sendAdminIdempotently(31L, "REG_CERT_READY", replayParams, " REG_CERT:RC-1:READY ");

        assertEquals(firstId, replayId);
        assertEquals(1, notifyMessageMapper.selectList().size());
    }

    @Test
    void sameBusinessKeyRejectsRecipientTemplateOrParameterConflict() {
        sendAdminIdempotently(31L, "REG_CERT_READY",
                Map.of("certificateId", "RC-1"), "REG_CERT:RC-1:READY");

        assertServiceCode(() -> sendAdminIdempotently(32L, "REG_CERT_READY",
                Map.of("certificateId", "RC-1"), "REG_CERT:RC-1:READY"), BUSINESS_KEY_CONFLICT);
        assertServiceCode(() -> sendAdminIdempotently(31L, "OTHER_TEMPLATE",
                Map.of("certificateId", "RC-1"), "REG_CERT:RC-1:READY"), BUSINESS_KEY_CONFLICT);
        assertServiceCode(() -> sendAdminIdempotently(31L, "REG_CERT_READY",
                Map.of("certificateId", "RC-2"), "REG_CERT:RC-1:READY"), BUSINESS_KEY_CONFLICT);

        assertEquals(1, notifyMessageMapper.selectList().size());
    }

    @Test
    void sameBusinessKeyRejectsUserTypeConflict() {
        Map<String, Object> templateParams = Map.of("certificateId", "RC-1");
        Long existingId = notifyMessageService.createNotifyMessage(31L, UserTypeEnum.MEMBER.getValue(), template,
                "registration certificate is ready", templateParams, "REG_CERT:RC-1:READY");

        assertServiceCode(() -> sendAdminIdempotently(31L, "REG_CERT_READY", templateParams,
                "REG_CERT:RC-1:READY"), BUSINESS_KEY_CONFLICT);

        NotifyMessageDO existing = notifyMessageMapper.selectById(existingId);
        assertEquals(UserTypeEnum.MEMBER.getValue(), existing.getUserType());
        assertEquals(1, notifyMessageMapper.selectList().size());
    }

    @Test
    void sameBusinessKeyIsIsolatedByTenant() {
        Long tenantAMessageId = sendAdminIdempotently(31L, "REG_CERT_READY",
                Map.of("certificateId", "RC-1"), "REG_CERT:RC-1:READY");

        TenantContextHolder.setTenantId(TENANT_B);
        Long tenantBMessageId = sendAdminIdempotently(31L, "REG_CERT_READY",
                Map.of("certificateId", "RC-1"), "REG_CERT:RC-1:READY");
        assertEquals(tenantBMessageId, notifyMessageMapper
                .selectByBusinessKey(TENANT_B, "REG_CERT:RC-1:READY").getId());

        TenantContextHolder.setTenantId(TENANT_A);
        assertNotEquals(tenantAMessageId, tenantBMessageId);
        assertEquals(tenantAMessageId, notifyMessageMapper
                .selectByBusinessKey(TENANT_A, "REG_CERT:RC-1:READY").getId());
        assertEquals(2, notifyMessageMapper.selectList().size());
        assertEquals(tenantAMessageId, sendAdminIdempotently(31L, "REG_CERT_READY",
                Map.of("certificateId", "RC-1"), "REG_CERT:RC-1:READY"));
    }

    @Test
    void concurrentReplayReturnsOneMessageIdAndOneRow() throws Exception {
        int workers = 4;
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch insertReady = new CountDownLatch(workers);
        CountDownLatch releaseInsert = new CountDownLatch(1);
        doAnswer(invocation -> {
            insertReady.countDown();
            if (!releaseInsert.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("concurrent notify inserts did not become ready");
            }
            return invocation.callRealMethod();
        }).when(notifyMessageServiceSpy).createNotifyMessage(
                anyLong(), anyInt(), any(), anyString(), anyMap(), anyString());
        List<Future<Long>> futures = new ArrayList<>();
        try {
            for (int index = 0; index < workers; index++) {
                futures.add(executor.submit(() -> {
                    TenantContextHolder.setTenantId(TENANT_A);
                    try {
                        ready.countDown();
                        if (!start.await(10, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("concurrent notify send start timed out");
                        }
                        return sendAdminIdempotently(31L, "REG_CERT_READY",
                                Map.of("certificateId", "RC-1"), "REG_CERT:RC-1:READY");
                    } finally {
                        TenantContextHolder.clear();
                    }
                }));
            }
            if (!ready.await(10, TimeUnit.SECONDS)) {
                fail("concurrent notify send workers did not become ready");
            }
            start.countDown();
            if (!insertReady.await(10, TimeUnit.SECONDS)) {
                fail("concurrent notify sends did not reach the insert barrier");
            }
            releaseInsert.countDown();

            List<Long> messageIds = new ArrayList<>();
            for (Future<Long> future : futures) {
                messageIds.add(future.get(20, TimeUnit.SECONDS));
            }
            TenantContextHolder.setTenantId(TENANT_A);
            assertEquals(1, Set.copyOf(messageIds).size());
            assertEquals(1, notifyMessageMapper.selectList().size());
            verify(notifyMessageServiceSpy, atLeastOnce())
                    .getNotifyMessageByBusinessKeyForUpdate("REG_CERT:RC-1:READY");
        } finally {
            releaseInsert.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void blankOrOversizedBusinessKeyFailsBeforeMessageInsert() {
        assertServiceCode(() -> sendAdminIdempotently(31L, "REG_CERT_READY",
                Map.of("certificateId", "RC-1"), "   "), BUSINESS_KEY_INVALID);
        assertServiceCode(() -> sendAdminIdempotently(31L, "REG_CERT_READY",
                Map.of("certificateId", "RC-1"), "K".repeat(256)), BUSINESS_KEY_INVALID);
        assertEquals(0, notifyMessageMapper.selectList().size());
    }

    @Test
    void replayFailsWhenTemplateNoLongerExists() {
        sendAdminIdempotently(31L, "REG_CERT_READY",
                Map.of("certificateId", "RC-1"), "REG_CERT:RC-1:READY");
        when(notifyTemplateService.getNotifyTemplateByCodeFromCache("REG_CERT_READY")).thenReturn(null);

        assertServiceCode(() -> sendAdminIdempotently(31L, "REG_CERT_READY",
                Map.of("certificateId", "RC-1"), "REG_CERT:RC-1:READY"), TEMPLATE_NOT_EXISTS);

        assertEquals(1, notifyMessageMapper.selectList().size());
    }

    @Test
    void replayFailsWhenTemplateIsNowDisabled() {
        sendAdminIdempotently(31L, "REG_CERT_READY",
                Map.of("certificateId", "RC-1"), "REG_CERT:RC-1:READY");
        template.setStatus(CommonStatusEnum.DISABLE.getStatus());

        assertServiceCode(() -> sendAdminIdempotently(31L, "REG_CERT_READY",
                Map.of("certificateId", "RC-1"), "REG_CERT:RC-1:READY"), TEMPLATE_DISABLED);

        assertEquals(1, notifyMessageMapper.selectList().size());
    }

    @Test
    void replayFailsWhenTemplateNowRequiresAnAdditionalParam() {
        sendAdminIdempotently(31L, "REG_CERT_READY",
                Map.of("certificateId", "RC-1"), "REG_CERT:RC-1:READY");
        template.setParams(List.of("certificateId", "approvalNo"));

        assertServiceCode(() -> sendAdminIdempotently(31L, "REG_CERT_READY",
                Map.of("certificateId", "RC-1"), "REG_CERT:RC-1:READY"), TEMPLATE_PARAM_MISS);

        assertEquals(1, notifyMessageMapper.selectList().size());
    }

    @Test
    void changedBindingReportsConflictBeforeTemplateValidation() {
        sendAdminIdempotently(31L, "REG_CERT_READY",
                Map.of("certificateId", "RC-1"), "REG_CERT:RC-1:READY");
        when(notifyTemplateService.getNotifyTemplateByCodeFromCache("MISSING_TEMPLATE")).thenReturn(null);

        assertServiceCode(() -> sendAdminIdempotently(31L, "MISSING_TEMPLATE",
                Map.of("certificateId", "RC-1"), "REG_CERT:RC-1:READY"), BUSINESS_KEY_CONFLICT);
        assertServiceCode(() -> sendAdminIdempotently(31L, "REG_CERT_READY",
                Map.of(), "REG_CERT:RC-1:READY"), BUSINESS_KEY_CONFLICT);

        assertEquals(1, notifyMessageMapper.selectList().size());
    }

    @Test
    void legacyAdminSendRollsBackWhenInsertedMessageHasNoReturnedId() {
        doAnswer(invocation -> {
            invocation.callRealMethod();
            return null;
        }).when(notifyMessageServiceSpy).createNotifyMessage(
                anyLong(), anyInt(), any(), anyString(), anyMap());

        assertServiceCode(() -> notifySendService.sendSingleNotifyToAdmin(
                31L, "REG_CERT_READY", Map.of("certificateId", "RC-1")), MESSAGE_ID_EMPTY);

        assertEquals(0, notifyMessageMapper.selectList().size());
    }

    private NotifyTemplateDO template(Long id, String code) {
        return new NotifyTemplateDO()
                .setId(id)
                .setCode(code)
                .setType(1)
                .setNickname("system")
                .setContent("certificate {certificateId}")
                .setParams(List.of("certificateId"))
                .setStatus(CommonStatusEnum.ENABLE.getStatus());
    }

    private Long sendAdminIdempotently(Long userId, String templateCode,
                                       Map<String, Object> templateParams, String businessKey) {
        Method method;
        try {
            method = NotifySendService.class.getMethod("sendSingleNotifyToAdminIdempotently",
                    Long.class, String.class, Map.class, String.class);
        } catch (NoSuchMethodException exception) {
            return fail("idempotent Admin notify service method must exist", exception);
        }
        try {
            return (Long) method.invoke(notifySendService, userId, templateCode, templateParams, businessKey);
        } catch (IllegalAccessException exception) {
            return fail("idempotent Admin notify service method must be accessible", exception);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("idempotent Admin notify service invocation failed", cause);
        }
    }

    private void assertServiceCode(Runnable action, int expectedCode) {
        ServiceException exception = assertThrows(ServiceException.class, action::run);
        assertEquals(expectedCode, exception.getCode());
    }
}
