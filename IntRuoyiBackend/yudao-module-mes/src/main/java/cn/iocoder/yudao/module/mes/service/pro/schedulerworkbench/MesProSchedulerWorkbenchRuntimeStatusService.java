package cn.iocoder.yudao.module.mes.service.pro.schedulerworkbench;

import cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo.MesProSchedulerWorkbenchAutoScheduleJobStatusRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo.MesProSchedulerWorkbenchNightShiftCapacityStatusRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.job.vo.job.JobSaveReqVO;
import cn.iocoder.yudao.module.infra.service.job.JobLogService;
import cn.iocoder.yudao.module.infra.service.job.JobService;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.quartz.core.util.CronUtils;
import cn.iocoder.yudao.module.infra.controller.admin.job.vo.job.JobPageReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.job.vo.log.JobLogPageReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.job.JobDO;
import cn.iocoder.yudao.module.infra.dal.dataobject.job.JobLogDO;
import cn.iocoder.yudao.module.infra.enums.job.JobLogStatusEnum;
import cn.iocoder.yudao.module.infra.enums.job.JobStatusEnum;
import cn.iocoder.yudao.module.mes.dal.dataobject.cal.plan.MesCalPlanShiftDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdProductionLineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProCapacityPlanDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdProductionLineMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProCapacityPlanMapper;
import cn.iocoder.yudao.module.mes.service.cal.plan.MesCalPlanShiftService;
import cn.iocoder.yudao.module.mes.service.pro.schedule.CapacityWindowAllocator;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.enums.CommonStatusEnum.ENABLE;

@Service
public class MesProSchedulerWorkbenchRuntimeStatusService {

    static final String NIGHTLY_REPLAN_HANDLER_NAME = "mesProNightlyReplanJob";

    @Resource
    private JobService jobService;
    @Resource
    private JobLogService jobLogService;
    @Resource
    private MesMdProductionLineMapper productionLineMapper;
    @Resource
    private MesCalPlanShiftService planShiftService;
    @Resource
    private MesProCapacityPlanMapper capacityPlanMapper;
    @Resource
    private CapacityWindowAllocator capacityWindowAllocator;

    public MesProSchedulerWorkbenchAutoScheduleJobStatusRespVO getAutoScheduleJobStatus() {
        JobDO job = findNightlyReplanJob();
        MesProSchedulerWorkbenchAutoScheduleJobStatusRespVO response =
                new MesProSchedulerWorkbenchAutoScheduleJobStatusRespVO();
        response.setConfigured(job != null);
        response.setEnabled(job != null && JobStatusEnum.NORMAL.getStatus().equals(job.getStatus()));
        if (job == null) {
            return response;
        }
        response.setJobId(job.getId());
        response.setJobName(job.getName());
        response.setCronExpression(job.getCronExpression());
        List<java.time.LocalDateTime> nextTimes = CronUtils.getNextTimes(job.getCronExpression(), 1);
        response.setNextTriggerTime(nextTimes.isEmpty() ? null : nextTimes.get(0));
        JobLogDO latestLog = findLatestLog(job.getId());
        if (latestLog != null) {
            response.setLatestBeginTime(latestLog.getBeginTime());
            response.setLatestEndTime(latestLog.getEndTime());
            response.setLatestStatus(resolveJobLogStatus(latestLog.getStatus()));
            response.setLatestResult(latestLog.getResult());
        }
        return response;
    }

    public void updateNightlyReplanTime(String time) {
        JobDO job = findNightlyReplanJob();
        if (job == null) {
            throw new IllegalStateException("自动排产任务未注册，handlerName=" + NIGHTLY_REPLAN_HANDLER_NAME);
        }
        String[] parts = time.split(":", -1);
        if (parts.length != 2) {
            throw new IllegalArgumentException("自动重排时间必须为 HH:mm");
        }
        JobSaveReqVO update = new JobSaveReqVO();
        update.setId(job.getId());
        update.setName(job.getName());
        update.setHandlerName(job.getHandlerName());
        update.setHandlerParam(job.getHandlerParam());
        update.setCronExpression("0 " + Integer.parseInt(parts[1]) + " " + Integer.parseInt(parts[0]) + " * * ?");
        update.setRetryCount(Objects.requireNonNull(job.getRetryCount(), "自动排产任务缺少重试次数"));
        update.setRetryInterval(Objects.requireNonNull(job.getRetryInterval(), "自动排产任务缺少重试间隔"));
        update.setMonitorTimeout(job.getMonitorTimeout());
        try {
            jobService.updateJob(update);
        } catch (org.quartz.SchedulerException ex) {
            throw new IllegalStateException("更新自动排产任务时间失败", ex);
        }
    }

    public MesProSchedulerWorkbenchNightShiftCapacityStatusRespVO getNightShiftCapacityStatus() {
        List<MesMdProductionLineDO> lines = productionLineMapper.selectListByStatus(ENABLE.getStatus());
        Map<Long, List<MesMdProductionLineDO>> linesByPlanId = new LinkedHashMap<>();
        for (MesMdProductionLineDO line : safeList(lines)) {
            if (line.getCalendarPlanId() != null) {
                linesByPlanId.computeIfAbsent(line.getCalendarPlanId(), key -> new ArrayList<>()).add(line);
            }
        }
        List<MesProSchedulerWorkbenchNightShiftCapacityStatusRespVO.NightShift> shiftRows = new ArrayList<>();
        Set<Long> capacityLineIds = new LinkedHashSet<>();
        for (Map.Entry<Long, List<MesMdProductionLineDO>> entry : linesByPlanId.entrySet()) {
            List<MesCalPlanShiftDO> nightShifts = safeList(planShiftService.getPlanShiftListByPlanId(entry.getKey()))
                    .stream().filter(capacityWindowAllocator::isNightShift).toList();
            if (nightShifts.isEmpty()) {
                continue;
            }
            List<Long> lineIds = entry.getValue().stream().map(MesMdProductionLineDO::getId).filter(Objects::nonNull).toList();
            List<MesProCapacityPlanDO> capacities = capacityPlanMapper.selectListByLineIdsAndDate(
                    lineIds, LocalDate.now().atStartOfDay());
            for (MesCalPlanShiftDO shift : nightShifts) {
                Set<Long> currentCapacityLineIds = safeList(capacities).stream()
                        .filter(item -> Objects.equals(item.getShiftId(), shift.getId()))
                        .filter(item -> Boolean.TRUE.equals(item.getEnabled()))
                        .filter(item -> item.getCapacityMinutes() != null && item.getCapacityMinutes() > 0)
                        .map(MesProCapacityPlanDO::getLineId).filter(Objects::nonNull)
                        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
                capacityLineIds.addAll(currentCapacityLineIds);
                MesProSchedulerWorkbenchNightShiftCapacityStatusRespVO.NightShift row =
                        new MesProSchedulerWorkbenchNightShiftCapacityStatusRespVO.NightShift();
                row.setPlanId(entry.getKey());
                row.setShiftId(shift.getId());
                row.setShiftName(shift.getName());
                row.setStartTime(shift.getStartTime());
                row.setEndTime(shift.getEndTime());
                row.setCapacityLineCount(currentCapacityLineIds.size());
                shiftRows.add(row);
            }
        }
        MesProSchedulerWorkbenchNightShiftCapacityStatusRespVO response =
                new MesProSchedulerWorkbenchNightShiftCapacityStatusRespVO();
        response.setAvailableShiftCount(shiftRows.size());
        response.setCapacityLineCount(capacityLineIds.size());
        response.setAvailable(!capacityLineIds.isEmpty());
        response.setShifts(shiftRows);
        return response;
    }

    private JobDO findNightlyReplanJob() {
        JobPageReqVO request = new JobPageReqVO();
        request.setPageNo(1);
        request.setPageSize(100);
        request.setHandlerName(NIGHTLY_REPLAN_HANDLER_NAME);
        PageResult<JobDO> page = jobService.getJobPage(request);
        List<JobDO> exactMatches = safeList(page == null ? null : page.getList()).stream()
                .filter(item -> NIGHTLY_REPLAN_HANDLER_NAME.equals(item.getHandlerName())).toList();
        if (exactMatches.size() > 1) {
            throw new IllegalStateException("自动排产任务 handlerName 不唯一：" + NIGHTLY_REPLAN_HANDLER_NAME);
        }
        return exactMatches.isEmpty() ? null : exactMatches.get(0);
    }

    private JobLogDO findLatestLog(Long jobId) {
        JobLogPageReqVO request = new JobLogPageReqVO();
        request.setPageNo(1);
        request.setPageSize(1);
        request.setJobId(jobId);
        PageResult<JobLogDO> page = jobLogService.getJobLogPage(request);
        return safeList(page == null ? null : page.getList()).stream().findFirst().orElse(null);
    }

    private String resolveJobLogStatus(Integer status) {
        if (JobLogStatusEnum.RUNNING.getStatus().equals(status)) return "RUNNING";
        if (JobLogStatusEnum.SUCCESS.getStatus().equals(status)) return "SUCCESS";
        if (JobLogStatusEnum.FAILURE.getStatus().equals(status)) return "FAILURE";
        throw new IllegalStateException("未知自动排产任务日志状态：" + status);
    }

    private static <T> List<T> safeList(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }
}
