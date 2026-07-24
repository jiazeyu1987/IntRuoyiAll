package cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder;

import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.kingdee.MesKingdeeProductionMaterialListDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.kingdee.MesKingdeeProductionMaterialListGroupRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.kingdee.MesKingdeeProductionMaterialListPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesKingdeeProductionMaterialListDO;
import com.baomidou.mybatisplus.annotation.TableName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesKingdeeProductionMaterialListSchemaTest {

    @Test
    void shouldMapProductionMaterialListDoToDedicatedSyncTable() throws Exception {
        TableName tableName = MesKingdeeProductionMaterialListDO.class.getAnnotation(TableName.class);

        assertEquals("mes_kingdee_production_material_list", tableName.value());
        assertEquals(String.class, MesKingdeeProductionMaterialListDO.class.getDeclaredField("sourceBillNo").getType());
        assertEquals(String.class, MesKingdeeProductionMaterialListDO.class.getDeclaredField("productionOrderNo").getType());
        assertEquals(Integer.class, MesKingdeeProductionMaterialListDO.class.getDeclaredField("productionOrderLineNo").getType());
        assertEquals(String.class, MesKingdeeProductionMaterialListDO.class.getDeclaredField("childMaterialCode").getType());
        assertEquals(BigDecimal.class, MesKingdeeProductionMaterialListDO.class.getDeclaredField("requiredQuantity").getType());
        assertEquals(LocalDateTime.class, MesKingdeeProductionMaterialListDO.class.getDeclaredField("demandTime").getType());
        assertEquals(String.class, MesKingdeeProductionMaterialListDO.class.getDeclaredField("rawPayload").getType());
    }

    @Test
    void shouldDeclareRequiredSqlFieldsAndIndexes() throws Exception {
        String sql = Files.readString(
                Path.of("../sql/mysql/20260613_mes_kingdee_production_material_list.sql").normalize(),
                StandardCharsets.UTF_8);

        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `mes_kingdee_production_material_list`"));
        assertTrue(sql.contains("`source_bill_no` varchar(64) NOT NULL"));
        assertTrue(sql.contains("`production_order_no` varchar(64) NOT NULL"));
        assertTrue(sql.contains("`child_material_code` varchar(64) NOT NULL"));
        assertTrue(sql.contains("`required_quantity` decimal(24,6) NOT NULL"));
        assertTrue(sql.contains("`raw_payload` longtext DEFAULT NULL"));
        assertTrue(sql.contains("uk_mes_kingdee_prod_material_list_source"));
        assertTrue(sql.contains("idx_mes_kingdee_prod_material_list_order"));
        assertTrue(sql.contains("idx_mes_kingdee_prod_material_list_work_order"));
    }

    @Test
    void shouldExposePageQueryForProductionMaterialList() throws Exception {
        assertNotNull(MesKingdeeProductionMaterialListMapper.class.getMethod("selectPage",
                MesKingdeeProductionMaterialListPageReqVO.class));
        assertNotNull(MesKingdeeProductionMaterialListMapper.class.getMethod("selectGroupList",
                MesKingdeeProductionMaterialListPageReqVO.class));
        assertNotNull(MesKingdeeProductionMaterialListMapper.class.getMethod("selectListBySourceBillNo",
                String.class));
        assertEquals(String.class,
                MesKingdeeProductionMaterialListPageReqVO.class.getDeclaredField("sourceBillNo").getType());
        assertEquals(String.class,
                MesKingdeeProductionMaterialListPageReqVO.class.getDeclaredField("productionOrderNo").getType());
        assertEquals(String.class,
                MesKingdeeProductionMaterialListPageReqVO.class.getDeclaredField("productCode").getType());
        assertEquals(String.class,
                MesKingdeeProductionMaterialListPageReqVO.class.getDeclaredField("childMaterialCode").getType());
    }

    @Test
    void shouldExposeGroupedAndDetailRespFields() throws Exception {
        assertEquals(String.class,
                MesKingdeeProductionMaterialListGroupRespVO.class.getDeclaredField("sourceBillNo").getType());
        assertEquals(Long.class,
                MesKingdeeProductionMaterialListGroupRespVO.class.getDeclaredField("lineCount").getType());
        assertEquals(LocalDateTime.class,
                MesKingdeeProductionMaterialListGroupRespVO.class.getDeclaredField("sourceModifyTime").getType());
        assertEquals(LocalDateTime.class,
                MesKingdeeProductionMaterialListGroupRespVO.class.getDeclaredField("lastSyncTime").getType());

        assertEquals(String.class,
                MesKingdeeProductionMaterialListDetailRespVO.class.getDeclaredField("childMaterialCode").getType());
        assertEquals(String.class,
                MesKingdeeProductionMaterialListDetailRespVO.class.getDeclaredField("childMaterialName").getType());
        assertEquals(String.class,
                MesKingdeeProductionMaterialListDetailRespVO.class.getDeclaredField("childMaterialSpecification").getType());
        assertEquals(String.class,
                MesKingdeeProductionMaterialListDetailRespVO.class.getDeclaredField("childMaterialType").getType());
        assertEquals(BigDecimal.class,
                MesKingdeeProductionMaterialListDetailRespVO.class.getDeclaredField("numerator").getType());
        assertEquals(BigDecimal.class,
                MesKingdeeProductionMaterialListDetailRespVO.class.getDeclaredField("denominator").getType());
        assertEquals(String.class,
                MesKingdeeProductionMaterialListDetailRespVO.class.getDeclaredField("childUnitName").getType());
    }

    @Test
    void shouldDeclareErpProductionMaterialListMenu() throws Exception {
        String sql = Files.readString(
                Path.of("../sql/mysql/20260613_erp_production_material_list_menu.sql").normalize(),
                StandardCharsets.UTF_8);

        assertTrue(sql.contains("6020, '生产管理'"));
        assertTrue(sql.contains("6021, '生产用料清单'"));
        assertTrue(sql.contains("'erp/production/material-list/index'"));
        assertTrue(sql.contains("'ErpProductionMaterialList'"));
        assertTrue(sql.contains("6022, '生产用料清单查询', 'erp:production-material-list:query'"));
        assertTrue(sql.contains("WHERE rm.`menu_id` = 2563"));
    }

}
