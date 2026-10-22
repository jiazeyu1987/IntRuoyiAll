package cn.iocoder.yudao.module.mes.controller.admin.pro.productionrelease;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.productionrelease.vo.MesPqcProductionReleasePageReqVO;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc.MesPqcProductionReleasePageQuery;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc.MesPqcProductionReleaseService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.BeanWrapperImpl;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.*;

class MesPqcReleaseExactPageControllerTest {
    @Test
    void exactApplicationAndTaskLongIdentitiesAreForwardedWithoutTruncation() {
        var service = mock(MesPqcProductionReleaseService.class);
        when(service.getPqcReleasePage(nullable(Long.class), any())).thenReturn(new PageResult<>(List.of(), 0L));
        var req = new MesPqcProductionReleasePageReqVO();
        req.setViewStatus("PENDING");
        var request = new BeanWrapperImpl(req);
        assertTrue(request.isWritableProperty("applicationId"), "HTTP contract must expose exact application identity");
        assertTrue(request.isWritableProperty("pqcReleaseWorkTaskId"), "HTTP contract must expose exact task identity");
        request.setPropertyValue("applicationId", 9007199254740993L);
        request.setPropertyValue("pqcReleaseWorkTaskId", 9007199254740995L);
        new MesProductionReleaseController(service).getPqcReleasePage(req);
        var captor = ArgumentCaptor.forClass(MesPqcProductionReleasePageQuery.class);
        verify(service).getPqcReleasePage(nullable(Long.class), captor.capture());
        var forwarded = new BeanWrapperImpl(captor.getValue());
        assertEquals(9007199254740993L, forwarded.getPropertyValue("applicationId"));
        assertEquals(9007199254740995L, forwarded.getPropertyValue("pqcReleaseWorkTaskId"));
    }
}
