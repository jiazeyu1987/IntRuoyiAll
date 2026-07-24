package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.infra.controller.admin.job.vo.log.JobLogPageReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.logger.vo.apiaccesslog.ApiAccessLogPageReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.logger.vo.apierrorlog.ApiErrorLogPageReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlBusinessHealthItemRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlBusinessHealthRespVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileConfigDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileConfigMapper;
import cn.iocoder.yudao.module.infra.dal.mysql.job.JobLogMapper;
import cn.iocoder.yudao.module.infra.dal.mysql.logger.ApiAccessLogMapper;
import cn.iocoder.yudao.module.infra.dal.mysql.logger.ApiErrorLogMapper;
import cn.iocoder.yudao.module.infra.enums.job.JobLogStatusEnum;
import cn.iocoder.yudao.module.infra.enums.logger.ApiErrorLogProcessStatusEnum;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Service
public class RuntimeOpsBusinessHealthServiceImpl implements RuntimeOpsBusinessHealthService {

    private static final int SLOW_REQUEST_THRESHOLD_MILLIS = 2_000;
    private static final List<RequiredHealthItem> REQUIRED_HEALTH_ITEMS = List.of(
            new RequiredHealthItem("login", "登录"),
            new RequiredHealthItem("erp", "ERP"),
            new RequiredHealthItem("mes", "MES"),
            new RequiredHealthItem("file-object", "文件对象"),
            new RequiredHealthItem("api-error", "API 错误"),
            new RequiredHealthItem("slow-request", "慢请求"),
            new RequiredHealthItem("job-failure", "任务失败")
    );

    private final List<RuntimeOpsBusinessHealthCollector> collectors;
    private final ApiErrorLogMapper apiErrorLogMapper;
    private final ApiAccessLogMapper apiAccessLogMapper;
    private final JobLogMapper jobLogMapper;
    private final FileConfigMapper fileConfigMapper;

    @Autowired
    public RuntimeOpsBusinessHealthServiceImpl(List<RuntimeOpsBusinessHealthCollector> collectors,
                                               ObjectProvider<ApiErrorLogMapper> apiErrorLogMapper,
                                               ObjectProvider<ApiAccessLogMapper> apiAccessLogMapper,
                                               ObjectProvider<JobLogMapper> jobLogMapper,
                                               ObjectProvider<FileConfigMapper> fileConfigMapper) {
        this.collectors = collectors;
        this.apiErrorLogMapper = apiErrorLogMapper.getIfAvailable();
        this.apiAccessLogMapper = apiAccessLogMapper.getIfAvailable();
        this.jobLogMapper = jobLogMapper.getIfAvailable();
        this.fileConfigMapper = fileConfigMapper.getIfAvailable();
    }

    public RuntimeOpsBusinessHealthServiceImpl(List<RuntimeOpsBusinessHealthCollector> collectors) {
        this.collectors = collectors;
        this.apiErrorLogMapper = null;
        this.apiAccessLogMapper = null;
        this.jobLogMapper = null;
        this.fileConfigMapper = null;
    }

    @Override
    public RuntimeControlBusinessHealthRespVO getBusinessHealth() {
        LocalDateTime sampledAt = LocalDateTime.now();
        Map<String, RuntimeControlBusinessHealthItemRespVO> items = new LinkedHashMap<>();
        for (RequiredHealthItem requiredItem : REQUIRED_HEALTH_ITEMS) {
            RuntimeControlBusinessHealthItemRespVO item = collectBuiltIn(requiredItem, sampledAt);
            items.put(item.getCode(), item);
        }
        List<RuntimeControlBusinessHealthItemRespVO> collectorFailures = new ArrayList<>();
        for (RuntimeOpsBusinessHealthCollector collector : collectors) {
            try {
                RuntimeControlBusinessHealthItemRespVO item = toItem(collector.collect());
                items.put(item.getCode(), item);
            } catch (RuntimeException ex) {
                collectorFailures.add(blockedCollectorFailure(ex, sampledAt));
            }
        }
        List<RuntimeControlBusinessHealthItemRespVO> resultItems = new ArrayList<>(items.values());
        resultItems.addAll(collectorFailures);
        RuntimeControlBusinessHealthRespVO response = new RuntimeControlBusinessHealthRespVO();
        response.setItems(resultItems);
        response.setSampledAt(sampledAt);
        response.setStatus(aggregate(resultItems.stream().map(RuntimeControlBusinessHealthItemRespVO::getStatus).toList()));
        return response;
    }

    private RuntimeControlBusinessHealthItemRespVO collectBuiltIn(RequiredHealthItem item, LocalDateTime sampledAt) {
        return switch (item.code()) {
            case "login" -> blocked(item, "缺少登录真实用户路径只读采集器或合成登录探针配置", sampledAt);
            case "erp" -> blocked(item, "缺少 ERP 跨模块只读健康采集器配置", sampledAt);
            case "mes" -> blocked(item, "缺少 MES 跨模块只读健康采集器配置", sampledAt);
            case "file-object" -> collectFileObject(item, sampledAt);
            case "api-error" -> collectApiError(item, sampledAt);
            case "slow-request" -> collectSlowRequest(item, sampledAt);
            case "job-failure" -> collectJobFailure(item, sampledAt);
            default -> blocked(item, "未知业务健康项：" + item.code(), sampledAt);
        };
    }

    private RuntimeControlBusinessHealthItemRespVO collectFileObject(RequiredHealthItem item, LocalDateTime sampledAt) {
        if (fileConfigMapper == null) {
            return blocked(item, "FileConfigMapper 未注入，无法读取文件对象主配置", sampledAt);
        }
        try {
            FileConfigDO masterConfig = fileConfigMapper.selectByMaster();
            if (masterConfig == null) {
                return blocked(item, "文件对象主配置缺失，不能确认文件上传/读取状态", sampledAt);
            }
            return pass(item, "masterConfigId=" + masterConfig.getId() + ", storage=" + masterConfig.getStorage(),
                    sampledAt);
        } catch (RuntimeException ex) {
            return blocked(item, "文件对象配置读取失败：" + failureReason(ex), sampledAt);
        }
    }

    private RuntimeControlBusinessHealthItemRespVO collectApiError(RequiredHealthItem item, LocalDateTime sampledAt) {
        if (apiErrorLogMapper == null) {
            return blocked(item, "ApiErrorLogMapper 未注入，无法读取未处理 API 错误日志", sampledAt);
        }
        try {
            ApiErrorLogPageReqVO reqVO = new ApiErrorLogPageReqVO();
            reqVO.setPageSize(1);
            reqVO.setProcessStatus(ApiErrorLogProcessStatusEnum.INIT.getStatus());
            long total = totalOf(() -> apiErrorLogMapper.selectPage(reqVO));
            return total == 0 ? pass(item, "未处理 API 错误数=0", sampledAt)
                    : warn(item, "未处理 API 错误数=" + total, sampledAt);
        } catch (RuntimeException ex) {
            return blocked(item, "API 错误日志读取失败：" + failureReason(ex), sampledAt);
        }
    }

    private RuntimeControlBusinessHealthItemRespVO collectSlowRequest(RequiredHealthItem item, LocalDateTime sampledAt) {
        if (apiAccessLogMapper == null) {
            return blocked(item, "ApiAccessLogMapper 未注入，无法读取慢请求日志", sampledAt);
        }
        try {
            ApiAccessLogPageReqVO reqVO = new ApiAccessLogPageReqVO();
            reqVO.setPageSize(1);
            reqVO.setDuration(SLOW_REQUEST_THRESHOLD_MILLIS);
            long total = totalOf(() -> apiAccessLogMapper.selectPage(reqVO));
            return total == 0 ? pass(item, "慢请求数=0, thresholdMillis=" + SLOW_REQUEST_THRESHOLD_MILLIS, sampledAt)
                    : warn(item, "慢请求数=" + total + ", thresholdMillis=" + SLOW_REQUEST_THRESHOLD_MILLIS, sampledAt);
        } catch (RuntimeException ex) {
            return blocked(item, "慢请求日志读取失败：" + failureReason(ex), sampledAt);
        }
    }

    private RuntimeControlBusinessHealthItemRespVO collectJobFailure(RequiredHealthItem item, LocalDateTime sampledAt) {
        if (jobLogMapper == null) {
            return blocked(item, "JobLogMapper 未注入，无法读取任务失败日志", sampledAt);
        }
        try {
            JobLogPageReqVO reqVO = new JobLogPageReqVO();
            reqVO.setPageSize(1);
            reqVO.setStatus(JobLogStatusEnum.FAILURE.getStatus());
            long total = totalOf(() -> jobLogMapper.selectPage(reqVO));
            return total == 0 ? pass(item, "任务失败数=0", sampledAt)
                    : warn(item, "任务失败数=" + total, sampledAt);
        } catch (RuntimeException ex) {
            return blocked(item, "任务失败日志读取失败：" + failureReason(ex), sampledAt);
        }
    }

    private long totalOf(Supplier<PageResult<?>> supplier) {
        PageResult<?> page = supplier.get();
        if (page == null || page.getTotal() == null) {
            throw new IllegalStateException("分页查询结果缺少 total");
        }
        return page.getTotal();
    }

    private RuntimeControlBusinessHealthItemRespVO pass(RequiredHealthItem item, String evidence, LocalDateTime sampledAt) {
        return item(item, RuntimeOpsInspectionStatus.PASS, evidence, null, sampledAt);
    }

    private RuntimeControlBusinessHealthItemRespVO warn(RequiredHealthItem item, String reason, LocalDateTime sampledAt) {
        return item(item, RuntimeOpsInspectionStatus.WARN, null, reason, sampledAt);
    }

    private RuntimeControlBusinessHealthItemRespVO blocked(RequiredHealthItem item, String reason, LocalDateTime sampledAt) {
        return item(item, RuntimeOpsInspectionStatus.BLOCKED, null, reason, sampledAt);
    }

    private RuntimeControlBusinessHealthItemRespVO item(RequiredHealthItem definition, RuntimeOpsInspectionStatus status,
                                                        String evidence, String reason, LocalDateTime sampledAt) {
        RuntimeControlBusinessHealthItemRespVO item = new RuntimeControlBusinessHealthItemRespVO();
        item.setCode(definition.code());
        item.setName(definition.name());
        item.setStatus(status);
        item.setEvidence(evidence);
        item.setReason(reason);
        item.setSampledAt(sampledAt);
        return item;
    }

    private RuntimeControlBusinessHealthItemRespVO blockedCollectorFailure(RuntimeException ex, LocalDateTime sampledAt) {
        RuntimeControlBusinessHealthItemRespVO item = new RuntimeControlBusinessHealthItemRespVO();
        item.setCode("business-health-collector-failed");
        item.setName("业务健康采集失败");
        item.setStatus(RuntimeOpsInspectionStatus.BLOCKED);
        item.setReason("业务健康采集失败：" + StrUtil.blankToDefault(ex.getMessage(), ex.getClass().getSimpleName()));
        item.setSampledAt(sampledAt);
        return item;
    }

    private String failureReason(RuntimeException ex) {
        return StrUtil.blankToDefault(ex.getMessage(), ex.getClass().getSimpleName());
    }

    private RuntimeControlBusinessHealthItemRespVO toItem(RuntimeOpsBusinessHealthCheckResult result) {
        RuntimeControlBusinessHealthItemRespVO item = new RuntimeControlBusinessHealthItemRespVO();
        item.setCode(result.getCode());
        item.setName(result.getName());
        item.setStatus(result.getStatus());
        item.setEvidence(result.getEvidence());
        item.setReason(result.getReason());
        item.setSampledAt(result.getSampledAt());
        return item;
    }

    static RuntimeOpsInspectionStatus aggregate(List<RuntimeOpsInspectionStatus> statuses) {
        if (statuses.stream().anyMatch(status -> RuntimeOpsInspectionStatus.NO_GO == status
                || RuntimeOpsInspectionStatus.BLOCKED == status)) {
            return RuntimeOpsInspectionStatus.NO_GO;
        }
        if (statuses.stream().anyMatch(status -> RuntimeOpsInspectionStatus.WARN == status)) {
            return RuntimeOpsInspectionStatus.WARN;
        }
        return RuntimeOpsInspectionStatus.PASS;
    }

    private record RequiredHealthItem(String code, String name) {
    }
}
