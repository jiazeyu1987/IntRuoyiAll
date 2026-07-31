package cn.iocoder.yudao.module.mes;

import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesFrontlineBindingSourceSchemaTest {

    @Test
    void shouldDeclareFormalFrontlineBindingTablesAndProductionBeans() throws Exception {
        Class<?> routeBindingSource = Class.forName(
                "cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineDeviceAccountRouteBindingSourceImpl");
        Class<?> templateBindingSource = Class.forName(
                "cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineTemplateBindingSourceImpl");
        assertNotNull(routeBindingSource.getAnnotation(Service.class));
        assertNotNull(templateBindingSource.getAnnotation(Service.class));

        String migration = Files.readString(Path.of("..", "sql", "mysql",
                "20260730_mes_frontline_binding_sources.sql"), StandardCharsets.UTF_8);
        assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS `mes_frontline_device_account_route_binding`"));
        assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS `mes_frontline_employee_template_binding`"));
        assertTrue(migration.contains("UNIQUE KEY `uk_mes_frontline_route_binding`"));
        assertTrue(migration.contains("UNIQUE KEY `uk_mes_frontline_template_binding`"));

        String h2Schema = Files.readString(Path.of("src", "test", "resources", "sql", "create_tables.sql"),
                StandardCharsets.UTF_8);
        assertTrue(h2Schema.contains("\"mes_frontline_device_account_route_binding\""));
        assertTrue(h2Schema.contains("\"mes_frontline_employee_template_binding\""));
    }
}
