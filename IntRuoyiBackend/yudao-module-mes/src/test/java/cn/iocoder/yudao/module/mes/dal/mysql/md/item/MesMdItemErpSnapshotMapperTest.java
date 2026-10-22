package cn.iocoder.yudao.module.mes.dal.mysql.md.item;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MesMdItemErpSnapshotMapperTest extends BaseDbUnitTest {
    @Resource private MesMdItemMapper mapper;
    @Test void clearsSpecificationAndPreservesLocalBatchConfiguration() {
        var row = new MesMdItemDO().setCode("ERP-CLEAR-SPEC").setName("旧物料")
                .setSpecification("旧规格").setUnitMeasureId(1L).setItemTypeId(1L).setStatus(0)
                .setBatchFlag(true).setHighValue(true).setRemark("本地配置");
        mapper.insert(row);
        assertEquals(1, mapper.updateErpMaterialSnapshot(new MesMdItemDO().setId(row.getId())
                .setCode(row.getCode()).setName("新物料").setSpecification(null)
                .setUnitMeasureId(2L).setItemTypeId(1L).setStatus(0)));
        var actual = mapper.selectById(row.getId());
        assertNull(actual.getSpecification());assertEquals("新物料",actual.getName());
        assertTrue(actual.getBatchFlag());assertTrue(actual.getHighValue());assertEquals("本地配置",actual.getRemark());
    }
}
