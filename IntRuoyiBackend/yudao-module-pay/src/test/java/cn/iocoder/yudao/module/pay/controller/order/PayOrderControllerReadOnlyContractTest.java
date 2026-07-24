package cn.iocoder.yudao.module.pay.controller.order;

import cn.iocoder.yudao.module.pay.controller.admin.order.PayOrderController;
import cn.iocoder.yudao.module.pay.controller.app.order.AppPayOrderController;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayOrderControllerReadOnlyContractTest {

    @Test
    void getOrderEndpointsShouldNotAcceptSyncAndShouldExposePostSyncCommand() {
        assertReadOnlyGetAndExplicitSync(PayOrderController.class);
        assertReadOnlyGetAndExplicitSync(AppPayOrderController.class);
    }

    private static void assertReadOnlyGetAndExplicitSync(Class<?> controllerClass) {
        Method getOrder = Arrays.stream(controllerClass.getDeclaredMethods())
                .filter(method -> method.getName().equals("getOrder"))
                .filter(method -> method.isAnnotationPresent(GetMapping.class))
                .findFirst()
                .orElseThrow();
        assertFalse(Arrays.asList(getOrder.getParameterTypes()).contains(Boolean.class),
                controllerClass.getSimpleName() + " 的 GET 查询不得接受 sync 写入开关");

        boolean hasSyncPost = Arrays.stream(controllerClass.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(PostMapping.class))
                .anyMatch(method -> Arrays.asList(method.getAnnotation(PostMapping.class).value()).contains("/sync"));
        assertTrue(hasSyncPost, controllerClass.getSimpleName() + " 应提供显式 POST /sync 命令");
    }
}
