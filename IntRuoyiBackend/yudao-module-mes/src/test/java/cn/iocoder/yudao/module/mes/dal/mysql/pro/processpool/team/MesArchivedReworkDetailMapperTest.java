package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class MesArchivedReworkDetailMapperTest {
    private Configuration configuration() throws Exception {
        Configuration configuration = new Configuration();
        var file = Path.of("src/main/resources/mapper/pro/processpool/MesProcessPoolActiveOrderDetailReadMapper.xml");
        try(var input = Files.newInputStream(file)) {
            new XMLMapperBuilder(input,configuration,file.toString(),configuration.getSqlFragments()).parse();
        }
        return configuration;
    }
    @Test
    void liveDetailStillRequiresANonDeletedActiveOrder() throws Exception {
        var sql = configuration().getMappedStatement(MesProcessPoolActiveOrderDetailReadMapper.class.getName()+".selectByActiveOrderId")
                .getBoundSql(Map.of("activeOrderId",419L,"archivedTenantId",1L,"archivedReworkSource",true)).getSql().replaceAll("\\s+"," ");
        assertTrue(sql.contains("active_order.deleted = 0"));
        assertFalse(sql.contains("active_order.business_status = 'REWORKED'"));
        assertTrue(sql.contains("process_snapshot.active_order_id = ?"));
    }
    @Test
    void archivedSourceOnlyReleasesTheOrderSoftDeletePredicateAndRetainsFormalTenantAndProcessBounds() throws Exception {
        var bound = configuration().getMappedStatement(MesProcessPoolActiveOrderDetailReadMapper.class.getName()+".selectArchivedReworkByActiveOrderId")
                .getBoundSql(Map.of("activeOrderId",419L,"archivedTenantId",1L));
        var sql = bound.getSql().replaceAll("\\s+"," ");
        assertFalse(sql.contains("active_order.deleted = 0"));
        assertTrue(sql.contains("active_order.tenant_id = ?"));
        assertTrue(sql.contains("active_order.business_status = 'REWORKED'"));
        assertTrue(sql.contains("active_order.active_status = 'REMOVED'"));
        assertTrue(sql.contains("active_order.tenant_id = process_snapshot.tenant_id"));
        assertTrue(sql.contains("process_snapshot.active_order_id = ?"));
        assertTrue(sql.contains("process_snapshot.deleted = 0"));
        assertTrue(sql.contains("work_order.deleted = 0"));
        assertEquals(java.util.List.of("archivedTenantId","activeOrderId"),
                bound.getParameterMappings().stream().map(org.apache.ibatis.mapping.ParameterMapping::getProperty).toList());
    }
}
