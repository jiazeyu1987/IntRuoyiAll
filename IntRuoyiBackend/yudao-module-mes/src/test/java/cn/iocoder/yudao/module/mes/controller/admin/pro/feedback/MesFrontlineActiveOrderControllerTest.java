package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesFrontlineActiveOrderRespVO;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineActiveOrderCandidate;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlinePqcContextService;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineDeviceAccountContextService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderRow;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesFrontlineActiveOrderControllerTest {

    @Mock
    private MesFrontlinePqcContextService pqcContextService;
    @Mock
    private MesFrontlineDeviceAccountContextService contextService;
    @Mock
    private MesTeamLeaderActiveOrderService activeOrderService;

    @InjectMocks
    private MesFrontlineDeviceAccountController controller;

    @Test
    void getPqcActiveOrders_mapsFormalProductNameAndProductionQuantity() {
        when(pqcContextService.listActiveOrders()).thenReturn(List.of(new MesFrontlineActiveOrderCandidate(
                5001L, 1001L, "WO-PQC-001", "PQC 活跃订单", 3001L, "ITEM-PQC", "PQC 产品",
                new BigDecimal("125.500"), 2001L, "ROUTE-PQC", "PQC 产品路线",
                LocalDateTime.of(2026, 8, 1, 9, 0))));

        CommonResult<List<MesFrontlineActiveOrderRespVO>> response = controller.getPqcActiveOrders();

        MesFrontlineActiveOrderRespVO activeOrder = response.getData().get(0);
        assertEquals(5001L, activeOrder.getActiveOrderId());
        assertEquals("PQC 产品", activeOrder.getProductName());
        assertEquals(new BigDecimal("125.500"), activeOrder.getQuantity());
    }

    @Test
    void getProductionActiveOrders_returnsOnlyResponsibleLeaderOrders() {
        when(contextService.resolveResponsibleLeaderUserId(null)).thenReturn(7001L);
        when(activeOrderService.listActiveOrders(7001L)).thenReturn(List.of(
                new MesTeamLeaderActiveOrderRow()
                        .setId(5001L)
                        .setWorkOrderId(1001L)
                        .setWorkOrderCode("WO-PROD-001")
                        .setProductId(3001L)
                        .setProductName("生产产品")
                        .setProductCode("ITEM-PROD")
                        .setQuantity(new BigDecimal("100"))
                        .setRouteId(2001L)
                        .setRouteName("生产路线")
                        .setJoinedAt(LocalDateTime.of(2026, 8, 12, 9, 0))));

        CommonResult<List<MesFrontlineActiveOrderRespVO>> response = controller.getProductionActiveOrders();

        assertEquals(5001L, response.getData().get(0).getActiveOrderId());
        assertEquals("WO-PROD-001", response.getData().get(0).getWorkOrderCode());
        assertEquals(3001L, response.getData().get(0).getProductId());
        assertEquals(new BigDecimal("100"), response.getData().get(0).getQuantity());
    }
}
