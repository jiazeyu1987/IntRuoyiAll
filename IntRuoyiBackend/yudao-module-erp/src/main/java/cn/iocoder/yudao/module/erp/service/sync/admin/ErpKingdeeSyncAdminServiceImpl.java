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
        return runManualSync(syncType, false);
    }

    @Override
    public ErpKingdeeFullSyncRespVO runFullSync(String syncType) {
        return runManualSync(syncType, true);
    }

    private ErpKingdeeFullSyncRespVO runManualSync(String syncType, boolean fullSync) {
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
        JobDO job = findJob(type.getHandlerName());
        try {
            String originalHandlerParam = fullSync ? ErpKingdeeFullSyncHandler.FULL_SYNC_JOB_PARAM
                    : job.getHandlerParam();
            String handlerParam = TenantJobParam.forTenant(TenantContextHolder.getRequiredTenantId(), originalHandlerParam);
            jobService.triggerJob(job.getId(), handlerParam);
        } catch (SchedulerException ex) {
            throw exception(KINGDEE_TABLE_AUTO_SYNC_EXECUTE_FAILED, ex.getMessage());
        }
        return new ErpKingdeeFullSyncRespVO(type.getSyncType(), type.getHandlerName(), job.getId(),
                fullSync ? "已提交全量同步任务" : "已提交增量同步任务");
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
