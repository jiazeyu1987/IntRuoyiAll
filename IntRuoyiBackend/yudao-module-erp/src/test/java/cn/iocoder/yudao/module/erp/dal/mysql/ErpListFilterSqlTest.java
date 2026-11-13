package cn.iocoder.yudao.module.erp.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionPickListMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionReplenishmentListMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.kingdee.ErpKingdeeStockMoveListMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ErpListFilterSqlTest {
    @Test
    void associatedProductNamesAndPricesUseBoundSqlPredicates() {
        PageParam request = new PageParam();
        request.setListFilters("[{\"key\":\"categoryName\",\"operator\":\"contains\",\"value\":\"精密%\"},"
                + "{\"key\":\"salePrice\",\"operator\":\"between\",\"value\":12,\"valueEnd\":22}]");
        var query = ListFilterQueryUtils.wrapper(request, ErpProductMapper.listFilterFields());
        String sql = query.getSqlSegment();
        assertTrue(sql.contains("erp_product_category"));
        assertTrue(sql.contains("c.tenant_id=erp_product.tenant_id"));
        assertTrue(sql.contains("erp_product.sale_price >="));
        assertTrue(sql.contains("erp_product.sale_price <="));
        assertFalse(sql.contains("精密"));
        assertTrue(query.getParamNameValuePairs().containsValue("%精密!%%"));
    }

    @Test
    void stockSelectionRetainsExactProductIdAndAssociatedDisplayFields() {
        PageParam request = new PageParam();
        request.setListFilters("[{\"key\":\"productId\",\"operator\":\"eq\",\"value\":903},"
                + "{\"key\":\"unitName\",\"operator\":\"contains\",\"value\":\"盒\"}]");
        var query = ListFilterQueryUtils.wrapper(request, ErpStockMapper.listFilterFields());
        assertTrue(query.getSqlSegment().contains("erp_stock.product_id"));
        assertTrue(query.getSqlSegment().contains("erp_product_unit"));
        assertTrue(query.getParamNameValuePairs().containsValue("903"));
    }

    @Test
    void completeJoinedChildDisplayHasNoGroupConcatLengthLimit() throws Exception {
        for (var fields : java.util.List.of(ErpKingdeeProductionPickListMapper.listFilterFields(),
                ErpKingdeeProductionReplenishmentListMapper.listFilterFields(),
                ErpKingdeeStockMoveListMapper.listFilterFields())) {
            String sql = fields.get("materialNames").expression();
            assertFalse(sql.contains("GROUP_CONCAT"));
            assertTrue(sql.contains("JSON_ARRAYAGG"));
            assertTrue(sql.contains("UNBOUNDED FOLLOWING"));
            assertTrue(sql.contains("ORDER BY i.id"));
            assertTrue(sql.contains("i.deleted=0"));
            assertTrue(sql.contains("i.tenant_id="));
            // Tenant/data-permission interceptors parse the same subqueries for both page and COUNT.
            assertNotNull(net.sf.jsqlparser.parser.CCJSqlParserUtil.parse("SELECT " + sql + " FROM erp_stock"));
        }
    }
}
