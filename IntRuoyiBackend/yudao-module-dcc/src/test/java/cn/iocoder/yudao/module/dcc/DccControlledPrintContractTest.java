package cn.iocoder.yudao.module.dcc;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DccControlledPrintContractTest {

    private static final Path BACKEND_ROOT = resolveBackendRoot(Path.of("").toAbsolutePath());
    private static final Path REPO_ROOT = BACKEND_ROOT.getParent();

    @Test
    void schemaDefinesControlledPrintRecordTable() throws Exception {
        String baseSchema = readBackend("sql/mysql/20260513_dcc_base_schema.sql");
        String testSchema = readBackend("yudao-module-dcc/src/test/resources/sql/create_tables.sql");
        assertControlledPrintSchema(baseSchema, "base schema");
        assertControlledPrintSchema(testSchema, "test schema");
    }

    @Test
    void backendExposesFormalControlledPrintApiAndPermission() throws Exception {
        String controller = readBackend("yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/DccControlledFileController.java");
        assertTrue(controller.contains("DccControlledFilePrintService"),
                "controller must use a formal controlled print service");
        assertTrue(controller.contains("dcc:controlled-file:print"),
                "controlled print write endpoints must require dcc:controlled-file:print");
        assertTrue(controller.contains("/{id:\\\\d+}/controlled-print"),
                "controller must expose controlled print creation endpoint under the controlled file");
        assertTrue(controller.contains("/{id:\\\\d+}/controlled-print/records"),
                "controller must expose controlled print records endpoint under the controlled file");
        assertTrue(controller.contains("controlled-print/print-html"),
                "controller must expose generated controlled print HTML with watermarked metadata");

        assertTrue(Files.exists(BACKEND_ROOT.resolve("yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFilePrintService.java")),
                "controlled print service interface must exist");
        assertTrue(Files.exists(BACKEND_ROOT.resolve("yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/dataobject/file/DccControlledFilePrintRecordDO.java")),
                "controlled print record DO must exist");
        assertTrue(Files.exists(BACKEND_ROOT.resolve("yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/mysql/file/DccControlledFilePrintRecordMapper.java")),
                "controlled print record mapper must exist");
    }

    @Test
    void backendPrintHtmlExposesDirectPrintPolicyAndCopyNumbers() throws Exception {
        String service = readBackend("yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFilePrintServiceImpl.java");
        assertTrue(service.contains("副本编号"),
                "generated controlled print HTML must expose per-copy copy numbers");
        assertTrue(service.contains("buildControlledPrintCopyNumbers"),
                "copy numbers must be derived consistently from print number and copies");
        assertTrue(service.contains("直接受控打印"),
                "generated controlled print HTML must expose the current direct print/no approval policy");
    }

    @Test
    void backendPrintEligibilityDoesNotRequireGeneratedArtifactIds() throws Exception {
        String forbiddenArtifactGate = "getPublishedFileId() == null && file.getStampedFileId() == null";
        String printService = readBackend("yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFilePrintServiceImpl.java");
        String queryService = readBackend("yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java");
        assertFalse(printService.contains(forbiddenArtifactGate),
                "controlled print creation must not reject the current ACTIVE file only because generated artifact ids are empty");
        assertFalse(queryService.contains(forbiddenArtifactGate),
                "action projection must expose PRINT for current ACTIVE files with PRINT permission even if generated artifact ids are empty");
    }

    @Test
    void backendDefinesFailFastControlledPrintErrors() throws Exception {
        String errors = readBackend("yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/enums/ErrorCodeConstants.java");
        assertTrue(errors.contains("CONTROLLED_FILE_PRINT_NOT_ALLOWED"),
                "missing permission/current-version controlled print error");
        assertTrue(errors.contains("CONTROLLED_FILE_PRINT_REQUIRED_FIELD_MISSING"),
                "missing required-field controlled print error");
    }

    private static void assertControlledPrintSchema(String schema, String label) {
        assertTrue(schema.contains("CREATE TABLE IF NOT EXISTS `dcc_controlled_file_print_record`"),
                label + " must create dcc_controlled_file_print_record");
        assertTrue(schema.contains("`controlled_file_id` BIGINT NOT NULL"),
                label + " must persist controlled_file_id");
        assertTrue(schema.contains("`print_no` VARCHAR(64) NOT NULL"),
                label + " must persist a traceable print_no");
        assertTrue(schema.contains("`purpose` VARCHAR(255) NOT NULL"),
                label + " must persist print purpose");
        assertTrue(schema.contains("`copies` INT NOT NULL"),
                label + " must persist print copies");
        assertTrue(schema.contains("`receiving_department` VARCHAR(128) NOT NULL"),
                label + " must persist receiving department");
        assertTrue(schema.contains("`use_location` VARCHAR(128) NOT NULL"),
                label + " must persist use location");
        assertTrue(schema.contains("`print_user_id` BIGINT NOT NULL"),
                label + " must persist print user");
        assertTrue(schema.contains("`print_time` DATETIME NOT NULL"),
                label + " must persist print time");
        assertTrue(schema.contains("`approval_status` VARCHAR(32) NOT NULL"),
                label + " must persist direct/approval status");
    }

    private static String readBackend(String relativePath) throws Exception {
        return Files.readString(BACKEND_ROOT.resolve(relativePath), StandardCharsets.UTF_8);
    }

    private static Path resolveBackendRoot(Path start) {
        Path current = start;
        while (current != null) {
            if (Files.exists(current.resolve("sql/mysql/20260513_dcc_base_schema.sql"))
                    && Files.exists(current.resolve("yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/DccControlledFileController.java"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot resolve IntRuoyi backend root from " + start);
    }

    @SuppressWarnings("unused")
    private static String readRepo(String relativePath) throws Exception {
        return Files.readString(REPO_ROOT.resolve(relativePath), StandardCharsets.UTF_8);
    }
}
