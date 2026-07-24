package cn.iocoder.yudao.module.iot.service.ota;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.iot.dal.dataobject.ota.IotOtaTaskDO;
import cn.iocoder.yudao.module.iot.dal.mysql.ota.IotOtaTaskMapper;
import cn.iocoder.yudao.module.iot.enums.ota.IotOtaTaskStatusEnum;
import cn.iocoder.yudao.module.iot.service.device.IotDeviceService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomLongId;
import static cn.iocoder.yudao.module.iot.enums.ErrorCodeConstants.OTA_TASK_PAUSE_FAIL_STATUS_ERROR;
import static cn.iocoder.yudao.module.iot.enums.ErrorCodeConstants.OTA_TASK_RESUME_FAIL_STATUS_ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link IotOtaTaskServiceImpl} 的单元测试。
 */
class IotOtaTaskServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private IotOtaTaskServiceImpl otaTaskService;

    @Mock
    private IotOtaTaskMapper otaTaskMapper;

    @Mock
    private IotDeviceService deviceService;

    @Mock
    private IotOtaFirmwareService otaFirmwareService;

    @Mock
    private IotOtaTaskRecordService otaTaskRecordService;

    @Test
    void pauseOtaTask_whenInProgress_updatesToPaused() {
        Long taskId = randomLongId();
        when(otaTaskMapper.selectById(taskId)).thenReturn(task(taskId, IotOtaTaskStatusEnum.IN_PROGRESS.getStatus()));
        when(otaTaskMapper.updateByIdAndStatus(eq(taskId), eq(IotOtaTaskStatusEnum.IN_PROGRESS.getStatus()),
                any(IotOtaTaskDO.class))).thenReturn(1);

        otaTaskService.pauseOtaTask(taskId);

        verify(otaTaskMapper).updateByIdAndStatus(eq(taskId), eq(IotOtaTaskStatusEnum.IN_PROGRESS.getStatus()),
                argThat(update -> IotOtaTaskStatusEnum.PAUSED.getStatus().equals(update.getStatus())));
    }

    @Test
    void resumeOtaTask_whenPaused_updatesToInProgress() {
        Long taskId = randomLongId();
        when(otaTaskMapper.selectById(taskId)).thenReturn(task(taskId, IotOtaTaskStatusEnum.PAUSED.getStatus()));
        when(otaTaskMapper.updateByIdAndStatus(eq(taskId), eq(IotOtaTaskStatusEnum.PAUSED.getStatus()),
                any(IotOtaTaskDO.class))).thenReturn(1);

        otaTaskService.resumeOtaTask(taskId);

        verify(otaTaskMapper).updateByIdAndStatus(eq(taskId), eq(IotOtaTaskStatusEnum.PAUSED.getStatus()),
                argThat(update -> IotOtaTaskStatusEnum.IN_PROGRESS.getStatus().equals(update.getStatus())));
    }

    @Test
    void pauseOtaTask_whenNotInProgress_failsFast() {
        Long taskId = randomLongId();
        when(otaTaskMapper.selectById(taskId)).thenReturn(task(taskId, IotOtaTaskStatusEnum.END.getStatus()));

        ServiceException exception = assertThrows(ServiceException.class, () -> otaTaskService.pauseOtaTask(taskId));

        assertEquals(OTA_TASK_PAUSE_FAIL_STATUS_ERROR.getCode(), exception.getCode());
    }

    @Test
    void resumeOtaTask_whenNotPaused_failsFast() {
        Long taskId = randomLongId();
        when(otaTaskMapper.selectById(taskId)).thenReturn(task(taskId, IotOtaTaskStatusEnum.IN_PROGRESS.getStatus()));

        ServiceException exception = assertThrows(ServiceException.class, () -> otaTaskService.resumeOtaTask(taskId));

        assertEquals(OTA_TASK_RESUME_FAIL_STATUS_ERROR.getCode(), exception.getCode());
    }

    private static IotOtaTaskDO task(Long id, Integer status) {
        return IotOtaTaskDO.builder().id(id).status(status).build();
    }

}
