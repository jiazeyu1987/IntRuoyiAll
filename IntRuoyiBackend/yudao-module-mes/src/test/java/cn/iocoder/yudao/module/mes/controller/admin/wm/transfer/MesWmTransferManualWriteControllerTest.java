package cn.iocoder.yudao.module.mes.controller.admin.wm.transfer;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.wm.transfer.vo.MesWmTransferSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.wm.transfer.vo.detail.MesWmTransferDetailSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.wm.transfer.vo.line.MesWmTransferLineSaveReqVO;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import cn.iocoder.yudao.module.mes.service.md.unitmeasure.MesMdUnitMeasureService;
import cn.iocoder.yudao.module.mes.service.wm.transfer.MesWmTransferDetailService;
import cn.iocoder.yudao.module.mes.service.wm.transfer.MesWmTransferLineService;
import cn.iocoder.yudao.module.mes.service.wm.transfer.MesWmTransferService;
import cn.iocoder.yudao.module.mes.service.wm.warehouse.MesWmWarehouseAreaService;
import cn.iocoder.yudao.module.mes.service.wm.warehouse.MesWmWarehouseLocationService;
import cn.iocoder.yudao.module.mes.service.wm.warehouse.MesWmWarehouseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.WM_TRANSFER_MANUAL_OPERATION_FORBIDDEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MesWmTransferManualWriteControllerTest {

    @Mock
    private MesWmTransferService transferService;
    @Mock
    private MesWmTransferLineService transferLineService;
    @Mock
    private MesWmTransferDetailService transferDetailService;
    @Mock
    private MesMdItemService itemService;
    @Mock
    private MesMdUnitMeasureService unitMeasureService;
    @Mock
    private MesWmWarehouseService warehouseService;
    @Mock
    private MesWmWarehouseLocationService locationService;
    @Mock
    private MesWmWarehouseAreaService areaService;

    @InjectMocks
    private MesWmTransferController transferController;
    @InjectMocks
    private MesWmTransferLineController transferLineController;
    @InjectMocks
    private MesWmTransferDetailController transferDetailController;

    @Test
    void transferWriteEndpointsRejectManualOperationAndDoNotCallService() {
        assertManualWriteForbidden(() -> transferController.createTransfer(new MesWmTransferSaveReqVO()));
        assertManualWriteForbidden(() -> transferController.updateTransfer(new MesWmTransferSaveReqVO()));
        assertManualWriteForbidden(() -> transferController.deleteTransfer(100L));
        assertManualWriteForbidden(() -> transferController.submitTransfer(100L));
        assertManualWriteForbidden(() -> transferController.confirmTransfer(100L));
        assertManualWriteForbidden(() -> transferController.stockTransfer(100L));
        assertManualWriteForbidden(() -> transferController.finishTransfer(100L));
        assertManualWriteForbidden(() -> transferController.cancelTransfer(100L));

        verifyNoInteractions(transferService);
    }

    @Test
    void transferLineWriteEndpointsRejectManualOperationAndDoNotCallService() {
        assertManualWriteForbidden(() -> transferLineController.createTransferLine(new MesWmTransferLineSaveReqVO()));
        assertManualWriteForbidden(() -> transferLineController.updateTransferLine(new MesWmTransferLineSaveReqVO()));
        assertManualWriteForbidden(() -> transferLineController.deleteTransferLine(100L));

        verifyNoInteractions(transferLineService);
    }

    @Test
    void transferDetailWriteEndpointsRejectManualOperationAndDoNotCallService() {
        assertManualWriteForbidden(() -> transferDetailController.createTransferDetail(new MesWmTransferDetailSaveReqVO()));
        assertManualWriteForbidden(() -> transferDetailController.updateTransferDetail(new MesWmTransferDetailSaveReqVO()));
        assertManualWriteForbidden(() -> transferDetailController.deleteTransferDetail(100L));

        verifyNoInteractions(transferDetailService);
    }

    private void assertManualWriteForbidden(Runnable runnable) {
        ServiceException exception = assertThrows(ServiceException.class, runnable::run);
        assertEquals(WM_TRANSFER_MANUAL_OPERATION_FORBIDDEN.getCode(), exception.getCode());
        assertNotNull(exception.getMessage());
    }
}
