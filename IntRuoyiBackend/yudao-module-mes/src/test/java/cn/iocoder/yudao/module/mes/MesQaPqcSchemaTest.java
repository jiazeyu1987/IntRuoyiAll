package cn.iocoder.yudao.module.mes;

import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesQaPqcSchemaTest {

    @Test
    void qaRegulationSchemaMustProvideOwnedVersionedPublishedModel() throws Exception {
        Class<?> regulationClass = Class.forName(
                "cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO");
        assertEquals("mes_qa_inspection_regulation", tableName(regulationClass));
        assertField(regulationClass, "dccProjectCodeId", Long.class);
        assertField(regulationClass, "productId", Long.class);
        assertField(regulationClass, "routeId", Long.class);
        assertField(regulationClass, "routeVersionId", Long.class);
        assertField(regulationClass, "routeProcessId", Long.class);
        assertField(regulationClass, "processId", Long.class);
        assertField(regulationClass, "ownerModule", String.class);
        assertField(regulationClass, "regulationCode", String.class);
        assertField(regulationClass, "regulationName", String.class);
        assertField(regulationClass, "lifecycleStatus", String.class);
        assertField(regulationClass, "currentVersionId", Long.class);

        Class<?> qaProcessClass = Class.forName(
                "cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationProcessDO");
        assertEquals("mes_qa_inspection_regulation_process", tableName(qaProcessClass));
        assertField(qaProcessClass, "regulationVersionId", Long.class);
        assertField(qaProcessClass, "processCode", String.class);
        assertField(qaProcessClass, "processName", String.class);
        assertField(qaProcessClass, "sort", Integer.class);

        Class<?> versionClass = Class.forName(
                "cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO");
        assertEquals("mes_qa_inspection_regulation_version", tableName(versionClass));
        assertField(versionClass, "regulationId", Long.class);
        assertField(versionClass, "versionNo", String.class);
        assertField(versionClass, "lifecycleStatus", String.class);
        assertField(versionClass, "publishedAt", LocalDateTime.class);
        assertField(versionClass, "retiredAt", LocalDateTime.class);
        assertField(versionClass, "finalInspectionApplicable", Boolean.class);
        assertField(versionClass, "finalInspectionNotApplicableReason", String.class);
        assertField(versionClass, "snapshotJson", String.class);

        Class<?> itemClass = Class.forName(
                "cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemDO");
        assertEquals("mes_qa_inspection_regulation_item", tableName(itemClass));
        assertField(itemClass, "regulationVersionId", Long.class);
        assertField(itemClass, "qaProcessId", Long.class);
        assertField(itemClass, "itemSort", Integer.class);
        assertField(itemClass, "inspectionType", String.class);
        assertField(itemClass, "itemCode", String.class);
        assertField(itemClass, "itemName", String.class);
        assertField(itemClass, "inspectionMethod", String.class);
        assertField(itemClass, "standardText", String.class);
        assertField(itemClass, "resultType", String.class);
        assertField(itemClass, "firstInspectionQuantity", Integer.class);
        assertField(itemClass, "patrolInspectionRatio", BigDecimal.class);

        String sql = readBackendSql("sql/mysql/20260802_mes_qa_inspection_regulation.sql",
                "sql/mysql/20260805_mes_qa_final_inspection_applicability.sql",
                "sql/mysql/20260811_mes_qa_dcc_project_scope.sql");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `mes_qa_inspection_regulation`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `mes_qa_inspection_regulation_version`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `mes_qa_inspection_regulation_item`"));
        assertTrue(sql.contains("`lifecycle_status` varchar(32) NOT NULL COMMENT '生命周期：DRAFT/PUBLISHED/RETIRED'"));
        assertTrue(sql.contains("`final_inspection_applicable` bit(1) DEFAULT NULL COMMENT '末检是否适用'"));
        assertTrue(sql.contains("`final_inspection_not_applicable_reason` varchar(512) DEFAULT NULL COMMENT '末检不适用依据'"));
        assertTrue(sql.contains("`dcc_project_code_id` bigint DEFAULT NULL COMMENT 'DCC项目代码ID'"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `mes_qa_inspection_regulation_process`"));
        assertTrue(sql.contains("UNIQUE KEY `uk_mes_qa_regulation_dcc_project`"));
        assertTrue(sql.contains("DROP INDEX `uk_mes_qa_regulation_route_process`"));
        assertTrue(sql.contains("`qa_process_id` bigint DEFAULT NULL COMMENT 'QA工序ID'"));
        assertTrue(sql.contains("`item_sort` int DEFAULT NULL COMMENT 'QA工序内项目排序'"));
    }

    @Test
    void pqcTaskSchemaMustFreezeRegulationVersionTaskIdentityAndPieceDetails() throws Exception {
        Class<?> taskClass = Class.forName(
                "cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO");
        assertEquals("mes_pqc_inspection_task", tableName(taskClass));
        assertField(taskClass, "activeOrderId", Long.class);
        assertField(taskClass, "workOrderId", Long.class);
        assertField(taskClass, "routeId", Long.class);
        assertField(taskClass, "routeVersionId", Long.class);
        assertField(taskClass, "routeProcessId", Long.class);
        assertField(taskClass, "processId", Long.class);
        assertField(taskClass, "qaProcessId", Long.class);
        assertField(taskClass, "regulationVersionId", Long.class);
        assertField(taskClass, "inspectionType", String.class);
        assertField(taskClass, "businessDate", LocalDate.class);
        assertField(taskClass, "shiftCode", String.class);
        assertField(taskClass, "roundNo", Integer.class);
        assertField(taskClass, "plannedInspectionQuantity", Integer.class);
        assertField(taskClass, "actualInspectionQuantity", Integer.class);
        assertField(taskClass, "taskStatus", String.class);

        Class<?> detailClass = Class.forName(
                "cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionPieceDetailDO");
        assertEquals("mes_pqc_inspection_piece_detail", tableName(detailClass));
        assertField(detailClass, "taskId", Long.class);
        assertField(detailClass, "sampleNo", Integer.class);
        assertField(detailClass, "itemCode", String.class);
        assertField(detailClass, "itemName", String.class);
        assertField(detailClass, "inspectionMethod", String.class);
        assertField(detailClass, "standardText", String.class);
        assertField(detailClass, "resultType", String.class);
        assertField(detailClass, "itemResult", String.class);
        assertField(detailClass, "measuredValue", String.class);
        assertField(detailClass, "judgement", String.class);

        String sql = readBackendSql("sql/mysql/20260802_mes_pqc_inspection_task.sql",
                "sql/mysql/20260811_mes_qa_dcc_project_scope.sql");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `mes_pqc_inspection_task`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `mes_pqc_inspection_piece_detail`"));
        assertTrue(sql.contains("`qa_process_id` bigint DEFAULT NULL COMMENT 'QA工序ID'"));
        assertTrue(sql.contains("UNIQUE KEY `uk_mes_pqc_task_qa_identity`"));
        assertTrue(sql.contains("DROP INDEX `uk_mes_pqc_task_identity`"));
        assertTrue(sql.contains("MODIFY COLUMN `route_process_id` bigint NULL"));
        assertTrue(sql.contains("MODIFY COLUMN `process_id` bigint NULL"));
        assertTrue(sql.contains("`regulation_version_id` bigint NOT NULL COMMENT 'QA规程发布版本ID'"));
        assertTrue(sql.contains("`task_status` varchar(32) NOT NULL COMMENT '任务状态：PENDING/SUBMITTED/CONFIRMED/CANCELLED'"));
        assertTrue(sql.contains("`sample_no` int NOT NULL COMMENT '逐件样本序号'"));
    }

    @Test
    void qaRegulationItemSchemaMustProvideEquipmentAndNumericStandardSnapshot() throws Exception {
        Class<?> itemClass = Class.forName(
                "cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemDO");
        assertField(itemClass, "standardLowerLimit", BigDecimal.class);
        assertField(itemClass, "standardUpperLimit", BigDecimal.class);
        assertField(itemClass, "standardUnit", String.class);
        assertField(itemClass, "standardPrecision", Integer.class);
        assertField(itemClass, "equipmentRequired", Boolean.class);

        Class<?> equipmentClass = Class.forName(
                "cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemEquipmentDO");
        assertEquals("mes_qa_inspection_regulation_item_equipment", tableName(equipmentClass));
        assertField(equipmentClass, "regulationVersionId", Long.class);
        assertField(equipmentClass, "inspectionType", String.class);
        assertField(equipmentClass, "itemCode", String.class);
        assertField(equipmentClass, "equipmentId", Long.class);
        assertField(equipmentClass, "equipmentCode", String.class);
        assertField(equipmentClass, "equipmentName", String.class);
        assertField(equipmentClass, "equipmentNumber", String.class);
        assertField(equipmentClass, "defaultFlag", Boolean.class);
        assertField(equipmentClass, "sort", Integer.class);

        String sql = readBackendSql("sql/mysql/20260802_mes_qa_inspection_regulation.sql",
                "sql/mysql/20260803_mes_pqc_item_equipment_standard_snapshot.sql");
        assertTrue(sql.contains("`standard_lower_limit` decimal(18,6) DEFAULT NULL COMMENT '接收标准下限'"));
        assertTrue(sql.contains("`standard_upper_limit` decimal(18,6) DEFAULT NULL COMMENT '接收标准上限'"));
        assertTrue(sql.contains("`standard_unit` varchar(32) DEFAULT NULL COMMENT '接收标准单位'"));
        assertTrue(sql.contains("`equipment_required` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否必须选择检验设备'"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `mes_qa_inspection_regulation_item_equipment`"));
        assertTrue(sql.contains("`equipment_id` bigint NOT NULL COMMENT 'MES设备台账ID'"));
        assertTrue(sql.contains("`equipment_number` varchar(64) NOT NULL COMMENT '设备编号/出厂编号/台账编码快照'"));
        assertTrue(sql.contains("UNIQUE KEY `uk_mes_qa_regulation_item_equipment`"));
    }

    @Test
    void pqcSubmitContractAndPieceSchemaMustFreezeItemEquipmentStandardSnapshot() throws Exception {
        Class<?> detailClass = Class.forName(
                "cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionPieceDetailDO");
        assertField(detailClass, "selectedEquipmentId", Long.class);
        assertField(detailClass, "selectedEquipmentCode", String.class);
        assertField(detailClass, "selectedEquipmentName", String.class);
        assertField(detailClass, "selectedEquipmentNumber", String.class);
        assertField(detailClass, "standardLowerLimit", BigDecimal.class);
        assertField(detailClass, "standardUpperLimit", BigDecimal.class);
        assertField(detailClass, "standardUnit", String.class);
        assertField(detailClass, "standardPrecision", Integer.class);

        Class<?> reqClass = Class.forName(
                "cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesFrontlinePqcSubmitReqVO");
        assertField(reqClass, "itemResults", List.class);
        assertField(reqClass, "signaturePassword", String.class);
        assertField(reqClass, "scrapQuantity", Integer.class);
        assertMissingField(reqClass, "signatureId");
        assertMissingField(reqClass, "signatureEmployeeId");
        assertMissingField(reqClass, "inspectionResult");
        Class<?> reqItemClass = Class.forName(
                "cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesFrontlinePqcSubmitReqVO$ItemResult");
        assertField(reqItemClass, "itemCode", String.class);
        assertField(reqItemClass, "selectedEquipmentId", Long.class);
        assertField(reqItemClass, "selectedEquipmentNumber", String.class);
        assertField(reqItemClass, "sampleValues", List.class);
        assertFieldMissingAnnotation(reqItemClass, "selectedEquipmentId", NotNull.class);
        assertFieldMissingAnnotation(reqItemClass, "selectedEquipmentNumber", NotBlank.class);

        Class<?> commandClass = Class.forName(
                "cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlinePqcSubmitCommand");
        assertField(commandClass, "itemResults", List.class);
        assertField(commandClass, "signaturePassword", String.class);
        assertField(commandClass, "scrapQuantity", Integer.class);

        Class<?> resultClass = Class.forName(
                "cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlinePqcSubmitResult");
        assertField(resultClass, "pqcTaskId", Long.class);
        assertField(resultClass, "pqcEventId", Long.class);
        assertField(resultClass, "pqcRecordId", Long.class);
        assertField(resultClass, "signatureId", Long.class);
        assertField(resultClass, "inspectionResult", String.class);
        assertField(resultClass, "serverSubmitTime", LocalDateTime.class);
        Class<?> commandItemClass = Class.forName(
                "cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlinePqcSubmitCommand$ItemResult");
        assertField(commandItemClass, "itemCode", String.class);
        assertField(commandItemClass, "selectedEquipmentId", Long.class);
        assertField(commandItemClass, "selectedEquipmentNumber", String.class);
        assertField(commandItemClass, "sampleValues", List.class);

        String sql = readBackendSql("sql/mysql/20260802_mes_pqc_inspection_task.sql",
                "sql/mysql/20260803_mes_pqc_item_equipment_standard_snapshot.sql",
                "sql/mysql/20260804_mes_pqc_piece_detail_legacy_equipment_nullable.sql");
        assertTrue(sql.contains("MODIFY COLUMN `selected_equipment_id` bigint NULL"));
        assertTrue(sql.contains("MODIFY COLUMN `selected_equipment_code` varchar(64) NULL"));
        assertTrue(sql.contains("MODIFY COLUMN `selected_equipment_name` varchar(128) NULL"));
        assertTrue(sql.contains("MODIFY COLUMN `selected_equipment_number` varchar(64) NULL"));
        assertTrue(sql.contains("`standard_lower_limit` decimal(18,6) DEFAULT NULL COMMENT '提交时接收标准下限快照'"));
        assertTrue(sql.contains("`standard_upper_limit` decimal(18,6) DEFAULT NULL COMMENT '提交时接收标准上限快照'"));
    }

    @Test
    void pqcProcessInspectionAggregationSchemaMustFreezeApprovedReviewDetails() throws Exception {
        Class<?> aggregateDetailClass = Class.forName(
                "cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailDO");
        assertEquals("mes_pqc_process_inspection_aggregate_detail", tableName(aggregateDetailClass));
        assertField(aggregateDetailClass, "sourcePqcRecordId", Long.class);
        assertField(aggregateDetailClass, "eventId", Long.class);
        assertField(aggregateDetailClass, "reviewId", Long.class);
        assertField(aggregateDetailClass, "productionSubmitEventId", Long.class);
        assertField(aggregateDetailClass, "pqcTaskId", Long.class);
        assertField(aggregateDetailClass, "sourcePieceDetailId", Long.class);
        assertField(aggregateDetailClass, "itemCode", String.class);
        assertField(aggregateDetailClass, "selectedEquipmentNumber", String.class);
        assertField(aggregateDetailClass, "standardLowerLimit", BigDecimal.class);
        assertField(aggregateDetailClass, "standardUpperLimit", BigDecimal.class);
        assertField(aggregateDetailClass, "measuredValue", String.class);
        assertField(aggregateDetailClass, "judgement", String.class);
        assertField(aggregateDetailClass, "aggregatedAt", LocalDateTime.class);

        String sql = Files.readString(resolveBackendPath(
                "sql/mysql/20260805_mes_process_pool_ac_m20_pqc_review_closure.sql"), StandardCharsets.UTF_8);
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `mes_pqc_process_inspection_aggregate_detail`"));
        assertTrue(sql.contains("`task_status` varchar(32) NOT NULL COMMENT '任务状态：PENDING/SUBMITTED/CONFIRMED/CANCELLED'"));
        assertTrue(sql.contains("UNIQUE KEY `uk_mes_pqc_process_aggregate_piece`"));
    }

    @Test
    void pqcProcessInspectionAggregateRuntimeClosureMustRepairExistingTable() throws Exception {
        String sql = Files.readString(resolveBackendPath(
                "sql/mysql/20260805_mes_pqc_process_inspection_aggregate_runtime_closure.sql"),
                StandardCharsets.UTF_8);

        assertTrue(sql.contains(
                "dependsOn=20260805_mes_process_pool_ac_m20_pqc_review_closure,"
                        + "20260805_mes_pqc_process_inspection_aggregate_detail"));
        assertTrue(sql.contains("'active_order_id'"));
        assertTrue(sql.contains("'route_version_id'"));
        assertTrue(sql.contains("'actual_inspection_quantity'"));
        assertTrue(sql.contains("JOIN `mes_pqc_inspection_task` `task`"));
        assertTrue(sql.contains("`aggregate_detail`.`actual_inspection_quantity` = "
                + "`task`.`actual_inspection_quantity`"));
        assertTrue(sql.contains(
                "MODIFY COLUMN `actual_inspection_quantity` int NOT NULL COMMENT '实际检验数量'"));
        assertTrue(sql.contains("uk_mes_pqc_process_inspection_aggregate"));
        assertTrue(sql.contains("idx_mes_pqc_process_inspection_review"));
        assertTrue(sql.contains("idx_mes_pqc_process_inspection_submit_event"));
    }

    @Test
    void c00SchemaMigrationMustFreezeRouteDccActiveOrderAndPqcTaskContracts() throws Exception {
        String schemaSql = readBackendSql("sql/mysql/20260812_mes_pqc_dcc_qa_c00_schema.sql");
        String preflightSql = readBackendSql("sql/mysql/20260812_mes_pqc_dcc_qa_c00_preflight.sql");
        String backfillSql = readBackendSql("sql/mysql/20260812_mes_pqc_dcc_qa_c00_backfill.sql");
        String postflightSql = readBackendSql("sql/mysql/20260812_mes_pqc_dcc_qa_c00_postflight.sql");
        String rollbackSql = readBackendSql("sql/mysql/20260812_mes_pqc_dcc_qa_c00_rollback.sql");
        String fullSql = schemaSql + preflightSql + backfillSql + postflightSql + rollbackSql;

        assertTrue(preflightSql.contains("dependsOn=20260811_mes_qa_dcc_project_scope"));
        assertTrue(preflightSql.contains("type=data"));
        assertTrue(backfillSql.contains("type=data"));
        assertTrue(postflightSql.contains("type=schema"));
        assertTrue(rollbackSql.contains("type=data"));
        assertFalse(fullSql.contains("type=preflight"));
        assertFalse(fullSql.contains("type=backfill"));
        assertFalse(fullSql.contains("type=postflight"));
        assertFalse(fullSql.contains("type=rollback-dry-run"));
        assertFalse(preflightSql.contains("dependsOn=20260812_mes_pqc_dcc_qa_c00_schema"));
        assertTrue(preflightSql.contains("information_schema.tables"));
        assertTrue(preflightSql.contains("@c00_route_dcc_binding_ready"));
        assertTrue(preflightSql.contains("PREPARE stmt FROM @sql"));
        assertFalse(preflightSql.contains("task.task_status <> 'PENDING'"));
        assertTrue(preflightSql.contains("task.task_status IN ('SUBMITTED', 'CONFIRMED')"));

        assertTrue(schemaSql.contains("dependsOn=20260811_mes_qa_dcc_project_scope"));
        assertTrue(schemaSql.contains("CREATE TABLE IF NOT EXISTS `mes_pro_route_dcc_project_binding`"));
        assertTrue(schemaSql.contains("`dcc_project_code_id` bigint NOT NULL COMMENT 'DCC项目代码ID'"));
        assertTrue(schemaSql.contains("`version` bigint NOT NULL COMMENT '同租户同路线单调递增版本'"));
        assertTrue(schemaSql.contains("`active_route_id` BIGINT GENERATED ALWAYS AS"));
        assertTrue(schemaSql.contains("UNIQUE KEY `uk_mes_pro_route_dcc_current` (`tenant_id`, `active_route_id`)"));
        assertTrue(schemaSql.contains("UNIQUE KEY `uk_mes_pro_route_dcc_history_version` (`tenant_id`, `route_id`, `version`)"));

        assertTrue(schemaSql.contains("ALTER TABLE `mes_pro_process_pool_active_order`"));
        assertTrue(schemaSql.contains("`dcc_project_code_id` bigint DEFAULT NULL COMMENT '订单锁定DCC项目代码ID'"));
        assertTrue(schemaSql.contains("`qa_regulation_id` bigint DEFAULT NULL COMMENT '订单锁定QA规程ID'"));
        assertTrue(schemaSql.contains("`qa_regulation_version_id` bigint DEFAULT NULL COMMENT '订单锁定QA规程发布版本ID'"));

        assertTrue(schemaSql.contains("`inspection_rule_key` varchar(32) DEFAULT NULL COMMENT '正式检验规则身份'"));
        assertTrue(schemaSql.contains("`submitted_content_hash` char(64) DEFAULT NULL COMMENT 'CanonicalPqcSubmissionV1内容哈希'"));
        assertTrue(schemaSql.contains("`submitted_event_id` bigint DEFAULT NULL COMMENT '唯一正式PQC提交事件ID'"));
        assertFalse(schemaSql.contains("DROP INDEX `uk_mes_pqc_task_qa_identity`"));
        assertFalse(schemaSql.contains("uk_mes_pqc_task_rule_identity"));
        assertFalse(schemaSql.contains("uk_mes_pqc_task_submitted_event"));

        assertTrue(schemaSql.contains("`pqc_submission_task_id` BIGINT GENERATED ALWAYS AS"));
        assertTrue(schemaSql.contains("`event_type` = 'PQC_INSPECTION'"));
        assertTrue(schemaSql.contains("`feedback_source_type` = 'MES_PQC_INSPECTION_TASK'"));
        assertFalse(schemaSql.contains("uk_mes_pro_process_pool_event_pqc_task"));

        assertTrue(postflightSql.contains("SET @c00_postflight_blocker_count := (SELECT COUNT(1) FROM c00_postflight_blocker_report)"));
        assertTrue(postflightSql.contains("DROP INDEX `uk_mes_pqc_task_qa_identity`"));
        assertTrue(postflightSql.contains("ADD UNIQUE KEY `uk_mes_pqc_task_rule_identity`"));
        assertTrue(postflightSql.contains("`inspection_rule_key`, `business_date`, `deleted`"));
        assertTrue(postflightSql.contains("ADD UNIQUE KEY `uk_mes_pqc_task_submitted_event` (`tenant_id`, `submitted_event_id`, `deleted`)"));
        assertTrue(postflightSql.contains("ADD UNIQUE KEY `uk_mes_pro_process_pool_event_pqc_task` (`tenant_id`, `pqc_submission_task_id`)"));
        assertTrue(postflightSql.contains("MODIFY COLUMN `inspection_rule_key` varchar(32) NOT NULL COMMENT ''正式检验规则身份''"));

        assertTrue(preflightSql.contains("c00_preflight_release_metadata"));
        assertTrue(backfillSql.contains("c00_backfill_approved_route_dcc_binding"));
        assertTrue(backfillSql.contains("c00_backfill_approved_active_order_snapshot"));
        assertTrue(backfillSql.contains("c00_backfill_approved_task_submission"));
        assertTrue(backfillSql.contains("'APPROVED_MANIFEST'"));
        assertTrue(backfillSql.contains("active_order_manifest_task_version_ambiguous"));
        assertTrue(backfillSql.contains("active_order_manifest_missing"));
        assertFalse(backfillSql.contains("UNIQUE_TASK_VERSION"));
        assertFalse(backfillSql.contains("unique-task-version:"));
        assertFalse(backfillSql.contains("tmp_c00_active_order_unique_task_version"));
        assertFalse(backfillSql.contains("task_status <> 'PENDING'"));
        assertTrue(backfillSql.contains("task_status IN ('SUBMITTED', 'CONFIRMED')"));
        assertTrue(backfillSql.contains("START TRANSACTION"));
        assertTrue(backfillSql.contains("INSERT INTO `mes_pro_route_dcc_project_binding`"));
        assertTrue(backfillSql.contains("UPDATE `mes_pro_process_pool_active_order`"));
        assertTrue(backfillSql.contains("UPDATE `mes_pqc_inspection_task`"));
        assertTrue(backfillSql.contains("ROW_COUNT()"));
        assertTrue(backfillSql.contains("SIGNAL SQLSTATE '45000'"));
        assertTrue(backfillSql.contains("canonical_payload_json"));
        assertTrue(backfillSql.contains("SHA2(manifest.canonical_payload_json, 256)"));
        assertTrue(backfillSql.contains("JSON_TABLE(manifest.canonical_payload_json"));
        assertTrue(backfillSql.contains("canonical_payload_field_mismatch"));
        assertTrue(backfillSql.contains("canonical_payload_item_mismatch"));
        assertFalse(backfillSql.contains(
                "C00 backfill apply mode must be executed from the controlled maintenance runbook"));
        assertTrue(postflightSql.contains("c00_postflight_blocker_report"));
        assertTrue(rollbackSql.contains("C00 rollback requires active-order and PQC submit writes stopped"));
        assertTrue(fullSql.contains("CanonicalPqcSubmissionV1"));
        assertTrue(fullSql.contains("input_manifest_sha256"));
        assertTrue(fullSql.contains("affected_row_count"));
        assertTrue(fullSql.contains("blocker_reason"));

        assertFalse(fullSql.contains("dcc_project_code_qa_regulation_binding"));
        assertFalse(fullSql.contains("mes_qa_inspection_regulation_item_type"));
        assertFalse(fullSql.contains("mes_pro_process_pool_active_order_pqc_context"));
    }

    @Test
    void activeOrderQaSnapshotColumnsMustBecomeRequiredAfterBackfill() throws Exception {
        String schemaSql = readBackendSql("sql/mysql/20260812_mes_pqc_dcc_qa_c00_schema.sql");
        String postflightSql = readBackendSql("sql/mysql/20260812_mes_pqc_dcc_qa_c00_postflight.sql");

        assertTrue(schemaSql.contains("`dcc_project_code_id` bigint DEFAULT NULL COMMENT '订单锁定DCC项目代码ID'"));
        assertTrue(schemaSql.contains("`qa_regulation_id` bigint DEFAULT NULL COMMENT '订单锁定QA规程ID'"));
        assertTrue(schemaSql.contains("`qa_regulation_version_id` bigint DEFAULT NULL COMMENT '订单锁定QA规程发布版本ID'"));
        assertTrue(postflightSql.contains("WHERE dcc_project_code_id IS NULL"));
        assertTrue(postflightSql.contains("OR qa_regulation_id IS NULL"));
        assertTrue(postflightSql.contains("OR qa_regulation_version_id IS NULL"));
        assertTrue(postflightSql.contains("MODIFY COLUMN `dcc_project_code_id` bigint NOT NULL"));
        assertTrue(postflightSql.contains("MODIFY COLUMN `qa_regulation_id` bigint NOT NULL"));
        assertTrue(postflightSql.contains("MODIFY COLUMN `qa_regulation_version_id` bigint NOT NULL"));
    }

    private static String tableName(Class<?> clazz) {
        return clazz.getAnnotation(TableName.class).value();
    }

    private static void assertField(Class<?> clazz, String name, Class<?> type) throws Exception {
        Field field = clazz.getDeclaredField(name);
        assertEquals(type, field.getType(), clazz.getSimpleName() + "." + name);
    }

    private static void assertMissingField(Class<?> clazz, String name) {
        try {
            clazz.getDeclaredField(name);
            throw new AssertionError(clazz.getSimpleName() + " must not expose field " + name);
        } catch (NoSuchFieldException expected) {
            // Expected for server-owned formal submission fields.
        }
    }

    private static void assertFieldMissingAnnotation(Class<?> clazz, String name,
                                                     Class<? extends java.lang.annotation.Annotation> annotation)
            throws Exception {
        Field field = clazz.getDeclaredField(name);
        assertFalse(field.isAnnotationPresent(annotation),
                clazz.getSimpleName() + "." + name + " must not require equipment for no-device QA items");
    }

    private static String readBackendSql(String... relatives) throws Exception {
        StringBuilder sql = new StringBuilder();
        for (String relative : relatives) {
            sql.append(Files.readString(resolveBackendPath(relative), StandardCharsets.UTF_8)).append('\n');
        }
        return sql.toString();
    }

    private static Path resolveBackendPath(String relative) {
        Path cwd = Paths.get("").toAbsolutePath();
        if ("yudao-module-mes".equals(cwd.getFileName().toString())) {
            return cwd.getParent().resolve(relative);
        }
        return cwd.resolve(relative);
    }
}
