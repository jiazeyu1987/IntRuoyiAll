package cn.iocoder.yudao.module.system.service.tenant;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.controller.admin.dict.vo.data.DictDataSaveReqVO;
import cn.iocoder.yudao.module.system.controller.admin.dict.vo.type.DictTypeSaveReqVO;
import cn.iocoder.yudao.module.system.service.dict.*;
import cn.iocoder.yudao.module.system.api.tenant.TenantCreatedEvent;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@Import({DictDataServiceImpl.class, DictTypeServiceImpl.class, TenantDictionaryInitializer.class,
        TenantNotificationIsolationTest.SqlConfiguration.class})
class TenantDictionaryIsolationTest extends BaseDbUnitTest {
    @Resource private DictTypeService typeService;
    @Resource private DictDataService dataService;
    @Resource private JdbcTemplate jdbcTemplate;
    @Resource private TenantDictionaryInitializer initializer;
    @Resource private PlatformTransactionManager transactions;

    @Test
    void dictionaryWritesAndMixedBatchDeletesStayInCurrentTenantEvenWhenIgnoreIsSet() {
        TenantContextHolder.setTenantId(11L);
        Long typeA = typeService.createDictType(type("Mode", "mode"));
        Long dataA = dataService.createDictData(data("Production"));
        TenantContextHolder.setTenantId(22L);
        Long typeB = typeService.createDictType(type("Mode", "mode"));
        Long dataB = dataService.createDictData(data("Test"));
        TenantContextHolder.setIgnore(true);
        assertNull(dataService.getDictData(dataA));
        assertNull(typeService.getDictType(typeA));
        assertThrows(RuntimeException.class, () -> dataService.updateDictData(data("Illegal").setId(dataA)));
        assertThrows(RuntimeException.class, () -> dataService.deleteDictDataList(List.of(dataB, dataA)));
        assertNotNull(dataService.getDictData(dataB));
        dataService.updateDictData(data("Test changed").setId(dataB));
        dataService.deleteDictData(dataB);
        assertThrows(RuntimeException.class, () -> typeService.deleteDictTypeList(List.of(typeB, typeA)));
        assertNotNull(typeService.getDictType(typeB));
        TenantContextHolder.setTenantId(11L);
        assertEquals("Production", dataService.getDictData("mode", "1").getLabel());
        TenantContextHolder.clear();
        assertThrows(RuntimeException.class, () -> dataService.getDictData(dataA));
    }

    @Test
    void initializationIsIndependentTransactionalAndDoesNotRefillAnIntentionallyEmptyDictionary() {
        jdbcTemplate.update("INSERT INTO system_dict_type (id,name,type,deleted_time) VALUES (10,'Mode','mode','1970-01-01')");
        jdbcTemplate.update("INSERT INTO system_dict_data (id,dict_type,value,label) VALUES (20,'mode','1','Original')");
        var tx = new TransactionTemplate(transactions);
        for (long id : new long[]{11,22}) {
            TenantContextHolder.setTenantId(id);
            tx.executeWithoutResult(status -> {
                jdbcTemplate.update("INSERT INTO system_tenant (id,name,contact_name,status,package_id,expire_time,account_count) VALUES (?,?,'Contact',0,1,'2099-01-01',10)", id, "Tenant " + id);
                initializer.initialize(new TenantCreatedEvent(id));
            });
        }
        Long id = dataService.getDictData("mode", "1").getId();
        dataService.deleteDictData(id);
        tx.executeWithoutResult(status -> initializer.initialize(new TenantCreatedEvent(22L)));
        assertNull(dataService.getDictData("mode", "1"));
        TenantContextHolder.setTenantId(11L);
        assertEquals("Original", dataService.getDictData("mode", "1").getLabel());
        TenantContextHolder.setTenantId(33L);
        assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(status -> {
            jdbcTemplate.update("INSERT INTO system_tenant (id,name,contact_name,status,package_id,expire_time,account_count) VALUES (33,'Rollback','Contact',0,1,'2099-01-01',10)");
            initializer.initialize(new TenantCreatedEvent(33L));
            throw new IllegalStateException("LATER_STEP_FAILED");
        }));
        assertEquals(0L, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM system_tenant_dict_data WHERE tenant_id=33", Long.class));
        assertEquals(0L, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM system_tenant_dict_snapshot WHERE tenant_id=33", Long.class));
    }

    private DictTypeSaveReqVO type(String name, String type) {
        return new DictTypeSaveReqVO().setName(name).setType(type).setStatus(0);
    }
    private DictDataSaveReqVO data(String label) {
        return new DictDataSaveReqVO().setDictType("mode").setValue("1").setLabel(label).setSort(1).setStatus(0).setColorType("").setCssClass("");
    }
}
