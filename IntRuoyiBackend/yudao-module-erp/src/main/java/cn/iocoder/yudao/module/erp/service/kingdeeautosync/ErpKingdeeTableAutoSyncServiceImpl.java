package cn.iocoder.yudao.module.erp.service.kingdeeautosync;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.kingdeeautosync.vo.ErpKingdeeTableAutoSyncPlanItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.kingdeeautosync.vo.ErpKingdeeTableAutoSyncPlanItemSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.kingdeeautosync.vo.ErpKingdeeTableAutoSyncPlanRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.kingdeeautosync.vo.ErpKingdeeTableAutoSyncPlanSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.kingdeeautosync.vo.ErpKingdeeTableAutoSyncRunItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.kingdeeautosync.vo.ErpKingdeeTableAutoSyncRunOnceRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.kingdeeautosync.vo.ErpKingdeeTableAutoSyncTypeRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeSyncRunPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeSyncRunRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeSyncWatermarkRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.kingdeeautosync.ErpKingdeeTableAutoSyncPlanDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.kingdeeautosync.ErpKingdeeTableAutoSyncPlanItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.kingdeeautosync.ErpKingdeeTableAutoSyncPlanItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.kingdeeautosync.ErpKingdeeTableAutoSyncPlanMapper;
import cn.iocoder.yudao.module.erp.enums.kingdeeautosync.ErpKingdeeTableAutoSyncTypeEnum;
import cn.iocoder.yudao.module.erp.service.sync.admin.ErpKingdeeSyncAdminService;
import cn.iocoder.yudao.module.infra.controller.admin.job.vo.job.JobPageReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.job.vo.job.JobSaveReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.job.JobDO;
import cn.iocoder.yudao.module.infra.enums.job.JobStatusEnum;
import cn.iocoder.yudao.module.infra.service.job.JobService;
import jakarta.annotation.Resource;
import org.quartz.SchedulerException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_TABLE_AUTO_SYNC_EXECUTE_FAILED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_TABLE_AUTO_SYNC_JOB_HANDLER_INVALID;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_TABLE_AUTO_SYNC_JOB_HANDLER_MISSING;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_TABLE_AUTO_SYNC_JOB_SAVE_FAILED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_TABLE_AUTO_SYNC_PLAN_DISABLED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_TABLE_AUTO_SYNC_PLAN_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_TABLE_AUTO_SYNC_START_TIME_REQUIRED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_TABLE_AUTO_SYNC_TYPE_REQUIRED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_TABLE_AUTO_SYNC_TYPE_UNSUPPORTED;

@Service
@Validated
public class ErpKingdeeTableAutoSyncServiceImpl implements ErpKingdeeTableAutoSyncService {

    private static final String HANDLER_NAME = "erpKingdeeTableAutoSyncJob";
    private static final String DISPATCHER_CRON = "0 * * * * ?";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";

    @Resource
    private ErpKingdeeTableAutoSyncPlanMapper planMapper;
    @Resource
    private ErpKingdeeTableAutoSyncPlanItemMapper planItemMapper;
    @Resource
    private JobService jobService;
    @Resource
    private ErpKingdeeSyncAdminService syncAdminService;

    @Override
    public ErpKingdeeTableAutoSyncPlanRespVO getPlan() {
        ErpKingdeeTableAutoSyncPlanDO plan = planMapper.selectCurrentTenantPlan();
        if (plan == null) {
            return defaultPlanResp();
        }
        return buildPlanResp(plan);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpKingdeeTableAutoSyncPlanRespVO savePlan(ErpKingdeeTableAutoSyncPlanSaveReqVO reqVO) {
        List<ErpKingdeeTableAutoSyncPlanItemDO> items = normalizeItems(reqVO.getItems());
        if (Boolean.TRUE.equals(reqVO.getEnabled())) {
            validateEnabledPlan(reqVO, items);
        }
        ErpKingdeeTableAutoSyncPlanDO plan = planMapper.selectCurrentTenantPlan();
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        if (plan == null) {
            plan = new ErpKingdeeTableAutoSyncPlanDO().setTenantId(tenantId);
            planMapper.insert(plan);
        }
        plan.setEnabled(Boolean.TRUE.equals(reqVO.getEnabled()));
        plan.setDailyStartTime(reqVO.getDailyStartTime());
        plan.setCronExpression(reqVO.getDailyStartTime() == null ? null : buildBusinessCron(reqVO.getDailyStartTime()));
        if (Boolean.TRUE.equals(plan.getEnabled())) {
            plan.setJobId(ensureDispatcherJob());
        }
        planMapper.updateById(plan);

        savePlanItems(plan.getId(), tenantId, items);
        return buildPlanResp(plan);
    }

    @Override
    public List<ErpKingdeeTableAutoSyncTypeRespVO> getSyncTypes() {
        return ErpKingdeeTableAutoSyncTypeEnum.list().stream()
                .map(type -> new ErpKingdeeTableAutoSyncTypeRespVO(type.getSyncType(), type.getLabel(),
                        type.getHandlerName()))
                .toList();
    }

    @Override
    public ErpKingdeeTableAutoSyncRunOnceRespVO runOnce() {
        return executeSelectedSyncs(validateExecutablePlan(), false);
    }

    @Override
    public PageResult<ErpKingdeeSyncRunRespVO> getRunPage(ErpKingdeeSyncRunPageReqVO pageReqVO) {
        return syncAdminService.getRunPage(pageReqVO);
    }

    @Override
    public List<ErpKingdeeSyncWatermarkRespVO> getWatermarks() {
        return syncAdminService.getWatermarks();
    }

    @Override
    public String executeAutoForCurrentTenant() {
        ErpKingdeeTableAutoSyncPlanDO plan = planMapper.selectCurrentTenantPlan();
        if (plan == null || !Boolean.TRUE.equals(plan.getEnabled())) {
            return "skipped: ERP table auto sync disabled";
        }
        if (!isAutoDue(plan)) {
            return "skipped: not due";
        }
        if (alreadyRanToday(plan)) {
            return "skipped: already run today";
        }
        ErpKingdeeTableAutoSyncRunOnceRespVO result = executeSelectedSyncs(plan, true);
        return "status=" + result.getStatus() + ", selected=" + result.getTotalSyncCount()
                + ", success=" + result.getSuccessSyncCount();
    }

    private ErpKingdeeTableAutoSyncPlanRespVO defaultPlanResp() {
        ErpKingdeeTableAutoSyncPlanRespVO respVO = new ErpKingdeeTableAutoSyncPlanRespVO();
        respVO.setEnabled(false);
        respVO.setDailyStartTime(LocalTime.of(2, 0));
        respVO.setItems(getSyncTypes().stream()
                .map(type -> new ErpKingdeeTableAutoSyncPlanItemRespVO()
                        .setSyncType(type.getSyncType())
                        .setEnabled(false)
                        .setSortOrder(0))
                .toList());
        return respVO;
    }

    private ErpKingdeeTableAutoSyncPlanRespVO buildPlanResp(ErpKingdeeTableAutoSyncPlanDO plan) {
        ErpKingdeeTableAutoSyncPlanRespVO respVO = BeanUtils.toBean(plan, ErpKingdeeTableAutoSyncPlanRespVO.class);
        respVO.setItems(BeanUtils.toBean(planItemMapper.selectListByPlanId(plan.getId()),
                ErpKingdeeTableAutoSyncPlanItemRespVO.class));
        return respVO;
    }

    private List<ErpKingdeeTableAutoSyncPlanItemDO> normalizeItems(List<ErpKingdeeTableAutoSyncPlanItemSaveReqVO> reqItems) {
        if (CollUtil.isEmpty(reqItems)) {
            return List.of();
        }
        Set<String> seen = new HashSet<>();
        List<ErpKingdeeTableAutoSyncPlanItemDO> items = new ArrayList<>();
        int index = 0;
        for (ErpKingdeeTableAutoSyncPlanItemSaveReqVO reqItem : reqItems) {
            ErpKingdeeTableAutoSyncTypeEnum type = validateSyncType(reqItem.getSyncType());
            if (!seen.add(type.getSyncType())) {
                continue;
            }
            items.add(new ErpKingdeeTableAutoSyncPlanItemDO()
                    .setSyncType(type.getSyncType())
                    .setEnabled(Boolean.TRUE.equals(reqItem.getEnabled()))
                    .setSortOrder(reqItem.getSortOrder() == null ? index * 10 : reqItem.getSortOrder()));
            index++;
        }
        return items;
    }

    private void savePlanItems(Long planId, Long tenantId, List<ErpKingdeeTableAutoSyncPlanItemDO> items) {
        Map<String, ErpKingdeeTableAutoSyncPlanItemDO> existingByType = planItemMapper.selectListByPlanId(planId)
                .stream()
                .collect(java.util.stream.Collectors.toMap(ErpKingdeeTableAutoSyncPlanItemDO::getSyncType,
                        item -> item));
        Set<String> requestedTypes = new HashSet<>();
        for (ErpKingdeeTableAutoSyncPlanItemDO item : items) {
            requestedTypes.add(item.getSyncType());
            ErpKingdeeTableAutoSyncPlanItemDO existing = existingByType.get(item.getSyncType());
            if (existing == null) {
                item.setTenantId(tenantId);
                item.setPlanId(planId);
                planItemMapper.insert(item);
                continue;
            }
            existing.setEnabled(item.getEnabled())
                    .setSortOrder(item.getSortOrder());
            planItemMapper.updateById(existing);
        }
        existingByType.values().stream()
                .filter(existing -> !requestedTypes.contains(existing.getSyncType()))
                .forEach(existing -> {
                    existing.setEnabled(false);
                    planItemMapper.updateById(existing);
                });
    }

    private ErpKingdeeTableAutoSyncTypeEnum validateSyncType(String syncType) {
        try {
            return ErpKingdeeTableAutoSyncTypeEnum.requiredOf(syncType);
        } catch (IllegalArgumentException ex) {
            throw exception(KINGDEE_TABLE_AUTO_SYNC_TYPE_UNSUPPORTED, syncType);
        }
    }

    private void validateEnabledPlan(ErpKingdeeTableAutoSyncPlanSaveReqVO reqVO,
                                     List<ErpKingdeeTableAutoSyncPlanItemDO> items) {
        if (reqVO.getDailyStartTime() == null) {
            throw exception(KINGDEE_TABLE_AUTO_SYNC_START_TIME_REQUIRED);
        }
        List<ErpKingdeeTableAutoSyncPlanItemDO> enabledItems = items.stream()
                .filter(item -> Boolean.TRUE.equals(item.getEnabled()))
                .toList();
        if (CollUtil.isEmpty(enabledItems)) {
            throw exception(KINGDEE_TABLE_AUTO_SYNC_TYPE_REQUIRED);
        }
        enabledItems.forEach(item -> resolveJobHandler(validateSyncType(item.getSyncType())));
    }

    private ErpKingdeeTableAutoSyncPlanDO validateExecutablePlan() {
        ErpKingdeeTableAutoSyncPlanDO plan = planMapper.selectCurrentTenantPlan();
        if (plan == null) {
            throw exception(KINGDEE_TABLE_AUTO_SYNC_PLAN_NOT_EXISTS);
        }
        if (!Boolean.TRUE.equals(plan.getEnabled())) {
            throw exception(KINGDEE_TABLE_AUTO_SYNC_PLAN_DISABLED);
        }
        if (CollUtil.isEmpty(enabledItems(plan))) {
            throw exception(KINGDEE_TABLE_AUTO_SYNC_TYPE_REQUIRED);
        }
        return plan;
    }

    private Long ensureDispatcherJob() {
        JobSaveReqVO saveReqVO = new JobSaveReqVO();
        saveReqVO.setName("ERP 表格自动同步 Job");
        saveReqVO.setHandlerName(HANDLER_NAME);
        saveReqVO.setHandlerParam("");
        saveReqVO.setCronExpression(DISPATCHER_CRON);
        saveReqVO.setRetryCount(0);
        saveReqVO.setRetryInterval(0);
        saveReqVO.setMonitorTimeout(0);
        try {
            JobDO job = findDispatcherJob();
            if (job == null) {
                return jobService.createJob(saveReqVO);
            }
            if (Objects.equals(job.getStatus(), JobStatusEnum.STOP.getStatus())) {
                jobService.updateJobStatus(job.getId(), JobStatusEnum.NORMAL.getStatus());
                job = jobService.getJob(job.getId());
            }
            if (Objects.equals(job.getStatus(), JobStatusEnum.NORMAL.getStatus())) {
                saveReqVO.setId(job.getId());
                jobService.updateJob(saveReqVO);
            }
            return job.getId();
        } catch (SchedulerException ex) {
            throw exception(KINGDEE_TABLE_AUTO_SYNC_JOB_SAVE_FAILED,
                    StrUtil.blankToDefault(ex.getMessage(), ex.getClass().getSimpleName()));
        }
    }

    private JobDO findDispatcherJob() {
        JobPageReqVO reqVO = new JobPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setHandlerName(HANDLER_NAME);
        return jobService.getJobPage(reqVO).getList().stream()
                .filter(job -> HANDLER_NAME.equals(job.getHandlerName()))
                .findFirst()
                .orElse(null);
    }

    private ErpKingdeeTableAutoSyncRunOnceRespVO executeSelectedSyncs(ErpKingdeeTableAutoSyncPlanDO plan,
                                                                      boolean autoRun) {
        List<ErpKingdeeTableAutoSyncPlanItemDO> enabledItems = enabledItems(plan);
        if (CollUtil.isEmpty(enabledItems)) {
            throw exception(KINGDEE_TABLE_AUTO_SYNC_TYPE_REQUIRED);
        }
        ErpKingdeeTableAutoSyncRunOnceRespVO respVO = new ErpKingdeeTableAutoSyncRunOnceRespVO()
                .setStatus(STATUS_SUCCESS)
                .setTotalSyncCount(enabledItems.size())
                .setSuccessSyncCount(0);
        List<ErpKingdeeTableAutoSyncRunItemRespVO> runItems = new ArrayList<>();
        int successCount = 0;
        try {
            for (ErpKingdeeTableAutoSyncPlanItemDO item : enabledItems) {
                ErpKingdeeTableAutoSyncTypeEnum type = validateSyncType(item.getSyncType());
                JobHandler handler = resolveJobHandler(type);
                String message = handler.execute("");
                runItems.add(new ErpKingdeeTableAutoSyncRunItemRespVO()
                        .setSyncType(type.getSyncType())
                        .setLabel(type.getLabel())
                        .setHandlerName(type.getHandlerName())
                        .setStatus(STATUS_SUCCESS)
                        .setMessage(message));
                successCount++;
            }
            String message = "ERP 表格自动同步完成：selected=" + enabledItems.size() + ", success=" + successCount;
            respVO.setItems(runItems).setSuccessSyncCount(successCount);
            if (autoRun) {
                markAutoRunDateAfterSuccess(plan.getId());
            }
            updateLastStatus(plan.getId(), STATUS_SUCCESS, message);
            return respVO;
        } catch (Exception ex) {
            String failureMessage = rootMessage(ex);
            updateLastStatus(plan.getId(), STATUS_FAILED, failureMessage);
            throw exception(KINGDEE_TABLE_AUTO_SYNC_EXECUTE_FAILED, failureMessage);
        }
    }

    private void markAutoRunDateAfterSuccess(Long planId) {
        planMapper.updateById(new ErpKingdeeTableAutoSyncPlanDO()
                .setId(planId)
                .setLastAutoRunDate(LocalDate.now()));
    }

    private JobHandler resolveJobHandler(ErpKingdeeTableAutoSyncTypeEnum type) {
        Object bean;
        try {
            bean = SpringUtil.getBean(type.getHandlerName());
        } catch (Exception ex) {
            throw exception(KINGDEE_TABLE_AUTO_SYNC_JOB_HANDLER_MISSING, type.getHandlerName());
        }
        if (!(bean instanceof JobHandler jobHandler)) {
            throw exception(KINGDEE_TABLE_AUTO_SYNC_JOB_HANDLER_INVALID, type.getHandlerName());
        }
        return jobHandler;
    }

    private void updateLastStatus(Long planId, String status, String message) {
        planMapper.updateById(new ErpKingdeeTableAutoSyncPlanDO()
                .setId(planId)
                .setLastRunTime(LocalDateTime.now())
                .setLastStatus(status)
                .setLastMessage(message));
    }

    private List<ErpKingdeeTableAutoSyncPlanItemDO> enabledItems(ErpKingdeeTableAutoSyncPlanDO plan) {
        return planItemMapper.selectListByPlanId(plan.getId()).stream()
                .filter(item -> Boolean.TRUE.equals(item.getEnabled()))
                .sorted(Comparator.comparing(ErpKingdeeTableAutoSyncPlanItemDO::getSortOrder,
                        Comparator.nullsLast(Integer::compareTo)))
                .toList();
    }

    private boolean isAutoDue(ErpKingdeeTableAutoSyncPlanDO plan) {
        LocalTime startTime = plan.getDailyStartTime();
        return startTime != null && !LocalTime.now().isBefore(startTime);
    }

    private boolean alreadyRanToday(ErpKingdeeTableAutoSyncPlanDO plan) {
        return plan.getLastAutoRunDate() != null && LocalDate.now().equals(plan.getLastAutoRunDate());
    }

    private static String buildBusinessCron(LocalTime time) {
        return "0 " + time.getMinute() + " " + time.getHour() + " * * ?";
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return StrUtil.blankToDefault(current.getMessage(), current.getClass().getSimpleName());
    }
}
