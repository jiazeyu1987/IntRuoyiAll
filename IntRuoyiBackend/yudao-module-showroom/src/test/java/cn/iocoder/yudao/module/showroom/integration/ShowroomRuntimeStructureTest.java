package cn.iocoder.yudao.module.showroom.integration;

import cn.iocoder.yudao.module.showroom.controller.ShowroomApiRuntime;
import cn.iocoder.yudao.module.showroom.controller.display.ShowroomDisplayController;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;

class ShowroomRuntimeStructureTest {

    @Test
    void runtimeShouldNotExposeStandaloneFallbackEntryPoints() {
        assertFalse(hasMethod(ShowroomApiRuntime.class, "shared"),
                "ShowroomApiRuntime.shared() should be removed");
        assertFalse(hasMethod(ShowroomApiRuntime.class, "publishPreviewAssetForTest"),
                "publishPreviewAssetForTest should be removed");
        assertFalse(hasZeroArgConstructor(ShowroomApiRuntime.class),
                "ShowroomApiRuntime no-arg constructor should be removed");
        assertFalse(hasZeroArgConstructor(ShowroomDisplayController.class),
                "ShowroomDisplayController no-arg constructor should be removed");
    }

    private static boolean hasMethod(Class<?> type, String methodName) {
        return Arrays.stream(type.getDeclaredMethods())
                .map(Method::getName)
                .anyMatch(methodName::equals);
    }

    private static boolean hasZeroArgConstructor(Class<?> type) {
        return Arrays.stream(type.getDeclaredConstructors())
                .map(Constructor::getParameterCount)
                .anyMatch(count -> count == 0);
    }

}
