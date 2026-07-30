package cn.iocoder.yudao.module.mes;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.mesprocess.MesProMesProcessCatalogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.mesprocess.MesProMesProcessCatalogMachineryDO;
import com.baomidou.mybatisplus.annotation.TableName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProMesProcessCatalogSchemaTest {

    @Test
    void shouldDefineReadonlyMesProcessCatalogContract() throws Exception {
        assertEquals("mes_pro_mes_process_catalog", tableName(MesProMesProcessCatalogDO.class));
        assertEquals("mes_pro_mes_process_catalog_machinery",
                tableName(MesProMesProcessCatalogMachineryDO.class));

        assertField(MesProMesProcessCatalogDO.class, "sourceRowNo", Integer.class);
        assertField(MesProMesProcessCatalogDO.class, "productName", String.class);
        assertField(MesProMesProcessCatalogDO.class, "mesProcessCode", String.class);
        assertField(MesProMesProcessCatalogDO.class, "mesProcessName", String.class);
        assertField(MesProMesProcessCatalogDO.class, "deviceQuantity", Integer.class);
        assertField(MesProMesProcessCatalogDO.class, "dailyCapacityTenHalfHours", BigDecimal.class);
        assertField(MesProMesProcessCatalogDO.class, "dailyManpower", BigDecimal.class);
        assertField(MesProMesProcessCatalogDO.class, "processUnitPrice", BigDecimal.class);
        assertField(MesProMesProcessCatalogDO.class, "reportWorkFlag", String.class);
        assertField(MesProMesProcessCatalogDO.class, "batchRecordFlag", String.class);
        assertField(MesProMesProcessCatalogDO.class, "batchRecordProcessName", String.class);
        assertField(MesProMesProcessCatalogDO.class, "executionProcessId", Long.class);

        assertField(MesProMesProcessCatalogMachineryDO.class, "catalogId", Long.class);
        assertField(MesProMesProcessCatalogMachineryDO.class, "machineryId", Long.class);
        assertField(MesProMesProcessCatalogMachineryDO.class, "sourceDeviceCode", String.class);
        assertField(MesProMesProcessCatalogMachineryDO.class, "sourceDeviceName", String.class);
        assertField(MesProMesProcessCatalogMachineryDO.class, "sort", Integer.class);

        String schemaSql = readBackendFile("sql/mysql/20260730_mes_process_readonly_catalog.sql");
        assertTrue(schemaSql.contains("CREATE TABLE IF NOT EXISTS `mes_pro_mes_process_catalog`"));
        assertTrue(schemaSql.contains("CREATE TABLE IF NOT EXISTS `mes_pro_mes_process_catalog_machinery`"));
        assertTrue(schemaSql.contains("UNIQUE KEY `uk_mes_process_catalog_source_row` (`tenant_id`, `source_row_no`, `deleted`)"));
        assertTrue(schemaSql.contains("KEY `idx_mes_process_catalog_execution_process` (`tenant_id`, `execution_process_id`)"));
        assertTrue(schemaSql.contains("UNIQUE KEY `uk_mes_process_catalog_machinery` (`tenant_id`, `catalog_id`, `machinery_id`, `deleted`)"));

        String menuSql = readBackendFile("sql/mysql/20260730_mes_process_readonly_catalog_menu.sql");
        assertTrue(menuSql.contains("'mes-process'"));
        assertTrue(menuSql.contains("'mes/pro/mes-process/index'"));
        assertTrue(menuSql.contains("'MesProMesProcess'"));
        assertTrue(menuSql.contains("'mes:pro-mes-process:query'"));
        assertTrue(menuSql.contains("CONVERT(UNHEX('4D4553E5B7A5E5BA8F') USING utf8mb4)"));

        String seedSql = readBackendFile("sql/mysql/20260730_mes_process_readonly_catalog_seed.sql");
        assertTrue(seedSql.contains("'pressure-pump-g2'"));
        assertTrue(seedSql.contains("source_row_no"));
        assertTrue(seedSql.contains("B09032/G01160"));
        assertTrue(seedSql.contains("SIGNAL SQLSTATE '45000'"));
        assertTrue(seedSql.contains("expected 32 catalog rows"));
        assertFalse(seedSql.contains("588"));
        assertFalse(seedSql.contains("7481"));
        assertFalse(seedSql.contains("10225"));
    }

    private static String tableName(Class<?> clazz) {
        return clazz.getAnnotation(TableName.class).value();
    }

    private static void assertField(Class<?> clazz, String name, Class<?> type) throws Exception {
        Field field = clazz.getDeclaredField(name);
        assertEquals(type, field.getType(), clazz.getSimpleName() + "." + name);
    }

    private static String readBackendFile(String relative) throws Exception {
        return Files.readString(resolveBackendPath(relative), StandardCharsets.UTF_8);
    }

    private static Path resolveBackendPath(String relative) {
        Path cwd = Paths.get("").toAbsolutePath();
        if ("yudao-module-mes".equals(cwd.getFileName().toString())) {
            return cwd.getParent().resolve(relative);
        }
        return cwd.resolve(relative);
    }
}
