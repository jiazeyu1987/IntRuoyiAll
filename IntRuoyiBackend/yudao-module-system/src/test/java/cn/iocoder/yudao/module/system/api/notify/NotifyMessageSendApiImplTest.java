package cn.iocoder.yudao.module.system.api.notify;

import cn.iocoder.yudao.module.system.service.notify.NotifySendService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.annotation.Validated;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class NotifyMessageSendApiImplTest {

    private static final String REQUEST_CLASS =
            "cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserIdempotentReqDTO";

    @Test
    void idempotentAdminRequestRequiresStableBusinessKeyAndTemplateParams() throws Exception {
        Class<?> requestClass = requireRequestClass();

        assertNotNull(requestClass.getDeclaredField("businessKey").getAnnotation(NotBlank.class));
        Size businessKeySize = requestClass.getDeclaredField("businessKey").getAnnotation(Size.class);
        assertNotNull(businessKeySize);
        assertEquals(255, businessKeySize.max());
        assertNotNull(requestClass.getDeclaredField("templateParams").getAnnotation(NotNull.class));
    }

    @Test
    void idempotentAdminApiDelegatesTheStableBusinessKey() throws Exception {
        AtomicReference<Method> calledMethod = new AtomicReference<>();
        AtomicReference<Object[]> calledArguments = new AtomicReference<>();
        NotifySendService notifySendService = (NotifySendService) Proxy.newProxyInstance(
                NotifySendService.class.getClassLoader(), new Class<?>[]{NotifySendService.class},
                (proxy, method, args) -> {
                    calledMethod.set(method);
                    calledArguments.set(args);
                    return 991L;
                });
        NotifyMessageSendApiImpl api = new NotifyMessageSendApiImpl();
        ReflectionTestUtils.setField(api, "notifySendService", notifySendService);

        Class<?> requestClass = requireRequestClass();
        Object request = requestClass.getConstructor().newInstance();
        invokeSetter(requestClass, request, "setUserId", Long.class, 31L);
        invokeSetter(requestClass, request, "setTemplateCode", String.class, "REG_CERT_READY");
        invokeSetter(requestClass, request, "setTemplateParams", Map.class, Map.of("certificateId", "RC-1"));
        invokeSetter(requestClass, request, "setBusinessKey", String.class, " REG_CERT:RC-1:READY ");

        Method apiMethod = requireMethod(NotifyMessageSendApi.class,
                "sendSingleMessageIdempotentlyToAdmin", requestClass);
        Object result = apiMethod.invoke(api, request);

        assertEquals(991L, result);
        assertNotNull(calledMethod.get());
        assertEquals("sendSingleNotifyToAdminIdempotently", calledMethod.get().getName());
        assertEquals(31L, calledArguments.get()[0]);
        assertEquals("REG_CERT_READY", calledArguments.get()[1]);
        assertEquals(Map.of("certificateId", "RC-1"), calledArguments.get()[2]);
        assertEquals(" REG_CERT:RC-1:READY ", calledArguments.get()[3]);
    }

    @Test
    void idempotentAdminApiEnablesMethodValidation() {
        assertNotNull(NotifyMessageSendApiImpl.class.getAnnotation(Validated.class));
        Method method = requireMethod(NotifyMessageSendApiImpl.class,
                "sendSingleMessageIdempotentlyToAdmin", requireRequestClass());
        assertNotNull(method.getParameters()[0].getAnnotation(Valid.class));
    }

    private Class<?> requireRequestClass() {
        try {
            return Class.forName(REQUEST_CLASS);
        } catch (ClassNotFoundException exception) {
            return fail("idempotent Admin notify request DTO must exist", exception);
        }
    }

    private Method requireMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        try {
            return type.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException exception) {
            return fail("idempotent Admin notify API method must exist", exception);
        }
    }

    private void invokeSetter(Class<?> requestClass, Object request, String name,
                              Class<?> parameterType, Object value) throws Exception {
        Method method = requestClass.getMethod(name, parameterType);
        Object returned = method.invoke(request, value);
        assertTrue(returned == null || returned == request);
    }
}
