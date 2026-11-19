package cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder;

import org.junit.jupiter.api.Test;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.kingdee.MesKingdeeProductionMaterialListPageReqVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MesKingdeeProductionMaterialListMapperXmlTest {

    private static final Path MAPPER_XML = Path.of("src", "main", "resources", "mapper", "pro", "workorder",
            "MesKingdeeProductionMaterialListMapper.xml");

    @Test
    void groupPage_shouldRenderDeferredSummaryAndKeepTenantScope() throws IOException {
        var request = new MesKingdeeProductionMaterialListPageReqVO();
        BoundSql bound = groupSql(request, new QueryWrapper<>());
        String sql = bound.getSql();
        assertTrue(sql.indexOf("JSON_ARRAYAGG") < sql.indexOf("SELECT\n                base.source_bill_no"), sql);
        assertFalse(sql.contains("summary.production_order_summary"), sql);
        String scoped = tenantInterceptor().parserSingle(sql, null);
        for (String alias : new String[]{"base", "matched", "i", "earlier"}) {
            assertTrue(scoped.contains(alias + ".tenant_id = 186"), scoped);
        }
        Files.createDirectories(Path.of("target", "erp-material-query"));
        Files.writeString(Path.of("target", "erp-material-query", "default.sql"), scoped, StandardCharsets.UTF_8);
    }

    @Test
    void groupPage_exactCodeAndSummaryColumnFilter_shouldRenderTogether() throws IOException {
        var request = new MesKingdeeProductionMaterialListPageReqVO();
        request.setSyncCode("881PPBOM00000440");
        request.setListFilters("[{\"key\":\"productionOrderSummary\",\"operator\":\"contains\",\"value\":\"881MO099824\"}]");
        BoundSql bound = groupSql(request, MesKingdeeProductionMaterialListMapper.groupFilters(request));
        assertEquals(3, bound.getParameterMappings().size());
        assertTrue(bound.getSql().contains("base.source_bill_no = ?"));
        assertFalse(bound.getSql().contains("summary.production_order_summary"));
        String scoped = tenantInterceptor().parserSingle(bound.getSql(), null);
        assertTrue(scoped.replaceAll("\\s+", "").contains("JSON_ARRAYAGG(i.production_order_no)"), scoped);
        Files.createDirectories(Path.of("target", "erp-material-query"));
        Files.writeString(Path.of("target", "erp-material-query", "filtered.sql"), scoped, StandardCharsets.UTF_8);
    }

    private static BoundSql groupSql(MesKingdeeProductionMaterialListPageReqVO request,
                                     QueryWrapper<Object> filters) throws IOException {
        Configuration configuration = new Configuration();
        try (var input = Files.newInputStream(MAPPER_XML)) {
            new XMLMapperBuilder(input, configuration, MAPPER_XML.toString(), configuration.getSqlFragments()).parse();
        }
        return configuration.getMappedStatement(MesKingdeeProductionMaterialListMapper.class.getName()
                + ".selectGroupPage").getBoundSql(Map.of("reqVO", request, "ew", filters));
    }

    private static TenantLineInnerInterceptor tenantInterceptor() {
        return new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                return new LongValue(186);
            }
        });
    }

    @Test
    void refreshWorkOrderLinks_shouldPassRuntimeTenantSqlParser() throws IOException {
        String mapperXml = Files.readString(MAPPER_XML, StandardCharsets.UTF_8);
        String sql = selectSql(mapperXml, "refreshWorkOrderLinks", "update")
                .replaceAll("<update[^>]*>|</update>", "")
                .replace("#{tenantId}", "?").replace("&lt;", "<").replace("&gt;", ">");
        var interceptor = new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                return new LongValue(186);
            }
        });
        String scoped = interceptor.parserMulti(sql, null);
        assertTrue(scoped.contains("CAST(w.code AS BINARY)"), scoped);
        assertTrue(scoped.contains("m.tenant_id = 186"), scoped);
        assertTrue(scoped.contains("w.tenant_id = ? AND w.deleted = 0"), scoped);
        assertTrue(scoped.contains("WHERE tenant_id = ? AND deleted = 0 GROUP BY"), scoped);
    }

    @Test
    void selectGroupPage_shouldFilterMatchedBillsButAggregateWholeBill() throws IOException {
        String mapperXml = Files.readString(MAPPER_XML, StandardCharsets.UTF_8);
        String sql = selectSql(mapperXml, "selectGroupPage");
        String sharedSql = selectSql(mapperXml, "GroupPageQuery", "sql");

        assertTrue(sharedSql.contains("COUNT(*) AS line_count"),
                "分组主表必须按整单聚合全部子项数量");
        assertTrue(sharedSql.contains("MAX(base.source_modify_time) AS source_modify_time"),
                "分组主表必须按整单聚合 ERP 修改时间");
        assertTrue(sharedSql.contains("MAX(base.last_sync_time) AS last_sync_time"),
                "分组主表必须按整单聚合最后同步时间");
        assertTrue(sharedSql.contains("WHERE EXISTS"),
                "分组主表必须先按命中明细筛选入选单据");
        assertTrue(sharedSql.contains("matched.source_bill_no = summary.source_bill_no"),
                "命中过滤必须回到整单汇总结果上");
        assertTrue(selectSql(mapperXml, "MatchedBillFilters", "sql")
                        .contains("matched.child_material_code LIKE CONCAT('%', #{reqVO.childMaterialCode}, '%')"),
                "子项编码筛选必须作用于命中明细");
        assertTrue(selectSql(mapperXml, "MatchedBillFilters", "sql")
                        .contains("matched.child_material_name LIKE CONCAT('%', #{reqVO.childMaterialName}, '%')"),
                "子项名称筛选必须作用于命中明细");
        assertTrue(sql.contains("ORDER BY summary.source_modify_time DESC, summary.last_sync_time DESC, summary.source_bill_no DESC"),
                "分组主表排序必须稳定");
    }

    @Test
    void selectGroupList_shouldReuseSameWholeBillSemantics() throws IOException {
        String mapperXml = Files.readString(MAPPER_XML, StandardCharsets.UTF_8);
        String sql = selectSql(mapperXml, "selectGroupList");

        assertTrue(sql.contains("<include refid=\"GroupPageQuery\"/>"),
                "不分页分组列表必须复用同一套整单聚合语义");
    }

    private static String selectSql(String mapperXml, String selectId) {
        return selectSql(mapperXml, selectId, "select");
    }

    private static String selectSql(String mapperXml, String selectId, String tagName) {
        Pattern pattern = Pattern.compile("<" + tagName + " id=\"" + selectId + "\"[\\s\\S]*?</" + tagName + ">");
        Matcher matcher = pattern.matcher(mapperXml);
        assertTrue(matcher.find(), "Mapper XML 缺少 " + tagName + ": " + selectId);
        return matcher.group();
    }

}
