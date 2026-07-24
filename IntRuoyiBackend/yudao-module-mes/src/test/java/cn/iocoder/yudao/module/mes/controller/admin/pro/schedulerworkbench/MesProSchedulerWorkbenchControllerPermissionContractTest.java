package cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProSchedulerWorkbenchControllerPermissionContractTest {

    private static final Path CONTROLLER = Path.of("src", "main", "java", "cn", "iocoder", "yudao",
            "module", "mes", "controller", "admin", "pro", "schedulerworkbench",
            "MesProSchedulerWorkbenchController.java");
    private static final Path PERMISSION_SQL = Path.of("..", "sql", "mysql",
            "20260624_mes_scheduler_workbench_permission_split.sql");

    @Test
    void writeEndpoints_shouldNotReuseQueryPermission() throws IOException {
        String source = Files.readString(CONTROLLER, StandardCharsets.UTF_8);

        assertEndpointPermission(source, "saveShiftHoursSetting", "mes:pro-scheduler-workbench:update");
        assertEndpointPermission(source, "savePolicySettings", "mes:pro-scheduler-workbench:update");
        assertEndpointPermission(source, "importFullConfigPackage", "mes:pro-scheduler-workbench:update");
        assertEndpointPermission(source, "startSmokeTest", "mes:pro-scheduler-workbench:smoke-test");
        assertEndpointPermission(source, "stopSmokeTest", "mes:pro-scheduler-workbench:smoke-test");
        assertEndpointPermission(source, "exportFullConfigPackage", "mes:pro-scheduler-workbench:query");

        assertFalse(methodBlock(source, "saveShiftHoursSetting").contains("mes:pro-scheduler-workbench:query"),
                "保存班次小时不得复用工作台查询权限");
        assertFalse(methodBlock(source, "savePolicySettings").contains("mes:pro-scheduler-workbench:query"),
                "保存策略不得复用工作台查询权限");
        assertFalse(methodBlock(source, "importFullConfigPackage").contains("mes:pro-scheduler-workbench:query"),
                "导入全部数据包不得复用工作台查询权限");
        assertFalse(methodBlock(source, "startSmokeTest").contains("mes:pro-scheduler-workbench:query"),
                "启动冒烟测试不得复用工作台查询权限");
        assertFalse(methodBlock(source, "stopSmokeTest").contains("mes:pro-scheduler-workbench:query"),
                "停止冒烟测试不得复用工作台查询权限");
        assertFalse(methodBlock(source, "exportFullConfigPackage").contains("mes:pro-scheduler-workbench:update"),
                "导出全部数据包不得复用工作台更新权限");
    }

    @Test
    void splitPermissions_shouldHaveMenuMigration() throws IOException {
        String sql = Files.readString(PERMISSION_SQL, StandardCharsets.UTF_8);

        assertTrue(sql.contains("mes:pro-scheduler-workbench:update"),
                "设置保存权限必须写入 system_menu");
        assertTrue(sql.contains("mes:pro-scheduler-workbench:smoke-test"),
                "冒烟测试权限必须写入 system_menu");
        assertTrue(sql.contains("900170") && sql.contains("900171"),
                "权限菜单必须使用稳定菜单 ID，便于租户套餐和角色绑定");
        assertTrue(sql.contains("system_role_menu"), "权限菜单必须绑定到角色菜单");
        assertTrue(sql.contains("system_tenant_package"), "权限菜单必须进入租户套餐 menu_ids");
    }

    private static void assertEndpointPermission(String source, String methodName, String permission) {
        assertTrue(methodBlock(source, methodName).contains(permission),
                methodName + " 必须使用独立权限 " + permission);
    }

    private static String methodBlock(String source, String methodName) {
        int methodIndex = source.indexOf(methodName + "(");
        assertTrue(methodIndex >= 0, "缺少方法: " + methodName);
        int start = source.lastIndexOf("\n    @", methodIndex);
        int end = source.indexOf("\n    }\n", methodIndex);
        assertTrue(start >= 0 && end > methodIndex, "缺少方法块: " + methodName);
        return source.substring(start, end + "\n    }\n".length());
    }
}
