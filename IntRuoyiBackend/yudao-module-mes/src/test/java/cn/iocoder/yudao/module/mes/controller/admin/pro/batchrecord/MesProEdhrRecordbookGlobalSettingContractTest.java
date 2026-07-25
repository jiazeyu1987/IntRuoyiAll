package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrRecordbookGlobalSettingRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrRecordbookGlobalSettingUpdateReqVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrRecordbookGlobalSettingService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.validation.constraints.NotNull;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProEdhrRecordbookGlobalSettingContractTest {

    private static final String GOLDEN_FINGER_PERMISSION =
            "mes:pro-batch-record-execution:golden-finger";
    private static final String CONFIG_KEY = "mes.edhr.recordbook.global.enabled";

    @Test
    void controllerContract_exposesGlobalSettingWithGoldenFingerPermission() throws Exception {
        RequestMapping mapping = MesProEdhrRecordbookSettingController.class.getAnnotation(RequestMapping.class);
        assertArrayEquals(new String[]{"/mes/pro/edhr-recordbook-setting"}, mapping.value());

        Method get = MesProEdhrRecordbookSettingController.class.getDeclaredMethod("getGlobal");
        assertArrayEquals(new String[]{"/global"}, get.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('" + GOLDEN_FINGER_PERMISSION + "')",
                get.getAnnotation(PreAuthorize.class).value());

        Method update = MesProEdhrRecordbookSettingController.class.getDeclaredMethod("updateGlobal",
                EdhrRecordbookGlobalSettingUpdateReqVO.class);
        assertArrayEquals(new String[]{"/global"}, update.getAnnotation(PutMapping.class).value());
        assertEquals("@ss.hasPermission('" + GOLDEN_FINGER_PERMISSION + "')",
                update.getAnnotation(PreAuthorize.class).value());
    }

    @Test
    void serviceAndVoContract_exposeFailFastGlobalConfigShape() throws Exception {
        assertEquals(CONFIG_KEY, MesProEdhrRecordbookGlobalSettingService.CONFIG_KEY);
        MesProEdhrRecordbookGlobalSettingService.class.getDeclaredMethod("getGlobalSetting");
        MesProEdhrRecordbookGlobalSettingService.class.getDeclaredMethod("updateGlobalSetting",
                EdhrRecordbookGlobalSettingUpdateReqVO.class);
        MesProEdhrRecordbookGlobalSettingService.class.getDeclaredMethod("isGlobalRecordbookEnabled");
        MesProEdhrRecordbookGlobalSettingService.class.getDeclaredMethod("resolveEffectiveRecordbookEnabled",
                Boolean.class, String.class);
        MesProEdhrRecordbookGlobalSettingService.class.getDeclaredMethod("requireRecordbookWriteAllowed",
                Boolean.class, String.class);

        requireGetter(EdhrRecordbookGlobalSettingRespVO.class, "getEnabled");
        requireGetter(EdhrRecordbookGlobalSettingRespVO.class, "getConfigKey");
        requireGetter(EdhrRecordbookGlobalSettingRespVO.class, "getUpdatedBy");
        requireGetter(EdhrRecordbookGlobalSettingRespVO.class, "getUpdatedAt");

        Field enabled = EdhrRecordbookGlobalSettingUpdateReqVO.class.getDeclaredField("enabled");
        assertNotNull(enabled.getAnnotation(NotNull.class));
        requireGetter(EdhrRecordbookGlobalSettingUpdateReqVO.class, "getEnabled");
    }

    @Test
    void runtimeContract_appliesGlobalSwitchWithoutMutatingFrozenRouteData() throws Exception {
        Path projectDir = findProjectDir();
        String batchService = read(projectDir,
                "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/"
                        + "MesProEdhrBatchExecutionServiceImpl.java");
        String executionService = read(projectDir,
                "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/"
                        + "MesProBatchRecordExecutionServiceImpl.java");
        String fieldAuditService = read(projectDir,
                "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/"
                        + "MesProBatchRecordExecutionFieldAuditServiceImpl.java");

        assertTrue(batchService.contains("MesProEdhrRecordbookGlobalSettingService"));
        assertTrue(batchService.contains("resolveEffectiveRecordbookEnabled(task.getRecordbookEnabled()"));
        assertTrue(batchService.contains("executionPageQuery.put(\"recordbookEnabled\", effectiveRecordbookEnabled)"));
        assertTrue(batchService.contains(".setRecordbookEnabled(effectiveRecordbookEnabled)"));

        assertTrue(executionService.contains("MesProEdhrRecordbookGlobalSettingService"));
        assertTrue(executionService.contains("resolveEffectiveRecordbookEnabled(execution.getRecordbookEnabled()"));
        assertTrue(executionService.contains(".setRecordbookEnabled(effectiveRecordbookEnabled)"));

        assertTrue(fieldAuditService.contains("MesProEdhrRecordbookGlobalSettingService"));
        assertTrue(fieldAuditService.contains("requireRecordbookWriteAllowed(execution.getRecordbookEnabled()"));
    }

    private static void requireGetter(Class<?> type, String getterName) throws Exception {
        type.getDeclaredMethod(getterName);
    }

    private static String read(Path projectDir, String relativePath) throws Exception {
        return Files.readString(projectDir.resolve(relativePath), StandardCharsets.UTF_8);
    }

    private static Path findProjectDir() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("yudao-module-mes"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new AssertionError("Unable to locate backend project directory");
    }
}
