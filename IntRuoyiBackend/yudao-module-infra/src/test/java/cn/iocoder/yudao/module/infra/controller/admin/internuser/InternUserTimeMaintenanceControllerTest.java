package cn.iocoder.yudao.module.infra.controller.admin.internuser;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.infra.controller.admin.internuser.vo.InternUserFileUploadTimeUpdateReqVO;
import cn.iocoder.yudao.module.infra.service.internuser.InternUserTimeMaintenanceService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

class InternUserTimeMaintenanceControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private InternUserTimeMaintenanceController controller;

    @Mock
    private InternUserTimeMaintenanceService timeMaintenanceService;

    @Test
    void updateFileUploadTime_delegatesToModuleService() {
        InternUserFileUploadTimeUpdateReqVO reqVO = new InternUserFileUploadTimeUpdateReqVO()
                .setId(88L)
                .setCreateTime(LocalDateTime.of(2026, 8, 20, 10, 30));

        CommonResult<Boolean> result = controller.updateFileUploadTime(reqVO);

        verify(timeMaintenanceService).updateFileUploadTime(eq(reqVO));
        assertEquals(Boolean.TRUE, result.getData());
    }

}
