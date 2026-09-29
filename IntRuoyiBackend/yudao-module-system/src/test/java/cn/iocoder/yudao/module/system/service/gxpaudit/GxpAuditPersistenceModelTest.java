package cn.iocoder.yudao.module.system.service.gxpaudit;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventDO;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditPolicyOperationDO;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditEventMapper;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditPolicyOperationMapper;
import com.baomidou.mybatisplus.annotation.TableName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class GxpAuditPersistenceModelTest {

    @Test
    void shouldBindAuditCoreObjectsToUnifiedTables() {
        assertEquals("gxp_audit_event", GxpAuditEventDO.class.getAnnotation(TableName.class).value());
        assertEquals("gxp_audit_policy_operation",
                GxpAuditPolicyOperationDO.class.getAnnotation(TableName.class).value());
        assertEquals("gxp_audit_event_relation", tableName("GxpAuditEventRelationDO"));
        assertEquals("gxp_audit_policy_activation", tableName("GxpAuditPolicyActivationDO"));
        assertEquals("gxp_audit_legacy_fact_baseline", tableName("GxpAuditLegacyFactBaselineDO"));
    }

    @Test
    void shouldExposeOnlyAppendCentricLedgerFields() {
        Set<String> fields = Arrays.stream(GxpAuditEventDO.class.getDeclaredFields())
                .map(field -> field.getName())
                .collect(Collectors.toSet());

        assertTrue(fields.contains("ledgerSequence"));
        assertTrue(fields.contains("previousEventHash"));
        assertTrue(fields.contains("eventHash"));
        assertTrue(fields.contains("idempotencyPayloadHash"));
        assertFalse(fields.contains("updateReason"));
        assertFalse(fields.contains("deleteReason"));
    }

    @Test
    void shouldUseBaseMappersForAuditCoreTables() {
        assertTrue(BaseMapperX.class.isAssignableFrom(GxpAuditEventMapper.class));
        assertTrue(BaseMapperX.class.isAssignableFrom(GxpAuditPolicyOperationMapper.class));
        assertTrue(baseMapper("GxpAuditEventRelationMapper"));
        assertTrue(baseMapper("GxpAuditPolicyActivationMapper"));
        assertTrue(baseMapper("GxpAuditLegacyFactBaselineMapper"));
    }

    private String tableName(String simpleName) {
        try {
            return Class.forName("cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit." + simpleName)
                    .getAnnotation(TableName.class).value();
        } catch (ReflectiveOperationException exception) {
            return "MISSING:" + simpleName;
        }
    }

    private boolean baseMapper(String simpleName) {
        try {
            return BaseMapperX.class.isAssignableFrom(Class.forName(
                    "cn.iocoder.yudao.module.system.dal.mysql.gxpaudit." + simpleName));
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }

}
