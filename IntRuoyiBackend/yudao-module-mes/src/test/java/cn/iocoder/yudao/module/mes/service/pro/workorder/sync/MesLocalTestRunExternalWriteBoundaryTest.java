package cn.iocoder.yudao.module.mes.service.pro.workorder.sync;

import cn.iocoder.yudao.module.erp.service.config.ErpKingdeeConfigService;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProductionOrderClient;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.testdata.MesLocalTestRunManifestDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.testdata.MesLocalTestRunManifestMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesKingdeeProductionOrderSyncRecordMapper;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import cn.iocoder.yudao.module.mes.service.md.unitmeasure.MesMdUnitMeasureService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesLocalTestRunSourceGuard;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class MesLocalTestRunExternalWriteBoundaryTest {
    @ParameterizedTest
    @CsvSource({"true,false", "true,true", "false,false", "false,true"})
    void eitherLocalMarkerOrManifestBlocksBothWorkOrderWriteEntrypointsBeforeAnyErpInteraction(
            boolean marked, boolean deterministic) {
        var order = new MesProWorkOrderDO();
        order.setId(19L);
        order.setOrderSourceCode(marked ? "LOCAL_TEST_RUN:R01:A:77" : "original-marker-removed");
        var manifests = mock(MesLocalTestRunManifestMapper.class);
        if (!marked) when(manifests.selectByWorkOrderId(19L)).thenReturn(new MesLocalTestRunManifestDO());
        var guard = new MesLocalTestRunSourceGuard(manifests, null, null, null, null, null, null, null);
        var orders = mock(MesProWorkOrderService.class);
        when(orders.validateWorkOrderExists(19L)).thenReturn(order);
        var client = mock(ErpKingdeeProductionOrderClient.class);
        var config = mock(ErpKingdeeConfigService.class);
        var products = mock(MesMdItemService.class);
        var units = mock(MesMdUnitMeasureService.class);
        var sync = mock(MesKingdeeProductionOrderSyncRecordMapper.class);
        var service = new MesKingdeeProductionOrderCreateServiceImpl(client, config, orders, products, units, sync);
        ReflectionTestUtils.setField(service, "localTestRunSourceGuard", guard);
        if (deterministic) {
            var command = new MesKingdeeProductionOrderDeterministicCreateCommand().setRunId("R01")
                    .setSlot("O01").setQuantity(BigDecimal.TEN).setBatchNumber("B01");
            assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                    () -> service.createAndSubmitProductionOrder(19L, command));
        } else {
            assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                    () -> service.createAndSubmitProductionOrder(19L));
        }
        verifyNoInteractions(client, config, products, units, sync);
    }
}
