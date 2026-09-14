package cn.iocoder.yudao.module.mes.service.wm.miscreceipt;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.miscreceipt.MesWmMiscReceiptDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.miscreceipt.MesWmMiscReceiptLineDO;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.miscreceipt.MesWmMiscReceiptMapper;
import cn.iocoder.yudao.module.mes.enums.wm.MesWmMiscReceiptStatusEnum;
import cn.iocoder.yudao.module.mes.service.wm.transaction.MesWmTransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

@Import(MesWmMiscReceiptServiceImpl.class)
class MesWmMiscReceiptServiceImplTest extends BaseDbUnitTest {

    @Resource
    private MesWmMiscReceiptServiceImpl miscReceiptService;

    @Resource
    @MockitoBean
    private MesWmMiscReceiptMapper miscReceiptMapper;

    @MockitoBean
    private MesWmMiscReceiptLineService miscReceiptLineService;

    @MockitoBean
    private MesWmTransactionService transactionService;

    @Test
    void finishMiscReceipt_shouldKeepLineBatchCodeInTransaction() {
        MesWmMiscReceiptDO receipt = new MesWmMiscReceiptDO()
                .setId(900001L)
                .setCode("STAGE6-RECEIPT-TEST")
                .setName("Stage6 receipt test")
                .setType(1)
                .setStatus(MesWmMiscReceiptStatusEnum.APPROVED.getStatus());
        MesWmMiscReceiptLineDO line = new MesWmMiscReceiptLineDO()
                .setReceiptId(receipt.getId())
                .setItemId(900200L)
                .setQuantity(BigDecimal.ONE)
                .setBatchCode("RRM-20260801-PP-BATCH-01")
                .setWarehouseId(1L)
                .setLocationId(1L)
                .setAreaId(1L);
        org.mockito.Mockito.when(miscReceiptLineService.getMiscReceiptLineListByReceiptId(receipt.getId()))
                .thenReturn(List.of(line));
        org.mockito.Mockito.when(miscReceiptMapper.selectById(receipt.getId())).thenReturn(receipt);

        miscReceiptService.finishMiscReceipt(receipt.getId());

        verify(transactionService).createTransactionList(argThat(transactions ->
                transactions.size() == 1
                        && "RRM-20260801-PP-BATCH-01".equals(transactions.get(0).getBatchCode())));
    }
}
