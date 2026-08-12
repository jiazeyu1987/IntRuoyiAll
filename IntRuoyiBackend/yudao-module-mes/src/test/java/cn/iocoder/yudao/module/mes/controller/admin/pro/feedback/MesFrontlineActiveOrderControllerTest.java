package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesFrontlineActiveOrderRespVO;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineActiveOrderCandidate;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlinePqcContextService;
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
}
