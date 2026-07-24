package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOperationAuditPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOperationAuditRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionGateCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionGateService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrOperationAuditService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionErrorCodeConstants.PRO_EDHR_PERMISSION_CONTEXT_MISSING;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionErrorCodeConstants.PRO_EDHR_PERMISSION_SCOPE_REQUIRED;

@Tag(name = "管理后台 - MES eDHR 操作审计")
@RestController
@RequestMapping("/mes/pro/edhr-operation-audit")
@Validated
public class MesProEdhrOperationAuditController {

    private static final String OBJECT_TYPE_BATCH_EXECUTION = "BATCH_EXECUTION";
    private static final String OBJECT_TYPE_BATCH_EXECUTION_TASK = "BATCH_EXECUTION_TASK";
    private static final String OBJECT_TYPE_BATCH_RECORD_EXECUTION = "BATCH_RECORD_EXECUTION";
    private static final String ABILITY_AUDIT_VIEW = "AUDIT_VIEW";
    private static final String PERMISSION_CODE_OPERATION_AUDIT_QUERY = "mes:pro-edhr-operation-audit:query";
    private static final String ACTION_OPERATION_AUDIT_QUERY = "查询 eDHR 操作审计";

    @Resource
    private MesProEdhrOperationAuditService auditService;
    @Resource
    private MesProEdhrPermissionGateService permissionGateService;
    @Resource
    private MesProEdhrBatchExecutionTaskMapper batchTaskMapper;
    @Resource
    private MesProBatchRecordExecutionMapper executionMapper;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-operation-audit:query')")
    public CommonResult<PageResult<MesProEdhrOperationAuditRespVO>> page(
            @Valid MesProEdhrOperationAuditPageReqVO reqVO) {
        requireAuditViewAbility(reqVO);
        return success(auditService.getPage(reqVO));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-operation-audit:query')")
    public CommonResult<MesProEdhrOperationAuditRespVO> get(@PathVariable("id") Long id) {
        MesProEdhrOperationAuditRespVO event = auditService.get(id);
        requireAuditViewAbility(event);
        return success(event);
    }

    private void requireAuditViewAbility(MesProEdhrOperationAuditPageReqVO reqVO) {
        if (reqVO == null) {
            throw exception(PRO_EDHR_PERMISSION_CONTEXT_MISSING);
        }
        if (StrUtil.isBlank(reqVO.getObjectType()) || StrUtil.isBlank(reqVO.getObjectId())) {
            if (reqVO.getBatchExecutionId() != null) {
                requireAuditViewAbility(OBJECT_TYPE_BATCH_EXECUTION, String.valueOf(reqVO.getBatchExecutionId()),
                        reqVO.getBatchExecutionId(), reqVO.getExecutionId(), reqVO.getWorkTaskId(),
                        reqVO.getRouteId(), reqVO.getRouteProcessId(), reqVO.getReportId(), reqVO.getRecordCategory());
                return;
            }
            throw exception(PRO_EDHR_PERMISSION_CONTEXT_MISSING);
        }
        requireAuditViewAbility(reqVO.getObjectType(), reqVO.getObjectId(), reqVO.getBatchExecutionId(),
                reqVO.getExecutionId(), reqVO.getWorkTaskId(), reqVO.getRouteId(), reqVO.getRouteProcessId(),
                reqVO.getReportId(), reqVO.getRecordCategory());
    }

    private void requireAuditViewAbility(MesProEdhrOperationAuditRespVO event) {
        if (event == null || StrUtil.isBlank(event.getObjectType()) || StrUtil.isBlank(event.getObjectId())) {
            throw exception(PRO_EDHR_PERMISSION_CONTEXT_MISSING);
        }
        requireAuditViewAbility(event.getObjectType(), event.getObjectId(), event.getBatchExecutionId(),
                event.getExecutionId(), event.getWorkTaskId(), event.getRouteId(), event.getRouteProcessId(),
                event.getReportId(), event.getRecordCategory());
    }

    private void requireAuditViewAbility(String objectType, String objectId, Long batchExecutionId, Long executionId,
                                         Long workTaskId, Long routeId, Long routeProcessId, String reportId,
                                         String recordCategory) {
        for (MesProEdhrPermissionGateCommand command : resolveAuditViewCommands(objectType, objectId,
                batchExecutionId, executionId, workTaskId, routeId, routeProcessId, reportId, recordCategory)) {
            permissionGateService.requireAbility(command);
        }
    }

    private List<MesProEdhrPermissionGateCommand> resolveAuditViewCommands(String objectType, String objectId,
                                                                           Long batchExecutionId, Long executionId,
                                                                           Long workTaskId, Long routeId,
                                                                           Long routeProcessId, String reportId,
                                                                           String recordCategory) {
        if (OBJECT_TYPE_BATCH_EXECUTION.equals(objectType)) {
            return resolveBatchExecutionAuditCommands(objectId, batchExecutionId, routeId);
        }
        if (OBJECT_TYPE_BATCH_EXECUTION_TASK.equals(objectType)) {
            return List.of(resolveBatchTaskAuditCommand(objectId, batchExecutionId, workTaskId, routeId));
        }
        if (OBJECT_TYPE_BATCH_RECORD_EXECUTION.equals(objectType)) {
            return List.of(resolveRecordExecutionAuditCommand(objectId, executionId, batchExecutionId, workTaskId));
        }
        return List.of(baseAuditViewCommand(objectType, objectId, batchExecutionId, executionId, workTaskId, routeId,
                routeProcessId, reportId, recordCategory));
    }

    private List<MesProEdhrPermissionGateCommand> resolveBatchExecutionAuditCommands(String objectId,
                                                                                     Long batchExecutionId,
                                                                                     Long routeId) {
        Long resolvedBatchExecutionId = batchExecutionId != null ? batchExecutionId : parsePositiveLong(objectId);
        if (resolvedBatchExecutionId == null) {
            throw exception(PRO_EDHR_PERMISSION_CONTEXT_MISSING);
        }
        List<MesProEdhrBatchExecutionTaskDO> tasks = batchTaskMapper.selectListByBatchExecutionId(resolvedBatchExecutionId);
        if (tasks == null || tasks.isEmpty()) {
            throw exception(PRO_EDHR_PERMISSION_SCOPE_REQUIRED,
                    OBJECT_TYPE_BATCH_EXECUTION + ":" + resolvedBatchExecutionId);
        }
        Set<Long> resolvedScopeIds = new LinkedHashSet<>();
        List<MesProEdhrPermissionGateCommand> commands = new ArrayList<>();
        for (MesProEdhrBatchExecutionTaskDO task : tasks) {
            if (task == null || task.getPermissionScopeId() == null
                    || !resolvedScopeIds.add(task.getPermissionScopeId())) {
                continue;
            }
            commands.add(baseAuditViewCommand(OBJECT_TYPE_BATCH_EXECUTION_TASK, String.valueOf(task.getId()),
                    resolvedBatchExecutionId, task.getExecutionId(), null, routeId, task.getRouteProcessId(),
                    task.getBatchRecordReportId(), task.getRecordCategory())
                    .setScopeId(task.getPermissionScopeId()));
        }
        if (commands.isEmpty()) {
            throw exception(PRO_EDHR_PERMISSION_SCOPE_REQUIRED,
                    OBJECT_TYPE_BATCH_EXECUTION + ":" + resolvedBatchExecutionId);
        }
        return commands;
    }

    private MesProEdhrPermissionGateCommand resolveBatchTaskAuditCommand(String objectId, Long batchExecutionId,
                                                                         Long workTaskId, Long routeId) {
        Long batchTaskId = parsePositiveLong(objectId);
        if (batchTaskId == null) {
            throw exception(PRO_EDHR_PERMISSION_CONTEXT_MISSING);
        }
        MesProEdhrBatchExecutionTaskDO task = batchTaskMapper.selectById(batchTaskId);
        if (task == null || task.getPermissionScopeId() == null) {
            throw exception(PRO_EDHR_PERMISSION_SCOPE_REQUIRED,
                    OBJECT_TYPE_BATCH_EXECUTION_TASK + ":" + batchTaskId);
        }
        return baseAuditViewCommand(OBJECT_TYPE_BATCH_EXECUTION_TASK, String.valueOf(task.getId()),
                batchExecutionId != null ? batchExecutionId : task.getBatchExecutionId(), task.getExecutionId(),
                workTaskId, routeId, task.getRouteProcessId(), task.getBatchRecordReportId(), task.getRecordCategory())
                .setScopeId(task.getPermissionScopeId());
    }

    private MesProEdhrPermissionGateCommand resolveRecordExecutionAuditCommand(String objectId, Long executionId,
                                                                              Long batchExecutionId, Long workTaskId) {
        Long resolvedExecutionId = executionId != null ? executionId : parsePositiveLong(objectId);
        if (resolvedExecutionId == null) {
            throw exception(PRO_EDHR_PERMISSION_CONTEXT_MISSING);
        }
        MesProBatchRecordExecutionDO execution = executionMapper.selectById(resolvedExecutionId);
        if (execution == null || execution.getPermissionScopeId() == null) {
            throw exception(PRO_EDHR_PERMISSION_SCOPE_REQUIRED,
                    OBJECT_TYPE_BATCH_RECORD_EXECUTION + ":" + resolvedExecutionId);
        }
        return baseAuditViewCommand(OBJECT_TYPE_BATCH_RECORD_EXECUTION, String.valueOf(execution.getId()),
                batchExecutionId, execution.getId(), workTaskId, execution.getRouteId(),
                execution.getRouteProcessId(), execution.getBatchRecordReportId(), execution.getRecordCategory())
                .setScopeId(execution.getPermissionScopeId());
    }

    private MesProEdhrPermissionGateCommand baseAuditViewCommand(String objectType, String objectId,
                                                                 Long batchExecutionId, Long executionId,
                                                                 Long workTaskId, Long routeId,
                                                                 Long routeProcessId, String reportId,
                                                                 String recordCategory) {
        return new MesProEdhrPermissionGateCommand()
                .setObjectType(objectType)
                .setObjectId(objectId)
                .setAbility(ABILITY_AUDIT_VIEW)
                .setBatchExecutionId(batchExecutionId)
                .setExecutionId(executionId)
                .setWorkTaskId(workTaskId)
                .setRouteId(routeId)
                .setRouteProcessId(routeProcessId)
                .setReportId(reportId)
                .setRecordCategory(recordCategory)
                .setPermissionCode(PERMISSION_CODE_OPERATION_AUDIT_QUERY)
                .setActionName(ACTION_OPERATION_AUDIT_QUERY);
    }

    private Long parsePositiveLong(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            long parsed = Long.parseLong(StrUtil.trim(value));
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
