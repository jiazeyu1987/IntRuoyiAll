package cn.iocoder.yudao.module.dcc.controller.admin.file;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccPaperDistributionRecordRespVO;
import cn.iocoder.yudao.module.dcc.service.file.DccPaperDistributionAckService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ACCESS_DENIED;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccPaperDistributionControllerTest extends BaseMockitoUnitTest {

    @Mock
    private DccPaperDistributionAckService paperDistributionAckService;

    @InjectMocks
    private DccPaperDistributionController controller;

    @Test
    void recordsEndpointPassesLoginUserAndPropagatesFileAuthorizationDenial() {
        when(paperDistributionAckService.getPaperDistributionRecords(99L, 910L))
                .thenThrow(exception(CONTROLLED_FILE_ACCESS_DENIED));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            assertThrows(ServiceException.class, () -> controller.getPaperDistributionRecords(910L));
        }

        verify(paperDistributionAckService).getPaperDistributionRecords(99L, 910L);
    }

    @Test
    void recordsEndpointReturnsAuthorizedRowsForAuthenticatedUser() {
        List<DccPaperDistributionRecordRespVO> records = List.of(new DccPaperDistributionRecordRespVO());
        when(paperDistributionAckService.getPaperDistributionRecords(99L, 910L)).thenReturn(records);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            org.junit.jupiter.api.Assertions.assertEquals(records,
                    controller.getPaperDistributionRecords(910L).getData());
        }
    }
}
