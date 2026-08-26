package cn.iocoder.yudao.server;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.migration.DccRegistrationCertificateMigrationService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.migration.DccRegistrationCertificateMigrationService.BatchCommand;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.migration.DccRegistrationCertificateMigrationService.EntrustedEnterpriseCommand;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.migration.DccRegistrationCertificateMigrationService.RowCommand;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = YudaoServerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.quartz.auto-startup=false",
                "spring.task.scheduling.enabled=false",
                "spring.main.lazy-initialization=true"
        })
@ActiveProfiles("local")
class RegistrationCertificateHistoricalRuntimeSyncTest {

    private static final long TENANT_ID = 1L;
    private static final long ACTOR_ID = 1L;
    private static final String SOURCE_HASH =
            "D42162DC354E8976CED450FA8A2BB00A2AB6099EDDF19AB907FEC3366EF94FF4";
    private static final Path PLAN = Path.of(
            "E:/IntRuoyi/doc/tasks/20260824-registration-certificate-excel-sync/commit-plan-post-mapping.json");

    @Resource
    private DccRegistrationCertificateMigrationService migrationService;
    @Resource
    private DataSource dataSource;

    private static JdbcTemplate jdbcTemplate;

    @BeforeAll
    static void beforeAll() {
        TenantContextHolder.setTenantId(TENANT_ID);
    }

    @AfterAll
    static void afterAll() {
        TenantContextHolder.clear();
    }

    @Test
    void importsReadyRowsAtomicallyAndReplaysWithoutFiles() throws Exception {
        jdbcTemplate = new JdbcTemplate(dataSource);
        int beforeAudit = countSourceAudits();
        int beforeFiles = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dcc_registration_certificate_file WHERE tenant_id = ?", Integer.class,
                TENANT_ID);
        if (beforeAudit != 0) {
            throw new IllegalStateException("historical source already exists; refusing to rerun write test");
        }

        BatchCommand command = readCommand();
        DccRegistrationCertificateMigrationService.Result first = migrationService.commitHistoricalBatch(command);
        assertEquals(109, first.committedCount());
        assertEquals(0, first.replayedCount());
        assertEquals(109, first.restrictedCount());
        assertEquals(109, countSourceAudits());
        assertEquals(beforeFiles, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dcc_registration_certificate_file WHERE tenant_id = ?", Integer.class,
                TENANT_ID));

        DccRegistrationCertificateMigrationService.Result replay = migrationService.commitHistoricalBatch(command);
        assertEquals(0, replay.committedCount());
        assertEquals(109, replay.replayedCount());
        assertEquals(109, replay.restrictedCount());
        assertEquals(109, countSourceAudits());
        assertEquals(beforeFiles, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dcc_registration_certificate_file WHERE tenant_id = ?", Integer.class,
                TENANT_ID));
    }

    private BatchCommand readCommand() throws Exception {
        JsonNode root = new ObjectMapper().readTree(Files.readString(PLAN));
        List<RowCommand> rows = new ArrayList<>();
        for (JsonNode node : root.get("rows")) {
            if (!"READY_FOR_COMMIT".equals(node.path("status").asText())) {
                continue;
            }
            List<EntrustedEnterpriseCommand> entrusted = new ArrayList<>();
            JsonNode enterpriseNames = node.withArray("entrusted_enterprises");
            JsonNode enterpriseIds = node.withArray("entrusted_enterprise_ids");
            for (int index = 0; index < enterpriseNames.size(); index++) {
                entrusted.add(new EntrustedEnterpriseCommand(
                        enterpriseIds.get(index).asLong(), enterpriseNames.get(index).asText()));
            }
            rows.add(new RowCommand(
                    node.path("source_row").asInt(),
                    node.path("owner_company_id").asLong(),
                    node.path("product_master_id").asLong(),
                    node.path("project_code_id").isNull() ? null : node.path("project_code_id").asLong(),
                    node.path("product_name").asText(),
                    node.path("registrant_name").asText(),
                    node.path("certificate_no").asText(),
                    LocalDate.parse(node.path("first_obtained_date").asText()),
                    LocalDate.parse(node.path("approval_date").asText()),
                    LocalDate.parse(node.path("effective_date").asText()),
                    LocalDate.parse(node.path("expiry_date").asText()),
                    node.path("classification").asText(),
                    "", "", "", "", "", "",
                    node.path("entrusted_production").asBoolean(),
                    node.path("self_production").asBoolean(),
                    entrusted,
                    jsonStrings(node.withArray("restricted_reasons"))));
        }
        return new BatchCommand(TENANT_ID, ACTOR_ID, SOURCE_HASH,
                "regcert-excel-sync-20260824", rows);
    }

    private List<String> jsonStrings(JsonNode array) {
        List<String> result = new ArrayList<>();
        array.forEach(node -> result.add(node.asText()));
        return result;
    }

    private int countSourceAudits() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dcc_registration_certificate_audit "
                        + "WHERE tenant_id = ? AND event_key LIKE ?", Integer.class,
                TENANT_ID, "HISTORICAL_IMPORT:" + SOURCE_HASH + ":%");
    }
}
