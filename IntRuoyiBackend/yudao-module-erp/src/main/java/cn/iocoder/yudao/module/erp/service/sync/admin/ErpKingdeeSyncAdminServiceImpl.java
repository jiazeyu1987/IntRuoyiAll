package cn.iocoder.yudao.module.erp.service.sync.admin;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJobParam;
import cn.iocoder.yudao.module.infra.controller.admin.job.vo.job.JobPageReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.job.JobDO;
import cn.iocoder.yudao.module.infra.service.job.JobService;
import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeFullSyncRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeSyncRunPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeSyncRunRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeSyncWatermarkRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sync.ErpKingdeeSyncRunDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sync.ErpKingdeeSyncRunMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sync.ErpKingdeeSyncWatermarkMapper;
import cn.iocoder.yudao.module.erp.enums.kingdeeautosync.ErpKingdeeTableAutoSyncTypeEnum;
import cn.iocoder.yudao.module.erp.service.config.ErpKingdeeConfigService;
import jakarta.annotation.Resource;
import org.quartz.SchedulerException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.quartz.ObjectAlreadyExistsException;
import cn.iocoder.yudao.framework.quartz.core.scheduler.SchedulerManager;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_TABLE_AUTO_SYNC_JOB_HANDLER_INVALID;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_TABLE_AUTO_SYNC_JOB_HANDLER_MISSING;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_TABLE_AUTO_SYNC_JOB_NOT_CONFIGURED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_TABLE_AUTO_SYNC_EXECUTE_FAILED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_TABLE_AUTO_SYNC_TYPE_UNSUPPORTED;

@Service
@Validated
public class ErpKingdeeSyncAdminServiceImpl implements ErpKingdeeSyncAdminService {

    @Resource
    private ErpKingdeeSyncRunMapper runMapper;
    @Resource
    private ErpKingdeeSyncWatermarkMapper watermarkMapper;
    @Resource
    private ErpKingdeeConfigService kingdeeConfigService;
    @Resource
    private JobService jobService;
    @Resource
    private ApplicationContext applicationContext;
    @Resource
    private SchedulerManager schedulerManager;

    @Override
    public PageResult<ErpKingdeeSyncRunRespVO> getRunPage(ErpKingdeeSyncRunPageReqVO pageReqVO) {
        PageResult<ErpKingdeeSyncRunDO> pageResult = runMapper.selectPage(pageReqVO);
        return new PageResult<>(BeanUtils.toBean(pageResult.getList(), ErpKingdeeSyncRunRespVO.class),
                pageResult.getTotal());
    }

    @Override
    public List<ErpKingdeeSyncWatermarkRespVO> getWatermarks() {
        return BeanUtils.toBean(watermarkMapper.selectListOrderBySyncType(), ErpKingdeeSyncWatermarkRespVO.class);
    }

    @Override
    public ErpKingdeeFullSyncRespVO runIncrementalSync(String syncType) {
        return runManualSync(syncType, null);
    }

    @Override
    public ErpKingdeeFullSyncRespVO runFullSync(String syncType) {
        return runManualSync(syncType, ErpKingdeeFullSyncHandler.FULL_SYNC_JOB_PARAM);
    }

    @Override
    public ErpKingdeeFullSyncRespVO runRecentFullSync(String syncType, int days, long clickedAt) {
        if (days < 1) {
            throw new IllegalArgumentException("同步天数必须大于0");
        }
        LocalDateTime end = LocalDateTime.ofInstant(Instant.ofEpochMilli(clickedAt), ZoneId.of("Asia/Shanghai"));
        return runManualSync(syncType, ErpKingdeeFullSyncHandler.windowParam(end.minusDays(days), end));
    }

    private ErpKingdeeFullSyncRespVO runManualSync(String syncType, String fullParam) {
        boolean fullSync = fullParam != null;
        kingdeeConfigService.getActiveConnection();
        ErpKingdeeTableAutoSyncTypeEnum type = resolveSyncType(syncType);
        Object bean;
        try {
            bean = applicationContext.getBean(type.getHandlerName());
        } catch (Exception ex) {
            throw exception(KINGDEE_TABLE_AUTO_SYNC_JOB_HANDLER_MISSING, type.getHandlerName());
        }
        if (fullSync && !(bean instanceof ErpKingdeeFullSyncHandler)) {
            throw exception(KINGDEE_TABLE_AUTO_SYNC_JOB_HANDLER_INVALID, type.getHandlerName());
        }
        if (fullSync && runMapper.selectRunningBySyncType(type.getSyncType()) != null) {
            throw busy();
        }
        JobDO job = findJob(type.getHandlerName());
        try {
            String originalHandlerParam = fullSync ? fullParam
                    : job.getHandlerParam();
            String handlerParam = TenantJobParam.forTenant(TenantContextHolder.getRequiredTenantId(), originalHandlerParam);
            if (fullSync) {
                // Fixed trigger identity is claimed atomically by Quartz, including its pending/executing lifetime.
                schedulerManager.triggerUniqueJob(job.getId(), type.getHandlerName(), handlerParam,
                        "erp-full-" + type.getSyncType());
            } else {
                jobService.triggerJob(job.getId(), handlerParam);
            }
        } catch (ObjectAlreadyExistsException ex) {
            throw busy();
        } catch (SchedulerException ex) {
            throw exception(KINGDEE_TABLE_AUTO_SYNC_EXECUTE_FAILED, ex.getMessage());
        }
        return new ErpKingdeeFullSyncRespVO(type.getSyncType(), type.getHandlerName(), job.getId(),
                fullSync ? "已提交全量同步任务" : "已提交增量同步任务");
    }

    private cn.iocoder.yudao.framework.common.exception.ServiceException busy() {
        return cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception0(
                KINGDEE_TABLE_AUTO_SYNC_EXECUTE_FAILED.getCode(), "当前有其他任务正在同步，请稍后再试");
    }

    private JobDO findJob(String handlerName) {
        JobPageReqVO reqVO = new JobPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(1);
        reqVO.setHandlerName(handlerName);
        JobDO job = jobService.getJobPage(reqVO).getList().stream()
                .filter(item -> handlerName.equals(item.getHandlerName()))
                .findFirst()
                .orElse(null);
        if (job == null) {
            throw exception(KINGDEE_TABLE_AUTO_SYNC_JOB_NOT_CONFIGURED, handlerName);
        }
        return job;
    }

    private ErpKingdeeTableAutoSyncTypeEnum resolveSyncType(String syncType) {
        try {
            return ErpKingdeeTableAutoSyncTypeEnum.requiredOf(StrUtil.trim(syncType));
        } catch (IllegalArgumentException ex) {
            throw exception(KINGDEE_TABLE_AUTO_SYNC_TYPE_UNSUPPORTED, syncType);
        }
    }
}
