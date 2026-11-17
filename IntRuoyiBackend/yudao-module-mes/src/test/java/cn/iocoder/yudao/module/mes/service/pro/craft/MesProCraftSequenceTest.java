package cn.iocoder.yudao.module.mes.service.pro.craft;

import cn.iocoder.yudao.module.mes.dal.mysql.pro.craft.MesProCraftMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class MesProCraftSequenceTest {
    private JdbcTemplate jdbc;
    private String ensureSql;
    private MesProCraftService service;

    @BeforeEach
    void setup() throws Exception {
        var source = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE mes_pro_craft_sequence (tenant_id BIGINT PRIMARY KEY,last_number BIGINT NOT NULL)");
        jdbc.execute("CREATE TABLE mes_pro_craft (tenant_id BIGINT,code VARCHAR(64),deleted BOOLEAN,UNIQUE(tenant_id,code))");
        ensureSql = String.join(" ", MesProCraftMapper.class.getMethod("ensureSequence", Long.class)
                .getAnnotation(Insert.class).value()).replace("#{tenantId}", "?");
        String maxSql = String.join(" ", MesProCraftMapper.class.getMethod("selectMaxCodeNumber", Long.class)
                .getAnnotation(Select.class).value()).replace("#{tenantId}", "?");
        var mapper = mock(MesProCraftMapper.class);
        doAnswer(call -> { jdbc.update(ensureSql, (Long) call.getArgument(0)); return null; })
                .when(mapper).ensureSequence(anyLong());
        when(mapper.lockSequence(anyLong())).thenAnswer(call -> jdbc.queryForObject(
                "SELECT last_number FROM mes_pro_craft_sequence WHERE tenant_id=? FOR UPDATE", Long.class, (Long) call.getArgument(0)));
        when(mapper.selectMaxCodeNumber(anyLong())).thenAnswer(call -> jdbc.queryForObject(maxSql, Long.class, (Long) call.getArgument(0)));
        when(mapper.advanceSequence(anyLong(), anyLong())).thenAnswer(call -> jdbc.update(
                "UPDATE mes_pro_craft_sequence SET last_number=? WHERE tenant_id=?", (Long) call.getArgument(1), (Long) call.getArgument(0)));
        service = new MesProCraftService();
        ReflectionTestUtils.setField(service, "craftMapper", mapper);
    }

    private long ensure(long tenant) {
        TenantContextHolder.setTenantId(tenant);
        service.lockCatalog();
        return jdbc.queryForObject("SELECT last_number FROM mes_pro_craft_sequence WHERE tenant_id=?", Long.class, tenant);
    }

    @AfterEach void clearTenant() { TenantContextHolder.clear(); }

    @Test void emptyTenantStartsAtZero() { assertEquals(0L, ensure(189)); }

    @Test void importedCraftsSeedMissingSequenceAndRemainTenantScoped() {
        jdbc.update("INSERT INTO mes_pro_craft VALUES (189,'GY-0009',false),(189,'GY-0045',false),(190,'GY-9999',false)");
        assertEquals(45L, ensure(189));
    }

    @Test void staleSequenceIncludesDeletedCodesAndNumericOrder() {
        jdbc.update("INSERT INTO mes_pro_craft_sequence VALUES (189,0)");
        jdbc.update("INSERT INTO mes_pro_craft VALUES (189,'GY-9999',false),(189,'GY-10000',true)");
        assertEquals(10000L, ensure(189));
    }

    @Test void advancedSequenceIsNeverRewound() {
        jdbc.update("INSERT INTO mes_pro_craft_sequence VALUES (189,80)");
        jdbc.update("INSERT INTO mes_pro_craft VALUES (189,'GY-0045',false)");
        assertEquals(80L, ensure(189));
        assertEquals(80L, ensure(189));
    }
}
