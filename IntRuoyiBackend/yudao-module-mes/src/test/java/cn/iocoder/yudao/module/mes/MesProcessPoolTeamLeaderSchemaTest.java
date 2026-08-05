package cn.iocoder.yudao.module.mes;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolDefectReasonDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolDeviceParameterRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolOrderProcessCompletionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamDeviceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamEmployeeBindingDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamEmployeeProfileDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamProcessDeviceDO;
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
    void activeOrderAuthoritySchemaMustFreezeRouteVersionQuantityStatusAndOptimisticLock() throws Exception {
        assertField(MesProcessPoolActiveOrderDO.class, "routeId", Long.class);
        assertField(MesProcessPoolActiveOrderDO.class, "routeVersionId", Long.class);
        assertField(MesProcessPoolActiveOrderDO.class, "erpFixedQuantitySnapshot", BigDecimal.class);
        assertField(MesProcessPoolActiveOrderDO.class, "businessStatus", String.class);
        assertField(MesProcessPoolActiveOrderDO.class, "version", Integer.class);

        String sql = Files.readString(resolveBackendPath(
                "sql/mysql/20260802_mes_process_pool_active_order_authority.sql"), StandardCharsets.UTF_8);
        assertTrue(sql.contains("dependsOn=20260801_mes_process_pool_team_leader_p4_order_completion_backfill"));
        assertTrue(sql.contains("`route_id` bigint NOT NULL COMMENT '正式工艺路线ID'"));
        assertTrue(sql.contains("`route_version_id` bigint NOT NULL COMMENT '正式工艺路线版本ID'"));
        assertTrue(sql.contains("`erp_fixed_quantity_snapshot` decimal(24,6) NOT NULL COMMENT 'ERP固定生产数量快照'"));
        assertTrue(sql.contains("`business_status` varchar(32) NOT NULL COMMENT '跨角色活跃订单业务状态：ACTIVE/REMOVED/TERMINATED/COMPLETED'"));
        assertTrue(sql.contains("`version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本'"));
        assertTrue(sql.contains("UNIQUE KEY `uk_mes_pp_active_order` (`tenant_id`, `work_order_id`, `route_id`, `route_version_id`, `deleted`)"));
        assertFalse(sql.contains("UNIQUE KEY `uk_mes_pp_active_order` (`tenant_id`, `leader_user_id`, `work_order_id`, `deleted`)"));
    }

    @Test
    void activeOrderProcessSnapshotSchemaMustFreezePerProcessProductionTarget() throws Exception {
        Class<?> snapshotClass = Class.forName(
                "cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO");
        assertEquals("mes_pro_process_pool_active_order_process_snapshot", tableName(snapshotClass));
        assertField(snapshotClass, "activeOrderId", Long.class);
        assertField(snapshotClass, "workOrderId", Long.class);
        assertField(snapshotClass, "routeId", Long.class);
        assertField(snapshotClass, "routeVersionId", Long.class);
        assertField(snapshotClass, "routeProcessId", Long.class);
        assertField(snapshotClass, "processId", Long.class);
        assertField(snapshotClass, "erpFixedQuantitySnapshot", BigDecimal.class);
        assertField(snapshotClass, "productionQuantityFactorSnapshot", BigDecimal.class);
        assertField(snapshotClass, "plannedQuantitySnapshot", BigDecimal.class);

        String sql = Files.readString(resolveBackendPath(
                "sql/mysql/20260802_mes_process_pool_active_order_process_snapshot.sql"), StandardCharsets.UTF_8);
        assertTrue(sql.contains("dependsOn=20260802_mes_process_pool_active_order_authority"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_active_order_process_snapshot`"));
        assertTrue(sql.contains("`production_quantity_factor_snapshot` decimal(24,6) NOT NULL COMMENT '生产数量系数快照'"));
        assertTrue(sql.contains("`planned_quantity_snapshot` decimal(24,6) NOT NULL COMMENT '工序目标数量快照'"));
        assertTrue(sql.contains("UNIQUE KEY `uk_mes_pp_active_order_process_snapshot` (`tenant_id`, `active_order_id`, `route_process_id`, `process_id`, `deleted`)"));
    }

    @Test
    void shouldCreateTeamLeaderWorkbenchAndMaintenanceTables() throws Exception {
        assertEquals("mes_pro_process_pool_team_leader_scope", tableName(MesProcessPoolTeamLeaderScopeDO.class));
        assertEquals("mes_pro_process_pool_submission_review", tableName(MesProcessPoolSubmissionReviewDO.class));
        assertEquals("mes_pro_process_pool_work_order_abnormal", tableName(MesProcessPoolWorkOrderAbnormalDO.class));
        assertEquals("mes_pro_process_pool_team_employee_binding", tableName(MesProcessPoolTeamEmployeeBindingDO.class));
        assertEquals("mes_pro_process_pool_defect_reason", tableName(MesProcessPoolDefectReasonDO.class));
        assertEquals("mes_pro_process_pool_device_parameter_rule", tableName(MesProcessPoolDeviceParameterRuleDO.class));
        assertEquals("mes_pro_process_pool_team_maintenance_audit", tableName(MesProcessPoolTeamMaintenanceAuditDO.class));
        assertEquals("mes_pro_process_pool_active_order", tableName(MesProcessPoolActiveOrderDO.class));
        assertEquals("mes_pro_process_pool_team_employee_profile", tableName(MesProcessPoolTeamEmployeeProfileDO.class));
        assertEquals("mes_pro_process_pool_team_device", tableName(MesProcessPoolTeamDeviceDO.class));
        assertEquals("mes_pro_process_pool_team_process_device", tableName(MesProcessPoolTeamProcessDeviceDO.class));
        assertEquals("mes_pro_process_pool_report_allocation", tableName(MesProcessPoolReportAllocationDO.class));
        assertEquals("mes_pro_process_pool_order_process_completion", tableName(MesProcessPoolOrderProcessCompletionDO.class));

        assertField(MesProcessPoolTeamLeaderScopeDO.class, "leaderUserId", Long.class);
        assertField(MesProcessPoolTeamLeaderScopeDO.class, "leaderType", String.class);
        assertField(MesProcessPoolTeamLeaderScopeDO.class, "scopeType", String.class);
        assertField(MesProcessPoolTeamLeaderScopeDO.class, "employeeUserId", Long.class);
        assertField(MesProcessPoolTeamLeaderScopeDO.class, "processId", Long.class);
        assertField(MesProcessPoolTeamLeaderScopeDO.class, "workstationId", Long.class);
        assertField(MesProcessPoolTeamLeaderScopeDO.class, "productionLineId", Long.class);
        assertField(MesProcessPoolTeamLeaderScopeDO.class, "equipmentId", Long.class);
        assertField(MesProcessPoolTeamLeaderScopeDO.class, "workOrderId", Long.class);
        assertField(MesProcessPoolTeamLeaderScopeDO.class, "enabled", Boolean.class);

        assertField(MesProcessPoolSubmissionReviewDO.class, "eventId", Long.class);
        assertField(MesProcessPoolSubmissionReviewDO.class, "leaderUserId", Long.class);
        assertField(MesProcessPoolSubmissionReviewDO.class, "leaderType", String.class);
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
        assertField(MesProcessPoolTeamEmployeeBindingDO.class, "employeeProfileId", Long.class);
        assertField(MesProcessPoolTeamEmployeeBindingDO.class, "employeeUserId", Long.class);
        assertField(MesProcessPoolTeamEmployeeBindingDO.class, "enabled", Boolean.class);
        assertField(MesProcessPoolTeamEmployeeBindingDO.class, "disabledAt", LocalDateTime.class);

        assertField(MesProcessPoolDefectReasonDO.class, "leaderUserId", Long.class);
        assertField(MesProcessPoolDefectReasonDO.class, "reasonType", String.class);
        assertField(MesProcessPoolDefectReasonDO.class, "reasonCode", String.class);
        assertField(MesProcessPoolDefectReasonDO.class, "reasonName", String.class);
        assertField(MesProcessPoolDefectReasonDO.class, "routeProcessId", Long.class);
        assertField(MesProcessPoolDefectReasonDO.class, "processId", Long.class);
        assertField(MesProcessPoolDefectReasonDO.class, "enabled", Boolean.class);

        assertField(MesProcessPoolDeviceParameterRuleDO.class, "leaderUserId", Long.class);
        assertField(MesProcessPoolDeviceParameterRuleDO.class, "processId", Long.class);
        assertField(MesProcessPoolDeviceParameterRuleDO.class, "deviceId", Long.class);
        assertField(MesProcessPoolDeviceParameterRuleDO.class, "parameterCode", String.class);
        assertField(MesProcessPoolDeviceParameterRuleDO.class, "unit", String.class);
        assertField(MesProcessPoolDeviceParameterRuleDO.class, "lowerLimit", BigDecimal.class);
        assertField(MesProcessPoolDeviceParameterRuleDO.class, "upperLimit", BigDecimal.class);
        assertField(MesProcessPoolDeviceParameterRuleDO.class, "defaultValue", BigDecimal.class);
        assertField(MesProcessPoolDeviceParameterRuleDO.class, "enabled", Boolean.class);

        assertField(MesProcessPoolActiveOrderDO.class, "leaderUserId", Long.class);
        assertField(MesProcessPoolActiveOrderDO.class, "workOrderId", Long.class);
        assertField(MesProcessPoolActiveOrderDO.class, "activeStatus", String.class);
        assertField(MesProcessPoolActiveOrderDO.class, "joinedAt", LocalDateTime.class);
        assertField(MesProcessPoolActiveOrderDO.class, "removedAt", LocalDateTime.class);

        assertField(MesProcessPoolTeamEmployeeProfileDO.class, "leaderUserId", Long.class);
        assertField(MesProcessPoolTeamEmployeeProfileDO.class, "systemUserId", Long.class);
        assertField(MesProcessPoolTeamEmployeeProfileDO.class, "employeeCode", String.class);
        assertField(MesProcessPoolTeamEmployeeProfileDO.class, "employeeName", String.class);
        assertField(MesProcessPoolTeamEmployeeProfileDO.class, "employeeType", String.class);
        assertField(MesProcessPoolTeamEmployeeProfileDO.class, "enabled", Boolean.class);

        assertField(MesProcessPoolTeamDeviceDO.class, "leaderUserId", Long.class);
        assertField(MesProcessPoolTeamDeviceDO.class, "deviceCode", String.class);
        assertField(MesProcessPoolTeamDeviceDO.class, "deviceName", String.class);
        assertField(MesProcessPoolTeamDeviceDO.class, "deviceStatus", String.class);
        assertField(MesProcessPoolTeamDeviceDO.class, "enabled", Boolean.class);
        assertField(MesProcessPoolTeamDeviceDO.class, "statusChangedAt", LocalDateTime.class);

        assertField(MesProcessPoolTeamProcessDeviceDO.class, "leaderUserId", Long.class);
        assertField(MesProcessPoolTeamProcessDeviceDO.class, "processId", Long.class);
        assertField(MesProcessPoolTeamProcessDeviceDO.class, "deviceId", Long.class);
        assertField(MesProcessPoolTeamProcessDeviceDO.class, "enabled", Boolean.class);

        assertField(MesProcessPoolReportAllocationDO.class, "eventId", Long.class);
        assertField(MesProcessPoolReportAllocationDO.class, "reviewId", Long.class);
        assertField(MesProcessPoolReportAllocationDO.class, "leaderUserId", Long.class);
        assertField(MesProcessPoolReportAllocationDO.class, "activeOrderId", Long.class);
        assertField(MesProcessPoolReportAllocationDO.class, "workOrderId", Long.class);
        assertField(MesProcessPoolReportAllocationDO.class, "routeProcessId", Long.class);
        assertField(MesProcessPoolReportAllocationDO.class, "processId", Long.class);
        assertField(MesProcessPoolReportAllocationDO.class, "allocatedQuantity", BigDecimal.class);
        assertField(MesProcessPoolReportAllocationDO.class, "allocationMode", String.class);
        assertField(MesProcessPoolReportAllocationDO.class, "confirmedAt", LocalDateTime.class);

        assertField(MesProcessPoolOrderProcessCompletionDO.class, "workOrderId", Long.class);
        assertField(MesProcessPoolOrderProcessCompletionDO.class, "routeProcessId", Long.class);
        assertField(MesProcessPoolOrderProcessCompletionDO.class, "processId", Long.class);
        assertField(MesProcessPoolOrderProcessCompletionDO.class, "targetQuantity", BigDecimal.class);
        assertField(MesProcessPoolOrderProcessCompletionDO.class, "confirmedQuantity", BigDecimal.class);
        assertField(MesProcessPoolOrderProcessCompletionDO.class, "completionStatus", String.class);
        assertField(MesProcessPoolOrderProcessCompletionDO.class, "completedAt", LocalDateTime.class);
        assertField(MesProcessPoolOrderProcessCompletionDO.class, "backfillStatus", String.class);
        assertField(MesProcessPoolOrderProcessCompletionDO.class, "backfillExecutionId", Long.class);
        assertField(MesProcessPoolOrderProcessCompletionDO.class, "backfillError", String.class);
        assertField(MesProcessPoolOrderProcessCompletionDO.class, "lastEventId", Long.class);
        assertField(MesProcessPoolOrderProcessCompletionDO.class, "lastReviewId", Long.class);
        assertField(MesProcessPoolOrderProcessCompletionDO.class, "sourceEventIdsJson", String.class);
        assertField(MesProcessPoolOrderProcessCompletionDO.class, "sourceAllocationIdsJson", String.class);
        assertField(MesProcessPoolOrderProcessCompletionDO.class, "aggregateHash", String.class);
        assertField(MesProcessPoolOrderProcessCompletionDO.class, "backfillIdempotencyKey", String.class);

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
        assertTrue(sql.contains("负责范围类型：EMPLOYEE/PROCESS/WORKSTATION/PRODUCTION_LINE/EQUIPMENT/ORDER"));
        assertTrue(sql.contains("`production_line_id` bigint DEFAULT NULL COMMENT '负责生产线ID'"));
        assertTrue(sql.contains("`equipment_id` bigint DEFAULT NULL COMMENT '负责设备ID'"));
        assertTrue(sql.contains("`work_order_id` bigint DEFAULT NULL COMMENT '负责生产订单ID'"));
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

        String acM20Sql = Files.readString(resolveBackendPath(
                "sql/mysql/20260805_mes_process_pool_ac_m20_pqc_review_closure.sql"), StandardCharsets.UTF_8);
        assertTrue(acM20Sql.contains("dependsOn=20260803_mes_process_pool_pqc_process_inspection_aggregation"));
        assertTrue(acM20Sql.contains("`leader_type` varchar(32)"));
        assertTrue(acM20Sql.contains("UNIQUE KEY `uk_mes_pp_submission_review_event_terminal` (`tenant_id`, `event_id`, `deleted`)"));

        String p1Sql = Files.readString(resolveBackendPath(
                "sql/mysql/20260731_mes_process_pool_team_leader_p1_runtime_config.sql"), StandardCharsets.UTF_8);
        assertTrue(p1Sql.contains("dependsOn=20260730_mes_process_pool_team_leader"));
        assertTrue(p1Sql.contains("CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_active_order`"));
        assertTrue(p1Sql.contains("CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_team_employee_profile`"));
        assertTrue(p1Sql.contains("CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_team_device`"));
        assertTrue(p1Sql.contains("CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_team_process_device`"));
        assertTrue(p1Sql.contains("`device_status` varchar(32) NOT NULL COMMENT '设备状态：ENABLED/REPAIRING/DISABLED'"));
        assertTrue(p1Sql.contains("'employee_profile_id'"));
        assertTrue(p1Sql.contains("'default_value'"));
        assertTrue(p1Sql.contains("MODIFY COLUMN `employee_user_id` bigint DEFAULT NULL"));

        String p3Sql = Files.readString(resolveBackendPath(
                "sql/mysql/20260801_mes_process_pool_team_leader_p3_report_allocation.sql"), StandardCharsets.UTF_8);
        assertTrue(p3Sql.contains("dependsOn=20260731_mes_process_pool_team_leader_p1_runtime_config"));
        assertTrue(p3Sql.contains("CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_report_allocation`"));
        assertTrue(p3Sql.contains("`allocated_quantity` decimal(24,6) NOT NULL COMMENT '确认分配数量'"));
        assertTrue(p3Sql.contains("`allocation_mode` varchar(32) NOT NULL COMMENT '分配方式：FIFO/MANUAL'"));
        assertTrue(p3Sql.contains("KEY `idx_mes_pp_report_alloc_event` (`tenant_id`, `event_id`)"));
        assertTrue(p3Sql.contains("KEY `idx_mes_pp_report_alloc_work_order_process` (`tenant_id`, `work_order_id`, `route_process_id`, `process_id`)"));

        String p4Sql = Files.readString(resolveBackendPath(
                "sql/mysql/20260801_mes_process_pool_team_leader_p4_order_completion_backfill.sql"), StandardCharsets.UTF_8);
        assertTrue(p4Sql.contains("dependsOn=20260801_mes_process_pool_team_leader_p3_report_allocation"));
        assertTrue(p4Sql.contains("CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_order_process_completion`"));
        assertTrue(p4Sql.contains("`target_quantity` decimal(24,6) NOT NULL COMMENT '订单工序目标数量'"));
        assertTrue(p4Sql.contains("`confirmed_quantity` decimal(24,6) NOT NULL COMMENT '当前累计确认分配数量'"));
        assertTrue(p4Sql.contains("`completion_status` varchar(32) NOT NULL COMMENT '订单工序完成状态：IN_PROGRESS/COMPLETED'"));
        assertTrue(p4Sql.contains("`backfill_status` varchar(32) NOT NULL COMMENT '批记录回填状态：NOT_REQUIRED/SUCCESS'"));
        assertTrue(p4Sql.contains("`source_event_ids_json` json NOT NULL COMMENT '本次批记录回填聚合源事件ID集合'"));
        assertTrue(p4Sql.contains("`source_allocation_ids_json` json NOT NULL COMMENT '本次批记录回填聚合分配ID集合'"));
        assertTrue(p4Sql.contains("`aggregate_hash` char(64) NOT NULL COMMENT '订单工序完成批记录聚合版本哈希'"));
        assertTrue(p4Sql.contains("`backfill_idempotency_key` varchar(160) NOT NULL COMMENT '批记录回填聚合版本幂等键'"));
        assertTrue(p4Sql.contains("UNIQUE KEY `uk_mes_pp_order_process_completion` (`tenant_id`, `work_order_id`, `route_process_id`, `process_id`, `deleted`)"));
        assertTrue(p4Sql.contains("KEY `idx_mes_pp_order_process_completion_status` (`tenant_id`, `completion_status`, `backfill_status`)"));
        assertTrue(p4Sql.contains("KEY `idx_mes_pp_order_process_completion_aggregate` (`tenant_id`, `aggregate_hash`)"));

        String acM16Sql = Files.readString(resolveBackendPath(
                "sql/mysql/20260805_mes_process_pool_ac_m16_terminal_constraints.sql"), StandardCharsets.UTF_8);
        assertTrue(acM16Sql.contains("dependsOn=20260804_mes_process_pool_timeline_performance_indexes"));
        assertTrue(acM16Sql.contains("SIGNAL SQLSTATE '45000'"));
        assertTrue(acM16Sql.contains("Duplicate MES process-pool submission reviews block AC-M16 terminal constraint"));
        assertTrue(acM16Sql.contains("UNIQUE KEY `uk_mes_pp_submission_review_event` (`tenant_id`, `event_id`, `deleted`)"));
    }

    @Test
    void processLossReasonSchemaMustBeRouteProcessSharedAndSnapshotFeedbackHistory() throws Exception {
        assertField(MesProcessPoolDefectReasonDO.class, "routeProcessId", Long.class);
        assertField(MesProcessPoolDefectReasonDO.class, "processId", Long.class);
        assertField(MesProcessPoolDefectReasonDO.class, "enabled", Boolean.class);

        Class<?> feedbackClass = Class.forName(
                "cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO");
        assertField(feedbackClass, "lossReasonId", Long.class);
        assertField(feedbackClass, "lossReasonCodeSnapshot", String.class);
        assertField(feedbackClass, "lossReasonNameSnapshot", String.class);

        String sql = Files.readString(resolveBackendPath(
                "sql/mysql/20260805_mes_process_loss_reasons.sql"), StandardCharsets.UTF_8);
        assertTrue(sql.contains("dependsOn=20260802_mes_process_pool_active_order_authority"));
        assertTrue(sql.contains("DROP INDEX `uk_mes_pp_defect_reason` ON `mes_pro_process_pool_defect_reason`"));
        assertTrue(sql.contains("UNIQUE KEY `uk_mes_pp_loss_reason_route_process` (`tenant_id`, `route_process_id`, `reason_type`, `reason_code`, `deleted`)"));
        assertTrue(sql.contains("KEY `idx_mes_pp_loss_reason_route_process` (`tenant_id`, `route_process_id`, `reason_type`, `enabled`)"));
        assertFalse(sql.contains("`tenant_id`, `leader_user_id`, `reason_type`, `reason_code`, `process_id`, `deleted`"),
                "loss reasons must be shared by route-process instead of owned by one production leader");
        assertTrue(sql.contains("`loss_reason_id` bigint DEFAULT NULL COMMENT '损耗原因ID快照来源'"));
        assertTrue(sql.contains("`loss_reason_code_snapshot` varchar(64) DEFAULT NULL COMMENT '损耗原因编码快照'"));
        assertTrue(sql.contains("`loss_reason_name_snapshot` varchar(255) DEFAULT NULL COMMENT '损耗原因名称快照'"));
    }

    @Test
    void productionPersonnelSchemaMustSupportFormalTemporarySignatureAndTraceability() throws Exception {
        assertField(MesProcessPoolTeamEmployeeProfileDO.class, "displayName", String.class);
        assertField(MesProcessPoolTeamEmployeeProfileDO.class, "signaturePasswordHash", String.class);
        assertField(MesProcessPoolTeamEmployeeProfileDO.class, "signaturePasswordUpdatedAt", LocalDateTime.class);

        assertField(MesProcessPoolTeamEmployeeBindingDO.class, "displayNameSnapshot", String.class);

        assertField(MesProcessPoolTeamMaintenanceAuditDO.class, "operatorUserId", Long.class);
        assertField(MesProcessPoolTeamMaintenanceAuditDO.class, "resultStatus", String.class);
        assertField(MesProcessPoolTeamMaintenanceAuditDO.class, "changeSummary", String.class);

        String sql = Files.readString(resolveBackendPath(
                "sql/mysql/20260805_mes_process_pool_production_personnel.sql"), StandardCharsets.UTF_8);
        assertTrue(sql.contains("dependsOn=20260802_mes_process_pool_active_order_process_snapshot"));
        assertTrue(sql.contains("`display_name` varchar(128)"));
        assertTrue(sql.contains("`signature_password_hash` varchar(255)"));
        assertTrue(sql.contains("`signature_password_updated_at` datetime"));
        assertTrue(sql.contains("employee_type` varchar(32) NOT NULL COMMENT '员工来源：FORMAL/TEMPORARY'"));
        assertTrue(sql.contains("`active_display_name` varchar(128) GENERATED ALWAYS AS"));
        assertTrue(sql.contains("UNIQUE KEY `uk_mes_pp_team_employee_active_display_name`"));
        assertTrue(sql.contains("`display_name_snapshot` varchar(128)"));
        assertTrue(sql.contains("`operator_user_id` bigint"));
        assertTrue(sql.contains("`result_status` varchar(32)"));
        assertTrue(sql.contains("`change_summary` varchar(1000)"));
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
