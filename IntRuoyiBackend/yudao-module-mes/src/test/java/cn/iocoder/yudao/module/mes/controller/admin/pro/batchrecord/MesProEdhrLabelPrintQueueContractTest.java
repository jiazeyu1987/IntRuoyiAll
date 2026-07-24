package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrLabelInstancePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrLabelPreviewReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrLabelTemplateActivateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrLabelTemplateCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrLabelTemplatePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPrintTaskConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPrintTaskCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPrintTaskMarkFailedReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPrintTaskPageReqVO;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProEdhrLabelPrintQueueContractTest {

    private static final String SERVICE_IMPL =
            "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrLabelPrintServiceImpl.java";
    private static final String SCHEMA_SQL =
            "sql/mysql/20260618_mes_edhr_label_print_queue.sql";

    @Test
    void controllersExposeLabelPrintQueueRoutesAndPermissions() throws Exception {
        assertRequestMapping(MesProEdhrLabelTemplateController.class, "/mes/pro/edhr-label-template");
        assertGetMapping(MesProEdhrLabelTemplateController.class, "getPage",
                new Class<?>[]{MesProEdhrLabelTemplatePageReqVO.class},
                "/page", "mes:pro-edhr-label-template:query");
        assertPostMapping(MesProEdhrLabelTemplateController.class, "create",
                new Class<?>[]{MesProEdhrLabelTemplateCreateReqVO.class},
                "/create", "mes:pro-edhr-label-template:create");
        assertPostMapping(MesProEdhrLabelTemplateController.class, "activate",
                new Class<?>[]{MesProEdhrLabelTemplateActivateReqVO.class},
                "/activate", "mes:pro-edhr-label-template:activate");

        assertRequestMapping(MesProEdhrLabelController.class, "/mes/pro/edhr-label");
        assertGetMapping(MesProEdhrLabelController.class, "getPage",
                new Class<?>[]{MesProEdhrLabelInstancePageReqVO.class},
                "/page", "mes:pro-edhr-label:query");
        assertPostMapping(MesProEdhrLabelController.class, "preview",
                new Class<?>[]{MesProEdhrLabelPreviewReqVO.class},
                "/preview", "mes:pro-edhr-label:preview");

        assertRequestMapping(MesProEdhrPrintTaskController.class, "/mes/pro/edhr-print-task");
        assertGetMapping(MesProEdhrPrintTaskController.class, "getPage",
                new Class<?>[]{MesProEdhrPrintTaskPageReqVO.class},
                "/page", "mes:pro-edhr-print-task:query");
        assertPostMapping(MesProEdhrPrintTaskController.class, "create",
                new Class<?>[]{MesProEdhrPrintTaskCreateReqVO.class},
                "/create", "mes:pro-edhr-print-task:create");
        assertPostMapping(MesProEdhrPrintTaskController.class, "markFailed",
                new Class<?>[]{MesProEdhrPrintTaskMarkFailedReqVO.class},
                "/mark-failed", "mes:pro-edhr-print-task:mark-failed");
        assertPostMapping(MesProEdhrPrintTaskController.class, "confirm",
                new Class<?>[]{MesProEdhrPrintTaskConfirmReqVO.class},
                "/confirm", "mes:pro-edhr-print-task:confirm");
    }

    @Test
    void serviceRequiresAuditedReprintAndDoesNotFakePrintSuccess() throws Exception {
        String source = read(SERVICE_IMPL).replace("\r\n", "\n");

        assertTrue(source.contains("requireReprintReason(reqVO.getIsReprint(), reqVO.getReprintReason());\n"
                + "        requireOriginalPrintTask(reqVO.getIsReprint(), reqVO.getOriginalPrintTaskId());\n"
                + "        MesProEdhrPrintTaskDO existing = printTaskMapper.selectByIdempotencyKey"));
        assertTrue(source.contains("throw exception(PRO_EDHR_PRINT_ORIGINAL_TASK_REQUIRED);"));
        assertTrue(source.contains("printTaskMapper.selectById(originalPrintTaskId) == null"));
        assertTrue(source.contains("throw exception(PRO_EDHR_PRINT_TASK_NOT_EXISTS);"));
        assertTrue(source.contains("boolean printCountDeducted = false;"));
        assertTrue(source.contains("requireConfirmationEvidence(reqVO.getConfirmationEvidenceHash());"));

        assertFalse(source.contains("window.print"));
        assertFalse(source.contains("DEFAULT_SUCCESS"));
        assertFalse(source.contains("MOCK_PRINT"));
        assertFalse(source.contains("catch (Exception"));
    }

    @Test
    void schemaDeclaresMenusAndFailsFastWithoutDestructiveShortcuts() throws Exception {
        String schema = read(SCHEMA_SQL);

        for (String table : new String[]{
                "mes_pro_edhr_label_template",
                "mes_pro_edhr_label_instance",
                "mes_pro_edhr_print_task",
                "mes_pro_edhr_print_event"}) {
            assertTrue(schema.contains("CREATE TABLE IF NOT EXISTS `" + table + "`"), "Missing table " + table);
        }
        for (String permission : new String[]{
                "mes:pro-edhr-label-template:query",
                "mes:pro-edhr-label-template:create",
                "mes:pro-edhr-label-template:activate",
                "mes:pro-edhr-label:query",
                "mes:pro-edhr-label:preview",
                "mes:pro-edhr-print-task:query",
                "mes:pro-edhr-print-task:create",
                "mes:pro-edhr-print-task:mark-failed",
                "mes:pro-edhr-print-task:confirm"}) {
            assertTrue(schema.contains(permission), "Missing permission " + permission);
        }

        assertTrue(schema.contains("SIGNAL SQLSTATE '45000'"));
        assertTrue(schema.contains("system_role_menu"));
        assertTrue(schema.contains("tenant_admin"));
        assertFalse(Pattern.compile("\\b(DROP\\s+TABLE|TRUNCATE\\s+TABLE|DELETE\\s+FROM)\\b",
                        Pattern.CASE_INSENSITIVE).matcher(schema).find(),
                "Label print migration must not contain destructive table/data operations");
        assertFalse(Pattern.compile("\\b(INSERT\\s+IGNORE|ON\\s+DUPLICATE\\s+KEY\\s+UPDATE)\\b",
                        Pattern.CASE_INSENSITIVE).matcher(schema).find(),
                "Label print migration must not silently ignore or overwrite rows");
    }

    private static void assertRequestMapping(Class<?> controllerClass, String path) {
        RequestMapping requestMapping = controllerClass.getAnnotation(RequestMapping.class);
        assertArrayEquals(new String[]{path}, requestMapping.value());
    }

    private static void assertGetMapping(Class<?> controllerClass,
                                         String methodName,
                                         Class<?>[] parameterTypes,
                                         String path,
                                         String permission) throws Exception {
        Method method = controllerClass.getDeclaredMethod(methodName, parameterTypes);
        assertArrayEquals(new String[]{path}, method.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('" + permission + "')",
                method.getAnnotation(PreAuthorize.class).value());
    }

    private static void assertPostMapping(Class<?> controllerClass,
                                          String methodName,
                                          Class<?>[] parameterTypes,
                                          String path,
                                          String permission) throws Exception {
        Method method = controllerClass.getDeclaredMethod(methodName, parameterTypes);
        assertArrayEquals(new String[]{path}, method.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('" + permission + "')",
                method.getAnnotation(PreAuthorize.class).value());
    }

    private static String read(String relativePath) throws Exception {
        Path path = findProjectDir().resolve(relativePath);
        assertTrue(Files.exists(path), relativePath + " must exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static Path findProjectDir() {
        Path currentDir = Path.of(System.getProperty("user.dir"));
        return "yudao-module-mes".equals(currentDir.getFileName().toString()) ? currentDir.getParent() : currentDir;
    }
}
