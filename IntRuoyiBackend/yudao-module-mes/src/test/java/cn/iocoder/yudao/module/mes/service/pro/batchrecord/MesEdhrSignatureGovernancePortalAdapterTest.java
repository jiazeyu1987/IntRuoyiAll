package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionSignatureMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class MesEdhrSignatureGovernancePortalAdapterTest {

    @Test
    void routesPointToUnifiedElectronicSignatureTabs() {
        MesEdhrSignatureGovernancePortalAdapter adapter = new MesEdhrSignatureGovernancePortalAdapter(
                mock(MesProEdhrWorkTaskMapper.class),
                mock(MesProBatchRecordExecutionSignatureMapper.class));

        assertEquals("批记录签名", adapter.getModuleName());
        assertEquals("批记录签名记录", adapter.getPrimaryRouteLabel());
        assertEquals("/signature-governance/batch-signatures", adapter.getPrimaryRoute());
        assertEquals("工作任务", adapter.getSecondaryRouteLabel());
        assertEquals("/mes/pro/feedback/edhr-work-task", adapter.getSecondaryRoute());
    }
}
