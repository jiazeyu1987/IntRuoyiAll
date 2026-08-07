package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamDeviceSaveReqVO;
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
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderActiveOrderCandidateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderActiveOrderRemoveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderActiveOrderRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderActiveOrderTransferTraceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderLossReasonRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderLossReasonRowRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderLossReasonSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderLossReasonUpdateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderProcessConfigRowRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderAllocationTraceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderBatchRecordTraceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderOrderProcessTraceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderReportAllocationConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderReportAllocationLineReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderReportAllocationPreviewReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderReportAllocationPreviewRespVO;
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
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderCandidateBO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderRemoveReqBO;
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
    private final MesTeamLeaderReportConfirmationService reportConfirmationService;
    private final MesTeamLeaderRuntimeConfigService runtimeConfigService;
    private final MesPqcLeaderPersonnelService pqcPersonnelService;
    private final MesTeamLeaderLossReasonService lossReasonService;
    private final MesTeamLeaderTraceService traceService;
    private final MesActiveOrderTransferTraceService activeOrderTransferTraceService;

    public MesProcessPoolTeamLeaderController(MesTeamLeaderWorkbenchService workbenchService,
                                              MesTeamLeaderSubmissionReviewService submissionReviewService,
                                              MesWorkOrderAbnormalReportService abnormalReportService,
                                              MesDefectReasonCatalogService defectReasonCatalogService,
                                              MesTeamLeaderProcessConfigService processConfigService,
                                              MesTeamLeaderActiveOrderService activeOrderService,
                                              MesTeamLeaderReportConfirmationService reportConfirmationService,
                                              MesTeamLeaderRuntimeConfigService runtimeConfigService,
                                              MesPqcLeaderPersonnelService pqcPersonnelService,
                                              MesTeamLeaderLossReasonService lossReasonService,
                                              MesTeamLeaderTraceService traceService,
                                              MesActiveOrderTransferTraceService activeOrderTransferTraceService) {
        this.workbenchService = workbenchService;
        this.submissionReviewService = submissionReviewService;
        this.abnormalReportService = abnormalReportService;
        this.defectReasonCatalogService = defectReasonCatalogService;
        this.processConfigService = processConfigService;
        this.activeOrderService = activeOrderService;
        this.reportConfirmationService = reportConfirmationService;
        this.runtimeConfigService = runtimeConfigService;
        this.pqcPersonnelService = pqcPersonnelService;
        this.lossReasonService = lossReasonService;
        this.traceService = traceService;
        this.activeOrderTransferTraceService = activeOrderTransferTraceService;
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
                .reviewSignatureId(reqVO.getReviewSignatureId())
                .reviewSignatureUserId(reqVO.getReviewSignatureEmployeeUserId())
                .reviewSignatureSnapshotJson(reqVO.getReviewSignatureSnapshotJson())
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
                .abnormalReasonCode(reqVO.getAbnormalReasonCode())
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
    public CommonResult<List<MesTeamLeaderProcessConfigRowRespVO>> getProcessConfigList() {
        return success(processConfigService.listProcessConfigs(SecurityFrameworkUtils.getLoginUserId()).stream()
                .map(MesProcessPoolTeamLeaderController::toProcessConfigRowRespVO)
                .toList());
    }

    @PostMapping("/active-order/add")
    @Operation(summary = "加入生产组长活跃订单")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:maintain')")
    public CommonResult<Long> addActiveOrder(@Valid @RequestBody MesTeamLeaderActiveOrderAddReqVO reqVO) {
        return success(activeOrderService.addActiveOrder(MesTeamLeaderActiveOrderAddReqBO.builder()
                .leaderUserId(SecurityFrameworkUtils.getLoginUserId())
                .workOrderId(reqVO.getWorkOrderId())
                .build()));
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

    @GetMapping("/active-order/list")
    @Operation(summary = "查询生产组长活跃订单")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:query')")
    public CommonResult<List<MesTeamLeaderActiveOrderRespVO>> getActiveOrderList() {
        return success(activeOrderService.listActiveOrders(SecurityFrameworkUtils.getLoginUserId()).stream()
                .map(MesProcessPoolTeamLeaderController::toActiveOrderRespVO)
                .toList());
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
        MesTeamLeaderReportAllocationPreview preview = reportConfirmationService.previewFifoAllocation(
                MesTeamLeaderReportAllocationPreviewReqBO.builder()
                        .eventId(reqVO.getEventId())
                        .leaderUserId(SecurityFrameworkUtils.getLoginUserId())
                        .leaderType(reqVO.getLeaderType())
                        .build());
        return success(toReportAllocationPreviewRespVO(preview));
    }

    @PostMapping("/submission/allocation/confirm")
    @Operation(summary = "确认班组长报工并保存活跃订单分配")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:review')")
    public CommonResult<Long> confirmReportAllocation(
            @Valid @RequestBody MesTeamLeaderReportAllocationConfirmReqVO reqVO) {
        return success(reportConfirmationService.confirmSubmission(MesTeamLeaderReportConfirmationReqBO.builder()
                .eventId(reqVO.getEventId())
                .leaderUserId(SecurityFrameworkUtils.getLoginUserId())
                .leaderType(reqVO.getLeaderType())
                .allocationMode(reqVO.getAllocationMode())
                .reviewRemark(reqVO.getReviewRemark())
                .reviewSignatureId(reqVO.getReviewSignatureId())
                .reviewSignatureUserId(reqVO.getReviewSignatureEmployeeUserId())
                .reviewSignatureSnapshotJson(reqVO.getReviewSignatureSnapshotJson())
                .allocations(reqVO.getAllocations().stream()
                        .map(MesProcessPoolTeamLeaderController::toReportAllocationLineReqBO)
                        .toList())
                .build()));
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
                .setLowerLimit(parameter.getLowerLimit())
                .setTargetValue(parameter.getTargetValue())
                .setUpperLimit(parameter.getUpperLimit())
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

    private static MesTeamLeaderActiveOrderRespVO toActiveOrderRespVO(MesProcessPoolActiveOrderDO activeOrder) {
        return new MesTeamLeaderActiveOrderRespVO()
                .setId(activeOrder.getId())
                .setWorkOrderId(activeOrder.getWorkOrderId())
                .setRouteId(activeOrder.getRouteId())
                .setRouteVersionId(activeOrder.getRouteVersionId())
                .setErpFixedQuantitySnapshot(activeOrder.getErpFixedQuantitySnapshot())
                .setActiveStatus(activeOrder.getActiveStatus())
                .setBusinessStatus(activeOrder.getBusinessStatus())
                .setJoinedAt(activeOrder.getJoinedAt())
                .setRemovedAt(activeOrder.getRemovedAt())
                .setVersion(activeOrder.getVersion());
    }

    private static MesTeamLeaderActiveOrderCandidateRespVO toActiveOrderCandidateRespVO(
            MesTeamLeaderActiveOrderCandidateBO candidate) {
        return new MesTeamLeaderActiveOrderCandidateRespVO()
                .setWorkOrderId(candidate.getWorkOrderId())
                .setWorkOrderCode(candidate.getWorkOrderCode())
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
            MesTeamLeaderReportAllocationPreview preview) {
        return new MesTeamLeaderReportAllocationPreviewRespVO()
                .setTotalAllocatedQuantity(preview.getTotalAllocatedQuantity())
                .setLines(preview.getLines().stream()
                        .map(MesProcessPoolTeamLeaderController::toReportAllocationPreviewLineRespVO)
                        .toList());
    }

    private static MesTeamLeaderReportAllocationPreviewRespVO.Line toReportAllocationPreviewLineRespVO(
            MesTeamLeaderReportAllocationPreviewLine line) {
        return new MesTeamLeaderReportAllocationPreviewRespVO.Line()
                .setActiveOrderId(line.getActiveOrderId())
                .setWorkOrderId(line.getWorkOrderId())
                .setWorkOrderCode(line.getWorkOrderCode())
                .setAllocatedQuantity(line.getAllocatedQuantity())
                .setRemainingQuantityBeforeAllocation(line.getRemainingQuantityBeforeAllocation());
    }
}
