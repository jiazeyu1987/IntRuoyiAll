package cn.iocoder.yudao.module.mes.service.wm.itemreceipt;

import cn.iocoder.yudao.module.mes.controller.admin.wm.itemreceipt.vo.detail.MesWmItemReceiptDetailSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.itemreceipt.*;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.itemreceipt.MesWmItemReceiptDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.itemreceipt.MesWmItemReceiptMapper;
import cn.iocoder.yudao.module.mes.enums.wm.MesWmItemReceiptStatusEnum;
import cn.iocoder.yudao.module.mes.service.wm.materialstock.MesWmMaterialStockService;
import cn.iocoder.yudao.module.mes.service.wm.transaction.MesWmTransactionService;
import cn.iocoder.yudao.module.mes.service.wm.transaction.dto.MesWmTransactionSaveReqDTO;
import cn.iocoder.yudao.module.mes.service.wm.warehouse.MesWmWarehouseAreaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.WM_ITEM_RECEIPT_LINE_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MesWmItemReceiptBatchPropagationTest {
    @Mock MesWmItemReceiptMapper itemReceiptMapper;
    @Mock MesWmItemReceiptDetailMapper itemReceiptDetailMapper;
    @Mock MesWmItemReceiptLineService itemReceiptLineService;
    @Mock MesWmItemReceiptDetailService itemReceiptDetailService;
    @Mock MesWmTransactionService wmTransactionService;
    @Mock MesWmItemReceiptService itemReceiptService;
    @Mock MesWmWarehouseAreaService warehouseAreaService;
    @Mock MesWmMaterialStockService materialStockService;
    @InjectMocks MesWmItemReceiptServiceImpl receiptService;
    @InjectMocks MesWmItemReceiptDetailServiceImpl detailService;

    @Test
    void finishUsesPurchaseLineBatchWhenShelfDetailHasNoBatch() {
        givenReadyReceipt(1L);
        when(itemReceiptLineService.getItemReceiptLine(2L)).thenReturn(line(1L));
        receiptService.finishItemReceipt(1L);
        ArgumentCaptor<List<MesWmTransactionSaveReqDTO>> captor = ArgumentCaptor.forClass(List.class);
        verify(wmTransactionService).createTransactionList(captor.capture());
        assertEquals(4L, captor.getValue().get(0).getBatchId());
        assertEquals(BigDecimal.TEN, captor.getValue().get(0).getQuantity());
        verify(itemReceiptMapper).updateById(argThat((MesWmItemReceiptDO receipt) ->
                receipt.getStatus().equals(MesWmItemReceiptStatusEnum.FINISHED.getStatus())));
    }

    @Test
    void finishRejectsDetailFromAnotherReceiptBeforeStockWrite() {
        givenReadyReceipt(1L);
        when(itemReceiptLineService.getItemReceiptLine(2L)).thenReturn(line(99L));
        assertServiceException(() -> receiptService.finishItemReceipt(1L), WM_ITEM_RECEIPT_LINE_NOT_EXISTS);
        verifyNoInteractions(wmTransactionService);
        verify(itemReceiptMapper, never()).updateById(any(MesWmItemReceiptDO.class));
    }

    @Test
    void shelfCreationUsesParentBatchForMixingValidationAndPersistence() {
        when(itemReceiptLineService.getItemReceiptLine(2L)).thenReturn(line(1L));
        when(itemReceiptDetailMapper.selectListByLineId(2L)).thenReturn(List.of());
        MesWmItemReceiptDetailSaveReqVO request = new MesWmItemReceiptDetailSaveReqVO();
        request.setReceiptId(1L); request.setLineId(2L); request.setItemId(3L);
        request.setBatchId(999L); request.setQuantity(BigDecimal.TEN);
        request.setWarehouseId(5L); request.setLocationId(6L); request.setAreaId(7L);
        detailService.createItemReceiptDetail(request);
        verify(materialStockService).checkAreaMixingRule(7L, 3L, 4L);
        verify(itemReceiptDetailMapper).insert(argThat((MesWmItemReceiptDetailDO detail) ->
                Long.valueOf(4L).equals(detail.getBatchId())));
    }

    private void givenReadyReceipt(Long id) {
        when(itemReceiptMapper.selectById(id)).thenReturn(MesWmItemReceiptDO.builder().id(id)
                .status(MesWmItemReceiptStatusEnum.APPROVED.getStatus()).build());
        when(itemReceiptDetailService.getItemReceiptDetailListByReceiptId(id)).thenReturn(List.of(
                MesWmItemReceiptDetailDO.builder().lineId(2L).itemId(3L).quantity(BigDecimal.TEN).build()));
    }

    private MesWmItemReceiptLineDO line(Long receiptId) {
        return MesWmItemReceiptLineDO.builder().id(2L).receiptId(receiptId).itemId(3L)
                .batchId(4L).receivedQuantity(BigDecimal.TEN).build();
    }
}
