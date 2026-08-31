package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesFrontlineActiveOrderRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesFrontlineActiveOrderProcessRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesFrontlineEmployeeCandidateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesFrontlinePqcSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesFrontlinePqcSubmitRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesFrontlinePqcSwitchEmployeeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesFrontlinePqcProcessRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesFrontlinePqcSwitchEmployeeRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesFrontlineRuntimeConfigRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesFrontlineRouteProcessRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesFrontlineSwitchEmployeeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesFrontlineSwitchEmployeeRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesFrontlineTemplateRespVO;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineActiveOrderCandidate;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineActiveOrderProcess;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineActiveOrderProcessService;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineDeviceAccountContextService;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineEmployeeCandidate;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineEmployeeSwitchCommand;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineEmployeeSwitchResult;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineEmployeeSwitchService;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlinePqcContextService;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlinePqcEmployeeSwitchResult;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlinePqcInspectionItem;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlinePqcSubmitCommand;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlinePqcSubmitResult;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlinePqcTaskOption;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineProductionSubmitCandidate;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineProductionSubmitContext;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineRuntimeConfig;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineRuntimeConfigService;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineRouteProcessCandidate;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineTemplateDescriptor;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderRow;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderService;
import cn.iocoder.yudao.module.mes.service.pro.frontline.template.FrontlineTemplateCodes;
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
    @Resource
    private MesFrontlineRuntimeConfigService runtimeConfigService;
    @Resource
    private MesTeamLeaderActiveOrderService activeOrderService;
    @Resource
    private MesFrontlineActiveOrderProcessService activeOrderProcessService;

    @GetMapping("/processes")
    @Operation(summary = "获得设备账号可切换工序")
    @PreAuthorize("@ss.hasPermission('mes:pro-feedback:query')")
    public CommonResult<List<MesFrontlineRouteProcessRespVO>> getSwitchableProcesses() {
        return success(contextService.listSwitchableProcesses(getLoginUserId()).stream()
                .map(MesFrontlineDeviceAccountController::toRouteProcessRespVO)
                .toList());
    }

    @GetMapping("/active-orders")
    @Operation(summary = "获得当前生产组长维护的一线生产活跃订单")
    @PreAuthorize("@ss.hasPermission('mes:pro-feedback:query')")
    public CommonResult<List<MesFrontlineActiveOrderRespVO>> getProductionActiveOrders() {
        Long leaderUserId = contextService.resolveResponsibleLeaderUserId(getLoginUserId());
        return success(activeOrderService.listActiveOrders(leaderUserId).stream()
                .map(MesFrontlineDeviceAccountController::toProductionActiveOrderRespVO)
                .toList());
    }

    @GetMapping("/active-order/processes")
    @Operation(summary = "获得一线生产活跃订单冻结工序")
    @PreAuthorize("@ss.hasPermission('mes:pro-feedback:query')")
    public CommonResult<List<MesFrontlineActiveOrderProcessRespVO>> getProductionActiveOrderProcesses(
            @RequestParam("activeOrderId") @NotNull Long activeOrderId) {
        Long leaderUserId = contextService.resolveResponsibleLeaderUserId(getLoginUserId());
        return success(activeOrderProcessService.listProcesses(leaderUserId, activeOrderId).stream()
                .map(MesFrontlineDeviceAccountController::toProductionActiveOrderProcessRespVO)
                .toList());
    }

    @GetMapping("/employee-candidates")
    @Operation(summary = "获得当前工序可切换员工")
    @PreAuthorize("@ss.hasPermission('mes:pro-feedback:query')")
    public CommonResult<List<MesFrontlineEmployeeCandidateRespVO>> getEmployeeCandidates(
            @RequestParam(value = "activeOrderId", required = false) Long activeOrderId,
            @RequestParam("routeId") @NotNull Long routeId,
            @RequestParam("routeProcessId") @NotNull Long routeProcessId,
            @RequestParam("processId") @NotNull Long processId) {
        return success(contextService.listEmployeeCandidates(getLoginUserId(), activeOrderId,
                        routeId, routeProcessId, processId).stream()
                .map(MesFrontlineDeviceAccountController::toEmployeeCandidateRespVO)
                .toList());
    }

    @GetMapping("/runtime-config")
    @Operation(summary = "获得员工填报运行态配置")
    @PreAuthorize("@ss.hasPermission('mes:pro-feedback:query')")
    public CommonResult<MesFrontlineRuntimeConfigRespVO> getRuntimeConfig(
            @RequestParam(value = "activeOrderId", required = false) Long activeOrderId,
            @RequestParam("routeId") @NotNull Long routeId,
            @RequestParam("routeProcessId") @NotNull Long routeProcessId,
            @RequestParam("processId") @NotNull Long processId) {
        return success(toRuntimeConfigRespVO(runtimeConfigService.getRuntimeConfig(getLoginUserId(),
                activeOrderId, routeId, routeProcessId, processId)));
    }

    @PostMapping("/switch-employee")
    @Operation(summary = "切换当前工序实际填写员工")
    @PreAuthorize("@ss.hasPermission('mes:pro-feedback:create')")
    public CommonResult<MesFrontlineSwitchEmployeeRespVO> switchActualEmployee(
            @Valid @RequestBody MesFrontlineSwitchEmployeeReqVO reqVO) {
        MesFrontlineEmployeeSwitchResult result = employeeSwitchService.switchActualEmployee(
                new MesFrontlineEmployeeSwitchCommand(getLoginUserId(), reqVO.getActiveOrderId(), reqVO.getRouteId(),
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
    @Operation(summary = "获得 PQC 活跃订单所属 DCC 项目的 QA 工序")
    @PreAuthorize("@ss.hasPermission('mes:pro-feedback:query')")
    public CommonResult<List<MesFrontlinePqcProcessRespVO>> getPqcActiveOrderProcesses(
            @RequestParam("activeOrderId") @NotNull Long activeOrderId,
            @RequestParam(value = "actualEmployeeId", required = false) Long actualEmployeeId) {
        return success(pqcContextService.listProcessesByActiveOrder(activeOrderId, getLoginUserId(),
                actualEmployeeId));
    }

    @GetMapping("/pqc/personnel")
    @Operation(summary = "获得 PQC 员工和 PQC 组长")
    @PreAuthorize("@ss.hasPermission('mes:pro-feedback:query')")
    public CommonResult<List<MesFrontlineEmployeeCandidateRespVO>> getPqcPersonnel() {
        return success(pqcContextService.listPqcEmployeeCandidates(getLoginUserId()).stream()
                .map(MesFrontlineDeviceAccountController::toEmployeeCandidateRespVO)
                .toList());
    }

    @PostMapping("/pqc/switch-employee")
    @Operation(summary = "PQC 切换实际填写员工")
    @PreAuthorize("@ss.hasPermission('mes:pro-feedback:create')")
    public CommonResult<MesFrontlinePqcSwitchEmployeeRespVO> switchPqcActualEmployee(
            @Valid @RequestBody MesFrontlinePqcSwitchEmployeeReqVO reqVO) {
        MesFrontlinePqcEmployeeSwitchResult result = pqcContextService.switchPqcActualEmployee(getLoginUserId(),
                reqVO.getActiveOrderId(), reqVO.getRegulationVersionId(), reqVO.getQaProcessId(),
                reqVO.getPqcTaskId(), reqVO.getActualEmployeeId());
        return success(toPqcSwitchEmployeeRespVO(result));
    }

    @GetMapping("/pqc/submit-receipt")
    @Operation(summary = "只读确认 PQC 正式提交回执")
    @PreAuthorize("@ss.hasPermission('mes:pro-feedback:query')")
    public CommonResult<MesFrontlinePqcSubmitRespVO> getPqcSubmitReceipt(
            @RequestParam("pqcTaskId") @NotNull Long pqcTaskId) {
        return success(pqcContextService.getSubmittedPqcInspection(getLoginUserId(), pqcTaskId)
                .map(MesFrontlineDeviceAccountController::toPqcSubmitRespVO)
                .orElse(null));
    }

    @PostMapping("/pqc/submit")
    @Operation(summary = "PQC 检验提交到工序池")
    @PreAuthorize("@ss.hasPermission('mes:pro-feedback:create')")
    public CommonResult<MesFrontlinePqcSubmitRespVO> submitPqcInspection(
            @Valid @RequestBody MesFrontlinePqcSubmitReqVO reqVO) {
        Long loginUserId = getLoginUserId();
        MesFrontlinePqcSubmitResult result = pqcContextService.submitPqcInspection(loginUserId,
                MesFrontlinePqcSubmitCommand.builder()
                        .activeOrderId(reqVO.getActiveOrderId())
                        .pqcTaskId(reqVO.getPqcTaskId())
                        .regulationVersionId(reqVO.getRegulationVersionId())
                        .workOrderId(reqVO.getWorkOrderId())
                        .routeId(reqVO.getRouteId())
                        .qaProcessId(reqVO.getQaProcessId())
                        .inspectionType(reqVO.getInspectionType())
                        .businessDate(reqVO.getBusinessDate())
                        .shiftCode(reqVO.getShiftCode())
                        .roundNo(reqVO.getRoundNo())
                        .actualInspectionQuantity(reqVO.getActualInspectionQuantity())
                        .actualEmployeeId(reqVO.getActualEmployeeId())
                        .signaturePassword(reqVO.getSignaturePassword())
                        .templateType(FrontlineTemplateCodes.PQC_SIMPLIFIED)
                        .scrapQuantity(reqVO.getScrapQuantity())
                        .itemResults(reqVO.getItemResults().stream()
                                .map(MesFrontlineDeviceAccountController::toPqcItemResultCommand)
                                .toList())
                        .rawPayload(reqVO.getRawPayload())
                        .clientSubmitTime(reqVO.getClientSubmitTime())
                        .build());
        return success(toPqcSubmitRespVO(result));
    }

    private static MesFrontlineActiveOrderRespVO toActiveOrderRespVO(MesFrontlineActiveOrderCandidate candidate) {
        MesFrontlineActiveOrderRespVO respVO = new MesFrontlineActiveOrderRespVO();
        respVO.setActiveOrderId(candidate.activeOrderId());
        respVO.setWorkOrderId(candidate.workOrderId());
        respVO.setWorkOrderCode(candidate.workOrderCode());
        respVO.setWorkOrderName(candidate.workOrderName());
        respVO.setProductId(candidate.productId());
        respVO.setProductCode(candidate.productCode());
        respVO.setProductName(candidate.productName());
        respVO.setBatchCode(candidate.batchCode());
        respVO.setQuantity(candidate.quantity());
        respVO.setRouteId(candidate.routeId());
        respVO.setRouteCode(candidate.routeCode());
        respVO.setRouteName(candidate.routeName());
        respVO.setLatestSubmitTime(candidate.latestSubmitTime());
        return respVO;
    }

    private static MesFrontlineActiveOrderRespVO toProductionActiveOrderRespVO(
            MesTeamLeaderActiveOrderRow activeOrder) {
        return new MesFrontlineActiveOrderRespVO()
                .setActiveOrderId(activeOrder.getId())
                .setWorkOrderId(activeOrder.getWorkOrderId())
                .setWorkOrderCode(activeOrder.getWorkOrderCode())
                .setProductId(activeOrder.getProductId())
                .setProductCode(activeOrder.getProductCode())
                .setProductName(activeOrder.getProductName())
                .setBatchCode(activeOrder.getBatchCode())
                .setQuantity(activeOrder.getErpFixedQuantitySnapshot() == null
                        ? activeOrder.getQuantity() : activeOrder.getErpFixedQuantitySnapshot())
                .setRouteId(activeOrder.getRouteId())
                .setRouteName(activeOrder.getRouteName())
                .setLatestSubmitTime(activeOrder.getJoinedAt());
    }

    private static MesFrontlineActiveOrderProcessRespVO toProductionActiveOrderProcessRespVO(
            MesFrontlineActiveOrderProcess process) {
        MesFrontlineActiveOrderProcessRespVO respVO = new MesFrontlineActiveOrderProcessRespVO();
        respVO.setActiveOrderId(process.activeOrderId());
        respVO.setRouteId(process.routeId());
        respVO.setRouteVersionId(process.routeVersionId());
        respVO.setRouteCode(process.routeCode());
        respVO.setRouteName(process.routeName());
        respVO.setRouteProcessId(process.routeProcessId());
        respVO.setProcessId(process.processId());
        respVO.setProcessCode(process.processCode());
        respVO.setProcessName(process.processName());
        respVO.setSort(process.sort());
        respVO.setWorkstationId(process.workstationId());
        respVO.setWorkstationCode(process.workstationCode());
        respVO.setWorkstationName(process.workstationName());
        respVO.setProductionQuantityFactor(process.productionQuantityFactor());
        respVO.setTargetQuantity(process.targetQuantity());
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
        respVO.setActiveOrderId(candidate.activeOrderId());
        respVO.setPqcTaskId(candidate.pqcTaskId());
        respVO.setRegulationVersionId(candidate.regulationVersionId());
        respVO.setFinalInspectionApplicable(candidate.finalInspectionApplicable());
        respVO.setInspectionType(candidate.inspectionType());
        respVO.setBusinessDate(candidate.businessDate());
        respVO.setShiftCode(candidate.shiftCode());
        respVO.setRoundNo(candidate.roundNo());
        respVO.setPlannedInspectionQuantity(candidate.plannedInspectionQuantity());
        respVO.setInspectionItems(candidate.inspectionItems().stream()
                .map(MesFrontlineDeviceAccountController::toPqcInspectionItemRespVO)
                .toList());
        respVO.setPqcTaskOptions(candidate.pqcTaskOptions().stream()
                .map(MesFrontlineDeviceAccountController::toPqcTaskOptionRespVO)
                .toList());
        respVO.setProductionSubmitCandidates(candidate.productionSubmitCandidates().stream()
                .map(MesFrontlineDeviceAccountController::toProductionSubmitCandidateRespVO)
                .toList());
        return respVO;
    }

    private static MesFrontlineRouteProcessRespVO.PqcTaskOption toPqcTaskOptionRespVO(
            MesFrontlinePqcTaskOption option) {
        MesFrontlineRouteProcessRespVO.PqcTaskOption respVO =
                new MesFrontlineRouteProcessRespVO.PqcTaskOption();
        respVO.setPqcTaskId(option.pqcTaskId());
        respVO.setRegulationVersionId(option.regulationVersionId());
        respVO.setFinalInspectionApplicable(option.finalInspectionApplicable());
        respVO.setInspectionType(option.inspectionType());
        respVO.setBusinessDate(option.businessDate());
        respVO.setShiftCode(option.shiftCode());
        respVO.setRoundNo(option.roundNo());
        respVO.setPlannedInspectionQuantity(option.plannedInspectionQuantity());
        respVO.setInspectionItems(option.inspectionItems().stream()
                .map(MesFrontlineDeviceAccountController::toPqcInspectionItemRespVO)
                .toList());
        return respVO;
    }

    private static MesFrontlineRouteProcessRespVO.ProductionSubmitCandidate toProductionSubmitCandidateRespVO(
            MesFrontlineProductionSubmitCandidate candidate) {
        MesFrontlineRouteProcessRespVO.ProductionSubmitCandidate respVO =
                new MesFrontlineRouteProcessRespVO.ProductionSubmitCandidate();
        respVO.setEventId(candidate.eventId());
        respVO.setServerSubmitTime(candidate.serverSubmitTime());
        return respVO;
    }

    private static MesFrontlinePqcSubmitRespVO toPqcSubmitRespVO(MesFrontlinePqcSubmitResult result) {
        MesFrontlinePqcSubmitRespVO respVO = new MesFrontlinePqcSubmitRespVO();
        respVO.setPqcTaskId(result.pqcTaskId());
        respVO.setPqcEventId(result.pqcEventId());
        respVO.setSourceRevision(result.sourceRevision());
        respVO.setPayloadHash(result.payloadHash());
        respVO.setPqcRecordId(result.pqcRecordId());
        respVO.setSignatureId(result.signatureId());
        respVO.setInspectionResult(result.inspectionResult());
        respVO.setServerSubmitTime(result.serverSubmitTime());
        return respVO;
    }

    private static MesFrontlineRouteProcessRespVO.PqcInspectionItem toPqcInspectionItemRespVO(
            MesFrontlinePqcInspectionItem item) {
        MesFrontlineRouteProcessRespVO.PqcInspectionItem respVO =
                new MesFrontlineRouteProcessRespVO.PqcInspectionItem();
        respVO.setItemCode(item.itemCode());
        respVO.setItemName(item.itemName());
        respVO.setInspectionMethod(item.inspectionMethod());
        respVO.setStandardText(item.standardText());
        respVO.setAcceptanceStandard(item.standardText());
        respVO.setProcessInspectionMethod(item.inspectionMethod());
        respVO.setInspectionTool(item.inspectionTool());
        respVO.setSamplingPlanText(item.samplingPlanText());
        respVO.setStandardLowerLimit(item.standardLowerLimit());
        respVO.setStandardUpperLimit(item.standardUpperLimit());
        respVO.setStandardUnit(item.standardUnit());
        respVO.setStandardPrecision(item.standardPrecision());
        respVO.setEquipmentRequired(item.equipmentRequired());
        respVO.setResultType(item.resultType());
        respVO.setEquipmentOptions(item.equipmentOptions().stream()
                .map(MesFrontlineDeviceAccountController::toPqcEquipmentOptionRespVO)
                .toList());
        return respVO;
    }

    private static MesFrontlineRouteProcessRespVO.PqcEquipmentOption toPqcEquipmentOptionRespVO(
            MesFrontlinePqcInspectionItem.EquipmentOption option) {
        MesFrontlineRouteProcessRespVO.PqcEquipmentOption respVO =
                new MesFrontlineRouteProcessRespVO.PqcEquipmentOption();
        respVO.setEquipmentId(option.equipmentId());
        respVO.setEquipmentCode(option.equipmentCode());
        respVO.setEquipmentName(option.equipmentName());
        respVO.setEquipmentNumber(option.equipmentNumber());
        respVO.setDefaultFlag(option.defaultFlag());
        respVO.setSort(option.sort());
        return respVO;
    }

    private static MesFrontlinePqcSubmitCommand.ItemResult toPqcItemResultCommand(
            MesFrontlinePqcSubmitReqVO.ItemResult item) {
        return MesFrontlinePqcSubmitCommand.ItemResult.builder()
                .itemCode(item.getItemCode())
                .selectedEquipmentId(item.getSelectedEquipmentId())
                .selectedEquipmentNumber(item.getSelectedEquipmentNumber())
                .sampleValues(item.getSampleValues())
                .build();
    }

    private static MesFrontlineEmployeeCandidateRespVO toEmployeeCandidateRespVO(MesFrontlineEmployeeCandidate candidate) {
        MesFrontlineEmployeeCandidateRespVO respVO = new MesFrontlineEmployeeCandidateRespVO();
        respVO.setUserId(candidate.userId());
        respVO.setUsername(candidate.username());
        respVO.setNickname(candidate.nickname());
        return respVO;
    }

    private static MesFrontlineRuntimeConfigRespVO toRuntimeConfigRespVO(MesFrontlineRuntimeConfig config) {
        MesFrontlineRuntimeConfigRespVO respVO = new MesFrontlineRuntimeConfigRespVO();
        respVO.setRouteId(config.routeId());
        respVO.setRouteProcessId(config.routeProcessId());
        respVO.setProcessId(config.processId());
        respVO.setEmployees(config.employees().stream().map(employee -> {
            MesFrontlineRuntimeConfigRespVO.Employee item = new MesFrontlineRuntimeConfigRespVO.Employee();
            item.setEmployeeProfileId(employee.employeeProfileId());
            item.setSystemUserId(employee.systemUserId());
            item.setEmployeeCode(employee.employeeCode());
            item.setEmployeeName(employee.employeeName());
            item.setDisplayName(employee.displayName());
            item.setEmployeeType(employee.employeeType());
            return item;
        }).toList());
        respVO.setDevices(config.devices().stream().map(device -> {
            MesFrontlineRuntimeConfigRespVO.Device item = new MesFrontlineRuntimeConfigRespVO.Device();
            item.setDeviceId(device.deviceId());
            item.setDeviceCode(device.deviceCode());
            item.setDeviceName(device.deviceName());
            item.setDeviceStatus(device.deviceStatus());
            item.setParameters(device.parameters().stream().map(parameter -> {
                MesFrontlineRuntimeConfigRespVO.DeviceParameter parameterItem =
                        new MesFrontlineRuntimeConfigRespVO.DeviceParameter();
                parameterItem.setParameterCode(parameter.parameterCode());
                parameterItem.setParameterName(parameter.parameterName());
                parameterItem.setUnit(parameter.unit());
                parameterItem.setLowerLimit(parameter.lowerLimit());
                parameterItem.setUpperLimit(parameter.upperLimit());
                parameterItem.setDefaultValue(parameter.defaultValue());
                parameterItem.setValueType(parameter.valueType());
                parameterItem.setStandardText(parameter.standardText());
                parameterItem.setOptionValues(parameter.optionValues());
                parameterItem.setDefaultText(parameter.defaultText());
                parameterItem.setDecimalScale(parameter.decimalScale());
                return parameterItem;
            }).toList());
            return item;
        }).toList());
        respVO.setDefectReasons(config.defectReasons().stream().map(reason -> {
            MesFrontlineRuntimeConfigRespVO.DefectReason item = new MesFrontlineRuntimeConfigRespVO.DefectReason();
            item.setReasonId(reason.reasonId());
            item.setReasonType(reason.reasonType());
            item.setReasonCode(reason.reasonCode());
            item.setReasonName(reason.reasonName());
            return item;
        }).toList());
        respVO.setMaterials(config.materials().stream().map(material -> {
            MesFrontlineRuntimeConfigRespVO.Material item = new MesFrontlineRuntimeConfigRespVO.Material();
            item.setMaterialId(material.materialId());
            item.setMaterialCode(material.materialCode());
            item.setMaterialName(material.materialName());
            item.setMaterialSpecification(material.materialSpecification());
            item.setBomQuantity(material.bomQuantity());
            item.setBatchCodes(material.batchCodes());
            return item;
        }).toList());
        respVO.setProductionSubmitContext(toProductionSubmitContextRespVO(config.productionSubmitContext()));
        respVO.setEmployeeSwitchSnapshots(config.employeeSwitchSnapshots().stream()
                .map(MesFrontlineDeviceAccountController::toSwitchEmployeeRespVO)
                .toList());
        respVO.setFrontlineSessionSnapshotId(config.frontlineSessionSnapshotId());
        respVO.setFrontlineSessionSnapshotHash(config.frontlineSessionSnapshotHash());
        return respVO;
    }

    private static MesFrontlineRuntimeConfigRespVO.ProductionSubmitContext toProductionSubmitContextRespVO(
            MesFrontlineProductionSubmitContext context) {
        if (context == null) {
            return null;
        }
        MesFrontlineRuntimeConfigRespVO.ProductionSubmitContext item =
                new MesFrontlineRuntimeConfigRespVO.ProductionSubmitContext();
        item.setWorkOrderId(context.workOrderId());
        item.setWorkOrderCode(context.workOrderCode());
        item.setWorkOrderName(context.workOrderName());
        item.setTaskId(context.taskId());
        item.setRouteId(context.routeId());
        item.setRouteProcessId(context.routeProcessId());
        item.setProcessId(context.processId());
        item.setWorkstationId(context.workstationId());
        item.setItemId(context.itemId());
        item.setApproveUserId(context.approveUserId());
        item.setRecordbookId(context.recordbookId());
        item.setScheduledQuantity(context.scheduledQuantity());
        item.setExpireDate(context.expireDate());
        item.setActiveOrderProcessSnapshotId(context.activeOrderProcessSnapshotId());
        item.setParameterSnapshotSha256(context.parameterSnapshotSha256());
        item.setParameterSnapshotState(context.parameterSnapshotState());
        return item;
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

    private static MesFrontlinePqcSwitchEmployeeRespVO toPqcSwitchEmployeeRespVO(
            MesFrontlinePqcEmployeeSwitchResult result) {
        MesFrontlinePqcSwitchEmployeeRespVO respVO = new MesFrontlinePqcSwitchEmployeeRespVO();
        respVO.setLoginUserId(result.loginUserId());
        respVO.setActualEmployeeId(result.actualEmployeeId());
        respVO.setRouteId(result.routeId());
        respVO.setDccProjectCodeId(result.dccProjectCodeId());
        respVO.setRegulationVersionId(result.regulationVersionId());
        respVO.setQaProcessId(result.qaProcessId());
        respVO.setExtraVerificationRequired(result.extraVerificationRequired());
        MesFrontlinePqcSwitchEmployeeRespVO.PqcTemplate template =
                new MesFrontlinePqcSwitchEmployeeRespVO.PqcTemplate();
        template.setTemplateNo(result.template().templateNo());
        template.setTemplateType(result.template().templateType());
        template.setQaProcessId(result.template().qaProcessId());
        template.setActualEmployeeId(result.template().actualEmployeeId());
        respVO.setTemplate(template);
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
