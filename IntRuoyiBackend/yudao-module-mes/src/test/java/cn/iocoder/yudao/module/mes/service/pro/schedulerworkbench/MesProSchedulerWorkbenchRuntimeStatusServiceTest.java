package cn.iocoder.yudao.module.mes.service.pro.schedulerworkbench;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.infra.controller.admin.job.vo.job.JobSaveReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.job.JobDO;
import cn.iocoder.yudao.module.infra.dal.dataobject.job.JobLogDO;
import cn.iocoder.yudao.module.infra.enums.job.JobLogStatusEnum;
import cn.iocoder.yudao.module.infra.enums.job.JobStatusEnum;
import cn.iocoder.yudao.module.infra.service.job.JobLogService;
import cn.iocoder.yudao.module.infra.service.job.JobService;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo.MesProSchedulerWorkbenchAutoScheduleJobStatusRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo.MesProSchedulerWorkbenchNightShiftCapacityStatusRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.cal.plan.MesCalPlanDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.cal.plan.MesCalPlanShiftDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdProductionLineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProCapacityPlanDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdProductionLineMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProCapacityPlanMapper;
import cn.iocoder.yudao.module.mes.enums.cal.MesCalPlanStatusEnum;
import cn.iocoder.yudao.module.mes.service.cal.plan.MesCalPlanService;
import cn.iocoder.yudao.module.mes.service.cal.plan.MesCalPlanShiftService;
import cn.iocoder.yudao.module.mes.service.pro.schedule.CapacityWindowAllocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProSchedulerWorkbenchRuntimeStatusServiceTest {

    @InjectMocks
    private MesProSchedulerWorkbenchRuntimeStatusService service;

    @Mock
    private JobService jobService;
    @Mock
    private JobLogService jobLogService;
    @Mock
    private MesMdProductionLineMapper productionLineMapper;
    @Mock
    private MesCalPlanService planService;
    @Mock
    private MesCalPlanShiftService planShiftService;
    @Mock
    private MesProCapacityPlanMapper capacityPlanMapper;
    @Spy
    private CapacityWindowAllocator capacityWindowAllocator = new CapacityWindowAllocator();

    @Test
    void getAutoScheduleJobStatus_shouldExposeRegisteredJobAndLatestLog() {
        JobDO job = JobDO.builder()
                .id(71L)
                .name("MES nightly replan")
                .status(JobStatusEnum.NORMAL.getStatus())
                .handlerName(MesProSchedulerWorkbenchRuntimeStatusService.NIGHTLY_REPLAN_HANDLER_NAME)
                .cronExpression("0 0 2 * * ?")
                .retryCount(0)
                .retryInterval(0)
                .build();
        JobLogDO log = JobLogDO.builder()
                .jobId(71L)
                .beginTime(LocalDateTime.of(2026, 8, 13, 2, 0))
                .endTime(LocalDateTime.of(2026, 8, 13, 2, 1))
                .status(JobLogStatusEnum.SUCCESS.getStatus())
                .result("{\"1\":\"夜间重排完成：排产工单 3，生成任务 6，保护任务 1，阻塞 0，短缺 0\"}")
                .build();
        when(jobService.getJobPage(any())).thenReturn(new PageResult<>(List.of(job), 1L));
        when(jobLogService.getJobLogPage(any())).thenReturn(new PageResult<>(List.of(log), 1L));

        MesProSchedulerWorkbenchAutoScheduleJobStatusRespVO status = service.getAutoScheduleJobStatus();

        assertTrue(status.getConfigured());
        assertTrue(status.getEnabled());
        assertEquals(71L, status.getJobId());
        assertEquals("0 0 2 * * ?", status.getCronExpression());
        assertNotNull(status.getNextTriggerTime());
        assertEquals("SUCCESS", status.getLatestStatus());
        assertTrue(status.getLatestResult().contains("夜间重排完成"));
    }

    @Test
    void getAutoScheduleJobStatus_shouldExposePartialFailureWhenQuartzSuccessContainsTenantFailures() {
        JobDO job = JobDO.builder()
                .id(71L)
                .name("MES nightly replan")
                .status(JobStatusEnum.NORMAL.getStatus())
                .handlerName(MesProSchedulerWorkbenchRuntimeStatusService.NIGHTLY_REPLAN_HANDLER_NAME)
                .cronExpression("0 0 2 * * ?")
                .retryCount(0)
                .retryInterval(0)
                .build();
        JobLogDO log = JobLogDO.builder()
                .jobId(71L)
                .beginTime(LocalDateTime.of(2026, 8, 13, 2, 0))
                .endTime(LocalDateTime.of(2026, 8, 13, 2, 1))
                .status(JobLogStatusEnum.SUCCESS.getStatus())
                .result("{\"1\":\"ServiceException: Auto schedule apply requires a preview calendar context token\","
                        + "\"162\":\"夜间重排完成：没有待重排排产工单\"}")
                .build();
        when(jobService.getJobPage(any())).thenReturn(new PageResult<>(List.of(job), 1L));
        when(jobLogService.getJobLogPage(any())).thenReturn(new PageResult<>(List.of(log), 1L));

        MesProSchedulerWorkbenchAutoScheduleJobStatusRespVO status = service.getAutoScheduleJobStatus();

        assertEquals("PARTIAL_FAILURE", status.getLatestStatus());
        assertTrue(status.getLatestResult().contains("preview calendar context token"));
    }

    @Test
    void getAutoScheduleJobStatus_shouldReportMissingRegistration() {
        when(jobService.getJobPage(any())).thenReturn(PageResult.empty());

        MesProSchedulerWorkbenchAutoScheduleJobStatusRespVO status = service.getAutoScheduleJobStatus();

        assertFalse(status.getConfigured());
        assertFalse(status.getEnabled());
    }

    @Test
    void updateNightlyReplanTime_shouldUpdateTheRegisteredQuartzJob() throws org.quartz.SchedulerException {
        JobDO job = JobDO.builder()
                .id(72L)
                .name("MES nightly replan")
                .status(JobStatusEnum.NORMAL.getStatus())
                .handlerName(MesProSchedulerWorkbenchRuntimeStatusService.NIGHTLY_REPLAN_HANDLER_NAME)
                .handlerParam("")
                .cronExpression("0 0 2 * * ?")
                .retryCount(2)
                .retryInterval(1000)
                .monitorTimeout(300000)
                .build();
        when(jobService.getJobPage(any())).thenReturn(new PageResult<>(List.of(job), 1L));

        service.updateNightlyReplanTime("03:15");

        ArgumentCaptor<JobSaveReqVO> captor = ArgumentCaptor.forClass(JobSaveReqVO.class);
        verify(jobService).updateJob(captor.capture());
        assertEquals(72L, captor.getValue().getId());
        assertEquals("0 15 3 * * ?", captor.getValue().getCronExpression());
        assertEquals(2, captor.getValue().getRetryCount());
        assertEquals(1000, captor.getValue().getRetryInterval());
    }

    @Test
    void updateNightlyReplanTime_shouldFailFastWhenJobIsMissing() {
        when(jobService.getJobPage(any())).thenReturn(PageResult.empty());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.updateNightlyReplanTime("03:15"));

        assertTrue(exception.getMessage().contains("自动排产任务未注册"));
    }

    @Test
    void getNightShiftCapacityStatus_shouldCountAProductionLineOnlyOnceAcrossNightShifts() {
        MesMdProductionLineDO line1 = MesMdProductionLineDO.builder().id(101L).calendarPlanId(91L).build();
        MesMdProductionLineDO line2 = MesMdProductionLineDO.builder().id(102L).calendarPlanId(91L).build();
        MesCalPlanShiftDO nightA = MesCalPlanShiftDO.builder()
                .id(201L).planId(91L).name("夜班一").startTime("20:00").endTime("04:00").build();
        MesCalPlanShiftDO nightB = MesCalPlanShiftDO.builder()
                .id(202L).planId(91L).name("夜班二").startTime("22:00").endTime("06:00").build();
        when(productionLineMapper.selectListByStatus(0)).thenReturn(List.of(line1, line2));
        when(planService.getPlan(91L)).thenReturn(plan(91L, MesCalPlanStatusEnum.CONFIRMED));
        when(planShiftService.getPlanShiftListByPlanId(91L)).thenReturn(List.of(nightA, nightB));
        when(capacityPlanMapper.selectListByLineIdsAndDate(anyCollection(), any(LocalDateTime.class)))
                .thenReturn(List.of(
                        capacity(101L, 201L), capacity(102L, 201L),
                        capacity(101L, 202L), capacity(102L, 202L)));

        MesProSchedulerWorkbenchNightShiftCapacityStatusRespVO status = service.getNightShiftCapacityStatus();

        assertTrue(status.getAvailable());
        assertEquals(2, status.getAvailableShiftCount());
        assertEquals(2, status.getCapacityLineCount());
        assertEquals(2, status.getShifts().get(0).getCapacityLineCount());
        assertEquals(2, status.getShifts().get(1).getCapacityLineCount());
    }

    @Test
    void getNightShiftCapacityStatus_shouldIgnoreUnconfirmedPlan() {
        MesMdProductionLineDO line = MesMdProductionLineDO.builder().id(101L).calendarPlanId(91L).build();
        when(productionLineMapper.selectListByStatus(0)).thenReturn(List.of(line));
        when(planService.getPlan(91L)).thenReturn(plan(91L, MesCalPlanStatusEnum.PREPARE));

        MesProSchedulerWorkbenchNightShiftCapacityStatusRespVO status = service.getNightShiftCapacityStatus();

        assertFalse(status.getAvailable());
        assertEquals(0, status.getAvailableShiftCount());
        assertEquals(0, status.getCapacityLineCount());
        assertTrue(status.getShifts().isEmpty());
    }

    @Test
    void getNightShiftCapacityStatus_shouldIgnoreNightShiftWithInvalidDuration() {
        MesMdProductionLineDO line = MesMdProductionLineDO.builder().id(101L).calendarPlanId(91L).build();
        MesCalPlanShiftDO invalidNight = MesCalPlanShiftDO.builder()
                .id(201L).planId(91L).name("夜班").startTime("25:00").endTime("04:00").build();
        when(productionLineMapper.selectListByStatus(0)).thenReturn(List.of(line));
        when(planService.getPlan(91L)).thenReturn(plan(91L, MesCalPlanStatusEnum.CONFIRMED));
        when(planShiftService.getPlanShiftListByPlanId(91L)).thenReturn(List.of(invalidNight));

        MesProSchedulerWorkbenchNightShiftCapacityStatusRespVO status = service.getNightShiftCapacityStatus();

        assertFalse(status.getAvailable());
        assertEquals(0, status.getAvailableShiftCount());
        assertEquals(0, status.getCapacityLineCount());
        assertTrue(status.getShifts().isEmpty());
    }

    @Test
    void getNightShiftCapacityStatus_shouldExposeConfirmedValidNightShiftWithFuturePositiveCapacity() {
        MesMdProductionLineDO line = MesMdProductionLineDO.builder().id(101L).calendarPlanId(91L).build();
        MesCalPlanShiftDO night = MesCalPlanShiftDO.builder()
                .id(201L).planId(91L).name("夜班").startTime("20:00").endTime("04:00").build();
        when(productionLineMapper.selectListByStatus(0)).thenReturn(List.of(line));
        when(planService.getPlan(91L)).thenReturn(plan(91L, MesCalPlanStatusEnum.CONFIRMED));
        when(planShiftService.getPlanShiftListByPlanId(91L)).thenReturn(List.of(night));
        when(capacityPlanMapper.selectListByLineIdsAndDate(anyCollection(), any(LocalDateTime.class)))
                .thenReturn(List.of(capacity(101L, 201L)));

        MesProSchedulerWorkbenchNightShiftCapacityStatusRespVO status = service.getNightShiftCapacityStatus();

        assertTrue(status.getAvailable());
        assertEquals(1, status.getAvailableShiftCount());
        assertEquals(1, status.getCapacityLineCount());
        assertEquals(201L, status.getShifts().get(0).getShiftId());
    }

    private MesCalPlanDO plan(Long id, MesCalPlanStatusEnum status) {
        return MesCalPlanDO.builder().id(id).status(status.getStatus()).build();
    }

    private MesProCapacityPlanDO capacity(Long lineId, Long shiftId) {
        return MesProCapacityPlanDO.builder()
                .lineId(lineId)
                .shiftId(shiftId)
                .enabled(true)
                .capacityMinutes(480)
                .build();
    }
}
