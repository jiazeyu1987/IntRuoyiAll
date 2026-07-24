package cn.iocoder.yudao.module.mes.controller.admin.pro.workorder;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.MesProWorkOrderItemRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemTypeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.unitmeasure.MesMdUnitMeasureDO;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemTypeService;
import cn.iocoder.yudao.module.mes.service.md.unitmeasure.MesMdUnitMeasureService;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderBomService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProWorkOrderBomControllerTest {

    @Mock
    private MesProWorkOrderBomService workOrderBomService;
    @Mock
    private MesMdItemService itemService;
    @Mock
    private MesMdUnitMeasureService unitMeasureService;
    @Mock
    private MesMdItemTypeService itemTypeService;

    @InjectMocks
    private MesProWorkOrderBomController controller;

    @Test
    void getWorkOrderBomItemListByWorkOrderId_shouldBuildLeafMaterialResponseFromServiceDemand() {
        when(workOrderBomService.getWorkOrderMaterialDemandByWorkOrderId(1L))
                .thenReturn(Map.of(200L, new BigDecimal("6")));
        when(itemService.getItemMap(anyCollection())).thenReturn(Map.of(
                200L, new MesMdItemDO().setId(200L).setCode("MAT-200").setName("叶子物料").setSpecification("Spec-A")
                        .setUnitMeasureId(300L).setItemTypeId(400L)
        ));
        when(unitMeasureService.getUnitMeasureMap(anyCollection())).thenReturn(Map.of(
                300L, new MesMdUnitMeasureDO().setId(300L).setName("支")
        ));
        when(itemTypeService.getItemTypeMap(anyCollection())).thenReturn(Map.of(
                400L, new MesMdItemTypeDO().setId(400L).setItemOrProduct("ITEM")
        ));

        CommonResult<List<MesProWorkOrderItemRespVO>> result = controller.getWorkOrderBomItemListByWorkOrderId(1L);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
        MesProWorkOrderItemRespVO item = result.getData().get(0);
        assertEquals(200L, item.getItemId());
        assertEquals("MAT-200", item.getItemCode());
        assertEquals("叶子物料", item.getItemName());
        assertEquals("Spec-A", item.getItemSpecification());
        assertEquals("支", item.getUnitMeasureName());
        assertEquals("ITEM", item.getItemOrProduct());
        assertEquals(0, new BigDecimal("6").compareTo(item.getQuantity()));
    }
}
