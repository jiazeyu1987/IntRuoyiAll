package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.module.mes.controller.admin.md.item.MesMdItemController;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesMdItemNamesQueryTest {
    @Test
    void readsNamesInOneBatchWithoutLoadingUnrelatedMasterData() throws Exception {
        var service = mock(MesMdItemService.class);
        var controller = new MesMdItemController();
        ReflectionTestUtils.setField(controller, "itemService", service);
        var ids = List.of(11L, 12L, 13L);
        when(service.getItemList(ids)).thenReturn(List.of(
                new MesMdItemDO().setId(11L).setName("弹簧"),
                new MesMdItemDO().setId(12L).setName("导管")));
        assertEquals(Map.of(11L, "弹簧", 12L, "导管"), controller.getItemNames(ids).getData());
        verify(service).getItemList(ids);
        verifyNoMoreInteractions(service);
        assertEquals(MesMdItemController.class.getMethod("getItem", Long.class)
                        .getAnnotation(PreAuthorize.class).value(),
                MesMdItemController.class.getMethod("getItemNames", List.class)
                        .getAnnotation(PreAuthorize.class).value());
    }

    @Test
    void batchReadFailurePropagates() {
        var service = mock(MesMdItemService.class);
        var controller = new MesMdItemController();
        ReflectionTestUtils.setField(controller, "itemService", service);
        var failure = new IllegalStateException("物料查询失败");
        when(service.getItemList(List.of(11L))).thenThrow(failure);
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> controller.getItemNames(List.of(11L))));
    }
}
