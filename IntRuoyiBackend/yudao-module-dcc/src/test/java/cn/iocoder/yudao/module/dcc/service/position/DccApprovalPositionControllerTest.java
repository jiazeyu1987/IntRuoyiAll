package cn.iocoder.yudao.module.dcc.service.position;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.position.DccApprovalPositionController;
import cn.iocoder.yudao.module.dcc.controller.admin.position.vo.DccApprovalPositionImportRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.position.vo.DccApprovalPositionRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.position.DccApprovalPositionDO;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class DccApprovalPositionControllerTest extends BaseMockitoUnitTest {

    @Mock
    private DccApprovalPositionAdminService positionAdminService;

    @InjectMocks
    private DccApprovalPositionController controller;

    @Test
    void getPositionList_delegatesToService() {
        when(positionAdminService.getPositionList()).thenReturn(List.of(
                DccApprovalPositionDO.builder()
                        .id(21L)
                        .code("INTAUTH-21")
                        .name("Document Control Reviewer")
                        .active(Boolean.TRUE)
                        .source("INTAUTH:21")
                        .remark("Synchronized from IntAuth")
                        .build()
        ));
        when(positionAdminService.getAssignments(21L)).thenReturn(List.of());

        CommonResult<List<DccApprovalPositionRespVO>> result = controller.getPositionList();

        assertTrue(Boolean.TRUE.equals(result.isSuccess()));
        assertEquals(1, result.getData().size());
        assertEquals(21L, result.getData().get(0).getId());
        assertEquals("Document Control Reviewer", result.getData().get(0).getName());
        assertEquals("INTAUTH-21", result.getData().get(0).getCode());
    }

    @Test
    void importPositionsFromIntAuth_delegatesToService() {
        when(positionAdminService.importPositionsFromIntAuth())
                .thenReturn(new DccApprovalPositionImportResult(8, 2, 3, 1, 1));

        CommonResult<DccApprovalPositionImportRespVO> result = controller.importPositionsFromIntAuth();

        assertTrue(Boolean.TRUE.equals(result.isSuccess()));
        assertEquals(8, result.getData().getTotalCount());
        assertEquals(2, result.getData().getCreatedCount());
        assertEquals(3, result.getData().getAdoptedCount());
        assertEquals(1, result.getData().getUpdatedCount());
        assertEquals(1, result.getData().getDisabledCount());
    }
}
