package cn.iocoder.yudao.module.mes;

import com.baomidou.mybatisplus.annotation.TableName;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesQaPqcSchemaTest {

    @Test
    void qaRegulationSchemaMustProvideOwnedVersionedPublishedModel() throws Exception {
        Class<?> regulationClass = Class.forName(
                "cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO");
        assertEquals("mes_qa_inspection_regulation", tableName(regulationClass));
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

        Class<?> versionClass = Class.forName(
                "cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO");
        assertEquals("mes_qa_inspection_regulation_version", tableName(versionClass));
        assertField(versionClass, "regulationId", Long.class);
        assertField(versionClass, "versionNo", String.class);
        assertField(versionClass, "lifecycleStatus", String.class);
        assertField(versionClass, "publishedAt", LocalDateTime.class);
        assertField(versionClass, "retiredAt", LocalDateTime.class);
        assertField(versionClass, "snapshotJson", String.class);

        Class<?> itemClass = Class.forName(
                "cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemDO");
        assertEquals("mes_qa_inspection_regulation_item", tableName(itemClass));
        assertField(itemClass, "regulationVersionId", Long.class);
        assertField(itemClass, "inspectionType", String.class);
        assertField(itemClass, "itemCode", String.class);
        assertField(itemClass, "itemName", String.class);
        assertField(itemClass, "inspectionMethod", String.class);
        assertField(itemClass, "standardText", String.class);
        assertField(itemClass, "resultType", String.class);
        assertField(itemClass, "firstInspectionQuantity", Integer.class);
        assertField(itemClass, "patrolInspectionRatio", BigDecimal.class);

        String sql = Files.readString(resolveBackendPath(
                "sql/mysql/20260802_mes_qa_inspection_regulation.sql"), StandardCharsets.UTF_8);
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `mes_qa_inspection_regulation`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `mes_qa_inspection_regulation_version`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `mes_qa_inspection_regulation_item`"));
        assertTrue(sql.contains("`lifecycle_status` varchar(32) NOT NULL COMMENT '生命周期：DRAFT/PUBLISHED/RETIRED'"));
        assertTrue(sql.contains("UNIQUE KEY `uk_mes_qa_regulation_route_process`"));
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

        String sql = Files.readString(resolveBackendPath(
                "sql/mysql/20260802_mes_pqc_inspection_task.sql"), StandardCharsets.UTF_8);
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `mes_pqc_inspection_task`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `mes_pqc_inspection_piece_detail`"));
        assertTrue(sql.contains("UNIQUE KEY `uk_mes_pqc_task_identity`"));
        assertTrue(sql.contains("`regulation_version_id` bigint NOT NULL COMMENT 'QA规程发布版本ID'"));
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
        Class<?> reqItemClass = Class.forName(
                "cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesFrontlinePqcSubmitReqVO$ItemResult");
        assertField(reqItemClass, "itemCode", String.class);
        assertField(reqItemClass, "selectedEquipmentId", Long.class);
        assertField(reqItemClass, "selectedEquipmentNumber", String.class);
        assertField(reqItemClass, "sampleValues", List.class);

        Class<?> commandClass = Class.forName(
                "cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlinePqcSubmitCommand");
        assertField(commandClass, "itemResults", List.class);
        Class<?> commandItemClass = Class.forName(
                "cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlinePqcSubmitCommand$ItemResult");
        assertField(commandItemClass, "itemCode", String.class);
        assertField(commandItemClass, "selectedEquipmentId", Long.class);
        assertField(commandItemClass, "selectedEquipmentNumber", String.class);
        assertField(commandItemClass, "sampleValues", List.class);

        String sql = readBackendSql("sql/mysql/20260802_mes_pqc_inspection_task.sql",
                "sql/mysql/20260803_mes_pqc_item_equipment_standard_snapshot.sql");
        assertTrue(sql.contains("`selected_equipment_id` bigint NOT NULL COMMENT '实际检验设备ID快照'"));
        assertTrue(sql.contains("`selected_equipment_number` varchar(64) NOT NULL COMMENT '实际检验设备编号快照'"));
        assertTrue(sql.contains("`standard_lower_limit` decimal(18,6) DEFAULT NULL COMMENT '提交时接收标准下限快照'"));
        assertTrue(sql.contains("`standard_upper_limit` decimal(18,6) DEFAULT NULL COMMENT '提交时接收标准上限快照'"));
    }

    private static String tableName(Class<?> clazz) {
        return clazz.getAnnotation(TableName.class).value();
    }

    private static void assertField(Class<?> clazz, String name, Class<?> type) throws Exception {
        Field field = clazz.getDeclaredField(name);
        assertEquals(type, field.getType(), clazz.getSimpleName() + "." + name);
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
