package cn.iocoder.yudao.module.mes.dal.mysql.pro.inprocess;

import cn.iocoder.yudao.module.mes.controller.admin.pro.inprocess.vo.MesInProcessOrderPageReqVO;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MesInProcessOrderMapperTest {
    @Test void currentTenantOngoingCyclesAndCombinedFilters() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:inprocess;MODE=MySQL", "sa", "")) {
            var statement = connection.createStatement();
            statement.execute("CREATE TABLE mes_pro_process_pool_active_order(id BIGINT,tenant_id BIGINT,work_order_id BIGINT,leader_user_id BIGINT,active_status VARCHAR,business_status VARCHAR,deleted INT,joined_at TIMESTAMP)");
            statement.execute("CREATE TABLE mes_pro_work_order(id BIGINT,tenant_id BIGINT,product_id BIGINT,code VARCHAR,name VARCHAR,batch_code VARCHAR,deleted INT)");
            statement.execute("CREATE TABLE mes_md_item(id BIGINT,tenant_id BIGINT,name VARCHAR,deleted INT)");
            statement.execute("INSERT INTO mes_md_item VALUES(1,1,'导管',0),(2,2,'导管',0)");
            statement.execute("INSERT INTO mes_pro_work_order VALUES(1,1,1,'WO-1','一号工单','LOT-A',0),(2,1,1,'WO-2','二号工单','LOT-B',0),(3,2,2,'WO-3','其他租户','LOT-A',0)");
            statement.execute("INSERT INTO mes_pro_process_pool_active_order VALUES(1,1,1,10,'ACTIVE','ACTIVE',0,'2026-10-27 10:00:00'),(2,1,2,20,'ACTIVE','ACTIVE',0,'2026-10-27 11:00:00'),(3,1,1,10,'ACTIVE','COMPLETED',0,'2026-10-27 12:00:00'),(4,1,1,10,'REMOVED','VERSION_UPGRADED',0,'2026-10-27 12:00:00'),(5,2,3,30,'ACTIVE','ACTIVE',0,'2026-10-27 12:00:00'),(6,1,1,10,'ACTIVE','ACTIVE',1,'2026-10-27 12:00:00')");
            statement.execute("INSERT INTO mes_pro_process_pool_active_order VALUES(7,1,1,10,'ACTIVE','VERSION_UPGRADE_PENDING',0,'2026-10-27 09:00:00')");
            statement.execute("INSERT INTO mes_pro_process_pool_active_order VALUES(8,1,1,10,'CLOSED','RELEASED',0,'2026-10-27 13:00:00')");
            var query = new MesInProcessOrderPageReqVO();
            assertEquals(List.of(3L,2L,1L,7L), query(connection, query));
            query.setProductName(" 导管 "); query.setBatchCode("LOT-A");
            assertEquals(List.of(3L,1L,7L), query(connection, query));
            query.setWorkOrderCode("WO-2"); assertTrue(query(connection, query).isEmpty());
            query.setWorkOrderCode(null); query.setBatchCode(null); query.setProductName("二号");
            assertEquals(List.of(2L), query(connection, query));
            query.setProductName("' OR 1=1 --"); assertTrue(query(connection, query).isEmpty());
        }
    }
    private List<Long> query(Connection connection, MesInProcessOrderPageReqVO request) throws Exception {
        var config = new Configuration();
        String script = MesInProcessOrderMapper.class.getMethod("selectCandidates", Long.class, MesInProcessOrderPageReqVO.class).getAnnotation(Select.class).value()[0];
        var source = new XMLLanguageDriver().createSqlSource(config, script, Map.class);
        var params = Map.of("tenantId",1L,"q",request);
        var bound = source.getBoundSql(params);
        var mapped = new MappedStatement.Builder(config,"inprocess",source,SqlCommandType.SELECT).build();
        try (var statement = connection.prepareStatement(bound.getSql())) {
            new DefaultParameterHandler(mapped,params,bound).setParameters(statement);
            var rows = statement.executeQuery();
            List<Long> ids = new ArrayList<>();
            while (rows.next()) ids.add(rows.getLong("id"));
            return ids;
        }
    }
}
