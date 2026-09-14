package cn.iocoder.yudao.module.mes.productionrelease.core;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesReleaseFlowSchemaContractTest {

    @Test
    void migrationMustFreezeApplicationCasAndPqcTaskUniqueness() throws Exception {
        String sql = Files.readString(resolveBackendPath(
                "sql/mysql/20260814_mes_production_release_flow.sql"), StandardCharsets.UTF_8);

        assertTrue(sql.startsWith("-- release-migration: allowedEnvironments=test,backup,prod; "
                + "dependsOn=20260808_mes_active_order_release_application; type=schema; riskLevel=high"));
        assertTrue(sql.contains("LEGACY_RELEASE_APPLICATION_MIGRATION_REQUIRED"));
        assertTrue(sql.contains("LEGACY_BATCH_EXECUTION_MIGRATION_REQUIRED"));
        assertTrue(sql.contains("`pqc_release_work_task_id` bigint NULL"));
        assertTrue(sql.contains("`pqc_decision` varchar(32) NULL"));
        assertTrue(sql.contains("`pqc_decided_by` bigint NULL"));
        assertTrue(sql.contains("`pqc_decided_at` datetime NULL"));
        assertTrue(sql.contains("`pqc_reject_reason` varchar(500) NULL"));
        assertTrue(sql.contains("`report_snapshot_hash` varchar(128) NULL"));
        assertTrue(sql.contains("`version` int NOT NULL DEFAULT 1"));
        assertTrue(sql.contains("`uk_mes_pp_release_pqc_task`"));
        assertTrue(sql.contains("`tenant_id`, `pqc_release_work_task_id`, `deleted`"));
        assertTrue(sql.contains("`uk_mes_pp_release_batch_execution`"));
        assertTrue(sql.contains("`tenant_id`, `batch_execution_id`, `deleted`"));
        assertTrue(sql.contains("MODIFY COLUMN `batch_execution_id` bigint NULL"));
        assertTrue(sql.contains("`pqc_release_application_scope_id` bigint"));
        assertTrue(sql.contains("business_scope_type = 'RELEASE_APPLICATION'"));
        assertTrue(sql.contains("task_type = 'PQC_PRODUCTION_RELEASE'"));
        assertTrue(sql.contains("`uk_mes_edhr_work_task_release_application`"));
        assertTrue(sql.contains("information_schema.columns"));
        assertTrue(sql.contains("information_schema.statistics"));
        assertTrue(sql.contains("SIGNAL SQLSTATE '45000'"));
    }

    @Test
    void dataObjectsAndMapperMustExposeExactSharedContract() throws Exception {
        assertField(MesProcessPoolActiveOrderReleaseApplicationDO.class,
                "pqcReleaseWorkTaskId", Long.class);
        assertField(MesProcessPoolActiveOrderReleaseApplicationDO.class,
                "pqcDecision", String.class);
        assertField(MesProcessPoolActiveOrderReleaseApplicationDO.class,
                "pqcDecidedBy", Long.class);
        assertField(MesProcessPoolActiveOrderReleaseApplicationDO.class,
                "pqcDecidedAt", java.time.LocalDateTime.class);
        assertField(MesProcessPoolActiveOrderReleaseApplicationDO.class,
                "pqcRejectReason", String.class);
        assertField(MesProcessPoolActiveOrderReleaseApplicationDO.class,
                "reportSnapshotHash", String.class);
        assertField(MesProcessPoolActiveOrderReleaseApplicationDO.class,
                "version", Integer.class);
        assertField(MesProEdhrWorkTaskDO.class,
                "pqcReleaseApplicationScopeId", Long.class);

        Method lock = MesProcessPoolActiveOrderReleaseApplicationMapper.class
                .getMethod("selectByIdForUpdate", Long.class);
        String selectSql = lock.getAnnotation(Select.class).value()[0];
        assertTrue(selectSql.contains("FOR UPDATE"));

        Method cas = MesProcessPoolActiveOrderReleaseApplicationMapper.class
                .getMethod("compareAndSetStatus", Long.class, Integer.class, String.class, String.class);
        String updateSql = String.join(" ", cas.getAnnotation(Update.class).value());
        assertTrue(updateSql.contains("version = version + 1"));
        assertTrue(updateSql.contains("version = #{expectedVersion}"));
        assertTrue(updateSql.contains("application_status = #{expectedStatus}"));
    }

    private static void assertField(Class<?> type, String name, Class<?> fieldType) throws Exception {
        Field field = type.getDeclaredField(name);
        assertEquals(fieldType, field.getType());
    }

    private static Path resolveBackendPath(String relative) {
        Path current = Path.of("").toAbsolutePath().normalize();
        for (Path candidate = current; candidate != null; candidate = candidate.getParent()) {
            Path direct = candidate.resolve(relative);
            if (Files.exists(direct)) {
                return direct;
            }
            Path nested = candidate.resolve("IntRuoyiBackend").resolve(relative);
            if (Files.exists(nested)) {
                return nested;
            }
        }
        throw new IllegalStateException("Unable to locate backend path " + relative + " from " + current);
    }
}
