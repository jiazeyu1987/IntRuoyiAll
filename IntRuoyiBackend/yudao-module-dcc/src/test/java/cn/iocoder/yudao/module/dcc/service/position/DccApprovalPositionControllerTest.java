package cn.iocoder.yudao.module.dcc.service.position;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.position.DccApprovalPositionController;
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
        assertEquals(List.of("USER", "POST", "DIRECT_SUPERVISOR", "DEPT_LEADER"), result.getData().get(0).getSupportedAssignmentTypes());
    }

    @Test
    void createPosition_delegatesToLocalServiceAndReturnsPersistedRole() {
        var request = new cn.iocoder.yudao.module.dcc.controller.admin.position.vo.DccApprovalPositionCreateReqVO();
        request.setName("文控");
        request.setChangeReason("本地初始化");
        when(positionAdminService.createPosition("文控", "本地初始化")).thenReturn(
                DccApprovalPositionDO.builder().id(22L).code("LOCAL-test").name("文控").source("LOCAL").active(true).build());
        when(positionAdminService.getAssignments(22L)).thenReturn(List.of());
        var result = controller.createPosition(request);
        assertTrue(Boolean.TRUE.equals(result.isSuccess()));
        assertEquals("LOCAL", result.getData().getSource());
        assertEquals(22L, result.getData().getId());
    }

    @Test
    void relationshipCapabilitiesAreAdvertisedByTheBackendResponse() {
        assertEquals(List.of("USER", "POST", "DIRECT_SUPERVISOR", "DEPT_LEADER"),
                new DccApprovalPositionRespVO().getSupportedAssignmentTypes());
    }
}
