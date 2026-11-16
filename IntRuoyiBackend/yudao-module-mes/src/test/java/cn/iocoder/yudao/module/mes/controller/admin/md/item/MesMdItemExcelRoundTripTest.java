package cn.iocoder.yudao.module.mes.controller.admin.md.item;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.md.item.vo.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.unitmeasure.MesMdUnitMeasureDO;
import cn.iocoder.yudao.module.mes.service.md.item.*;
import cn.iocoder.yudao.module.mes.service.md.unitmeasure.MesMdUnitMeasureService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.mockito.Mockito.*;

class MesMdItemExcelRoundTripTest {
    @Test
    void controllerExportCanBeImportedWithUnitCodeAndCategoryId() throws Exception {
        var controller = new MesMdItemController();
        var service = mock(MesMdItemService.class);
        var types = mock(MesMdItemTypeService.class);
        var units = mock(MesMdUnitMeasureService.class);
        ReflectionTestUtils.setField(controller, "itemService", service);
        ReflectionTestUtils.setField(controller, "itemTypeService", types);
        ReflectionTestUtils.setField(controller, "unitMeasureService", units);
        var item = MesMdItemDO.builder().id(1L).code("ITEM01").name("物料")
                .unitMeasureId(31L).itemTypeId(21L).specification("规格01").build();
        when(service.getItemPage(any())).thenReturn(new PageResult<>(List.of(item), 1L));
        when(types.getItemTypeMap(any())).thenReturn(Map.of(21L,
                MesMdItemTypeDO.builder().id(21L).name("原料").build()));
        when(units.getUnitMeasureMap(any())).thenReturn(Map.of(31L,
                MesMdUnitMeasureDO.builder().id(31L).name("个").code("PCS").build()));
        var response = new MockHttpServletResponse();
        controller.exportItemExcel(new MesMdItemPageReqVO(), response);
        var file = new MockMultipartFile("file", "物料产品.xls", "application/octet-stream", response.getContentAsByteArray());
        for (boolean update : List.of(false, true)) {
            controller.importExcel(file, update);
            verify(service).importItemList(argThat(rows -> rows.size() == 1
                    && "ITEM01".equals(rows.get(0).getCode())
                    && "规格01".equals(rows.get(0).getSpecification())
                    && "PCS".equals(rows.get(0).getUnitMeasureCode())
                    && Long.valueOf(21).equals(rows.get(0).getItemTypeId())
                    && "原料".equals(rows.get(0).getItemTypeName())), eq(update));
        }
    }
}
