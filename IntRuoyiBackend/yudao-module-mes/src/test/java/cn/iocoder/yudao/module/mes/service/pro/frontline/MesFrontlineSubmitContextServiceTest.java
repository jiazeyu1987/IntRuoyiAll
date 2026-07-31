package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesFrontlineSubmitContextReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesFrontlineSubmitContextRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrRecordbookDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.frontline.MesFrontlineDeviceAccountRouteBindingDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.task.MesProTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrRecordbookMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.frontline.MesFrontlineDeviceAccountRouteBindingMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.task.MesProTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProFeedbackTypeEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProTaskStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProWorkOrderStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesFrontlineSubmitContextServiceTest {

    private static final Long LOGIN_USER_ID = 9001L;

    @Mock
    private MesFrontlineDeviceAccountContextService accountContextService;
    @Mock
    private MesFrontlineDeviceAccountRouteBindingMapper routeBindingMapper;
    @Mock
    private MesProTaskMapper taskMapper;
    @Mock
    private MesProWorkOrderMapper workOrderMapper;
    @Mock
    private MesProEdhrRecordbookMapper recordbookMapper;

    private MesFrontlineSubmitContextServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MesFrontlineSubmitContextServiceImpl(accountContextService, routeBindingMapper,
                taskMapper, workOrderMapper, recordbookMapper);
    }

    @Test
    void shouldResolveFormalSubmitContextFromTaskAndBinding() {
        MesFrontlineSubmitContextReqVO reqVO = req();
        when(accountContextService.requireAuthorizedProcess(LOGIN_USER_ID, 21L, 71L, 31L))
                .thenReturn(candidate());
        when(taskMapper.selectById(51L)).thenReturn(task());
        when(workOrderMapper.selectById(41L)).thenReturn(workOrder());
        when(routeBindingMapper.selectEnabledByDeviceAccountUserIdAndRouteIdAndDeviceIdAndWorkstationId(
                LOGIN_USER_ID, 21L, 501L, 11L)).thenReturn(binding());
        when(recordbookMapper.selectById(901L)).thenReturn(recordbook());

        MesFrontlineSubmitContextRespVO result = service.resolve(LOGIN_USER_ID, reqVO);

        assertEquals(41L, result.getWorkOrderId());
        assertEquals(51L, result.getTaskId());
        assertEquals(61L, result.getItemId());
        assertEquals(7001L, result.getApproveUserId());
        assertEquals(901L, result.getRecordbookId());
        assertEquals(MesProFeedbackTypeEnum.SELF.getType(), result.getFeedbackType());
        assertEquals(new BigDecimal("300.000"), result.getScheduledQuantity());
        assertEquals(LocalDateTime.of(2026, 8, 30, 0, 0), result.getExpireDate());
    }

    @Test
    void shouldFailFastWhenBindingMissingRecordbookOrApprover() {
        MesFrontlineSubmitContextReqVO reqVO = req();
        when(accountContextService.requireAuthorizedProcess(LOGIN_USER_ID, 21L, 71L, 31L))
                .thenReturn(candidate());
        when(taskMapper.selectById(51L)).thenReturn(task());
        when(workOrderMapper.selectById(41L)).thenReturn(workOrder());
        when(routeBindingMapper.selectEnabledByDeviceAccountUserIdAndRouteIdAndDeviceIdAndWorkstationId(
                LOGIN_USER_ID, 21L, 501L, 11L)).thenReturn(binding().setDefaultApproveUserId(null));

        assertThrows(ServiceException.class, () -> service.resolve(LOGIN_USER_ID, reqVO));
    }

    @Test
    void shouldRejectTaskOutsideSelectedProcess() {
        MesFrontlineSubmitContextReqVO reqVO = req();
        MesProTaskDO task = task().setProcessId(999L);
        when(accountContextService.requireAuthorizedProcess(LOGIN_USER_ID, 21L, 71L, 31L))
                .thenReturn(candidate());
        when(taskMapper.selectById(51L)).thenReturn(task);

        assertThrows(ServiceException.class, () -> service.resolve(LOGIN_USER_ID, reqVO));
    }

    private static MesFrontlineSubmitContextReqVO req() {
        return new MesFrontlineSubmitContextReqVO()
                .setTaskId(51L)
                .setRouteId(21L)
                .setRouteProcessId(71L)
                .setProcessId(31L);
    }

    private static MesFrontlineRouteProcessCandidate candidate() {
        return new MesFrontlineRouteProcessCandidate(21L, "R-21", "Route 21",
                71L, 31L, "P-31", "Process 31", 1,
                501L, "D-501", "Device 501",
                11L, "WS-11", "Workstation 11");
    }

    private static MesProTaskDO task() {
        return MesProTaskDO.builder()
                .id(51L)
                .code("TASK-51")
                .name("Task 51")
                .workOrderId(41L)
                .routeId(21L)
                .processId(31L)
                .workstationId(11L)
                .itemId(61L)
                .quantity(new BigDecimal("300.000"))
                .endTime(LocalDateTime.of(2026, 8, 30, 0, 0))
                .status(MesProTaskStatusEnum.IN_PROGRESS.getStatus())
                .build();
    }

    private static MesProWorkOrderDO workOrder() {
        return MesProWorkOrderDO.builder()
                .id(41L)
                .code("WO-41")
                .name("Work Order 41")
                .productId(61L)
                .status(MesProWorkOrderStatusEnum.CONFIRMED.getStatus())
                .build();
    }

    private static MesFrontlineDeviceAccountRouteBindingDO binding() {
        return new MesFrontlineDeviceAccountRouteBindingDO()
                .setDeviceAccountUserId(LOGIN_USER_ID)
                .setRouteId(21L)
                .setDeviceId(501L)
                .setWorkstationId(11L)
                .setDefaultApproveUserId(7001L)
                .setRecordbookId(901L)
                .setFeedbackType(MesProFeedbackTypeEnum.SELF.getType());
    }

    private static MesProEdhrRecordbookDO recordbook() {
        return new MesProEdhrRecordbookDO()
                .setId(901L)
                .setStatus("OPEN")
                .setRecordbookCode("RB-901")
                .setRecordbookName("Recordbook 901");
    }
}
