package cn.iocoder.yudao.module.dcc.dal.mysql.projectcode;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectProductRelationDO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.nio.file.Files;
import java.nio.file.Path;
import org.yaml.snakeyaml.Yaml;
import cn.iocoder.yudao.framework.tenant.config.TenantProperties;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.db.TenantDatabaseInterceptor;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.apache.ibatis.builder.MapperBuilderAssistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DccProjectProductRelationMapperTest extends BaseDbUnitTest {

    @Resource
    private DccProjectProductRelationMapper mapper;

    @Test
    void catalogLookupOnlyReturnsActiveUndeletedRelationsInRequestedPage() {
        var active = insert(10L, "ACTIVE");
        insert(11L, "INACTIVE");
        var deleted = insert(12L, "ACTIVE");
        mapper.deleteById(deleted.getId());
        insert(13L, "ACTIVE");

        var relations = mapper.selectActiveByProductCatalogIds(List.of(10L, 11L, 12L));

        assertEquals(List.of(active.getId()), relations.stream().map(DccProjectProductRelationDO::getId).toList());
        assertEquals(110L, relations.get(0).getProjectCodeId());
        assertTrue(mapper.selectActiveByProductCatalogIds(List.of()).isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shippedTenantConfigurationScopesFormalProjectRelations() throws Exception {
        Map<String, Object> root = new HashMap<>();
        try (var input = Files.newInputStream(Path.of("../yudao-server/src/main/resources/application.yaml"))) {
            for (Object document : new Yaml().loadAll(input)) {
                root.putAll((Map<String, Object>) document);
            }
        }
        var tenant = (Map<String, Object>) ((Map<String, Object>) root.get("yudao")).get("tenant");
        var properties = new TenantProperties();
        properties.setIgnoreTables(new HashSet<>((List<String>) tenant.get("ignore-tables")));
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                DccProjectProductRelationDO.class);
        var interceptor = new TenantLineInnerInterceptor(new TenantDatabaseInterceptor(properties));
        try {
            for (long tenantId : new long[] {1L, 2L}) {
                TenantContextHolder.setTenantId(tenantId);
                String sql = interceptor.parserSingle("SELECT * FROM dcc_project_product_relation "
                        + "WHERE product_catalog_id IN (10, 11) AND relation_status = 'ACTIVE' AND deleted = 0", null);
                assertTrue(sql.contains("tenant_id = " + tenantId), sql);
            }
        } finally {
            TenantContextHolder.clear();
        }
    }

    private DccProjectProductRelationDO insert(Long productCatalogId, String status) {
        var relation = DccProjectProductRelationDO.builder().requestId(productCatalogId)
                .projectCodeId(productCatalogId + 100L).productCatalogId(productCatalogId)
                .relationStatus(status).build();
        relation.setTenantId(1L);
        mapper.insert(relation);
        return relation;
    }
}
