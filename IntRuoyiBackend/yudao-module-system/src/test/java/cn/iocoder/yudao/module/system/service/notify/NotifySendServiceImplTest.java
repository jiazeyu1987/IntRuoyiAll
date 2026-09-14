package cn.iocoder.yudao.module.system.service.notify;

import cn.hutool.core.map.MapUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.system.dal.dataobject.notify.NotifyMessageDO;
import cn.iocoder.yudao.module.system.dal.dataobject.notify.NotifyTemplateDO;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static cn.hutool.core.util.RandomUtil.randomEle;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.*;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.NOTIFY_TEMPLATE_NOT_EXISTS;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.NOTIFY_SEND_TEMPLATE_PARAM_MISS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class NotifySendServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private NotifySendServiceImpl notifySendService;

    @Mock
    private NotifyTemplateService notifyTemplateService;
    @Mock
    private NotifyMessageService notifyMessageService;

    @Test
    public void testSendSingleNotifyToAdmin() {
        // 准备参数
        Long userId = randomLongId();
        String templateCode = randomString();
        Map<String, Object> templateParams = MapUtil.<String, Object>builder().put("code", "1234")
                .put("op", "login").build();
        // mock NotifyTemplateService 的方法
        NotifyTemplateDO template = randomPojo(NotifyTemplateDO.class, o -> {
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
            o.setContent("验证码为{code}, 操作为{op}");
            o.setParams(Lists.newArrayList("code", "op"));
        });
        when(notifyTemplateService.getNotifyTemplateByCodeFromCache(eq(templateCode))).thenReturn(template);
        String content = randomString();
        when(notifyTemplateService.formatNotifyTemplateContent(eq(template.getContent()), eq(templateParams)))
                .thenReturn(content);
        // mock NotifyMessageService 的方法
        Long messageId = randomLongId();
        when(notifyMessageService.createNotifyMessage(eq(userId), eq(UserTypeEnum.ADMIN.getValue()),
                eq(template), eq(content), eq(templateParams))).thenReturn(messageId);

        // 调用
        Long resultMessageId = notifySendService.sendSingleNotifyToAdmin(userId, templateCode, templateParams);
        // 断言
        assertEquals(messageId, resultMessageId);
    }

    @Test
    public void testSendSingleNotifyToMember() {
        // 准备参数
        Long userId = randomLongId();
        String templateCode = randomString();
        Map<String, Object> templateParams = MapUtil.<String, Object>builder().put("code", "1234")
                .put("op", "login").build();
        // mock NotifyTemplateService 的方法
        NotifyTemplateDO template = randomPojo(NotifyTemplateDO.class, o -> {
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
            o.setContent("验证码为{code}, 操作为{op}");
            o.setParams(Lists.newArrayList("code", "op"));
        });
        when(notifyTemplateService.getNotifyTemplateByCodeFromCache(eq(templateCode))).thenReturn(template);
        String content = randomString();
        when(notifyTemplateService.formatNotifyTemplateContent(eq(template.getContent()), eq(templateParams)))
                .thenReturn(content);
        // mock NotifyMessageService 的方法
        Long messageId = randomLongId();
        when(notifyMessageService.createNotifyMessage(eq(userId), eq(UserTypeEnum.MEMBER.getValue()),
                eq(template), eq(content), eq(templateParams))).thenReturn(messageId);

        // 调用
        Long resultMessageId = notifySendService.sendSingleNotifyToMember(userId, templateCode, templateParams);
        // 断言
        assertEquals(messageId, resultMessageId);
    }

    /**
     * 发送成功，当短信模板开启时
     */
    @Test
    public void testSendSingleNotify_successWhenMailTemplateEnable() {
        // 准备参数
        Long userId = randomLongId();
        Integer userType = randomEle(UserTypeEnum.values()).getValue();
        String templateCode = randomString();
        Map<String, Object> templateParams = MapUtil.<String, Object>builder().put("code", "1234")
                .put("op", "login").build();
        // mock NotifyTemplateService 的方法
        NotifyTemplateDO template = randomPojo(NotifyTemplateDO.class, o -> {
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
            o.setContent("验证码为{code}, 操作为{op}");
            o.setParams(Lists.newArrayList("code", "op"));
        });
        when(notifyTemplateService.getNotifyTemplateByCodeFromCache(eq(templateCode))).thenReturn(template);
        String content = randomString();
        when(notifyTemplateService.formatNotifyTemplateContent(eq(template.getContent()), eq(templateParams)))
                .thenReturn(content);
        // mock NotifyMessageService 的方法
        Long messageId = randomLongId();
        when(notifyMessageService.createNotifyMessage(eq(userId), eq(userType),
                eq(template), eq(content), eq(templateParams))).thenReturn(messageId);

        // 调用
        Long resultMessageId = notifySendService.sendSingleNotify(userId, userType, templateCode, templateParams);
        // 断言
        assertEquals(messageId, resultMessageId);
    }

    @Test
    public void testSendSingleNotify_failWhenTemplateDisabled() {
        // 准备参数
        Long userId = randomLongId();
        Integer userType = randomEle(UserTypeEnum.values()).getValue();
        String templateCode = randomString();
        Map<String, Object> templateParams = MapUtil.<String, Object>builder().put("code", "1234")
                .put("op", "login").build();
        // mock NotifyTemplateService 的方法
        NotifyTemplateDO template = randomPojo(NotifyTemplateDO.class, o -> {
            o.setStatus(CommonStatusEnum.DISABLE.getStatus());
            o.setContent("验证码为{code}, 操作为{op}");
            o.setParams(Lists.newArrayList("code", "op"));
        });
        when(notifyTemplateService.getNotifyTemplateByCodeFromCache(eq(templateCode))).thenReturn(template);

        ServiceException exception = Assertions.assertThrows(ServiceException.class,
                () -> notifySendService.sendSingleNotify(userId, userType, templateCode, templateParams));
        assertEquals(1_002_028_001, exception.getCode());
        verify(notifyTemplateService, never()).formatNotifyTemplateContent(anyString(), anyMap());
        verify(notifyMessageService, never()).createNotifyMessage(anyLong(), anyInt(), any(), anyString(), anyMap());
    }

    @Test
    public void testCheckMailTemplateValid_notExists() {
        // 准备参数
        String templateCode = randomString();
        // mock 方法

        // 调用，并断言异常
        assertServiceException(() -> notifySendService.validateNotifyTemplate(templateCode),
                NOTIFY_TEMPLATE_NOT_EXISTS);
    }

    @Test
    public void testCheckTemplateParams_paramMiss() {
        // 准备参数
        NotifyTemplateDO template = randomPojo(NotifyTemplateDO.class,
                o -> o.setParams(Lists.newArrayList("code")));
        Map<String, Object> templateParams = new HashMap<>();
        // mock 方法

        // 调用，并断言异常
        assertServiceException(() -> notifySendService.validateTemplateParams(template, templateParams),
                NOTIFY_SEND_TEMPLATE_PARAM_MISS, "code");
    }

    @Test
    public void testCheckTemplateParams_nullMapFailsExplicitly() {
        NotifyTemplateDO template = randomPojo(NotifyTemplateDO.class,
                o -> o.setParams(Lists.newArrayList("code")));

        assertServiceException(() -> notifySendService.validateTemplateParams(template, null),
                NOTIFY_SEND_TEMPLATE_PARAM_MISS, "code");
    }

    @Test
    public void testSendSingleNotify_failWhenMessageIdEmpty() {
        Long userId = randomLongId();
        Integer userType = randomEle(UserTypeEnum.values()).getValue();
        String templateCode = randomString();
        Map<String, Object> templateParams = Map.of("code", "1234");
        NotifyTemplateDO template = randomPojo(NotifyTemplateDO.class, o -> {
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
            o.setContent("验证码为{code}");
            o.setParams(Lists.newArrayList("code"));
        });
        when(notifyTemplateService.getNotifyTemplateByCodeFromCache(templateCode)).thenReturn(template);
        when(notifyTemplateService.formatNotifyTemplateContent(template.getContent(), templateParams))
                .thenReturn("验证码为1234");
        when(notifyMessageService.createNotifyMessage(userId, userType, template,
                "验证码为1234", templateParams)).thenReturn(null);

        ServiceException exception = Assertions.assertThrows(ServiceException.class,
                () -> notifySendService.sendSingleNotify(userId, userType, templateCode, templateParams));

        assertEquals(1_002_028_004, exception.getCode());
    }

    @Test
    public void testSendSingleNotifyIdempotently_failWhenMessageIdEmpty() {
        Long userId = randomLongId();
        String templateCode = randomString();
        Map<String, Object> templateParams = Map.of("code", "1234");
        NotifyTemplateDO template = randomPojo(NotifyTemplateDO.class, o -> {
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
            o.setContent("验证码为{code}");
            o.setParams(Lists.newArrayList("code"));
        });
        when(notifyTemplateService.getNotifyTemplateByCodeFromCache(templateCode)).thenReturn(template);
        when(notifyTemplateService.formatNotifyTemplateContent(template.getContent(), templateParams))
                .thenReturn("验证码为1234");
        when(notifyMessageService.createNotifyMessage(userId, UserTypeEnum.ADMIN.getValue(), template,
                "验证码为1234", templateParams, "EVENT-1")).thenReturn(null);

        ServiceException exception = Assertions.assertThrows(ServiceException.class,
                () -> invokeIdempotentAdmin(userId, templateCode, templateParams, "EVENT-1"));

        assertEquals(1_002_028_004, exception.getCode());
    }

    @Test
    public void testSendSingleNotifyIdempotently_duplicateUsesCurrentRead() {
        Long userId = randomLongId();
        Long existingMessageId = randomLongId();
        String templateCode = "REG_CERT_READY";
        String businessKey = "REG_CERT:RC-1:READY";
        Map<String, Object> templateParams = Map.of("certificateId", "RC-1");
        NotifyTemplateDO template = randomPojo(NotifyTemplateDO.class, o -> {
            o.setCode(templateCode);
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
            o.setContent("certificate {certificateId}");
            o.setParams(Lists.newArrayList("certificateId"));
        });
        NotifyMessageDO existing = new NotifyMessageDO()
                .setId(existingMessageId)
                .setUserId(userId)
                .setUserType(UserTypeEnum.ADMIN.getValue())
                .setTemplateCode(templateCode)
                .setTemplateParams(templateParams)
                .setBusinessKey(businessKey);
        NotifyMessageService currentReadAwareService = mock(NotifyMessageService.class, invocation -> {
            return switch (invocation.getMethod().getName()) {
                case "getNotifyMessageByBusinessKey" -> null;
                case "getNotifyMessageByBusinessKeyForUpdate" -> existing;
                case "createNotifyMessage" -> throw new DuplicateKeyException("concurrent replay");
                default -> RETURNS_DEFAULTS.answer(invocation);
            };
        });
        ReflectionTestUtils.setField(notifySendService, "notifyMessageService", currentReadAwareService);
        when(notifyTemplateService.getNotifyTemplateByCodeFromCache(templateCode)).thenReturn(template);
        when(notifyTemplateService.formatNotifyTemplateContent(template.getContent(), templateParams))
                .thenReturn("certificate RC-1");

        Long result = assertDoesNotThrow(
                () -> invokeIdempotentAdmin(userId, templateCode, templateParams, businessKey));

        assertEquals(existingMessageId, result);
    }

    @Test
    public void testSendBatchNotify() {
        // 准备参数
        // mock 方法

        // 调用
        UnsupportedOperationException exception = Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> notifySendService.sendBatchNotify(null, null, null, null, null)
        );
        // 断言
        assertEquals("暂时不支持该操作，感兴趣可以实现该功能哟！", exception.getMessage());
    }

    private Long invokeIdempotentAdmin(Long userId, String templateCode,
                                       Map<String, Object> templateParams, String businessKey) {
        Method method;
        try {
            method = NotifySendService.class.getMethod("sendSingleNotifyToAdminIdempotently",
                    Long.class, String.class, Map.class, String.class);
        } catch (NoSuchMethodException exception) {
            return Assertions.fail("idempotent Admin notify service method must exist", exception);
        }
        try {
            return (Long) method.invoke(notifySendService, userId, templateCode, templateParams, businessKey);
        } catch (IllegalAccessException exception) {
            return Assertions.fail("idempotent Admin notify service method must be accessible", exception);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("idempotent Admin notify service invocation failed", cause);
        }
    }

}
