package cn.iocoder.yudao.module.mes.service.pro.lossreason;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesProLossReasonServiceTest {
    private MesProLossReasonService service;
    private JdbcTemplate jdbc;
    private TransactionTemplate tx;
    private MockedStatic<SecurityFrameworkUtils> security;
    @BeforeEach void setup() {
        var ds = new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE mes_pro_loss_reason_sequence(tenant_id BIGINT PRIMARY KEY,next_number BIGINT NOT NULL)");
        jdbc.execute("CREATE TABLE mes_pro_loss_reason(id BIGINT PRIMARY KEY,tenant_id BIGINT,reason_number BIGINT,reason_code VARCHAR(40),reason_name VARCHAR(128),creator VARCHAR(64),create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, UNIQUE(tenant_id,reason_name),UNIQUE(tenant_id,reason_number))");
        service = new MesProLossReasonService();
        ReflectionTestUtils.setField(service, "jdbc", jdbc);
        tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        TenantContextHolder.setTenantId(1L);
        security = mockStatic(SecurityFrameworkUtils.class);
        security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(12L);
    }
    @AfterEach void cleanup() { security.close(); TenantContextHolder.clear(); }
    private List<MesProLossReasonService.Reason> create(String... values) {
        return tx.execute(status -> service.create(Arrays.asList(values)));
    }
    @Test void numbersAreAutomaticAndContinuePast999() {
        assertEquals("SHYY-001", create(" 调机损耗 ").get(0).reasonCode());
        assertEquals("调机损耗", service.list(null).get(0).reasonName());
        jdbc.update("UPDATE mes_pro_loss_reason_sequence SET next_number=999 WHERE tenant_id=1");
        var rows=create("原因999", "原因1000");
        assertEquals(List.of("SHYY-999","SHYY-1000"),rows.stream().map(MesProLossReasonService.Reason::reasonCode).toList());
    }
    @Test void concurrentWritersReceiveDistinctConsecutiveNumbers() throws Exception {
        var workers = java.util.concurrent.Executors.newFixedThreadPool(4);
        try {
            List<java.util.concurrent.Callable<String>> requests = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                final int index = i;
                requests.add(() -> {
                    TenantContextHolder.setTenantId(1L);
                    try (var login = mockStatic(SecurityFrameworkUtils.class)) {
                        login.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(12L);
                        return create("并发原因" + index).get(0).reasonCode();
                    } finally { TenantContextHolder.clear(); }
                });
            }
            Set<String> codes = new TreeSet<>();
            for (var result : workers.invokeAll(requests)) codes.add(result.get());
            assertEquals(Set.of("SHYY-001", "SHYY-002", "SHYY-003", "SHYY-004", "SHYY-005", "SHYY-006", "SHYY-007", "SHYY-008"), codes);
            assertEquals(8, service.list(null).size());
        } finally { workers.shutdownNow(); }
    }
    @Test void invalidImportIsAtomicAndDoesNotConsumeNumbers() {
        create("调机损耗");
        ServiceException error=assertThrows(ServiceException.class,()->create("新原因"," 调机损耗 ",""));
        assertTrue(error.getMessage().contains("第3行"));
        assertTrue(error.getMessage().contains("第4行"));
        assertEquals(1,service.list(null).size());
        assertEquals("SHYY-002",create("新原因").get(0).reasonCode());
        assertThrows(ServiceException.class,()->create("重复","重复"));
    }
    @Test void tenantCatalogAndNumberingAreIsolated() {
        var first=create("调机损耗").get(0);
        TenantContextHolder.setTenantId(2L);
        assertTrue(service.list(null).isEmpty());
        var second=create("调机损耗").get(0);
        assertEquals("SHYY-001",second.reasonCode());
        assertNotEquals(first.id(),second.id());
        assertThrows(ServiceException.class,()->service.validateBindings(config("[]"),config("[{\"id\":"+first.id()+",\"reasonCode\":\"SHYY-001\",\"reasonName\":\"调机损耗\",\"enabled\":true}]")));
    }
    @Test void bindingsAllowAppendButRejectRemovalEditingDuplicatesAndForgery() {
        var reason=create("调机损耗").get(0);
        String added="{\"id\":"+reason.id()+",\"reasonCode\":\"SHYY-001\",\"reasonName\":\"调机损耗\",\"enabled\":true}";
        JSONObject initial=config("[]"), bound=config("["+added+"]");
        assertDoesNotThrow(()->service.validateBindings(initial,bound));
        assertDoesNotThrow(()->service.validateBindings(bound,bound));
        assertThrows(ServiceException.class,()->service.validateBindings(bound,initial));
        assertThrows(ServiceException.class,()->service.validateBindings(bound,config("["+added.replace("调机损耗","改名")+"]")));
        assertThrows(ServiceException.class,()->service.validateBindings(initial,config("["+added+","+added+"]")));
        assertThrows(ServiceException.class,()->service.validateBindings(initial,config("["+added.replace("SHYY-001","伪造")+"]")));
        assertThrows(ServiceException.class,()->service.validateBindings(bound,JSON.parseObject("{productionProcessConfigs:[]}")));
    }
    @Test void existingFrozenLegacyReasonsRemainValidButCannotBeChanged() {
        var legacy=config("[{id:8301,reasonCode:'LOSS-001',reasonName:'原损耗',enabled:true}]");
        assertDoesNotThrow(()->service.validateBindings(legacy,legacy));
        assertThrows(ServiceException.class,()->service.validateBindings(config("[]"),legacy));
    }
    @Test void explicitMigrationPreservesOnlyExactFormalLegacyReasonsIncludingDisabledState() {
        legacyTables();
        jdbc.update("INSERT INTO mes_pro_route_process VALUES (51,1,90,20,0)");
        jdbc.update("INSERT INTO mes_pro_process_pool_defect_reason VALUES (8301,1,51,20,'LOSS','LOSS-001','原损耗',false,0)");
        var legacy = config("[{id:8301,reasonCode:'LOSS-001',reasonName:'原损耗',enabled:false}]");
        legacy.getJSONArray("productionProcessConfigs").getJSONObject(0).put("processId",20L);
        assertDoesNotThrow(() -> service.validateLegacyBindings(90L,legacy));
        assertThrows(ServiceException.class, () -> service.validateBindings(config("[]"),legacy));
        assertThrows(ServiceException.class, () -> service.validateLegacyBindings(91L,legacy));
        TenantContextHolder.setTenantId(2L);
        assertThrows(ServiceException.class, () -> service.validateLegacyBindings(90L,legacy));
        TenantContextHolder.setTenantId(1L);
        var forged = JSON.parseObject(legacy.toJSONString());
        forged.getJSONArray("productionProcessConfigs").getJSONObject(0).getJSONArray("lossReasons")
                .getJSONObject(0).put("enabled",true);
        assertThrows(ServiceException.class, () -> service.validateLegacyBindings(90L,forged));
        assertThrows(ServiceException.class, () -> service.validateLegacyBindings(90L,config("[]")));
        legacy.getJSONArray("productionProcessConfigs").getJSONObject(0).put("processId",21L);
        assertThrows(ServiceException.class, () -> service.validateLegacyBindings(90L,legacy));
    }
    private void legacyTables() {
        jdbc.execute("CREATE TABLE mes_pro_route_process(id BIGINT PRIMARY KEY,tenant_id BIGINT,route_id BIGINT,process_id BIGINT,deleted BOOLEAN)");
        jdbc.execute("CREATE TABLE mes_pro_process_pool_defect_reason(id BIGINT PRIMARY KEY,tenant_id BIGINT,route_process_id BIGINT,process_id BIGINT,reason_type VARCHAR(40),reason_code VARCHAR(40),reason_name VARCHAR(128),enabled BOOLEAN,deleted BOOLEAN)");
    }
    @Test void browserStringIdentitiesPreserveExistingBindingsWhenAppendingAnotherReason() {
        var catalog = create("调机损耗", "裁切损耗");
        var first = catalog.get(0);
        var second = catalog.get(1);
        String existing = "{id:" + first.id() + ",reasonCode:'SHYY-001',reasonName:'调机损耗',enabled:true}";
        String browserExisting = existing.replace("id:" + first.id(), "id:'" + first.id() + "'");
        String appended = "{id:'" + second.id() + "',reasonCode:'SHYY-002',reasonName:'裁切损耗',enabled:true}";
        JSONObject before = config("[" + existing + "]");
        JSONObject after = config("[" + browserExisting + "," + appended + "]");
        assertDoesNotThrow(() -> service.validateBindings(before, after));
        assertEquals(String.class, after.getJSONArray("productionProcessConfigs").getJSONObject(0)
                .getJSONArray("lossReasons").getJSONObject(0).get("id").getClass());
        assertThrows(ServiceException.class, () -> service.validateBindings(before,
                config("[" + browserExisting.replace("调机损耗", "改名") + "," + appended + "]")));
    }
    private JSONObject config(String reasons) {
        return JSON.parseObject("{productionProcessConfigs:[{routeProcessId:51,lossReasons:"+reasons+"}]}");
    }
}
