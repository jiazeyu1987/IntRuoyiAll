package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamDefectReasonSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamDeviceParameterRuleSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamEmployeeBindingDisableReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamEmployeeBindingSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderSubmissionPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderSubmissionReviewReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesWorkOrderAbnormalReportReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelineDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelineEventRespVO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesDefectReasonCatalogService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesDefectReasonSaveReqBO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesProcessDeviceParameterRuleSaveReqBO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesProcessDeviceParameterRuleService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamEmployeeBindingService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderSubmissionReviewReqBO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderSubmissionReviewService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderWorkbenchService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesWorkOrderAbnormalReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - MES 工序池班组长工作台")
@RestController
@RequestMapping("/mes/pro/process-pool/team-leader")
@Validated
public class MesProcessPoolTeamLeaderController {

    private final MesTeamLeaderWorkbenchService workbenchService;
    private final MesTeamLeaderSubmissionReviewService submissionReviewService;
    private final MesWorkOrderAbnormalReportService abnormalReportService;
    private final MesTeamEmployeeBindingService employeeBindingService;
    private final MesDefectReasonCatalogService defectReasonCatalogService;
    private final MesProcessDeviceParameterRuleService deviceParameterRuleService;

    public MesProcessPoolTeamLeaderController(MesTeamLeaderWorkbenchService workbenchService,
                                              MesTeamLeaderSubmissionReviewService submissionReviewService,
                                              MesWorkOrderAbnormalReportService abnormalReportService,
                                              MesTeamEmployeeBindingService employeeBindingService,
                                              MesDefectReasonCatalogService defectReasonCatalogService,
                                              MesProcessDeviceParameterRuleService deviceParameterRuleService) {
        this.workbenchService = workbenchService;
        this.submissionReviewService = submissionReviewService;
        this.abnormalReportService = abnormalReportService;
        this.employeeBindingService = employeeBindingService;
        this.defectReasonCatalogService = defectReasonCatalogService;
        this.deviceParameterRuleService = deviceParameterRuleService;
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
                .build()));
    }

    @PostMapping("/work-order/abnormal/report")
    @Operation(summary = "标记并上报生产工单异常")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:abnormal')")
    public CommonResult<Long> markAndReportWorkOrderAbnormal(@Valid @RequestBody MesWorkOrderAbnormalReportReqVO reqVO) {
        return success(abnormalReportService.markAndReport(
                cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesWorkOrderAbnormalReportReqBO.builder()
                .workOrderId(reqVO.getWorkOrderId())
                .routeProcessId(reqVO.getRouteProcessId())
                .processId(reqVO.getProcessId())
                .sourceEventId(reqVO.getSourceEventId())
                .markerUserId(SecurityFrameworkUtils.getLoginUserId())
                .abnormalReasonCode(reqVO.getAbnormalReasonCode())
                .abnormalDescription(reqVO.getAbnormalDescription())
                .build()));
    }

    @PostMapping("/employee-binding/add")
    @Operation(summary = "添加班组员工到工序")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:maintain')")
    public CommonResult<Long> addEmployeeBinding(@Valid @RequestBody MesTeamEmployeeBindingSaveReqVO reqVO) {
        return success(employeeBindingService.addEmployeeBinding(
                cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamEmployeeBindingSaveReqBO.builder()
                .leaderUserId(SecurityFrameworkUtils.getLoginUserId())
                .processId(reqVO.getProcessId())
                .employeeUserId(reqVO.getEmployeeUserId())
                .build()));
    }

    @PutMapping("/employee-binding/disable")
    @Operation(summary = "禁用班组员工工序绑定")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:maintain')")
    public CommonResult<Boolean> disableEmployeeBinding(@Valid @RequestBody MesTeamEmployeeBindingDisableReqVO reqVO) {
        employeeBindingService.disableEmployeeBinding(
                cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamEmployeeBindingDisableReqBO.builder()
                .leaderUserId(SecurityFrameworkUtils.getLoginUserId())
                .bindingId(reqVO.getBindingId())
                .build());
        return success(Boolean.TRUE);
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

    @PostMapping("/device-parameter-rule/save")
    @Operation(summary = "保存班组工序设备参数上下限")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:maintain')")
    public CommonResult<Long> saveDeviceParameterRule(@Valid @RequestBody MesTeamDeviceParameterRuleSaveReqVO reqVO) {
        return success(deviceParameterRuleService.saveRule(MesProcessDeviceParameterRuleSaveReqBO.builder()
                .leaderUserId(SecurityFrameworkUtils.getLoginUserId())
                .routeProcessId(reqVO.getRouteProcessId())
                .processId(reqVO.getProcessId())
                .deviceId(reqVO.getDeviceId())
                .parameterCode(reqVO.getParameterCode())
                .parameterName(reqVO.getParameterName())
                .lowerLimit(reqVO.getLowerLimit())
                .upperLimit(reqVO.getUpperLimit())
                .valueType(reqVO.getValueType())
                .build()));
    }
}
