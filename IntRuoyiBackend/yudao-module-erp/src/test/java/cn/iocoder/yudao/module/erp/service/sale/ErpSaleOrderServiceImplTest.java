package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOrderMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErpSaleOrderServiceImplTest {

    @Mock
    private ErpSaleOrderMapper saleOrderMapper;

    private ErpSaleOrderServiceImpl saleOrderService;

    @BeforeEach
    void setUp() {
        saleOrderService = new ErpSaleOrderServiceImpl();
        ReflectionTestUtils.setField(saleOrderService, "saleOrderMapper", saleOrderMapper);
    }

    @Test
    void updateSaleOrderStatus_treatsMissingOutAndReturnCountsAsZero() {
        ErpSaleOrderDO saleOrder = new ErpSaleOrderDO()
                .setId(501L)
                .setNo("XSDD20260613000001")
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(saleOrderMapper.selectById(501L)).thenReturn(saleOrder);
        when(saleOrderMapper.updateByIdAndStatus(501L, ErpAuditStatus.APPROVE.getStatus(),
                new ErpSaleOrderDO().setStatus(ErpAuditStatus.PROCESS.getStatus()))).thenReturn(1);

        saleOrderService.updateSaleOrderStatus(501L, ErpAuditStatus.PROCESS.getStatus());

        verify(saleOrderMapper).updateByIdAndStatus(501L, ErpAuditStatus.APPROVE.getStatus(),
                new ErpSaleOrderDO().setStatus(ErpAuditStatus.PROCESS.getStatus()));
    }

}
