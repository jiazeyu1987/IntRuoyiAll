package cn.iocoder.yudao.module.dcc.signature.portal;

import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSignatureMapper;
import cn.iocoder.yudao.module.dcc.signature.service.portal.DccSignatureGovernancePortalAdapter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class DccSignatureGovernancePortalAdapterTest {

    @Test
    void routesPointToUnifiedElectronicSignatureChildTabs() {
        DccSignatureGovernancePortalAdapter adapter = new DccSignatureGovernancePortalAdapter(
                mock(BpmTaskService.class),
                mock(DccControlledFileSignatureMapper.class));

        assertEquals("文件签名", adapter.getModuleName());
        assertEquals("文件签名记录", adapter.getPrimaryRouteLabel());
        assertEquals("/signature-governance/file-signatures", adapter.getPrimaryRoute());
        assertEquals("用户授权", adapter.getSecondaryRouteLabel());
        assertEquals("/signature-governance/authorizations", adapter.getSecondaryRoute());
    }
}
