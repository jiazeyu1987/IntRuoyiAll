package cn.iocoder.yudao.module.dcc.controller.admin.protection;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.protection.vo.DccUploadSizePolicyEffectiveRespVO;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadSizePolicyMatch;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadSizePolicyScopeType;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadSizePolicyService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class DccUploadSizePolicyControllerTest extends BaseMockitoUnitTest {

    @Mock
    private DccUploadSizePolicyService uploadSizePolicyService;

    @InjectMocks
    private DccUploadSizePolicyController controller;

    @Test
    void getEffectivePolicy_returnsResolvedPolicyContract() {
        when(uploadSizePolicyService.validateUploadSize(10L, "SOURCE_FILE", 512L, null))
                .thenReturn(new DccUploadSizePolicyMatch(99L, "UP-CAT-SRC-V1",
                        DccUploadSizePolicyScopeType.CATEGORY_PURPOSE, 10L, "SOURCE_FILE",
                        1024L, "v1", 1, 4));

        CommonResult<DccUploadSizePolicyEffectiveRespVO> result =
                controller.getEffectivePolicy(10L, "SOURCE_FILE", 512L);

        assertEquals(99L, result.getData().getPolicyId());
        assertEquals("UP-CAT-SRC-V1", result.getData().getPolicyCode());
        assertEquals("CATEGORY_PURPOSE", result.getData().getScopeType());
        assertEquals(1024L, result.getData().getMaxBytes());
        assertEquals("v1", result.getData().getPolicyVersion());
        assertEquals(4, result.getData().getScopePriority());
    }

}
