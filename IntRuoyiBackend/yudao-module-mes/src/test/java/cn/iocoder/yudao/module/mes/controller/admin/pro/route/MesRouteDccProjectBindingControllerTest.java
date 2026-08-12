package cn.iocoder.yudao.module.mes.controller.admin.pro.route;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.dcc.MesRouteDccProjectBindingRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.dcc.MesRouteDccProjectBindingSaveReqVO;
import cn.iocoder.yudao.module.mes.service.pro.route.MesRouteDccProjectBindingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesRouteDccProjectBindingControllerTest {

    @InjectMocks
    private MesRouteDccProjectBindingController controller;

    @Mock
    private MesRouteDccProjectBindingService service;

    @Test
    void getBinding_delegatesToService() {
        when(service.getBinding(100L)).thenReturn(new MesRouteDccProjectBindingRespVO()
                .setRouteId(100L)
                .setDccProjectCodeId(200L)
                .setVersion(1L)
                .setBound(true));

        CommonResult<MesRouteDccProjectBindingRespVO> result = controller.getBinding(100L);

        assertEquals(0, result.getCode());
        assertEquals(200L, result.getData().getDccProjectCodeId());
    }

    @Test
    void saveBinding_delegatesToServiceWithExpectedVersion() {
        MesRouteDccProjectBindingSaveReqVO reqVO = new MesRouteDccProjectBindingSaveReqVO()
                .setRouteId(100L)
                .setDccProjectCodeId(200L)
                .setExpectedVersion(1L);
        when(service.saveBinding(reqVO)).thenReturn(new MesRouteDccProjectBindingRespVO()
                .setRouteId(100L)
                .setDccProjectCodeId(200L)
                .setVersion(2L)
                .setBound(true));

        CommonResult<MesRouteDccProjectBindingRespVO> result = controller.saveBinding(reqVO);

        assertEquals(0, result.getCode());
        assertEquals(2L, result.getData().getVersion());
        verify(service).saveBinding(reqVO);
    }

    @Test
    void deleteBinding_usesRouteUpdateOnlyAndExpectedVersion() {
        when(service.deleteBinding(100L, 2L)).thenReturn(new MesRouteDccProjectBindingRespVO()
                .setRouteId(100L)
                .setVersion(3L)
                .setBound(false));

        CommonResult<MesRouteDccProjectBindingRespVO> result = controller.deleteBinding(100L, 2L);

        assertEquals(0, result.getCode());
        assertEquals(3L, result.getData().getVersion());
        verify(service).deleteBinding(100L, 2L);
    }
}
