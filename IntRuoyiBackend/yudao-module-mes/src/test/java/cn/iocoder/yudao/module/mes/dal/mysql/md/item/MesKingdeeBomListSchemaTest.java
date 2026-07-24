package cn.iocoder.yudao.module.mes.dal.mysql.md.item;

import cn.iocoder.yudao.module.mes.controller.admin.md.item.vo.kingdee.MesKingdeeBomListPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesKingdeeBomListDO;
import com.baomidou.mybatisplus.annotation.TableName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesKingdeeBomListSchemaTest {

    @Test
    void shouldMapKingdeeBomListToDedicatedTable() throws Exception {
        TableName tableName = MesKingdeeBomListDO.class.getAnnotation(TableName.class);

        assertEquals("mes_kingdee_bom_list", tableName.value());
        assertEquals(String.class, MesKingdeeBomListDO.class.getDeclaredField("sourceFid").getType());
        assertEquals(String.class, MesKingdeeBomListDO.class.getDeclaredField("bomNumber").getType());
        assertEquals(String.class, MesKingdeeBomListDO.class.getDeclaredField("parentMaterialCode").getType());
        assertEquals(String.class, MesKingdeeBomListDO.class.getDeclaredField("childMaterialCode").getType());
        assertEquals(BigDecimal.class, MesKingdeeBomListDO.class.getDeclaredField("numerator").getType());
        assertNotNull(MesKingdeeBomListMapper.class.getMethod("selectPage", MesKingdeeBomListPageReqVO.class));
    }

    @Test
    void shouldDeclareRequiredSqlAndMenu() throws Exception {
        String schemaSql = Files.readString(
                Path.of("../sql/mysql/20260613_mes_kingdee_bom_list.sql").normalize(), StandardCharsets.UTF_8);
        String menuSql = Files.readString(
                Path.of("../sql/mysql/20260613_erp_bom_list_menu.sql").normalize(), StandardCharsets.UTF_8);

        assertTrue(schemaSql.contains("CREATE TABLE IF NOT EXISTS `mes_kingdee_bom_list`"));
        assertTrue(schemaSql.contains("`source_form_id` varchar(64) NOT NULL DEFAULT 'ENG_BOM'"));
        assertTrue(schemaSql.contains("uk_mes_kingdee_bom_list_source"));
        assertTrue(menuSql.contains("6023, '物料清单'"));
        assertTrue(menuSql.contains("'erp/production/bom-list/index'"));
        assertTrue(menuSql.contains("'ErpBomList'"));
        assertTrue(menuSql.contains("6024, '物料清单查询', 'erp:bom-list:query'"));
    }

}
