package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamDeviceSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamDeviceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamDeviceStatusUpdateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamDefectReasonSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamDeviceParameterRuleSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamEmployeeDisplayNameUpdateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamEmployeeProfileSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamEmployeeStatusUpdateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamFormalEmployeeLinkReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamFormalUserCandidateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamMaintenanceAuditRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesProductionExecutionTraceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesPqcLeaderPersonnelLinkReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesPqcLeaderPersonnelRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesPqcLeaderPersonnelStatusUpdateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderActiveOrderAddReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderActiveOrderAddRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderActiveOrderCandidateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderPickListOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderActiveOrderMoveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderActiveOrderDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderActiveOrderRebuildPreviewRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderActiveOrderRebuildReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderActiveOrderRebuildResultRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderActiveOrderSimulationReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderActiveOrderSimulationRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderActiveOrderRemoveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderActiveOrderReleaseApplyReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderActiveOrderReleaseApplyRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderActiveOrderRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderActiveOrderTransferTraceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderLossReasonRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderLossReasonRowRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderLossReasonSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderLossReasonUpdateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderProcessConfigListReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderProcessConfigRowRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderAllocationTraceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderBatchRecordTraceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderOrderProcessTraceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderReportAllocationConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderReportAllocationAuditRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderReportAllocationLineReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderReportAllocationPreviewReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderReportAllocationPreviewRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderReportAllocationSnapshotRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderSubmissionPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderSubmissionReviewReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamProcessDefectReasonSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamProcessDeviceBindingSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamProductionEmployeeRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamTemporaryEmployeeCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamTemporarySignaturePasswordResetReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesWorkOrderAbnormalReportReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelineDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelineEventRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderTransferTraceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamEmployeeProfileDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditDO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesDefectReasonCatalogService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesDefectReasonSaveReqBO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesActiveOrderTransferTraceService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesPqcLeaderPersonnelBO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesPqcLeaderPersonnelLinkReqBO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesPqcLeaderPersonnelService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesPqcLeaderPersonnelStatusUpdateReqBO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamEmployeeDisplayNameUpdateReqBO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamEmployeeStatusUpdateReqBO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamFormalEmployeeLinkReqBO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamFormalUserCandidateBO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderAddReqBO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderAddResult;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderCandidateBO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderMoveReqBO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderRebuildPreview;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderRebuildReqBO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderRebuildResult;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderSimulationResult;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderSimulationService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderRemoveReqBO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderDetail;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderDetailService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderReleaseApplicationResult;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderReleaseApplicationService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderReleaseApplyCommand;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderRow;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderLossReasonItem;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderLossReasonRow;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderLossReasonSaveReqBO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderLossReasonService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderLossReasonUpdateReqBO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderProcessConfigDevice;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderProcessConfigParameter;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderProcessConfigRow;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderProcessConfigService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderReportAllocationLineReqBO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderReportAllocationPreview;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderReportAllocationPreviewLine;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderReportAllocationPreviewReqBO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderReportConfirmationReqBO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderReportConfirmationService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesReportAllocationCommandService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesReportAllocationSaveCommand;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesReportAllocationSaveLine;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesReportAllocationSnapshot;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesReportAllocationSnapshotLine;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationAdjustmentAuditDO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderRuntimeConfigService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderSubmissionReviewReqBO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderSubmissionReviewService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderTraceService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderWorkbenchService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamTempSignaturePasswordResetReqBO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamTemporaryEmployeeCreateReqBO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesWorkOrderAbnormalReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - MES 工序池班组长工作台")
@RestController
@RequestMapping("/mes/pro/process-pool/team-leader")
@Validated
public class MesProcessPoolTeamLeaderController {

    private final MesTeamLeaderWorkbenchService workbenchService;
    private final MesTeamLeaderSubmissionReviewService submissionReviewService;
    private final MesWorkOrderAbnormalReportService abnormalReportService;
    private final MesDefectReasonCatalogService defectReasonCatalogService;
    private final MesTeamLeaderProcessConfigService processConfigService;
    private final MesTeamLeaderActiveOrderService activeOrderService;
    private final MesTeamLeaderActiveOrderDetailService activeOrderDetailService;
    private final MesReportAllocationCommandService reportAllocationService;
    private final MesTeamLeaderRuntimeConfigService runtimeConfigService;
    private final MesPqcLeaderPersonnelService pqcPersonnelService;
    private final MesTeamLeaderLossReasonService lossReasonService;
    private final MesTeamLeaderTraceService traceService;
    private final MesActiveOrderTransferTraceService activeOrderTransferTraceService;
    private final MesTeamLeaderActiveOrderReleaseApplicationService releaseApplicationService;
    private final MesTeamLeaderActiveOrderSimulationService activeOrderSimulationService;

    public MesProcessPoolTeamLeaderController(MesTeamLeaderWorkbenchService workbenchService,
                                              MesTeamLeaderSubmissionReviewService submissionReviewService,
                                              MesWorkOrderAbnormalReportService abnormalReportService,
                                              MesDefectReasonCatalogService defectReasonCatalogService,
                                              MesTeamLeaderProcessConfigService processConfigService,
                                              MesTeamLeaderActiveOrderService activeOrderService,
                                              MesTeamLeaderActiveOrderDetailService activeOrderDetailService,
                                              MesReportAllocationCommandService reportAllocationService,
                                              MesTeamLeaderRuntimeConfigService runtimeConfigService,
                                              MesPqcLeaderPersonnelService pqcPersonnelService,
                                              MesTeamLeaderLossReasonService lossReasonService,
                                              MesTeamLeaderTraceService traceService,
                                              MesActiveOrderTransferTraceService activeOrderTransferTraceService,
                                              MesTeamLeaderActiveOrderReleaseApplicationService releaseApplicationService,
                                              MesTeamLeaderActiveOrderSimulationService activeOrderSimulationService) {
        this.workbenchService = workbenchService;
        this.submissionReviewService = submissionReviewService;
        this.abnormalReportService = abnormalReportService;
        this.defectReasonCatalogService = defectReasonCatalogService;
        this.processConfigService = processConfigService;
        this.activeOrderService = activeOrderService;
        this.activeOrderDetailService = activeOrderDetailService;
        this.reportAllocationService = reportAllocationService;
        this.runtimeConfigService = runtimeConfigService;
        this.pqcPersonnelService = pqcPersonnelService;
        this.lossReasonService = lossReasonService;
        this.traceService = traceService;
        this.activeOrderTransferTraceService = activeOrderTransferTraceService;
        this.releaseApplicationService = releaseApplicationService;
        this.activeOrderSimulationService = activeOrderSimulationService;
    }

    @GetMapping("/submission/page")
    @Operation(summary = "分页查询班组长负责员工的工序池提交")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:query')")
    public CommonResult<PageResult<ProcessPoolTimelineEventRespVO>> getSubmissionPage(
            @Valid MesTeamLeaderSubmissionPageReqVO pageReqVO) {
        return success(workbenchService.getSubmissionPage(SecurityFrameworkUtils.getLoginUserId(),
                pageReqVO.getLeaderType(), pageReqVO));
    }

    @GetMapping("/submission/detail")
    @Operation(summary = "查询班组长负责员工的提交详情")
    @Parameter(name = "id", description = "工序池提交事件编号", required = true)
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:query')")
    public CommonResult<ProcessPoolTimelineDetailRespVO> getSubmissionDetail(@RequestParam("id") Long id,
                                                                             @RequestParam("leaderType") String leaderType) {
        return success(workbenchService.getSubmissionDetail(SecurityFrameworkUtils.getLoginUserId(), leaderType, id));
    }

    @PostMapping("/submission/review")
    @Operation(summary = "复核班组负责员工提交")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:review')")
    public CommonResult<Long> reviewSubmission(@Valid @RequestBody MesTeamLeaderSubmissionReviewReqVO reqVO) {
        return success(submissionReviewService.reviewSubmission(MesTeamLeaderSubmissionReviewReqBO.builder()
                .eventId(reqVO.getEventId())
                .leaderUserId(SecurityFrameworkUtils.getLoginUserId())
                .leaderType(reqVO.getLeaderType())
                .reviewStatus(reqVO.getReviewStatus())
                .reviewRemark(reqVO.getReviewRemark())
                .signaturePassword(reqVO.getSignaturePassword())
                .build()));
    }

    @PostMapping("/work-order/abnormal/report")
    @Operation(summary = "标记并上报生产工单异常")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:abnormal')")
    public CommonResult<Long> markAndReportWorkOrderAbnormal(@Valid @RequestBody MesWorkOrderAbnormalReportReqVO reqVO) {
        return success(abnormalReportService.markAndReport(
                cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesWorkOrderAbnormalReportReqBO.builder()
                .workOrderId(reqVO.getWorkOrderId())
                .markerUserId(SecurityFrameworkUtils.getLoginUserId())
                .abnormalDescription(reqVO.getAbnormalDescription())
                .build()));
    }

    @PostMapping("/defect-reason/create")
    @Operation(summary = "新增班组不良原因")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:maintain')")
    public CommonResult<Long> createDefectReason(@Valid @RequestBody MesTeamDefectReasonSaveReqVO reqVO) {
        return success(defectReasonCatalogService.createReason(MesDefectReasonSaveReqBO.builder()
                .leaderUserId(SecurityFrameworkUtils.getLoginUserId())
                .routeProcessId(reqVO.getRouteProcessId())
                .processId(reqVO.getProcessId())
                .reasonType(reqVO.getReasonType())
                .reasonCode(reqVO.getReasonCode())
                .reasonName(reqVO.getReasonName())
                .build()));
    }

    @GetMapping("/loss-reasons/page")
    @Operation(summary = "查询生产组长可维护工序损耗原因标准列表")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:query')")
    public CommonResult<List<MesTeamLeaderLossReasonRowRespVO>> getLossReasonPage() {
        return success(lossReasonService.listLossReasonRows(SecurityFrameworkUtils.getLoginUserId()).stream()
                .map(MesProcessPoolTeamLeaderController::toLossReasonRowRespVO)
                .toList());
    }

    @PostMapping("/loss-reasons")
    @Operation(summary = "新增生产组长工序损耗原因")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:maintain')")
    public CommonResult<Long> createLossReason(@Valid @RequestBody MesTeamLeaderLossReasonSaveReqVO reqVO) {
        return success(lossReasonService.createLossReason(MesTeamLeaderLossReasonSaveReqBO.builder()
                .leaderUserId(SecurityFrameworkUtils.getLoginUserId())
                .routeProcessId(reqVO.getRouteProcessId())
                .reasonName(reqVO.getReasonName())
                .build()));
    }

    @PutMapping("/loss-reasons/{id}")
    @Operation(summary = "修改生产组长工序损耗原因")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:maintain')")
    public CommonResult<Boolean> updateLossReason(@PathVariable("id") Long id,
                                                   @Valid @RequestBody MesTeamLeaderLossReasonUpdateReqVO reqVO) {
        lossReasonService.updateLossReason(MesTeamLeaderLossReasonUpdateReqBO.builder()
                .leaderUserId(SecurityFrameworkUtils.getLoginUserId())
                .id(id)
                .reasonName(reqVO.getReasonName())
                .enabled(reqVO.getEnabled())
                .remark(reqVO.getRemark())
                .build());
        return success(Boolean.TRUE);
    }

    @DeleteMapping("/loss-reasons/{id}")
    @Operation(summary = "删除生产组长工序损耗原因")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:maintain')")
    public CommonResult<Boolean> deleteLossReason(@PathVariable("id") Long id) {
        lossReasonService.deleteLossReason(SecurityFrameworkUtils.getLoginUserId(), id);
        return success(Boolean.TRUE);
    }

    @GetMapping("/process-config/list")
    @Operation(summary = "查询生产组长统一工序配置表")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:query')")
    public CommonResult<List<MesTeamLeaderProcessConfigRowRespVO>> getProcessConfigList(
            @Valid MesTeamLeaderProcessConfigListReqVO reqVO) {
        return success(processConfigService.listProcessConfigs(SecurityFrameworkUtils.getLoginUserId(), reqVO).stream()
                .map(MesProcessPoolTeamLeaderController::toProcessConfigRowRespVO)
                .toList());
    }

    @PostMapping("/active-order/add")
    @Operation(summary = "加入生产组长活跃订单")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:maintain')")
    public CommonResult<MesTeamLeaderActiveOrderAddRespVO> addActiveOrder(
            @Valid @RequestBody MesTeamLeaderActiveOrderAddReqVO reqVO) {
        MesTeamLeaderActiveOrderAddResult result = activeOrderService.addActiveOrder(
                MesTeamLeaderActiveOrderAddReqBO.builder()
                .leaderUserId(SecurityFrameworkUtils.getLoginUserId())
                .workOrderId(reqVO.getWorkOrderId())
                .pickListId(reqVO.getPickListId())
                .pickListCandidateSnapshotHash(reqVO.getPickListCandidateSnapshotHash())
                .idempotencyKey(reqVO.getIdempotencyKey())
                .build());
        return success(new MesTeamLeaderActiveOrderAddRespVO()
                .setActiveOrderId(String.valueOf(result.getActiveOrderId()))
                .setAction(result.getAction())
                .setWorkOrderId(String.valueOf(result.getWorkOrderId()))
                .setPickListBindingId(String.valueOf(result.getPickListBindingId()))
                .setPickListId(String.valueOf(result.getPickListId()))
                .setSourceSnapshotHash(result.getSourceSnapshotHash())
                .setBindingVersion(result.getBindingVersion()));
    }

    @GetMapping("/active-order/pick-list-options")
    @Operation(summary = "查询活跃订单正式领料单候选")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:query')")
    public CommonResult<List<MesTeamLeaderPickListOptionRespVO>> listPickListOptions(
            @RequestParam("workOrderId") Long workOrderId) {
        return success(activeOrderService.listPickListOptions(workOrderId).stream().map(option ->
                new MesTeamLeaderPickListOptionRespVO()
                        .setPickListId(String.valueOf(option.getPickListId()))
                        .setSourceFid(option.getSourceFid())
                        .setSourceBillNo(option.getSourceBillNo())
                        .setDocumentStatus(option.getDocumentStatus())
                        .setSourceModifyTime(option.getSourceModifyTime())
                        .setProductionOrderNo(option.getProductionOrderNo())
                        .setDetailCount(option.getDetailCount())
                        .setDetailIds(option.getDetailIds().stream().map(String::valueOf).toList())
                        .setCandidateSnapshotHash(option.getCandidateSnapshotHash())
                        .setSelectable(option.isSelectable())
                        .setBlockerCode(option.getBlockerCode())).toList());
    }

    @PutMapping("/active-order/remove")
    @Operation(summary = "移除生产组长活跃订单")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:maintain')")
    public CommonResult<Boolean> removeActiveOrder(@Valid @RequestBody MesTeamLeaderActiveOrderRemoveReqVO reqVO) {
        activeOrderService.removeActiveOrder(MesTeamLeaderActiveOrderRemoveReqBO.builder()
                .leaderUserId(SecurityFrameworkUtils.getLoginUserId())
                .activeOrderId(reqVO.getActiveOrderId())
                .build());
        return success(Boolean.TRUE);
    }

    @PutMapping("/active-order/move")
    @Operation(summary = "上移或下移生产组长活跃订单")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:maintain')")
    public CommonResult<Boolean> moveActiveOrder(@Valid @RequestBody MesTeamLeaderActiveOrderMoveReqVO reqVO) {
        activeOrderService.moveActiveOrder(MesTeamLeaderActiveOrderMoveReqBO.builder()
                .leaderUserId(SecurityFrameworkUtils.getLoginUserId())
                .activeOrderId(reqVO.getActiveOrderId())
                .direction(reqVO.getDirection())
                .build());
        return success(Boolean.TRUE);
    }

    @GetMapping("/active-order/rebuild/preview")
    @Operation(summary = "预检生产组长活跃订单重建影响")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:maintain')")
    public CommonResult<MesTeamLeaderActiveOrderRebuildPreviewRespVO> previewRebuildActiveOrder(
            @RequestParam("activeOrderId") Long activeOrderId) {
        return success(toActiveOrderRebuildPreviewRespVO(activeOrderService.previewRebuildActiveOrder(
                SecurityFrameworkUtils.getLoginUserId(), activeOrderId)));
    }

    @PostMapping("/active-order/rebuild")
    @Operation(summary = "重建生产组长活跃订单生产和 PQC 快照")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:maintain')")
    public CommonResult<MesTeamLeaderActiveOrderRebuildResultRespVO> rebuildActiveOrder(
            @Valid @RequestBody MesTeamLeaderActiveOrderRebuildReqVO reqVO) {
        MesTeamLeaderActiveOrderRebuildResult result = activeOrderService.rebuildActiveOrder(
                MesTeamLeaderActiveOrderRebuildReqBO.builder()
                        .leaderUserId(SecurityFrameworkUtils.getLoginUserId())
                        .activeOrderId(reqVO.getActiveOrderId())
                        .confirmDeleteHistoricalRuntimeData(reqVO.getConfirmDeleteHistoricalRuntimeData())
                        .build());
        return success(toActiveOrderRebuildResultRespVO(result));
    }

    @PostMapping("/active-order/simulate-completion")
    @Operation(summary = "模拟完成生产组长活跃订单生产和 PQC 进度")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:maintain')")
    public CommonResult<MesTeamLeaderActiveOrderSimulationRespVO> simulateActiveOrderCompletion(
            @Valid @RequestBody MesTeamLeaderActiveOrderSimulationReqVO reqVO) {
        MesTeamLeaderActiveOrderSimulationResult result = activeOrderSimulationService.simulateActiveOrderCompletion(
                SecurityFrameworkUtils.getLoginUserId(), reqVO.getActiveOrderId());
        return success(toActiveOrderSimulationRespVO(result));
    }

    @GetMapping("/active-order/list")
    @Operation(summary = "查询生产组长活跃订单")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:query')")
    public CommonResult<List<MesTeamLeaderActiveOrderRespVO>> getActiveOrderList() {
        return success(activeOrderService.listActiveOrders(SecurityFrameworkUtils.getLoginUserId()).stream()
                .map(MesProcessPoolTeamLeaderController::toActiveOrderRespVO)
                .toList());
    }

    @GetMapping("/active-order/detail")
    @Operation(summary = "查询生产组长活跃订单逐工序提交详情")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:query')")
    public CommonResult<MesTeamLeaderActiveOrderDetailRespVO> getActiveOrderDetail(
            @RequestParam("activeOrderId") Long activeOrderId) {
        return success(toActiveOrderDetailRespVO(activeOrderDetailService.getDetail(
                SecurityFrameworkUtils.getLoginUserId(), activeOrderId)));
    }

    @PostMapping("/active-order/release/apply")
    @Operation(summary = "提交PQC生产放行申请")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:release-apply')")
    public CommonResult<MesTeamLeaderActiveOrderReleaseApplyRespVO> applyActiveOrderRelease(
            @Valid @RequestBody MesTeamLeaderActiveOrderReleaseApplyReqVO reqVO) {
        MesTeamLeaderActiveOrderReleaseApplicationResult result = releaseApplicationService.apply(
                SecurityFrameworkUtils.getLoginUserId(),
                new MesTeamLeaderActiveOrderReleaseApplyCommand()
                        .setActiveOrderId(reqVO.getActiveOrderId())
                        .setIdempotencyKey(reqVO.getIdempotencyKey())
                        .setApplyRemark(reqVO.getApplyRemark()));
        return success(toActiveOrderReleaseApplyRespVO(result));
    }

    @GetMapping("/active-order/release/get")
    @Operation(summary = "查询PQC生产放行申请回执")
    @PreAuthorize("@ss.hasAnyPermissions('mes:pro-process-pool-team-leader:query', 'mes:pro-production-release:query')")
    public CommonResult<MesTeamLeaderActiveOrderReleaseApplyRespVO> getActiveOrderRelease(
            @RequestParam("activeOrderId") Long activeOrderId) {
        return success(toActiveOrderReleaseApplyRespVO(releaseApplicationService.get(
                SecurityFrameworkUtils.getLoginUserId(), activeOrderId)));
    }

    @GetMapping("/active-order/candidates")
    @Operation(summary = "搜索生产组长可加入活跃订单候选")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:maintain')")
    public CommonResult<List<MesTeamLeaderActiveOrderCandidateRespVO>> searchActiveOrderCandidates(
            @RequestParam("keyword") String keyword) {
        return success(activeOrderService.searchActiveOrderCandidates(keyword).stream()
                .map(MesProcessPoolTeamLeaderController::toActiveOrderCandidateRespVO)
                .toList());
    }

    @GetMapping("/active-order/transfer-trace")
    @Operation(summary = "只读查询活跃订单调拨/发货/补退料/批次库存追溯")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:query')")
    public CommonResult<List<MesTeamLeaderActiveOrderTransferTraceRespVO>> getActiveOrderTransferTrace(
            @RequestParam("activeOrderId") Long activeOrderId) {
        return success(activeOrderTransferTraceService.listByActiveOrder(activeOrderId).stream()
                .map(MesProcessPoolTeamLeaderController::toActiveOrderTransferTraceRespVO)
                .toList());
    }

    @PostMapping("/submission/allocation/preview-fifo")
    @Operation(summary = "预览班组长报工 FIFO 分配")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:review')")
    public CommonResult<MesTeamLeaderReportAllocationPreviewRespVO> previewReportFifoAllocation(
            @Valid @RequestBody MesTeamLeaderReportAllocationPreviewReqVO reqVO) {
        MesReportAllocationSnapshot preview = reportAllocationService.previewFifo(reqVO.getEventId(),
                SecurityFrameworkUtils.getLoginUserId(), reqVO.getLeaderType());
        return success(toReportAllocationPreviewRespVO(preview));
    }

    @GetMapping("/submission/allocation/current")
    @Operation(summary = "查询报工共享分配池当前快照")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:query')")
    public CommonResult<MesTeamLeaderReportAllocationSnapshotRespVO> getCurrentReportAllocation(
            @RequestParam("eventId") Long eventId, @RequestParam("leaderType") String leaderType) {
        return success(toReportAllocationSnapshotRespVO(reportAllocationService.getCurrent(eventId,
                SecurityFrameworkUtils.getLoginUserId(), leaderType)));
    }

    @GetMapping("/submission/allocation/audit")
    @Operation(summary = "查询报工分配调整审计")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:query')")
    public CommonResult<List<MesTeamLeaderReportAllocationAuditRespVO>> getReportAllocationAudit(
            @RequestParam("eventId") Long eventId, @RequestParam("leaderType") String leaderType) {
        return success(reportAllocationService.listAudit(eventId, SecurityFrameworkUtils.getLoginUserId(), leaderType)
                .stream().map(MesProcessPoolTeamLeaderController::toReportAllocationAuditRespVO).toList());
    }

    @PostMapping("/submission/allocation/confirm")
    @Operation(summary = "确认班组长报工并保存活跃订单分配")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:review')")
    public CommonResult<MesTeamLeaderReportAllocationSnapshotRespVO> confirmReportAllocation(
            @Valid @RequestBody MesTeamLeaderReportAllocationConfirmReqVO reqVO) {
        return success(toReportAllocationSnapshotRespVO(reportAllocationService.save(
                MesReportAllocationSaveCommand.builder().eventId(reqVO.getEventId())
                        .leaderUserId(SecurityFrameworkUtils.getLoginUserId()).leaderType(reqVO.getLeaderType())
                        .expectedVersion(reqVO.getExpectedVersion()).idempotencyKey(reqVO.getIdempotencyKey())
                        .allocationMode(reqVO.getAllocationMode()).reason(reqVO.getReviewRemark())
                        .signaturePassword(reqVO.getSignaturePassword())
                        .allocations(reqVO.getAllocations().stream().map(line -> MesReportAllocationSaveLine.builder()
                                .activeOrderId(line.getActiveOrderId())
                                .allocatedQuantity(line.getAllocatedQuantity()).build()).toList())
                        .build())));
    }

    @PostMapping("/employee-profile/create")
    @Operation(summary = "新增班组员工档案")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:maintain')")
    public CommonResult<Long> createEmployeeProfile(@Valid @RequestBody MesTeamEmployeeProfileSaveReqVO reqVO) {
        return success(runtimeConfigService.createEmployee(
                cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamEmployeeProfileSaveReqBO.builder()
                        .leaderUserId(SecurityFrameworkUtils.getLoginUserId())
                        .systemUserId(reqVO.getSystemUserId())
                        .employeeCode(reqVO.getEmployeeCode())
                        .employeeName(reqVO.getEmployeeName())
                        .employeeType(reqVO.getEmployeeType())
                        .build()));
    }

    @GetMapping("/pqc-personnel/list")
    @Operation(summary = "查询当前 PQC 组长关联的检验员")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:query')")
    public CommonResult<List<MesPqcLeaderPersonnelRespVO>> getPqcPersonnelList(
            @RequestParam(value = "enabled", required = false) Boolean enabled) {
        return success(pqcPersonnelService.listPersonnel(SecurityFrameworkUtils.getLoginUserId(), enabled)
                .stream()
                .map(MesProcessPoolTeamLeaderController::toPqcPersonnelRespVO)
                .toList());
    }

    @GetMapping("/pqc-personnel/formal-candidates")
    @Operation(summary = "按姓名搜索拥有 PQC 权限角色的正式检验员候选")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:maintain')")
    public CommonResult<List<MesTeamFormalUserCandidateRespVO>> searchPqcFormalEmployeeCandidates(
            @RequestParam(value = "keyword", required = false) String keyword) {
        return success(pqcPersonnelService.searchFormalInspectorCandidates(SecurityFrameworkUtils.getLoginUserId(), keyword)
                .stream()
                .map(MesProcessPoolTeamLeaderController::toFormalUserCandidateRespVO)
                .toList());
    }

    @PostMapping("/pqc-personnel/formal/link")
    @Operation(summary = "关联全公司正式用户为当前 PQC 组长检验员")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:maintain')")
    public CommonResult<Long> linkPqcFormalEmployee(
            @Valid @RequestBody MesPqcLeaderPersonnelLinkReqVO reqVO) {
        return success(pqcPersonnelService.linkFormalInspector(MesPqcLeaderPersonnelLinkReqBO.builder()
                .leaderUserId(SecurityFrameworkUtils.getLoginUserId())
                .systemUserId(reqVO.getSystemUserId())
                .build()));
    }

    @PutMapping("/pqc-personnel/status/update")
    @Operation(summary = "启用或禁用当前 PQC 组长关联的检验员")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:maintain')")
    public CommonResult<Boolean> updatePqcPersonnelStatus(
            @Valid @RequestBody MesPqcLeaderPersonnelStatusUpdateReqVO reqVO) {
        pqcPersonnelService.updatePersonnelStatus(MesPqcLeaderPersonnelStatusUpdateReqBO.builder()
                .leaderUserId(SecurityFrameworkUtils.getLoginUserId())
                .scopeId(reqVO.getScopeId())
                .enabled(reqVO.getEnabled())
                .build());
        return success(Boolean.TRUE);
    }

    @GetMapping("/employee-profile/list")
    @Operation(summary = "查询当前生产组长关联的生产人员档案")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:query')")
    public CommonResult<List<MesTeamProductionEmployeeRespVO>> getProductionPersonnelList(
            @RequestParam(value = "enabled", required = false) Boolean enabled) {
        return success(runtimeConfigService.listEmployeeProfiles(SecurityFrameworkUtils.getLoginUserId(), enabled)
                .stream()
                .map(MesProcessPoolTeamLeaderController::toProductionEmployeeRespVO)
                .toList());
    }

    @GetMapping("/employee-profile/formal-candidates")
    @Operation(summary = "按姓名搜索全量系统正式工")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:maintain')")
    public CommonResult<List<MesTeamFormalUserCandidateRespVO>> searchFormalEmployeeCandidates(
            @RequestParam("keyword") String keyword) {
        return success(runtimeConfigService.searchFormalUserCandidates(SecurityFrameworkUtils.getLoginUserId(), keyword)
                .stream()
                .map(MesProcessPoolTeamLeaderController::toFormalUserCandidateRespVO)
                .toList());
    }

    @PostMapping("/employee-profile/temporary/create")
    @Operation(summary = "手动新增当前生产组长临时工档案")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:maintain')")
    public CommonResult<Long> createTemporaryEmployee(
            @Valid @RequestBody MesTeamTemporaryEmployeeCreateReqVO reqVO) {
        return success(runtimeConfigService.createTemporaryEmployee(MesTeamTemporaryEmployeeCreateReqBO.builder()
                .leaderUserId(SecurityFrameworkUtils.getLoginUserId())
                .displayName(reqVO.getDisplayName())
                .signaturePassword(reqVO.getSignaturePassword())
                .build()));
    }

    @PostMapping("/employee-profile/formal/link")
    @Operation(summary = "关联全量系统用户中的正式工")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:maintain')")
    public CommonResult<Long> linkFormalEmployee(@Valid @RequestBody MesTeamFormalEmployeeLinkReqVO reqVO) {
        return success(runtimeConfigService.linkFormalEmployee(MesTeamFormalEmployeeLinkReqBO.builder()
                .leaderUserId(SecurityFrameworkUtils.getLoginUserId())
                .systemUserId(reqVO.getSystemUserId())
                .displayName(reqVO.getDisplayName())
                .build()));
    }

    @PutMapping("/employee-profile/display-name/update")
    @Operation(summary = "修改当前生产组长关联员工显示名")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:maintain')")
    public CommonResult<Boolean> updateEmployeeDisplayName(
            @Valid @RequestBody MesTeamEmployeeDisplayNameUpdateReqVO reqVO) {
        runtimeConfigService.renameEmployee(MesTeamEmployeeDisplayNameUpdateReqBO.builder()
                .leaderUserId(SecurityFrameworkUtils.getLoginUserId())
                .employeeProfileId(reqVO.getEmployeeProfileId())
                .displayName(reqVO.getDisplayName())
                .build());
        return success(Boolean.TRUE);
    }

    @PutMapping("/employee-profile/status/update")
    @Operation(summary = "启用或禁用当前生产组长关联员工")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:maintain')")
    public CommonResult<Boolean> updateEmployeeStatus(@Valid @RequestBody MesTeamEmployeeStatusUpdateReqVO reqVO) {
        runtimeConfigService.updateEmployeeEnabled(MesTeamEmployeeStatusUpdateReqBO.builder()
                .leaderUserId(SecurityFrameworkUtils.getLoginUserId())
                .employeeProfileId(reqVO.getEmployeeProfileId())
                .enabled(reqVO.getEnabled())
                .build());
        return success(Boolean.TRUE);
    }

    @PutMapping("/employee-profile/temp-signature-password/reset")
    @Operation(summary = "重置当前生产组长临时工电子签名密码")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:maintain')")
    public CommonResult<Boolean> resetTemporarySignaturePassword(
            @Valid @RequestBody MesTeamTemporarySignaturePasswordResetReqVO reqVO) {
        runtimeConfigService.resetTemporaryEmployeeSignaturePassword(MesTeamTempSignaturePasswordResetReqBO.builder()
                .leaderUserId(SecurityFrameworkUtils.getLoginUserId())
                .employeeProfileId(reqVO.getEmployeeProfileId())
                .signaturePassword(reqVO.getSignaturePassword())
                .build());
        return success(Boolean.TRUE);
    }

    @GetMapping("/employee-profile/audit/list")
    @Operation(summary = "查询当前生产组长生产人员档案操作追溯")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:query')")
    public CommonResult<List<MesTeamMaintenanceAuditRespVO>> getEmployeeAuditList(
            @RequestParam(value = "employeeProfileId", required = false) Long employeeProfileId) {
        return success(runtimeConfigService.listEmployeeAuditRecords(SecurityFrameworkUtils.getLoginUserId(),
                        employeeProfileId)
                .stream()
                .map(MesProcessPoolTeamLeaderController::toMaintenanceAuditRespVO)
                .toList());
    }

    @PostMapping("/team-device/create")
    @Operation(summary = "新增班组设备")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:maintain')")
    public CommonResult<Long> createTeamDevice(@Valid @RequestBody MesTeamDeviceSaveReqVO reqVO) {
        return success(runtimeConfigService.createDevice(
                cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamDeviceSaveReqBO.builder()
                        .leaderUserId(SecurityFrameworkUtils.getLoginUserId())
                        .deviceCode(reqVO.getDeviceCode())
                        .deviceName(reqVO.getDeviceName())
                        .deviceStatus(reqVO.getDeviceStatus())
                        .build()));
    }

    @GetMapping("/team-device/list")
    @Operation(summary = "查询当前生产组长班组设备")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:query')")
    public CommonResult<List<MesTeamDeviceRespVO>> getTeamDeviceList(
            @RequestParam(value = "enabled", required = false) Boolean enabled) {
        return success(runtimeConfigService.listDevices(SecurityFrameworkUtils.getLoginUserId(), enabled).stream()
                .map(device -> new MesTeamDeviceRespVO()
                        .setDeviceId(device.getId())
                        .setDeviceCode(device.getDeviceCode())
                        .setDeviceName(device.getDeviceName())
                        .setDeviceStatus(device.getDeviceStatus())
                        .setEnabled(device.getEnabled()))
                .toList());
    }

    @PutMapping("/team-device/status/update")
    @Operation(summary = "更新班组设备状态")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:maintain')")
    public CommonResult<Boolean> updateTeamDeviceStatus(@Valid @RequestBody MesTeamDeviceStatusUpdateReqVO reqVO) {
        runtimeConfigService.updateDeviceStatus(
                cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamDeviceStatusUpdateReqBO.builder()
                        .leaderUserId(SecurityFrameworkUtils.getLoginUserId())
                        .deviceId(reqVO.getDeviceId())
                        .deviceStatus(reqVO.getDeviceStatus())
                        .build());
        return success(Boolean.TRUE);
    }

    @PostMapping("/process-config/device-binding/save")
    @Operation(summary = "保存生产组长路线工序设备绑定")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:maintain')")
    public CommonResult<Long> saveProcessConfigDeviceBinding(
            @Valid @RequestBody MesTeamProcessDeviceBindingSaveReqVO reqVO) {
        return success(runtimeConfigService.bindDeviceToProcess(
                cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamProcessDeviceBindingSaveReqBO.builder()
                        .leaderUserId(SecurityFrameworkUtils.getLoginUserId())
                        .routeProcessId(reqVO.getRouteProcessId())
                        .deviceId(reqVO.getDeviceId())
                        .build()));
    }

    @PostMapping("/process-config/device-parameter-rule/save")
    @Operation(summary = "保存生产组长路线工序设备参数标准")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:maintain')")
    public CommonResult<Long> saveProcessConfigDeviceParameterRule(
            @Valid @RequestBody MesTeamDeviceParameterRuleSaveReqVO reqVO) {
        return success(runtimeConfigService.saveDeviceParameterRule(
                cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamDeviceParameterRuleSaveReqBO.builder()
                        .leaderUserId(SecurityFrameworkUtils.getLoginUserId())
                        .routeProcessId(reqVO.getRouteProcessId())
                        .deviceId(reqVO.getDeviceId())
                        .parameterCode(reqVO.getParameterCode())
                        .parameterName(reqVO.getParameterName())
                        .unit(reqVO.getUnit())
                        .lowerLimit(reqVO.getLowerLimit())
                        .upperLimit(reqVO.getUpperLimit())
                        .targetValue(reqVO.getTargetValue())
                        .valueType(reqVO.getValueType())
                        .standardText(reqVO.getStandardText())
                        .optionValues(reqVO.getOptionValues())
                        .defaultText(reqVO.getDefaultText())
                        .decimalScale(reqVO.getDecimalScale())
                        .build()));
    }

    @PostMapping("/process-defect-reason/save")
    @Operation(summary = "保存班组工序异常原因")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:maintain')")
    public CommonResult<Long> saveProcessDefectReason(@Valid @RequestBody MesTeamProcessDefectReasonSaveReqVO reqVO) {
        return success(runtimeConfigService.saveProcessDefectReason(
                cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamProcessDefectReasonSaveReqBO.builder()
                        .leaderUserId(SecurityFrameworkUtils.getLoginUserId())
                        .routeProcessId(reqVO.getRouteProcessId())
                        .processId(reqVO.getProcessId())
                        .reasonType(reqVO.getReasonType())
                        .reasonCode(reqVO.getReasonCode())
                        .reasonName(reqVO.getReasonName())
                        .build()));
    }

    @GetMapping("/submission/allocation/trace")
    @Operation(summary = "P6 只读核验报工分配记录")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:query')")
    public CommonResult<MesTeamLeaderAllocationTraceRespVO> getReportAllocationTrace(
            @RequestParam("eventId") Long eventId,
            @RequestParam("workOrderId") Long workOrderId,
            @RequestParam("routeProcessId") Long routeProcessId,
            @RequestParam("processId") Long processId) {
        return success(traceService.getAllocationTrace(eventId, workOrderId, routeProcessId, processId));
    }

    @GetMapping("/order-process/trace")
    @Operation(summary = "P6 只读核验订单工序完成状态")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:query')")
    public CommonResult<MesTeamLeaderOrderProcessTraceRespVO> getOrderProcessTrace(
            @RequestParam("workOrderId") Long workOrderId,
            @RequestParam("routeProcessId") Long routeProcessId,
            @RequestParam("processId") Long processId) {
        return success(traceService.getOrderProcessTrace(workOrderId, routeProcessId, processId));
    }

    @GetMapping("/batch-record/trace")
    @Operation(summary = "P6 只读核验正式批记录回填证据")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:query')")
    public CommonResult<MesTeamLeaderBatchRecordTraceRespVO> getBatchRecordTrace(
            @RequestParam("workOrderId") Long workOrderId,
            @RequestParam("routeProcessId") Long routeProcessId,
            @RequestParam("processId") Long processId) {
        return success(traceService.getBatchRecordTrace(workOrderId, routeProcessId, processId));
    }

    @GetMapping("/production-execution/trace")
    @Operation(summary = "P0 生产执行主闭环追溯")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:query')")
    public CommonResult<MesProductionExecutionTraceRespVO> getProductionExecutionTrace(
            @RequestParam("processPoolEventId") Long processPoolEventId) {
        return success(traceService.getProductionExecutionTrace(processPoolEventId));
    }

    private static MesTeamLeaderProcessConfigRowRespVO toProcessConfigRowRespVO(MesTeamLeaderProcessConfigRow row) {
        return new MesTeamLeaderProcessConfigRowRespVO()
                .setRouteId(row.getRouteId())
                .setRouteCode(row.getRouteCode())
                .setRouteName(row.getRouteName())
                .setRouteProcessId(row.getRouteProcessId())
                .setProcessId(row.getProcessId())
                .setProcessCode(row.getProcessCode())
                .setProcessName(row.getProcessName())
                .setSort(row.getSort())
                .setLossReasons(row.getLossReasons().stream()
                        .map(MesProcessPoolTeamLeaderController::toProcessConfigLossReasonRespVO)
                        .toList())
                .setDevices(row.getDevices().stream()
                        .map(MesProcessPoolTeamLeaderController::toProcessConfigDeviceRespVO)
                        .toList());
    }

    private static MesTeamLeaderProcessConfigRowRespVO.LossReason toProcessConfigLossReasonRespVO(
            MesTeamLeaderLossReasonItem item) {
        return new MesTeamLeaderProcessConfigRowRespVO.LossReason()
                .setId(item.getId())
                .setReasonCode(item.getReasonCode())
                .setReasonName(item.getReasonName())
                .setEnabled(item.getEnabled());
    }

    private static MesTeamLeaderProcessConfigRowRespVO.Device toProcessConfigDeviceRespVO(
            MesTeamLeaderProcessConfigDevice device) {
        return new MesTeamLeaderProcessConfigRowRespVO.Device()
                .setBindingId(device.getBindingId())
                .setDeviceId(device.getDeviceId())
                .setDeviceCode(device.getDeviceCode())
                .setDeviceName(device.getDeviceName())
                .setDeviceStatus(device.getDeviceStatus())
                .setMapped(device.getMapped())
                .setParameters(device.getParameters().stream()
                        .map(MesProcessPoolTeamLeaderController::toProcessConfigParameterRespVO)
                        .toList());
    }

    private static MesTeamLeaderProcessConfigRowRespVO.Parameter toProcessConfigParameterRespVO(
            MesTeamLeaderProcessConfigParameter parameter) {
        return new MesTeamLeaderProcessConfigRowRespVO.Parameter()
                .setRuleId(parameter.getRuleId())
                .setParameterCode(parameter.getParameterCode())
                .setParameterName(parameter.getParameterName())
                .setUnit(parameter.getUnit())
                .setValueType(parameter.getValueType())
                .setStandardText(parameter.getStandardText())
                .setLowerLimit(parameter.getLowerLimit())
                .setTargetValue(parameter.getTargetValue())
                .setUpperLimit(parameter.getUpperLimit())
                .setOptionValues(parameter.getOptionValues())
                .setDefaultText(parameter.getDefaultText())
                .setDecimalScale(parameter.getDecimalScale())
                .setEnabled(parameter.getEnabled())
                .setActualAverage(parameter.getActualAverage())
                .setSampleCount(parameter.getSampleCount())
                .setStatisticsStartTime(parameter.getStatisticsStartTime())
                .setStatisticsEndTime(parameter.getStatisticsEndTime())
                .setStatisticsWindowDays(parameter.getStatisticsWindowDays());
    }

    private static MesTeamLeaderLossReasonRowRespVO toLossReasonRowRespVO(MesTeamLeaderLossReasonRow row) {
        return new MesTeamLeaderLossReasonRowRespVO()
                .setRouteId(row.getRouteId())
                .setRouteCode(row.getRouteCode())
                .setRouteName(row.getRouteName())
                .setRouteProcessId(row.getRouteProcessId())
                .setProcessId(row.getProcessId())
                .setProcessCode(row.getProcessCode())
                .setProcessName(row.getProcessName())
                .setSort(row.getSort())
                .setReasons(row.getReasons().stream()
                        .map(MesProcessPoolTeamLeaderController::toLossReasonRespVO)
                        .toList());
    }

    private static MesTeamLeaderLossReasonRespVO toLossReasonRespVO(MesTeamLeaderLossReasonItem item) {
        return new MesTeamLeaderLossReasonRespVO()
                .setId(item.getId())
                .setReasonCode(item.getReasonCode())
                .setReasonName(item.getReasonName())
                .setEnabled(item.getEnabled());
    }

    private static MesTeamProductionEmployeeRespVO toProductionEmployeeRespVO(
            MesProcessPoolTeamEmployeeProfileDO profile) {
        return new MesTeamProductionEmployeeRespVO()
                .setId(profile.getId())
                .setSystemUserId(profile.getSystemUserId())
                .setEmployeeCode(profile.getEmployeeCode())
                .setEmployeeName(profile.getEmployeeName())
                .setDisplayName(profile.getDisplayName())
                .setEmployeeType(profile.getEmployeeType())
                .setEnabled(profile.getEnabled())
                .setDisabledAt(profile.getDisabledAt())
                .setSignaturePasswordManagedBy(profile.getSystemUserId() == null
                        ? "TEMPORARY_PROFILE" : "SYSTEM_USER");
    }

    private static MesPqcLeaderPersonnelRespVO toPqcPersonnelRespVO(MesPqcLeaderPersonnelBO personnel) {
        return new MesPqcLeaderPersonnelRespVO()
                .setScopeId(personnel.getScopeId())
                .setSystemUserId(personnel.getSystemUserId())
                .setDisplayName(personnel.getDisplayName())
                .setUsername(personnel.getUsername())
                .setEnabled(personnel.getEnabled());
    }

    private static MesTeamFormalUserCandidateRespVO toFormalUserCandidateRespVO(MesTeamFormalUserCandidateBO candidate) {
        return new MesTeamFormalUserCandidateRespVO()
                .setSystemUserId(candidate.getSystemUserId())
                .setDisplayName(candidate.getDisplayName())
                .setDisabled(candidate.getDisabled())
                .setDisabledReason(candidate.getDisabledReason())
                .setOccupiedByOtherPqcLeader(candidate.getOccupiedByOtherPqcLeader())
                .setOccupiedLeaderUserId(candidate.getOccupiedLeaderUserId());
    }

    private static MesTeamMaintenanceAuditRespVO toMaintenanceAuditRespVO(
            MesProcessPoolTeamMaintenanceAuditDO audit) {
        return new MesTeamMaintenanceAuditRespVO()
                .setId(audit.getId())
                .setOperatorUserId(audit.getOperatorUserId())
                .setActionType(audit.getActionType())
                .setTargetType(audit.getTargetType())
                .setTargetId(audit.getTargetId())
                .setResultStatus(audit.getResultStatus())
                .setChangeSummary(audit.getChangeSummary())
                .setAuditTime(audit.getAuditTime());
    }

    private static MesTeamLeaderActiveOrderRespVO toActiveOrderRespVO(MesTeamLeaderActiveOrderRow activeOrder) {
        return new MesTeamLeaderActiveOrderRespVO()
                .setId(activeOrder.getId())
                .setWorkOrderId(activeOrder.getWorkOrderId())
                .setWorkOrderCode(activeOrder.getWorkOrderCode())
                .setProductName(activeOrder.getProductName())
                .setProductCode(activeOrder.getProductCode())
                .setQuantity(activeOrder.getQuantity())
                .setRouteId(activeOrder.getRouteId())
                .setRouteName(activeOrder.getRouteName())
                .setRouteVersionId(activeOrder.getRouteVersionId())
                .setRouteVersionNo(activeOrder.getRouteVersionNo())
                .setErpFixedQuantitySnapshot(activeOrder.getErpFixedQuantitySnapshot())
                .setProcessRemainingQuantities(toActiveOrderProcessRemainingQuantityRespVOs(
                        activeOrder.getProcessRemainingQuantities()))
                .setProductionProgressPercent(activeOrder.getProductionProgressPercent())
                .setInspectionProgressPercent(activeOrder.getInspectionProgressPercent())
                .setActiveStatus(activeOrder.getActiveStatus())
                .setBusinessStatus(activeOrder.getBusinessStatus())
                .setJoinedAt(activeOrder.getJoinedAt())
                .setRemovedAt(activeOrder.getRemovedAt())
                .setVersion(activeOrder.getVersion())
                .setAbnormal(activeOrder.getAbnormal())
                .setAbnormalReason(activeOrder.getAbnormalReason())
                .setAbnormalReportedAt(activeOrder.getAbnormalReportedAt())
                .setReleaseApplicationId(activeOrder.getReleaseApplicationId())
                .setPqcReleaseWorkTaskId(activeOrder.getPqcReleaseWorkTaskId())
                .setReleaseApplicationStatus(activeOrder.getReleaseApplicationStatus())
                .setReleaseSourceSnapshotHash(activeOrder.getReleaseSourceSnapshotHash())
                .setReleaseApplicationVersion(activeOrder.getReleaseApplicationVersion())
                .setQuantityConflict(activeOrder.getQuantityConflict())
                .setHasQuantityConflict(activeOrder.getHasQuantityConflict())
                .setQuantityConflictProcessCount(activeOrder.getQuantityConflictProcessCount())
                .setOverageQuantity(activeOrder.getOverageQuantity());
    }

    private static List<MesTeamLeaderActiveOrderRespVO.ProcessRemainingQuantity>
    toActiveOrderProcessRemainingQuantityRespVOs(
            List<MesTeamLeaderActiveOrderRow.ProcessRemainingQuantity> processRemainingQuantities) {
        return processRemainingQuantities.stream()
                .map(item -> new MesTeamLeaderActiveOrderRespVO.ProcessRemainingQuantity()
                        .setRouteProcessId(item.getRouteProcessId())
                        .setProcessId(item.getProcessId())
                        .setPlannedQuantity(item.getPlannedQuantity())
                        .setAllocatedQuantity(item.getAllocatedQuantity())
                        .setRemainingQuantity(item.getRemainingQuantity())
                        .setQuantityConflict(item.getQuantityConflict())
                        .setOverageQuantity(item.getOverageQuantity()))
                .toList();
    }

    private static MesTeamLeaderActiveOrderDetailRespVO toActiveOrderDetailRespVO(
            MesTeamLeaderActiveOrderDetail detail) {
        return new MesTeamLeaderActiveOrderDetailRespVO()
                .setActiveOrderId(detail.getActiveOrderId())
                .setWorkOrderId(detail.getWorkOrderId())
                .setWorkOrderCode(detail.getWorkOrderCode())
                .setRouteName(detail.getRouteName())
                .setProcesses(detail.getProcesses().stream()
                        .map(MesProcessPoolTeamLeaderController::toActiveOrderProcessDetailRespVO)
                        .toList());
    }

    private static MesTeamLeaderActiveOrderDetailRespVO.ProcessDetail toActiveOrderProcessDetailRespVO(
            MesTeamLeaderActiveOrderDetail.ProcessDetail process) {
        return new MesTeamLeaderActiveOrderDetailRespVO.ProcessDetail()
                .setRouteProcessId(process.getRouteProcessId())
                .setProcessId(process.getProcessId())
                .setProcessCode(process.getProcessCode())
                .setProcessName(process.getProcessName())
                .setRequiredQuantity(process.getRequiredQuantity())
                .setSubmittedQuantity(process.getSubmittedQuantity())
                .setSubmissionCount(process.getSubmissionCount())
                .setQuantityConflict(process.getQuantityConflict())
                .setOverageQuantity(process.getOverageQuantity())
                .setSubmissions(process.getSubmissions().stream()
                        .map(MesProcessPoolTeamLeaderController::toActiveOrderSubmissionDetailRespVO)
                        .toList());
    }

    private static MesTeamLeaderActiveOrderDetailRespVO.SubmissionDetail toActiveOrderSubmissionDetailRespVO(
            MesTeamLeaderActiveOrderDetail.SubmissionDetail submission) {
        return new MesTeamLeaderActiveOrderDetailRespVO.SubmissionDetail()
                .setEventId(submission.getEventId())
                .setSubmittedQuantity(submission.getSubmittedQuantity())
                .setSubmitterName(submission.getSubmitterName())
                .setReviewerName(submission.getReviewerName())
                .setSubmittedAt(submission.getSubmittedAt())
                .setQuantityConflict(submission.getQuantityConflict());
    }

    private static MesTeamLeaderActiveOrderReleaseApplyRespVO toActiveOrderReleaseApplyRespVO(
            MesTeamLeaderActiveOrderReleaseApplicationResult result) {
        return new MesTeamLeaderActiveOrderReleaseApplyRespVO()
                .setApplicationId(result.getApplicationId())
                .setActiveOrderId(result.getActiveOrderId())
                .setWorkOrderId(result.getWorkOrderId())
                .setWorkOrderCode(result.getWorkOrderCode())
                .setBatchCode(result.getBatchCode())
                .setRouteId(result.getRouteId())
                .setRouteVersionId(result.getRouteVersionId())
                .setPqcReleaseWorkTaskId(result.getPqcReleaseWorkTaskId())
                .setStatus(result.getStatus())
                .setSourceSnapshotHash(result.getSourceSnapshotHash())
                .setVersion(result.getVersion())
                .setAppliedAt(result.getAppliedAt());
    }

    private static MesTeamLeaderActiveOrderRebuildPreviewRespVO toActiveOrderRebuildPreviewRespVO(
            MesTeamLeaderActiveOrderRebuildPreview preview) {
        return new MesTeamLeaderActiveOrderRebuildPreviewRespVO()
                .setActiveOrderId(preview.getActiveOrderId())
                .setHasHistoricalRuntimeData(preview.isHasHistoricalRuntimeData())
                .setProductionReportCount(preview.getProductionReportCount())
                .setProductionProgressCount(preview.getProductionProgressCount())
                .setPqcInspectionResultCount(preview.getPqcInspectionResultCount())
                .setProcessSnapshotCount(preview.getProcessSnapshotCount())
                .setPqcTaskCount(preview.getPqcTaskCount())
                .setReleaseApplicationCount(preview.getReleaseApplicationCount())
                .setEventCount(preview.getEventCount());
    }

    private static MesTeamLeaderActiveOrderRebuildResultRespVO toActiveOrderRebuildResultRespVO(
            MesTeamLeaderActiveOrderRebuildResult result) {
        return new MesTeamLeaderActiveOrderRebuildResultRespVO()
                .setActiveOrderId(result.getActiveOrderId())
                .setHistoricalRuntimeDataDeleted(result.isHistoricalRuntimeDataDeleted())
                .setDeletedProductionReportCount(result.getDeletedProductionReportCount())
                .setDeletedProductionProgressCount(result.getDeletedProductionProgressCount())
                .setDeletedPqcInspectionResultCount(result.getDeletedPqcInspectionResultCount())
                .setDeletedProcessSnapshotCount(result.getDeletedProcessSnapshotCount())
                .setDeletedPqcTaskCount(result.getDeletedPqcTaskCount())
                .setRebuiltProcessSnapshotCount(result.getRebuiltProcessSnapshotCount())
                .setRebuiltPqcTaskCount(result.getRebuiltPqcTaskCount());
    }

    private static MesTeamLeaderActiveOrderSimulationRespVO toActiveOrderSimulationRespVO(
            MesTeamLeaderActiveOrderSimulationResult result) {
        return new MesTeamLeaderActiveOrderSimulationRespVO()
                .setActiveOrderId(result.getActiveOrderId())
                .setProductionSubmitCount(result.getProductionSubmitCount())
                .setProductionReviewCount(result.getProductionReviewCount())
                .setPqcSubmitCount(result.getPqcSubmitCount())
                .setPqcReviewCount(result.getPqcReviewCount())
                .setProductionProgressPercent(result.getProductionProgressPercent())
                .setInspectionProgressPercent(result.getInspectionProgressPercent());
    }

    private static MesTeamLeaderActiveOrderCandidateRespVO toActiveOrderCandidateRespVO(
            MesTeamLeaderActiveOrderCandidateBO candidate) {
        return new MesTeamLeaderActiveOrderCandidateRespVO()
                .setWorkOrderId(candidate.getWorkOrderId())
                .setWorkOrderCode(candidate.getWorkOrderCode())
                .setCandidateState(candidate.getCandidateState())
                .setEligible(candidate.isEligible())
                .setIneligibleReason(candidate.getIneligibleReason());
    }

    private static MesTeamLeaderActiveOrderTransferTraceRespVO toActiveOrderTransferTraceRespVO(
            MesProcessPoolActiveOrderTransferTraceDO trace) {
        return new MesTeamLeaderActiveOrderTransferTraceRespVO()
                .setId(trace.getId())
                .setActiveOrderId(trace.getActiveOrderId())
                .setWorkOrderId(trace.getWorkOrderId())
                .setRouteId(trace.getRouteId())
                .setRouteVersionId(trace.getRouteVersionId())
                .setSourceType(trace.getSourceType())
                .setDirection(trace.getDirection())
                .setTransferId(trace.getTransferId())
                .setTransferLineId(trace.getTransferLineId())
                .setTransferDetailId(trace.getTransferDetailId())
                .setMaterialStockId(trace.getMaterialStockId())
                .setBatchId(trace.getBatchId())
                .setItemId(trace.getItemId())
                .setQuantity(trace.getQuantity())
                .setSourceObjectType(trace.getSourceObjectType())
                .setSourceObjectId(trace.getSourceObjectId())
                .setSourceObjectCode(trace.getSourceObjectCode())
                .setSourceStatus(trace.getSourceStatus())
                .setSourceOccurredAt(trace.getSourceOccurredAt())
                .setIdempotencyKey(trace.getIdempotencyKey())
                .setSourceSnapshotJson(trace.getSourceSnapshotJson());
    }

    private static MesTeamLeaderReportAllocationLineReqBO toReportAllocationLineReqBO(
            MesTeamLeaderReportAllocationLineReqVO line) {
        return MesTeamLeaderReportAllocationLineReqBO.builder()
                .activeOrderId(line.getActiveOrderId())
                .allocatedQuantity(line.getAllocatedQuantity())
                .build();
    }

    private static MesTeamLeaderReportAllocationPreviewRespVO toReportAllocationPreviewRespVO(
            MesReportAllocationSnapshot preview) {
        return new MesTeamLeaderReportAllocationPreviewRespVO()
                .setPoolQuantity(preview.getPoolQuantity())
                .setTotalAllocatedQuantity(preview.getTotalAllocatedQuantity())
                .setUnallocatedQuantity(preview.getUnallocatedQuantity())
                .setLines(preview.getLines().stream()
                        .map(MesProcessPoolTeamLeaderController::toReportAllocationPreviewLineRespVO)
                        .toList());
    }

    private static MesTeamLeaderReportAllocationPreviewRespVO.Line toReportAllocationPreviewLineRespVO(
            MesReportAllocationSnapshotLine line) {
        return new MesTeamLeaderReportAllocationPreviewRespVO.Line()
                .setActiveOrderId(line.getActiveOrderId())
                .setWorkOrderId(line.getWorkOrderId())
                .setWorkOrderCode(line.getWorkOrderCode())
                .setRouteProcessId(line.getRouteProcessId())
                .setProcessId(line.getProcessId())
                .setAllocatedQuantity(line.getAllocatedQuantity())
                .setOverageQuantity(line.getOverageQuantity())
                .setNeedsAdjustment(line.getNeedsAdjustment())
                .setAllocationMode(line.getAllocationMode())
                .setReleased(line.getReleased())
                .setEditable(line.getEditable());
    }

    private static MesTeamLeaderReportAllocationSnapshotRespVO toReportAllocationSnapshotRespVO(
            MesReportAllocationSnapshot snapshot) {
        return new MesTeamLeaderReportAllocationSnapshotRespVO().setEventId(snapshot.getEventId())
                .setVersion(snapshot.getVersion()).setPoolQuantity(snapshot.getPoolQuantity())
                .setReleasedAllocatedQuantity(snapshot.getReleasedAllocatedQuantity())
                .setEditableAllocatedQuantity(snapshot.getEditableAllocatedQuantity())
                .setTotalAllocatedQuantity(snapshot.getTotalAllocatedQuantity())
                .setUnallocatedQuantity(snapshot.getUnallocatedQuantity())
                .setLines(snapshot.getLines().stream().map(line ->
                        new MesTeamLeaderReportAllocationSnapshotRespVO.Line()
                                .setAllocationId(line.getAllocationId()).setActiveOrderId(line.getActiveOrderId())
                                .setWorkOrderId(line.getWorkOrderId()).setWorkOrderCode(line.getWorkOrderCode())
                                .setRouteProcessId(line.getRouteProcessId()).setProcessId(line.getProcessId())
                                .setAllocatedQuantity(line.getAllocatedQuantity())
                                .setOverageQuantity(line.getOverageQuantity())
                                .setNeedsAdjustment(line.getNeedsAdjustment())
                                .setAllocationMode(line.getAllocationMode()).setReleased(line.getReleased())
                                .setEditable(line.getEditable())).toList());
    }

    private static MesTeamLeaderReportAllocationAuditRespVO toReportAllocationAuditRespVO(
            MesProcessPoolReportAllocationAdjustmentAuditDO audit) {
        return new MesTeamLeaderReportAllocationAuditRespVO().setId(audit.getId()).setEventId(audit.getEventId())
                .setAllocationVersion(audit.getAllocationVersion())
                .setSourceAllocationId(audit.getSourceAllocationId()).setActiveOrderId(audit.getActiveOrderId())
                .setWorkOrderId(audit.getWorkOrderId()).setRouteProcessId(audit.getRouteProcessId())
                .setProcessId(audit.getProcessId()).setBeforeQuantity(audit.getBeforeQuantity())
                .setAfterQuantity(audit.getAfterQuantity()).setDeltaQuantity(audit.getDeltaQuantity())
                .setActorUserId(audit.getActorUserId()).setAdjustmentReason(audit.getAdjustmentReason())
                .setAllocationMode(audit.getAllocationMode()).setChangeSource(audit.getChangeSource())
                .setOccurredAt(audit.getOccurredAt());
    }
}
