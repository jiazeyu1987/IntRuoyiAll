package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesFrontlineActiveOrderRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesFrontlineEmployeeCandidateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesFrontlinePqcSwitchEmployeeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesFrontlineRouteProcessRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesFrontlineSwitchEmployeeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesFrontlineSwitchEmployeeRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesFrontlineTemplateRespVO;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineActiveOrderCandidate;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineDeviceAccountContextService;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineEmployeeCandidate;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineEmployeeSwitchCommand;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineEmployeeSwitchResult;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineEmployeeSwitchService;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlinePqcContextService;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineRouteProcessCandidate;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineTemplateDescriptor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - MES 一线设备账号工序池")
@RestController
@RequestMapping("/mes/pro/feedback/frontline/device-account")
@Validated
public class MesFrontlineDeviceAccountController {

    @Resource
    private MesFrontlineDeviceAccountContextService contextService;
    @Resource
    private MesFrontlineEmployeeSwitchService employeeSwitchService;
    @Resource
    private MesFrontlinePqcContextService pqcContextService;

    @GetMapping("/processes")
    @Operation(summary = "获得设备账号可切换工序")
    @PreAuthorize("@ss.hasPermission('mes:pro-feedback:query')")
    public CommonResult<List<MesFrontlineRouteProcessRespVO>> getSwitchableProcesses() {
        return success(contextService.listSwitchableProcesses(getLoginUserId()).stream()
                .map(MesFrontlineDeviceAccountController::toRouteProcessRespVO)
                .toList());
    }

    @GetMapping("/employee-candidates")
    @Operation(summary = "获得当前工序可切换员工")
    @PreAuthorize("@ss.hasPermission('mes:pro-feedback:query')")
    public CommonResult<List<MesFrontlineEmployeeCandidateRespVO>> getEmployeeCandidates(
            @RequestParam("routeId") @NotNull Long routeId,
            @RequestParam("routeProcessId") @NotNull Long routeProcessId,
            @RequestParam("processId") @NotNull Long processId) {
        return success(contextService.listEmployeeCandidates(getLoginUserId(), routeId, routeProcessId, processId).stream()
                .map(MesFrontlineDeviceAccountController::toEmployeeCandidateRespVO)
                .toList());
    }

    @PostMapping("/switch-employee")
    @Operation(summary = "切换当前工序实际填写员工")
    @PreAuthorize("@ss.hasPermission('mes:pro-feedback:create')")
    public CommonResult<MesFrontlineSwitchEmployeeRespVO> switchActualEmployee(
            @Valid @RequestBody MesFrontlineSwitchEmployeeReqVO reqVO) {
        MesFrontlineEmployeeSwitchResult result = employeeSwitchService.switchActualEmployee(
                new MesFrontlineEmployeeSwitchCommand(getLoginUserId(), reqVO.getRouteId(),
                        reqVO.getRouteProcessId(), reqVO.getProcessId(), reqVO.getActualEmployeeId()));
        return success(toSwitchEmployeeRespVO(result));
    }

    @GetMapping("/pqc/active-orders")
    @Operation(summary = "获得 PQC 当前活跃订单")
    @PreAuthorize("@ss.hasPermission('mes:pro-feedback:query')")
    public CommonResult<List<MesFrontlineActiveOrderRespVO>> getPqcActiveOrders() {
        return success(pqcContextService.listActiveOrders().stream()
                .map(MesFrontlineDeviceAccountController::toActiveOrderRespVO)
                .toList());
    }

    @GetMapping("/pqc/active-order/processes")
    @Operation(summary = "获得 PQC 活跃订单对应工艺路线工序")
    @PreAuthorize("@ss.hasPermission('mes:pro-feedback:query')")
    public CommonResult<List<MesFrontlineRouteProcessRespVO>> getPqcActiveOrderProcesses(
            @RequestParam("workOrderId") @NotNull Long workOrderId,
            @RequestParam("routeId") @NotNull Long routeId) {
        return success(pqcContextService.listProcessesByActiveOrder(workOrderId, routeId).stream()
                .map(MesFrontlineDeviceAccountController::toRouteProcessRespVO)
                .toList());
    }

    @GetMapping("/pqc/personnel")
    @Operation(summary = "获得 PQC 员工和 PQC 组长")
    @PreAuthorize("@ss.hasPermission('mes:pro-feedback:query')")
    public CommonResult<List<MesFrontlineEmployeeCandidateRespVO>> getPqcPersonnel() {
        return success(pqcContextService.listPqcEmployeeCandidates().stream()
                .map(MesFrontlineDeviceAccountController::toEmployeeCandidateRespVO)
                .toList());
    }

    @PostMapping("/pqc/switch-employee")
    @Operation(summary = "PQC 切换实际填写员工")
    @PreAuthorize("@ss.hasPermission('mes:pro-feedback:create')")
    public CommonResult<MesFrontlineSwitchEmployeeRespVO> switchPqcActualEmployee(
            @Valid @RequestBody MesFrontlinePqcSwitchEmployeeReqVO reqVO) {
        MesFrontlineEmployeeSwitchResult result = pqcContextService.switchPqcActualEmployee(getLoginUserId(),
                reqVO.getWorkOrderId(), reqVO.getRouteId(), reqVO.getRouteProcessId(),
                reqVO.getProcessId(), reqVO.getActualEmployeeId());
        return success(toSwitchEmployeeRespVO(result));
    }

    private static MesFrontlineActiveOrderRespVO toActiveOrderRespVO(MesFrontlineActiveOrderCandidate candidate) {
        MesFrontlineActiveOrderRespVO respVO = new MesFrontlineActiveOrderRespVO();
        respVO.setWorkOrderId(candidate.workOrderId());
        respVO.setWorkOrderCode(candidate.workOrderCode());
        respVO.setWorkOrderName(candidate.workOrderName());
        respVO.setProductId(candidate.productId());
        respVO.setProductCode(candidate.productCode());
        respVO.setProductName(candidate.productName());
        respVO.setRouteId(candidate.routeId());
        respVO.setRouteCode(candidate.routeCode());
        respVO.setRouteName(candidate.routeName());
        respVO.setLatestSubmitTime(candidate.latestSubmitTime());
        return respVO;
    }

    private static MesFrontlineRouteProcessRespVO toRouteProcessRespVO(MesFrontlineRouteProcessCandidate candidate) {
        MesFrontlineRouteProcessRespVO respVO = new MesFrontlineRouteProcessRespVO();
        respVO.setRouteId(candidate.routeId());
        respVO.setRouteCode(candidate.routeCode());
        respVO.setRouteName(candidate.routeName());
        respVO.setRouteProcessId(candidate.routeProcessId());
        respVO.setProcessId(candidate.processId());
        respVO.setProcessCode(candidate.processCode());
        respVO.setProcessName(candidate.processName());
        respVO.setSort(candidate.sort());
        respVO.setDeviceId(candidate.deviceId());
        respVO.setDeviceCode(candidate.deviceCode());
        respVO.setDeviceName(candidate.deviceName());
        respVO.setWorkstationId(candidate.workstationId());
        respVO.setWorkstationCode(candidate.workstationCode());
        respVO.setWorkstationName(candidate.workstationName());
        return respVO;
    }

    private static MesFrontlineEmployeeCandidateRespVO toEmployeeCandidateRespVO(MesFrontlineEmployeeCandidate candidate) {
        MesFrontlineEmployeeCandidateRespVO respVO = new MesFrontlineEmployeeCandidateRespVO();
        respVO.setUserId(candidate.userId());
        respVO.setUsername(candidate.username());
        respVO.setNickname(candidate.nickname());
        return respVO;
    }

    private static MesFrontlineSwitchEmployeeRespVO toSwitchEmployeeRespVO(MesFrontlineEmployeeSwitchResult result) {
        MesFrontlineSwitchEmployeeRespVO respVO = new MesFrontlineSwitchEmployeeRespVO();
        respVO.setLoginUserId(result.loginUserId());
        respVO.setActualEmployeeId(result.actualEmployeeId());
        respVO.setRouteId(result.routeId());
        respVO.setRouteProcessId(result.routeProcessId());
        respVO.setProcessId(result.processId());
        respVO.setExtraVerificationRequired(result.extraVerificationRequired());
        respVO.setTemplate(toTemplateRespVO(result.template()));
        return respVO;
    }

    private static MesFrontlineTemplateRespVO toTemplateRespVO(MesFrontlineTemplateDescriptor template) {
        MesFrontlineTemplateRespVO respVO = new MesFrontlineTemplateRespVO();
        respVO.setTemplateNo(template.templateNo());
        respVO.setTemplateType(template.templateType());
        respVO.setRouteProcessId(template.routeProcessId());
        respVO.setProcessId(template.processId());
        respVO.setActualEmployeeId(template.actualEmployeeId());
        return respVO;
    }

}
