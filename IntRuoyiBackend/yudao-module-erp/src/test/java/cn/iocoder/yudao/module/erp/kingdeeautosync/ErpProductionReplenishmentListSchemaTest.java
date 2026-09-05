package cn.iocoder.yudao.module.erp.kingdeeautosync;

import cn.iocoder.yudao.module.erp.controller.admin.production.vo.ErpProductionReplenishmentListPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.production.vo.ErpProductionReplenishmentListRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionReplenishmentListDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionReplenishmentListItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionReplenishmentListItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionReplenishmentListMapper;
import com.baomidou.mybatisplus.annotation.TableName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpProductionReplenishmentListSchemaTest {

    @Test
    void shouldMapProductionReplenishmentListDoToDedicatedSyncTables() throws Exception {
        TableName headerTable = ErpKingdeeProductionReplenishmentListDO.class.getAnnotation(TableName.class);
        TableName itemTable = ErpKingdeeProductionReplenishmentListItemDO.class.getAnnotation(TableName.class);

        assertEquals("erp_kingdee_production_replenishment_list", headerTable.value());
        assertEquals("erp_kingdee_production_replenishment_list_item", itemTable.value());
        assertEquals(String.class, ErpKingdeeProductionReplenishmentListDO.class.getDeclaredField("sourceBillNo").getType());
        assertEquals(String.class, ErpKingdeeProductionReplenishmentListDO.class.getDeclaredField("sourceFid").getType());
        assertEquals(String.class, ErpKingdeeProductionReplenishmentListItemDO.class.getDeclaredField("sourceLineKey").getType());
        assertEquals(BigDecimal.class, ErpKingdeeProductionReplenishmentListItemDO.class.getDeclaredField("requestedQuantity").getType());
        assertEquals(BigDecimal.class, ErpKingdeeProductionReplenishmentListItemDO.class.getDeclaredField("actualQuantity").getType());
        assertEquals(LocalDateTime.class, ErpKingdeeProductionReplenishmentListDO.class.getDeclaredField("sourceModifyTime").getType());
    }

    @Test
    void migrationMustCreateTenantScopedHeaderItemTablesAndJob() throws IOException {
        String sql = Files.readString(
                Path.of("../sql/mysql/20260905_erp_kingdee_production_replenishment_list_sync.sql").normalize(),
                StandardCharsets.UTF_8);

        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS erp_kingdee_production_replenishment_list"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS erp_kingdee_production_replenishment_list_item"));
        assertTrue(sql.contains("source_form_id varchar(64) NOT NULL DEFAULT 'PRD_FeedMtrl'"));
        assertTrue(sql.contains("tenant_id bigint NOT NULL DEFAULT 0"));
        assertTrue(sql.contains("UNIQUE KEY uk_erp_kingdee_prod_replenishment_list_source"));
        assertTrue(sql.contains("UNIQUE KEY uk_erp_kingdee_prod_replenishment_list_item_source"));
        assertTrue(sql.contains("kingdeeProductionReplenishmentListSyncJob"));
        assertTrue(sql.contains("erp:production-replenishment-list:query"));
        assertTrue(sql.contains("ErpProductionReplenishmentList"));
        assertFalse(sql.contains("6034"), "生产补料单菜单迁移不得占用发票凭证打印菜单 ID 6034");
        assertFalse(sql.contains("6035"), "生产补料单查询权限迁移不得占用发票凭证打印权限 ID 6035");
    }

    @Test
    void mapperAndVoMustExposePageAndLineContract() throws Exception {
        assertNotNull(ErpKingdeeProductionReplenishmentListMapper.class.getMethod("selectPage",
                ErpProductionReplenishmentListPageReqVO.class));
        assertNotNull(ErpKingdeeProductionReplenishmentListMapper.class.getMethod("selectBySource",
                String.class, String.class));
        assertNotNull(ErpKingdeeProductionReplenishmentListItemMapper.class.getMethod(
                "selectReplenishmentListIdsByProductionOrderNo", String.class));
        assertNotNull(ErpKingdeeProductionReplenishmentListItemMapper.class.getMethod(
                "deleteByProductionReplenishmentListId", Long.class));
        assertEquals(String.class,
                ErpProductionReplenishmentListPageReqVO.class.getDeclaredField("sourceBillNo").getType());
        assertEquals(String.class,
                ErpProductionReplenishmentListRespVO.class.getDeclaredField("productionOrderNos").getType());
        assertEquals(String.class,
                ErpProductionReplenishmentListRespVO.Item.class.getDeclaredField("materialNumber").getType());
    }
}
