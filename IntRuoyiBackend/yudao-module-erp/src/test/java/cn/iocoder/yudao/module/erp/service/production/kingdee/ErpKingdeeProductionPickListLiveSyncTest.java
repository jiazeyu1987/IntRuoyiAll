package cn.iocoder.yudao.module.erp.service.production.kingdee;

import cn.hutool.extra.spring.SpringUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.datasource.config.YudaoDataSourceAutoConfiguration;
import cn.iocoder.yudao.framework.mybatis.config.YudaoMybatisAutoConfiguration;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.service.config.ErpKingdeeConfigService;
import cn.iocoder.yudao.module.erp.service.production.sync.ErpKingdeeProductionPickListClientImpl;
import cn.iocoder.yudao.module.erp.service.production.sync.ErpKingdeeProductionPickListSyncResult;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProperties;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeRestTemplateConfiguration;
import com.alibaba.druid.spring.boot3.autoconfigure.DruidDataSourceAutoConfigure;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.github.yulichang.autoconfigure.MybatisPlusJoinAutoConfiguration;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@EnabledIfSystemProperty(named = "erp.kingdee.live", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = ErpKingdeeProductionPickListLiveSyncTest.Application.class)
class ErpKingdeeProductionPickListLiveSyncTest {

    @Resource
    private ErpKingdeeProductionPickListService productionPickListService;
    @Resource
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private ErpKingdeeConfigService kingdeeConfigService;

    @DynamicPropertySource
    static void registerDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> requiredProperty("erp.kingdee.live.jdbc-url"));
        registry.add("spring.datasource.username", () -> requiredProperty("erp.kingdee.live.jdbc-user"));
        registry.add("spring.datasource.password",
                () -> requiredEnvironment("ERP_KINGDEE_LIVE_JDBC_PASSWORD"));
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.datasource.druid.async-init", () -> false);
        registry.add("yudao.info.base-package",
                () -> "cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee");
    }

    @Test
    void syncModifiedBetween_fromConfiguredTestAccount_persistsHeadersAndLines() {
        TenantContextHolder.setTenantId(1L);
        String activeConnection = jdbcTemplate.queryForObject("""
                SELECT value
                FROM infra_config
                WHERE config_key = 'yudao.erp.kingdee.connection.active'
                  AND deleted = b'0'
                LIMIT 1
                """, String.class);
        assertEquals("TEST", activeConnection);

        String configJson = jdbcTemplate.queryForObject("""
                SELECT value
                FROM infra_config
                WHERE config_key = 'yudao.erp.kingdee.config'
                  AND deleted = b'0'
                LIMIT 1
                """, String.class);
        ErpKingdeeProperties properties = JsonUtils.parseObject(configJson,
                ErpKingdeeProperties.class);
        assertNotNull(properties);
        properties.validateBaseConfig();
        when(kingdeeConfigService.getEffectiveProperties()).thenReturn(properties);

        jdbcTemplate.update("""
                INSERT INTO erp_kingdee_production_pick_list (
                  source_form_id, source_fid, source_bill_no, last_sync_time,
                  create_time, update_time, deleted, tenant_id
                )
                SELECT 'PRD_PickMtrl', '__LIVE_SYNC_SEED__',
                       '__LIVE_SYNC_SEED__', NOW(), NOW(), NOW(), b'0', 1
                WHERE NOT EXISTS (
                  SELECT 1
                  FROM erp_kingdee_production_pick_list
                  WHERE source_form_id = 'PRD_PickMtrl'
                    AND source_fid = '__LIVE_SYNC_SEED__'
                    AND deleted = b'0'
                )
                """);

        ErpKingdeeProductionPickListSyncResult result;
        try {
            result = productionPickListService.syncModifiedBetween(
                    LocalDateTime.of(2026, 1, 26, 0, 0),
                    LocalDateTime.of(2026, 1, 27, 0, 0));
        } finally {
            jdbcTemplate.update("""
                    DELETE FROM erp_kingdee_production_pick_list
                    WHERE source_form_id = 'PRD_PickMtrl'
                      AND source_fid = '__LIVE_SYNC_SEED__'
                    """);
        }

        Integer headerCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM erp_kingdee_production_pick_list
                WHERE source_form_id = 'PRD_PickMtrl'
                  AND deleted = b'0'
                  AND bill_date >= '2026-01-26 00:00:00'
                  AND bill_date < '2026-01-27 00:00:00'
                """, Integer.class);
        Integer lineCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM erp_kingdee_production_pick_list_item i
                INNER JOIN erp_kingdee_production_pick_list h
                  ON h.id = i.production_pick_list_id
                 AND h.deleted = b'0'
                WHERE i.source_form_id = 'PRD_PickMtrl'
                  AND i.deleted = b'0'
                  AND h.bill_date >= '2026-01-26 00:00:00'
                  AND h.bill_date < '2026-01-27 00:00:00'
                """, Integer.class);
        String sampleBillNo = jdbcTemplate.queryForObject("""
                SELECT source_bill_no
                FROM erp_kingdee_production_pick_list
                WHERE source_form_id = 'PRD_PickMtrl'
                  AND deleted = b'0'
                  AND bill_date >= '2026-01-26 00:00:00'
                  AND bill_date < '2026-01-27 00:00:00'
                ORDER BY bill_date DESC, id DESC
                LIMIT 1
                """, String.class);

        assertTrue(result.getCreatedCount() + result.getUpdatedCount() > 0);
        assertNotNull(headerCount);
        assertTrue(headerCount > 0);
        assertNotNull(lineCount);
        assertTrue(lineCount > 0);
        assertNotNull(sampleBillNo);
        assertTrue(sampleBillNo.startsWith("88"));
    }

    private static String requiredProperty(String name) {
        String value = System.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required system property: " + name);
        }
        return value;
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + name);
        }
        return value;
    }

    @Import({
            YudaoDataSourceAutoConfiguration.class,
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            JdbcTemplateAutoConfiguration.class,
            DruidDataSourceAutoConfigure.class,
            YudaoMybatisAutoConfiguration.class,
            MybatisPlusAutoConfiguration.class,
            MybatisPlusJoinAutoConfiguration.class,
            SpringUtil.class,
            ErpKingdeeRestTemplateConfiguration.class,
            ErpKingdeeProductionPickListClientImpl.class,
            ErpKingdeeProductionPickListServiceImpl.class
    })
    static class Application {
    }

}
