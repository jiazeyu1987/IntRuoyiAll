package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlActionReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlActionPreviewRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlAlertCreateReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlAlertPageReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlAlertRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlBackupPointRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlBusinessHealthRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlCapacityStatusRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlInspectionRunRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlIncidentActionReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlIncidentCloseReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlIncidentCreateReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlIncidentPageReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlIncidentRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlLogRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOperationRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOwnerMatrixRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOwnerMatrixSaveReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOverviewRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlProbeLatestRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlReleasePackageRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlReleaseStatusRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlRestartReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlRollbackCandidateRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlRestoreCandidateRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlRemoteRootCleanupReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlRemoteRootCleanupRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlRemoteRootDiskStatusRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlWizardRecommendationReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlWizardRecommendationRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlWizardScenarioRespVO;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeBackupDrillService;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeOpsAlertService;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeOpsBusinessHealthService;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeOpsCandidateService;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeOpsGuideService;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeOpsInspectionService;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeOpsProbeService;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeOpsResponsibilityService;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeControlService;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeIncidentService;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeStorageGuardService;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeRemoteRootDiskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED;

@Tag(name = "管理后台 - 运行控制台")
@RestController
@RequestMapping("/infra/runtime-control")
public class RuntimeControlController {

    @Resource
    private RuntimeControlService runtimeControlService;
    @Resource
    private RuntimeOpsAlertService runtimeOpsAlertService;
    @Resource
    private RuntimeOpsResponsibilityService runtimeOpsResponsibilityService;
    @Resource
    private RuntimeOpsGuideService runtimeOpsGuideService;
    @Resource
    private RuntimeOpsCandidateService runtimeOpsCandidateService;
    @Resource
    private RuntimeOpsInspectionService runtimeOpsInspectionService;
    @Resource
    private RuntimeOpsBusinessHealthService runtimeOpsBusinessHealthService;
    @Resource
    private RuntimeOpsProbeService runtimeOpsProbeService;
    @Resource
    private RuntimeStorageGuardService runtimeStorageGuardService;
    @Resource
    private RuntimeBackupDrillService runtimeBackupDrillService;
    @Resource
    private RuntimeIncidentService runtimeIncidentService;
    @Resource
    private RuntimeRemoteRootDiskService runtimeRemoteRootDiskService;

    @GetMapping("/overview")
    @Operation(summary = "获得运行控制台总览")
    @PreAuthorize("@ss.hasPermission('infra:runtime-control:query')")
    public CommonResult<RuntimeControlOverviewRespVO> getOverview() {
        return success(runtimeControlService.getOverview());
    }

    @PostMapping("/restart")
    @Operation(summary = "重启运行组件")
    @PreAuthorize("@ss.hasPermission('infra:runtime-control:restart')")
    public CommonResult<RuntimeControlOperationRespVO> restart(@Valid @RequestBody RuntimeControlRestartReqVO reqVO) {
        return success(runtimeControlService.restart(reqVO, requireLoginUserId()));
    }

    @PostMapping("/actions")
    @Operation(summary = "执行运行控制台运维动作")
    @PreAuthorize("@ss.hasPermission('infra:runtime-control:operate')")
    public CommonResult<RuntimeControlOperationRespVO> executeAction(@Valid @RequestBody RuntimeControlActionReqVO reqVO) {
        return success(runtimeControlService.executeAction(reqVO, requireLoginUserId()));
    }

    @PostMapping("/actions/preview")
    @Operation(summary = "预览运行控制台运维动作命令")
    @PreAuthorize("@ss.hasPermission('infra:runtime-control:operate')")
    public CommonResult<RuntimeControlActionPreviewRespVO> previewAction(
            @Valid @RequestBody RuntimeControlActionReqVO reqVO) {
        return success(runtimeControlService.previewAction(reqVO, requireLoginUserId()));
    }

    @GetMapping("/operations")
    @Operation(summary = "获得运行控制台操作记录")
    @PreAuthorize("@ss.hasPermission('infra:runtime-control:query')")
    public CommonResult<List<RuntimeControlOperationRespVO>> getOperations() {
        return success(runtimeControlService.getOperations());
    }

    @GetMapping("/release-packages")
    @Operation(summary = "获得运行控制台 NAS 发布包候选")
    @PreAuthorize("@ss.hasPermission('infra:runtime-control:query')")
    public CommonResult<List<RuntimeControlReleasePackageRespVO>> getReleasePackages() {
        return success(runtimeControlService.getReleasePackages());
    }

    @GetMapping("/release-status")
    @Operation(summary = "获得运行控制台发布状态只读快照")
    @PreAuthorize("@ss.hasPermission('infra:runtime-control:query')")
    public CommonResult<RuntimeControlReleaseStatusRespVO> getReleaseStatus() {
        return success(runtimeControlService.getReleaseStatus());
    }

    @GetMapping("/operations/{operationId}/log")
    @Operation(summary = "获得运行控制台操作日志")
    @PreAuthorize("@ss.hasPermission('infra:runtime-control:query')")
    public CommonResult<RuntimeControlLogRespVO> getOperationLog(@PathVariable("operationId") String operationId,
                                                                  @RequestParam(value = "maxBytes", required = false) Integer maxBytes) {
        return success(runtimeControlService.getOperationLog(operationId, maxBytes));
    }

    @GetMapping("/alerts/page")
    @Operation(summary = "获得运行控制台告警分页")
    @PreAuthorize("@ss.hasPermission('infra:runtime-control:query')")
    public CommonResult<PageResult<RuntimeControlAlertRespVO>> getAlertsPage(@Valid RuntimeControlAlertPageReqVO pageReqVO) {
        return success(runtimeOpsAlertService.getAlertsPage(pageReqVO));
    }

    @PostMapping("/alerts")
    @Operation(summary = "创建运行控制台告警并发送站内信")
    @PreAuthorize("@ss.hasPermission('infra:runtime-control:operate')")
    public CommonResult<RuntimeControlAlertRespVO> createAlert(@Valid @RequestBody RuntimeControlAlertCreateReqVO reqVO) {
        return success(runtimeOpsAlertService.createAlert(reqVO));
    }

    @PostMapping("/alerts/{id}/resend-site-message")
    @Operation(summary = "重发运行控制台告警站内信")
    @PreAuthorize("@ss.hasPermission('infra:runtime-control:operate')")
    public CommonResult<RuntimeControlAlertRespVO> resendAlertSiteMessage(@PathVariable("id") Long id) {
        return success(runtimeOpsAlertService.resendSiteMessage(id));
    }

    @PostMapping("/alerts/{id}/acknowledge")
    @Operation(summary = "确认运行控制台告警")
    @PreAuthorize("@ss.hasPermission('infra:runtime-control:operate')")
    public CommonResult<RuntimeControlAlertRespVO> acknowledgeAlert(@PathVariable("id") Long id) {
        return success(runtimeOpsAlertService.acknowledge(id, requireLoginUserId()));
    }

    @GetMapping("/wizard/scenarios")
    @Operation(summary = "获得运行控制台决策向导场景")
    @PreAuthorize("@ss.hasPermission('infra:runtime-control:query')")
    public CommonResult<List<RuntimeControlWizardScenarioRespVO>> getWizardScenarios() {
        return success(runtimeOpsGuideService.getScenarios());
    }

    @PostMapping("/wizard/recommendation")
    @Operation(summary = "获得运行控制台决策向导推荐")
    @PreAuthorize("@ss.hasPermission('infra:runtime-control:query')")
    public CommonResult<RuntimeControlWizardRecommendationRespVO> recommendWizardAction(
            @Valid @RequestBody RuntimeControlWizardRecommendationReqVO reqVO) {
        return success(runtimeOpsGuideService.recommend(reqVO));
    }

    @GetMapping("/rollback-candidates")
    @Operation(summary = "获得运行控制台回滚镜像候选")
    @PreAuthorize("@ss.hasPermission('infra:runtime-control:query')")
    public CommonResult<List<RuntimeControlRollbackCandidateRespVO>> getRollbackCandidates() {
        return success(runtimeOpsCandidateService.listRollbackCandidates());
    }

    @GetMapping("/restore-candidates")
    @Operation(summary = "获得运行控制台恢复备份候选")
    @PreAuthorize("@ss.hasPermission('infra:runtime-control:query')")
    public CommonResult<List<RuntimeControlRestoreCandidateRespVO>> getRestoreCandidates() {
        return success(runtimeOpsCandidateService.listRestoreCandidates());
    }

    @GetMapping("/owner-matrix")
    @Operation(summary = "获得运行控制台责任人矩阵")
    @PreAuthorize("@ss.hasPermission('infra:runtime-control:query')")
    public CommonResult<List<RuntimeControlOwnerMatrixRespVO>> getOwnerMatrix(
            @RequestParam(value = "environment", required = false) String environment,
            @RequestParam(value = "action", required = false) String action) {
        return success(runtimeOpsResponsibilityService.getOwnerMatrix(environment, action));
    }

    @PostMapping("/owner-matrix")
    @Operation(summary = "创建运行控制台责任人矩阵")
    @PreAuthorize("@ss.hasPermission('infra:runtime-control:operate')")
    public CommonResult<RuntimeControlOwnerMatrixRespVO> createOwnerMatrix(@Valid @RequestBody RuntimeControlOwnerMatrixSaveReqVO reqVO) {
        return success(runtimeOpsResponsibilityService.createOwner(reqVO));
    }

    @PutMapping("/owner-matrix/{id}")
    @Operation(summary = "更新运行控制台责任人矩阵")
    @PreAuthorize("@ss.hasPermission('infra:runtime-control:operate')")
    public CommonResult<RuntimeControlOwnerMatrixRespVO> updateOwnerMatrix(@PathVariable("id") Long id,
                                                                            @Valid @RequestBody RuntimeControlOwnerMatrixSaveReqVO reqVO) {
        return success(runtimeOpsResponsibilityService.updateOwner(id, reqVO));
    }

    @PostMapping("/inspection-runs")
    @Operation(summary = "执行运行控制台巡检")
    @PreAuthorize("@ss.hasPermission('infra:runtime-control:operate')")
    public CommonResult<RuntimeControlInspectionRunRespVO> runInspection() {
        return success(runtimeOpsInspectionService.runInspection());
    }

    @GetMapping("/inspection-runs/{id}")
    @Operation(summary = "获得运行控制台巡检报告")
    @PreAuthorize("@ss.hasPermission('infra:runtime-control:query')")
    public CommonResult<RuntimeControlInspectionRunRespVO> getInspectionRun(@PathVariable("id") Long id) {
        return success(runtimeOpsInspectionService.getInspectionRun(id));
    }

    @GetMapping("/business-health")
    @Operation(summary = "获得运行控制台业务健康")
    @PreAuthorize("@ss.hasPermission('infra:runtime-control:query')")
    public CommonResult<RuntimeControlBusinessHealthRespVO> getBusinessHealth() {
        return success(runtimeOpsBusinessHealthService.getBusinessHealth());
    }

    @PostMapping("/probes/run")
    @Operation(summary = "执行运行控制台探针")
    @PreAuthorize("@ss.hasPermission('infra:runtime-control:operate')")
    public CommonResult<RuntimeControlProbeLatestRespVO> runProbes() {
        return success(runtimeOpsProbeService.runProbes());
    }

    @GetMapping("/probes/latest")
    @Operation(summary = "获得运行控制台最新探针")
    @PreAuthorize("@ss.hasPermission('infra:runtime-control:query')")
    public CommonResult<RuntimeControlProbeLatestRespVO> getLatestProbes() {
        return success(runtimeOpsProbeService.getLatestProbes());
    }

    @GetMapping("/capacity/status")
    @Operation(summary = "获得运行控制台容量状态")
    @PreAuthorize("@ss.hasPermission('infra:runtime-control:query')")
    public CommonResult<RuntimeControlCapacityStatusRespVO> getCapacityStatus() {
        return success(runtimeStorageGuardService.getCapacityStatus());
    }

    @PostMapping("/capacity/refresh")
    @Operation(summary = "刷新运行控制台容量状态并执行告警")
    @PreAuthorize("@ss.hasPermission('infra:runtime-control:operate')")
    public CommonResult<RuntimeControlCapacityStatusRespVO> refreshCapacityStatus() {
        return success(runtimeStorageGuardService.refreshCapacityStatus());
    }

    @GetMapping("/remote-root-disk/status")
    @Operation(summary = "获得远程根分区容量状态")
    @PreAuthorize("@ss.hasPermission('infra:runtime-control:query')")
    public CommonResult<RuntimeControlRemoteRootDiskStatusRespVO> getRemoteRootDiskStatus(
            @RequestParam("targetEnvironment") String targetEnvironment) {
        return success(runtimeRemoteRootDiskService.getStatus(targetEnvironment));
    }

    @PostMapping("/remote-root-disk/cleanup")
    @Operation(summary = "清理远程根分区临时目录")
    @PreAuthorize("@ss.hasPermission('infra:runtime-control:operate')")
    public CommonResult<RuntimeControlRemoteRootCleanupRespVO> cleanupRemoteRootTemporaryFiles(
            @Valid @RequestBody RuntimeControlRemoteRootCleanupReqVO reqVO) {
        return success(runtimeRemoteRootDiskService.cleanup(reqVO, requireLoginUserId()));
    }

    @GetMapping("/backup-points")
    @Operation(summary = "获得运行控制台备份点")
    @PreAuthorize("@ss.hasPermission('infra:runtime-control:query')")
    public CommonResult<List<RuntimeControlBackupPointRespVO>> getBackupPoints() {
        return success(runtimeBackupDrillService.listBackupPoints());
    }

    @GetMapping("/backup-points/{backupId}")
    @Operation(summary = "获得运行控制台备份点详情")
    @PreAuthorize("@ss.hasPermission('infra:runtime-control:query')")
    public CommonResult<RuntimeControlBackupPointRespVO> getBackupPoint(@PathVariable("backupId") String backupId) {
        return success(runtimeBackupDrillService.getBackupPoint(backupId));
    }

    @GetMapping("/incidents/page")
    @Operation(summary = "获得运行控制台事故分页")
    @PreAuthorize("@ss.hasPermission('infra:runtime-control:query')")
    public CommonResult<PageResult<RuntimeControlIncidentRespVO>> getIncidentsPage(@Valid RuntimeControlIncidentPageReqVO pageReqVO) {
        return success(runtimeIncidentService.getIncidentsPage(pageReqVO));
    }

    @PostMapping("/incidents")
    @Operation(summary = "创建运行控制台事故")
    @PreAuthorize("@ss.hasPermission('infra:runtime-control:operate')")
    public CommonResult<RuntimeControlIncidentRespVO> createIncident(@Valid @RequestBody RuntimeControlIncidentCreateReqVO reqVO) {
        return success(runtimeIncidentService.createIncident(reqVO, requireLoginUserId()));
    }

    @PostMapping("/incidents/{id}/actions")
    @Operation(summary = "记录运行控制台事故动作")
    @PreAuthorize("@ss.hasPermission('infra:runtime-control:operate')")
    public CommonResult<RuntimeControlIncidentRespVO> recordIncidentAction(@PathVariable("id") Long id,
                                                                           @Valid @RequestBody RuntimeControlIncidentActionReqVO reqVO) {
        return success(runtimeIncidentService.recordAction(id, reqVO, requireLoginUserId()));
    }

    @PostMapping("/incidents/{id}/close")
    @Operation(summary = "关闭运行控制台事故")
    @PreAuthorize("@ss.hasPermission('infra:runtime-control:operate')")
    public CommonResult<RuntimeControlIncidentRespVO> closeIncident(@PathVariable("id") Long id,
                                                                    @Valid @RequestBody RuntimeControlIncidentCloseReqVO reqVO) {
        return success(runtimeIncidentService.closeIncident(id, reqVO, requireLoginUserId()));
    }

    private String requireLoginUserId() {
        Long loginUserId = getLoginUserId();
        if (loginUserId == null) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "loginUserId");
        }
        return loginUserId.toString();
    }
}
