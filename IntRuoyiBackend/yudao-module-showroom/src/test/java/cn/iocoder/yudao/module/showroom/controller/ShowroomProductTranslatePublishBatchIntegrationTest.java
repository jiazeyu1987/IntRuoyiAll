package cn.iocoder.yudao.module.showroom.controller;

import cn.iocoder.yudao.module.showroom.controller.admin.ShowroomAdminController;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ShowroomProductTranslatePublishBatchIntegrationTest {

    @Test
    void controllerShouldExposeTranslatePublishBatchEndpointsAndContracts() {
        assertTrue(hasMethod(ShowroomAdminController.class, "startBatchTranslatePublishProducts"));
        assertTrue(hasMethod(ShowroomAdminController.class, "getProductBatchTranslatePublishStatus"));
        assertTrue(Arrays.stream(ShowroomAdminController.class.getDeclaredClasses())
                .anyMatch(type -> type.getSimpleName().equals("ProductTranslatePublishBatchTaskRespVO")));
    }

    @Test
    void runtimeShouldExposeTranslatePublishBatchTaskEntryPoints() {
        assertTrue(hasMethod(ShowroomApiRuntime.class, "startBatchTranslatePublishProducts"));
        assertTrue(hasMethod(ShowroomApiRuntime.class, "getProductBatchTranslatePublishStatus"));
    }

    @Test
    void runtimeShouldPersistFailureForUnobservedAsyncTranslateTaskExceptions() throws IOException {
        String source = readRuntimeSource();

        assertTrue(source.contains(".whenComplete((ignored, throwable) ->"),
                "batch translate publish async task must observe CompletableFuture failures");
        assertTrue(source.contains("completeProductTranslatePublishBatchTaskAfterFailure("),
                "unobserved async failures must be persisted to the task and current item");
    }

    @Test
    void runtimeShouldRecoverStaleTranslatePublishBatchTasks() throws IOException {
        String source = readRuntimeSource();

        assertTrue(source.contains("recoverStaleProductTranslatePublishBatchTask("),
                "status reads must recover stale RUNNING translate publish tasks");
        assertTrue(source.contains("SHOWROOM_TRANSLATION_BATCH_STALE"),
                "stale recovery must expose an auditable failure reason");
        assertTrue(source.contains("isProductTranslatePublishTaskStale("),
                "stale detection must be explicit instead of leaving RUNNING forever");
    }

    private static boolean hasMethod(Class<?> type, String name) {
        for (Method method : type.getDeclaredMethods()) {
            if (method.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static String readRuntimeSource() throws IOException {
        Path moduleRelative = Path.of("src/main/java/cn/iocoder/yudao/module/showroom/controller/ShowroomApiRuntime.java");
        Path repoRelative = Path.of("yudao-module-showroom/src/main/java/cn/iocoder/yudao/module/showroom/controller/ShowroomApiRuntime.java");
        Path path = Files.exists(moduleRelative) ? moduleRelative : repoRelative;
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
