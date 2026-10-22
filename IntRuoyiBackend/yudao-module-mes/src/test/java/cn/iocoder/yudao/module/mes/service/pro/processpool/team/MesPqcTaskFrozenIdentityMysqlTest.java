package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/** Dalton-only, real MySQL and unmodified full Mapper projection. No schema creation. */
class MesPqcTaskFrozenIdentityMysqlTest {
    private static final String CONTAINER = "baaa88c01dd09ad427646c577547a0ed0efd0258e45e5367e4677a34573b2da9";
    private static SqlSessionTemplate sessions;
    private static MesPqcInspectionTaskMapper mapper;
    private static TransactionTemplate transactions;
    private static JdbcTemplate jdbc;
    private static JsonNode fixture;
    private static long tenant;

    @BeforeAll
    static void openFormalFixture() throws Exception {
        String manifest = System.getProperty("pqt.mysql.fixture");
        assertNotNull(manifest, "PREFLIGHT: pqt.mysql.fixture required, no skipped tests");
        fixture = JsonUtils.parseTree(Files.readString(Path.of(manifest), StandardCharsets.UTF_8));
        assertEquals("gxp-pqt-mapper-round3", fixture.path("owner").asText());
        assertEquals("gxp_pqt_schema_round3", fixture.path("database").asText());
        tenant = fixture.path("tenantId").asLong();
        assertTrue(tenant > 1, "PREFLIGHT: dedicated tenant required");
        assertTrue(fixture.path("insertId").asLong() > 0);
        assertTrue(fixture.path("historyNullId").asLong() > 0);
        assertTrue(fixture.path("historyValueId").asLong() > 0);
        assertNotEquals(fixture.path("historyNullId").asLong(), fixture.path("historyValueId").asLong());
        for (String key : List.of("formalDdlEvidence", "historyBeforeEvidence", "migrationReplayEvidence")) {
            assertFalse(fixture.path(key).asText().isBlank(), "PREFLIGHT: missing " + key);
            assertTrue(Files.isRegularFile(Path.of(fixture.path(key).asText())), "PREFLIGHT: absent " + key);
        }
        assertEquals(CONTAINER, docker("inspect", "--format", "{{.Id}}", CONTAINER));
        assertEquals("/gxp-integration-mysql-round2-dalton-jdbc",
                docker("inspect", "--format", "{{.Name}}", CONTAINER));
        assertEquals("gxp-integration-dalton-m9-round2",
                docker("inspect", "--format", "{{.Config.Labels.owner}}", CONTAINER));
        assertEquals("true", docker("inspect", "--format", "{{.State.Running}}", CONTAINER));
        assertEquals("127.0.0.1:59241", docker("port", CONTAINER, "3306/tcp"));
        String credential = docker("exec", CONTAINER, "cat", "/run/m9/root-password");
        assertFalse(credential.isEmpty(), "PREFLIGHT: empty credential");
        var source = new DriverManagerDataSource("jdbc:mysql://127.0.0.1:59241/gxp_pqt_schema_round3"
                + "?useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=5000&socketTimeout=15000"
                + "&characterEncoding=UTF-8", "root", credential);
        jdbc = new JdbcTemplate(source);
        assertEquals("gxp_pqt_schema_round3", jdbc.queryForObject("SELECT DATABASE()", String.class));
        assertTrue(Objects.requireNonNull(jdbc.queryForObject("SELECT VERSION()", String.class)).startsWith("8."));
        var config = new MybatisConfiguration();
        config.setMapUnderscoreToCamelCase(true);
        var global = new GlobalConfig();
        global.setDbConfig(new GlobalConfig.DbConfig().setIdType(IdType.AUTO));
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(source);
        factory.setConfiguration(config);
        factory.setGlobalConfig(global);
        factory.setTransactionFactory(new SpringManagedTransactionFactory());
        var sqlFactory = Objects.requireNonNull(factory.getObject());
        sqlFactory.getConfiguration().addMapper(MesPqcInspectionTaskMapper.class);
        sessions = new SqlSessionTemplate(sqlFactory);
        mapper = sessions.getMapper(MesPqcInspectionTaskMapper.class);
        transactions = new TransactionTemplate(new DataSourceTransactionManager(source));
        var physical = new HashSet<>(jdbc.queryForList("SELECT column_name FROM information_schema.columns "
                + "WHERE table_schema=DATABASE() AND table_name='mes_pqc_inspection_task'", String.class));
        var table = TableInfoHelper.getTableInfo(MesPqcInspectionTaskDO.class);
        assertTrue(physical.contains(table.getKeyColumn()), "PREFLIGHT: missing key column");
        for (var field : table.getFieldList()) {
            assertTrue(physical.contains(field.getColumn()), "PREFLIGHT: full Mapper column missing: " + field.getColumn());
        }
        assertNull(mapper.selectById(fixture.path("insertId").asLong()), "PREFLIGHT: insert ID occupied");
    }

    @Test
    void m01InsertAndFullReadPreserveNullEmptyUnicodeAndBoundaries() {
        for (int sample = 0; sample < 3; sample++) {
            final int variant = sample;
            rollback(task -> {
                if (variant == 1) task.setDccProjectCode("").setDccProjectName("").setQaRegulationVersionNo("");
                if (variant == 2) task.setDccProjectCodeId(Long.MAX_VALUE).setQaRegulationId(Long.MAX_VALUE - 1)
                        .setDccProjectCode("测".repeat(64)).setDccProjectName("😀".repeat(255))
                        .setQaRegulationVersionNo("版".repeat(32));
                assertEquals(1, mapper.insert(task));
                sessions.clearCache();
                sameFive(task, mapper.selectById(task.getId()));
                var rows = mapper.selectListByActiveOrderId(task.getActiveOrderId());
                sameFive(task, rows.stream().filter(row -> task.getId().equals(row.getId())).findFirst().orElseThrow());
            });
        }
    }

    @Test
    void m02RealTransactionalLocksReadAllFiveAndOverlengthInsertFails() {
        rollback(task -> {
            task.setDccProjectCodeId(71L).setQaRegulationId(72L).setDccProjectCode(" 冻结 ")
                    .setDccProjectName("历史😀").setQaRegulationVersionNo("V/1");
            assertEquals(1, mapper.insert(task));
            sessions.clearCache();
            sameFive(task, mapper.selectByIdForUpdate(task.getId()));
            sameFive(task, mapper.selectListByActiveOrderIdForUpdate(task.getActiveOrderId()).stream()
                    .filter(row -> task.getId().equals(row.getId())).findFirst().orElseThrow());
        });
        for (int column = 0; column < 3; column++) {
            final int field = column;
            rollback(task -> {
                // Require the actual connection mode, rather than silently enabling permissive truncation.
                String mode = Objects.requireNonNull(jdbc.queryForObject("SELECT @@SESSION.sql_mode", String.class));
                assertTrue(mode.contains("STRICT_ALL_TABLES") || mode.contains("STRICT_TRANS_TABLES"),
                        "PREFLIGHT: strict SQL mode required");
                if (field == 0) task.setDccProjectCode("测".repeat(65));
                if (field == 1) task.setDccProjectName("测".repeat(256));
                if (field == 2) task.setQaRegulationVersionNo("测".repeat(33));
                RuntimeException error = assertThrows(RuntimeException.class, () -> mapper.insert(task));
                Throwable cause = error;
                while (!(cause instanceof SQLException) && cause.getCause() != null) cause = cause.getCause();
                assertInstanceOf(SQLException.class, cause);
                assertEquals(1406, ((SQLException) cause).getErrorCode());
                sessions.clearCache();
                assertNull(mapper.selectById(task.getId()));
            });
        }
    }

    @Test
    void m03PreMigrationHistoryAndCompatibleValuesSurviveFormalReplay() {
        var unknown = mapper.selectById(fixture.path("historyNullId").asLong());
        assertNotNull(unknown);
        assertEquals(tenant, unknown.getTenantId());
        sameFive(new MesPqcInspectionTaskDO(), unknown);
        var history = mapper.selectById(fixture.path("historyValueId").asLong());
        assertNotNull(history);
        assertEquals(tenant, history.getTenantId());
        JsonNode expected = fixture.path("historyValueBefore");
        for (String key : List.of("dccProjectCodeId", "dccProjectCode", "dccProjectName", "qaRegulationId", "qaRegulationVersionNo")) {
            assertTrue(expected.hasNonNull(key), "PREFLIGHT: actual pre-replay nonNULL evidence required: " + key);
        }
        var before = new MesPqcInspectionTaskDO().setDccProjectCodeId(expected.get("dccProjectCodeId").longValue())
                .setDccProjectCode(expected.get("dccProjectCode").textValue())
                .setDccProjectName(expected.get("dccProjectName").textValue())
                .setQaRegulationId(expected.get("qaRegulationId").longValue())
                .setQaRegulationVersionNo(expected.get("qaRegulationVersionNo").textValue());
        sameFive(before, history);
    }

    private static void rollback(Consumer<MesPqcInspectionTaskDO> assertion) {
        transactions.executeWithoutResult(status -> {
            status.setRollbackOnly();
            var task = new MesPqcInspectionTaskDO().setId(fixture.path("insertId").asLong())
                    .setActiveOrderId(fixture.path("insertId").asLong()).setWorkOrderId(2L).setRouteId(3L)
                    .setRouteVersionId(4L).setRouteProcessId(5L).setProcessId(6L).setQaProcessId(7L)
                    .setQaItemCode("PQT-MAPPER").setRegulationVersionId(8L).setInspectionType("FIRST")
                    .setInspectionRuleKey("FIRST").setBusinessDate(LocalDate.of(2026, 9, 29))
                    .setShiftCode("FIRST").setRoundNo(1).setPlannedInspectionQuantity(1)
                    .setActualInspectionQuantity(0).setTaskStatus("PENDING").setSimulated(false);
            task.setTenantId(tenant);
            task.setDeleted(false);
            assertion.accept(task);
        });
        sessions.clearCache();
        assertNull(mapper.selectById(fixture.path("insertId").asLong()), "Rollback must leave no test insert");
    }

    private static void sameFive(MesPqcInspectionTaskDO expected, MesPqcInspectionTaskDO actual) {
        assertNotNull(actual);
        assertEquals(expected.getDccProjectCodeId(), actual.getDccProjectCodeId());
        assertEquals(expected.getDccProjectCode(), actual.getDccProjectCode());
        assertEquals(expected.getDccProjectName(), actual.getDccProjectName());
        assertEquals(expected.getQaRegulationId(), actual.getQaRegulationId());
        assertEquals(expected.getQaRegulationVersionNo(), actual.getQaRegulationVersionNo());
    }

    private static String docker(String... args) throws Exception {
        var command = new ArrayList<>(List.of("docker"));
        command.addAll(List.of(args));
        var process = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        if (!process.waitFor(10, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException("PREFLIGHT: dedicated container query timeout");
        }
        assertEquals(0, process.exitValue(), "PREFLIGHT: container query failed; credential output withheld");
        return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
    }
}
