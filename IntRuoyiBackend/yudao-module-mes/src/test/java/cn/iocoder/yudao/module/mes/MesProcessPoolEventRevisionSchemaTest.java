package cn.iocoder.yudao.module.mes;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventRevisionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventRevisionDiffDO;
import com.baomidou.mybatisplus.annotation.TableName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProcessPoolEventRevisionSchemaTest {

    @Test
    void shouldCreateEventRevisionTablesAndFields() throws Exception {
        assertEquals("mes_pro_process_pool_event_revision", tableName(MesProProcessPoolEventRevisionDO.class));
        assertEquals("mes_pro_process_pool_event_revision_diff", tableName(MesProProcessPoolEventRevisionDiffDO.class));

        for (String field : Set.of(
                "eventId", "poolId", "workOrderId", "routeId", "routeProcessId", "processId",
                "beforePayload", "afterPayload", "changeReason", "revisionSignatureId",
                "revisionSignatureUserId", "revisionSignatureSnapshot", "modifiedByUserId",
                "serverRevisionTime", "revisionStatus")) {
            assertField(MesProProcessPoolEventRevisionDO.class, field);
        }

        for (String field : Set.of(
                "revisionId", "eventId", "fieldCode", "fieldName", "beforeValue", "afterValue",
                "affectsQuantityFragment", "sourceQuantityFragmentId", "originalFieldCode", "originalFieldName")) {
            assertField(MesProProcessPoolEventRevisionDiffDO.class, field);
        }

        String sql = Files.readString(Path.of("..", "sql", "mysql",
                "20260730_mes_process_pool_event_revision.sql"), StandardCharsets.UTF_8);
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_event_revision`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_event_revision_diff`"));
        assertTrue(sql.contains("`before_payload` json NOT NULL"));
        assertTrue(sql.contains("`after_payload` json NOT NULL"));
        assertTrue(sql.contains("`change_reason` varchar(500) NOT NULL"));
        assertTrue(sql.contains("`revision_signature_id` bigint NOT NULL"));
        assertTrue(sql.contains("`server_revision_time` datetime NOT NULL"));
        assertTrue(sql.contains("`field_code` varchar(128) NOT NULL"));
        assertTrue(sql.contains("`affects_quantity_fragment` bit(1) NOT NULL"));
    }

    private static String tableName(Class<?> clazz) {
        return clazz.getAnnotation(TableName.class).value();
    }

    private static void assertField(Class<?> clazz, String fieldName) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        assertEquals(fieldName, field.getName());
    }
}
