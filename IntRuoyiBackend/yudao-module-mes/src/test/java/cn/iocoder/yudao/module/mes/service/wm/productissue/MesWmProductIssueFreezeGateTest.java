package cn.iocoder.yudao.module.mes.service.wm.productissue;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.productissue.MesWmProductIssueDO;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.productissue.MesWmProductIssueMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.enums.wm.MesWmProductIssueStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_WORK_ORDER_TEMPORARY_FROZEN_OPERATION_FORBIDDEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesWmProductIssueFreezeGateTest {

    private static final Long ISSUE_ID = 1001L;
    private static final Long WORK_ORDER_ID = 3001L;

    @Mock private MesWmProductIssueMapper issueMapper;
    @Mock private MesProWorkOrderMapper workOrderMapper;

    private MesWmProductIssueServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MesWmProductIssueServiceImpl();
        ReflectionTestUtils.setField(service, "issueMapper", issueMapper);
        ReflectionTestUtils.setField(service, "workOrderMapper", workOrderMapper);
    }

    @Test
    void submitIsBlockedWhenWorkOrderIsFrozen() {
        assertFrozenGate(MesWmProductIssueStatusEnum.PREPARE, "提交领料出库单",
                () -> service.submitProductIssue(ISSUE_ID));
    }

    @Test
    void stockIsBlockedWhenWorkOrderIsFrozen() {
        assertFrozenGate(MesWmProductIssueStatusEnum.APPROVING, "领料拣货",
                () -> service.stockProductIssue(ISSUE_ID));
    }

    @Test
    void finishIsBlockedWhenWorkOrderIsFrozen() {
        assertFrozenGate(MesWmProductIssueStatusEnum.APPROVED, "完成领料出库",
                () -> service.finishProductIssue(ISSUE_ID));
    }

    private void assertFrozenGate(MesWmProductIssueStatusEnum status, String action, Runnable command) {
        when(issueMapper.selectById(ISSUE_ID)).thenReturn(new MesWmProductIssueDO()
                .setId(ISSUE_ID).setWorkOrderId(WORK_ORDER_ID).setStatus(status.getStatus()));
        when(workOrderMapper.selectByIdForUpdate(WORK_ORDER_ID)).thenReturn(
                new MesProWorkOrderDO().setId(WORK_ORDER_ID).setTemporaryFrozen(true));

        ServiceException exception = assertThrows(ServiceException.class, command::run);

        assertEquals(PRO_WORK_ORDER_TEMPORARY_FROZEN_OPERATION_FORBIDDEN.getCode(), exception.getCode());
    }
}
