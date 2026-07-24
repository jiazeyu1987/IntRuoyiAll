package cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MesKingdeeProductionMaterialListMapperXmlTest {

    private static final Path MAPPER_XML = Path.of("src", "main", "resources", "mapper", "pro", "workorder",
            "MesKingdeeProductionMaterialListMapper.xml");

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
