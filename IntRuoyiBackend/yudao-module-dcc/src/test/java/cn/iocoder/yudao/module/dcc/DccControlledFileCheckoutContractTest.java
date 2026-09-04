package cn.iocoder.yudao.module.dcc;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DccControlledFileCheckoutContractTest {

    private static final Path BACKEND_ROOT = resolveBackendRoot(Path.of("").toAbsolutePath());

    @Test
    void backendExposesCheckoutAndCheckinApi() throws Exception {
        String controller = readBackend("yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/DccControlledFileController.java");
        assertTrue(controller.contains("/{id:\\\\d+}/checkout"));
        assertTrue(controller.contains("/{id:\\\\d+}/checkin"));
        assertTrue(controller.contains("queryService.checkoutControlledFile(getLoginUserId(), id)"));
        assertTrue(controller.contains("queryService.checkinControlledFile(getLoginUserId(), id)"));
    }

    @Test
    void checkoutStateIsPersistedAndProjectedWithOwner() throws Exception {
        String service = readBackend("yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java");
        String mapper = readBackend("yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/mysql/file/DccControlledFileMapper.java");
        String response = readBackend("yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileRespVO.java");
        String versionHistoryResponse = readBackend("yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileVersionHistoryRespVO.java");
        String dataObject = readBackend("yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/dataobject/file/DccControlledFileDO.java");
        assertTrue(service.contains("checkoutControlledFile"));
        assertTrue(service.contains("checkinControlledFile"));
        assertTrue(service.contains("requireCheckoutAccessibleControlledFile(userId, id)"));
        assertTrue(service.contains("private DccControlledFileDO requireCheckoutAccessibleControlledFile"));
        assertTrue(service.contains("canAccessQuery(userId, file, new DccControlledFilePageReqVO(), hasDirectoryManagementPermission)"));
        assertTrue(service.contains("setCheckedOutByName"));
        assertTrue(mapper.contains("checked_out_by IS NULL"));
        assertTrue(mapper.contains("checked_out_by = #{actorId}"));
        assertTrue(mapper.contains("checked_out_by = NULL"));
        assertTrue(response.contains("private Long checkedOutBy"));
        assertTrue(response.contains("private String checkedOutByName"));
        assertTrue(versionHistoryResponse.contains("private Long checkedOutBy"));
        assertTrue(versionHistoryResponse.contains("private String checkedOutByName"));
        assertTrue(service.contains("fillCheckoutProjection(respVO, history)"));
        assertTrue(dataObject.contains("private Long checkedOutBy"));
        assertTrue(dataObject.contains("private LocalDateTime checkedOutTime"));
    }

    @Test
    void schemaContainsCheckoutColumnsAndIndex() throws Exception {
        String baseSchema = readBackend("sql/mysql/20260513_dcc_base_schema.sql");
        String testSchema = readBackend("yudao-module-dcc/src/test/resources/sql/create_tables.sql");
        String migration = readBackend("sql/mysql/20260903_dcc_controlled_file_checkout.sql");
        for (String schema : new String[]{baseSchema, testSchema, migration}) {
            assertTrue(schema.contains("checked_out_by"));
            assertTrue(schema.contains("checked_out_time"));
        }
        assertTrue(migration.contains("idx_dcc_controlled_file_checkout"));
        assertTrue(migration.contains("release-migration:"));
        assertTrue(migration.contains("CREATE PROCEDURE ensure_dcc_checkout_column"));
        assertTrue(migration.contains("information_schema.columns"));
        assertTrue(migration.contains("information_schema.statistics"));
        assertTrue(migration.contains("DROP PROCEDURE IF EXISTS ensure_dcc_checkout_column"));
        assertTrue(migration.contains("DROP PROCEDURE IF EXISTS ensure_dcc_checkout_index"));
    }

    private static String readBackend(String relativePath) throws Exception {
        return Files.readString(BACKEND_ROOT.resolve(relativePath), StandardCharsets.UTF_8);
    }

    private static Path resolveBackendRoot(Path start) {
        Path current = start;
        while (current != null) {
            if (Files.exists(current.resolve("sql/mysql/20260513_dcc_base_schema.sql"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot resolve IntRuoyi backend root from " + start);
    }
}
