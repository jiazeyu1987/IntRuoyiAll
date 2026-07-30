package cn.iocoder.yudao.module.mes;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolDefectReasonDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolDeviceParameterRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamEmployeeBindingDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolWorkOrderAbnormalDO;
import com.baomidou.mybatisplus.annotation.TableName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProcessPoolTeamLeaderSchemaTest {

    @Test
    void shouldCreateTeamLeaderWorkbenchAndMaintenanceTables() throws Exception {
        assertEquals("mes_pro_process_pool_team_leader_scope", tableName(MesProcessPoolTeamLeaderScopeDO.class));
        assertEquals("mes_pro_process_pool_submission_review", tableName(MesProcessPoolSubmissionReviewDO.class));
        assertEquals("mes_pro_process_pool_work_order_abnormal", tableName(MesProcessPoolWorkOrderAbnormalDO.class));
        assertEquals("mes_pro_process_pool_team_employee_binding", tableName(MesProcessPoolTeamEmployeeBindingDO.class));
        assertEquals("mes_pro_process_pool_defect_reason", tableName(MesProcessPoolDefectReasonDO.class));
        assertEquals("mes_pro_process_pool_device_parameter_rule", tableName(MesProcessPoolDeviceParameterRuleDO.class));
        assertEquals("mes_pro_process_pool_team_maintenance_audit", tableName(MesProcessPoolTeamMaintenanceAuditDO.class));

        assertField(MesProcessPoolTeamLeaderScopeDO.class, "leaderUserId", Long.class);
        assertField(MesProcessPoolTeamLeaderScopeDO.class, "leaderType", String.class);
        assertField(MesProcessPoolTeamLeaderScopeDO.class, "scopeType", String.class);
        assertField(MesProcessPoolTeamLeaderScopeDO.class, "employeeUserId", Long.class);
        assertField(MesProcessPoolTeamLeaderScopeDO.class, "processId", Long.class);
        assertField(MesProcessPoolTeamLeaderScopeDO.class, "workstationId", Long.class);
        assertField(MesProcessPoolTeamLeaderScopeDO.class, "enabled", Boolean.class);

        assertField(MesProcessPoolSubmissionReviewDO.class, "eventId", Long.class);
        assertField(MesProcessPoolSubmissionReviewDO.class, "leaderUserId", Long.class);
        assertField(MesProcessPoolSubmissionReviewDO.class, "reviewStatus", String.class);
        assertField(MesProcessPoolSubmissionReviewDO.class, "reviewRemark", String.class);
        assertField(MesProcessPoolSubmissionReviewDO.class, "reviewedAt", LocalDateTime.class);

        assertField(MesProcessPoolWorkOrderAbnormalDO.class, "workOrderId", Long.class);
        assertField(MesProcessPoolWorkOrderAbnormalDO.class, "sourceEventId", Long.class);
        assertField(MesProcessPoolWorkOrderAbnormalDO.class, "abnormalReasonCode", String.class);
        assertField(MesProcessPoolWorkOrderAbnormalDO.class, "abnormalDescription", String.class);
        assertField(MesProcessPoolWorkOrderAbnormalDO.class, "reportStatus", String.class);
        assertField(MesProcessPoolWorkOrderAbnormalDO.class, "markerUserId", Long.class);
        assertField(MesProcessPoolWorkOrderAbnormalDO.class, "reportedAt", LocalDateTime.class);

        assertField(MesProcessPoolTeamEmployeeBindingDO.class, "leaderUserId", Long.class);
        assertField(MesProcessPoolTeamEmployeeBindingDO.class, "processId", Long.class);
        assertField(MesProcessPoolTeamEmployeeBindingDO.class, "employeeUserId", Long.class);
        assertField(MesProcessPoolTeamEmployeeBindingDO.class, "enabled", Boolean.class);
        assertField(MesProcessPoolTeamEmployeeBindingDO.class, "disabledAt", LocalDateTime.class);

        assertField(MesProcessPoolDefectReasonDO.class, "leaderUserId", Long.class);
        assertField(MesProcessPoolDefectReasonDO.class, "reasonType", String.class);
        assertField(MesProcessPoolDefectReasonDO.class, "reasonCode", String.class);
        assertField(MesProcessPoolDefectReasonDO.class, "reasonName", String.class);
        assertField(MesProcessPoolDefectReasonDO.class, "processId", Long.class);
        assertField(MesProcessPoolDefectReasonDO.class, "enabled", Boolean.class);

        assertField(MesProcessPoolDeviceParameterRuleDO.class, "leaderUserId", Long.class);
        assertField(MesProcessPoolDeviceParameterRuleDO.class, "processId", Long.class);
        assertField(MesProcessPoolDeviceParameterRuleDO.class, "deviceId", Long.class);
        assertField(MesProcessPoolDeviceParameterRuleDO.class, "parameterCode", String.class);
        assertField(MesProcessPoolDeviceParameterRuleDO.class, "lowerLimit", BigDecimal.class);
        assertField(MesProcessPoolDeviceParameterRuleDO.class, "upperLimit", BigDecimal.class);
        assertField(MesProcessPoolDeviceParameterRuleDO.class, "enabled", Boolean.class);

        assertField(MesProcessPoolTeamMaintenanceAuditDO.class, "leaderUserId", Long.class);
        assertField(MesProcessPoolTeamMaintenanceAuditDO.class, "actionType", String.class);
        assertField(MesProcessPoolTeamMaintenanceAuditDO.class, "targetType", String.class);
        assertField(MesProcessPoolTeamMaintenanceAuditDO.class, "targetId", Long.class);
        assertField(MesProcessPoolTeamMaintenanceAuditDO.class, "auditTime", LocalDateTime.class);

        String sql = Files.readString(resolveBackendPath("sql/mysql/20260730_mes_process_pool_team_leader.sql"),
                StandardCharsets.UTF_8);
        String normalizedSql = sql.replace("\r\n", "\n");
        assertTrue(normalizedSql.startsWith("-- release-migration: allowedEnvironments=test,backup,prod; "
                + "dependsOn=20260730_mes_process_pool_review_copy; type=schema; riskLevel=medium\n"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_team_leader_scope`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_submission_review`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_work_order_abnormal`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_team_employee_binding`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_defect_reason`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_device_parameter_rule`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_team_maintenance_audit`"));
        assertTrue(sql.contains("KEY `idx_mes_pp_tl_scope_employee` (`tenant_id`, `leader_user_id`, `employee_user_id`)"));
        assertTrue(sql.contains("KEY `idx_mes_pp_review_event` (`tenant_id`, `event_id`)"));
        assertTrue(sql.contains("KEY `idx_mes_pp_abnormal_work_order` (`tenant_id`, `work_order_id`, `report_status`)"));
        assertTrue(sql.contains("UNIQUE KEY `uk_mes_pp_team_employee_binding` (`tenant_id`, `leader_user_id`, `process_id`, `employee_user_id`, `deleted`)"));
        assertTrue(sql.contains("UNIQUE KEY `uk_mes_pp_defect_reason` (`tenant_id`, `leader_user_id`, `reason_type`, `reason_code`, `process_id`, `deleted`)"));
        assertTrue(sql.contains("UNIQUE KEY `uk_mes_pp_device_parameter_rule` (`tenant_id`, `process_id`, `device_id`, `parameter_code`, `deleted`)"));
        assertTrue(sql.contains("'工序池班组长工作台', 'mes:pro-process-pool-team-leader:query', 2"));
        assertTrue(sql.contains("'班组长提交复核', 'mes:pro-process-pool-team-leader:review'"));
        assertTrue(sql.contains("'生产工单异常上报', 'mes:pro-process-pool-team-leader:abnormal'"));
        assertTrue(sql.contains("'班组基础维护', 'mes:pro-process-pool-team-leader:maintain'"));
        assertTrue(sql.contains("JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('5700' AS JSON), '$')"));
        assertTrue(sql.contains("`role`.`code` = 'tenant_admin'"));
        assertFalse(sql.contains("mes_pro_feedback_surplus_pool"),
                "F9/F10 must extend process-pool governance and must not reuse the surplus pool");
    }

    private static String tableName(Class<?> clazz) {
        return clazz.getAnnotation(TableName.class).value();
    }

    private static void assertField(Class<?> clazz, String name, Class<?> type) throws Exception {
        Field field = clazz.getDeclaredField(name);
        assertEquals(type, field.getType(), clazz.getSimpleName() + "." + name);
    }

    private static Path resolveBackendPath(String relative) {
        Path cwd = Paths.get("").toAbsolutePath();
        if ("yudao-module-mes".equals(cwd.getFileName().toString())) {
            return cwd.getParent().resolve(relative);
        }
        return cwd.resolve(relative);
    }
}
