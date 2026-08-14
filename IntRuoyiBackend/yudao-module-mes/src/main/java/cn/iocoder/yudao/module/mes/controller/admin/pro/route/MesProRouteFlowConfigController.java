package cn.iocoder.yudao.module.mes.controller.admin.pro.route;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig.MesProRouteBatchRecordAttachmentOwnerInitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig.MesProRouteBatchRecordAttachmentOwnerRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig.MesProRouteBatchRecordAttachmentOwnerSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig.MesProRouteFlowConfigSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig.MesProRouteFlowProcessConfigRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig.MesProRouteStartProductionLeaderProductionLineRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig.MesProRouteStartProductionLeaderRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig.MesProRouteStartProductionLeaderSaveReqVO;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteFlowConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
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

@Tag(name = "管理后台 - MES 工艺流程配置")
@RestController
@RequestMapping("/mes/pro/route/flow-config")
@Validated
public class MesProRouteFlowConfigController {

    @Resource
    private MesProRouteFlowConfigService routeFlowConfigService;

    @GetMapping
    @Operation(summary = "获得工艺流程工序配置列表")
    @Parameter(name = "routeId", description = "工艺路线编号", required = true)
    @Parameter(name = "useType", description = "用途类型：SCHEDULE/BATCH", required = true)
    @Parameter(name = "routeVersionId", description = "候选路线版本编号")
    @PreAuthorize("@ss.hasAnyPermissions('mes:pro-route:schedule-config:query', 'mes:pro-route:batch-record-config:query')")
    public CommonResult<List<MesProRouteFlowProcessConfigRespVO>> getRouteFlowProcessConfigList(
            @RequestParam("routeId") Long routeId,
            @RequestParam("useType") String useType,
            @RequestParam(value = "routeVersionId", required = false) Long routeVersionId) {
        return success(routeFlowConfigService.getRouteFlowProcessConfigList(routeId, useType, routeVersionId));
    }

    @PostMapping("/schedule/save")
    @Operation(summary = "保存工艺流程排产配置")
    @PreAuthorize("@ss.hasPermission('mes:pro-route:schedule-config:update')")
    public CommonResult<Boolean> saveRouteFlowScheduleConfig(@Valid @RequestBody MesProRouteFlowConfigSaveReqVO saveReqVO) {
        saveReqVO.setUseType("SCHEDULE");
        routeFlowConfigService.saveRouteFlowConfig(saveReqVO);
        return success(true);
    }

    @PostMapping("/batch-record/save")
    @Operation(summary = "保存工艺流程批记录配置")
    @PreAuthorize("@ss.hasPermission('mes:pro-route:batch-record-config:update')")
    public CommonResult<Boolean> saveRouteFlowBatchRecordConfig(@Valid @RequestBody MesProRouteFlowConfigSaveReqVO saveReqVO) {
        saveReqVO.setUseType("BATCH");
        routeFlowConfigService.saveRouteFlowConfig(saveReqVO);
        return success(true);
    }

    @GetMapping("/batch-record-attachment-owners")
    @Operation(summary = "获得工艺路线批记录附件负责人配置")
    @Parameter(name = "routeId", description = "工艺路线编号", required = true)
    @Parameter(name = "routeVersionId", description = "候选路线版本编号")
    @PreAuthorize("@ss.hasPermission('mes:pro-route:batch-record-config:query')")
    public CommonResult<List<MesProRouteBatchRecordAttachmentOwnerRespVO>> getBatchRecordAttachmentOwners(
            @RequestParam("routeId") Long routeId,
            @RequestParam(value = "routeVersionId", required = false) Long routeVersionId) {
        return success(routeFlowConfigService.getBatchRecordAttachmentOwners(routeId, routeVersionId));
    }

    @PostMapping("/batch-record-attachment-owners/init-defaults")
    @Operation(summary = "初始化工艺路线批记录附件负责人默认角色")
    @PreAuthorize("@ss.hasPermission('mes:pro-route:batch-record-config:update')")
    public CommonResult<List<MesProRouteBatchRecordAttachmentOwnerRespVO>> initBatchRecordAttachmentOwners(
            @Valid @RequestBody MesProRouteBatchRecordAttachmentOwnerInitReqVO initReqVO) {
        return success(routeFlowConfigService.initializeBatchRecordAttachmentOwners(initReqVO));
    }

    @PostMapping("/batch-record-attachment-owners/save")
    @Operation(summary = "保存工艺路线批记录附件负责人配置")
    @PreAuthorize("@ss.hasPermission('mes:pro-route:batch-record-config:update')")
    public CommonResult<Boolean> saveBatchRecordAttachmentOwners(
            @Valid @RequestBody MesProRouteBatchRecordAttachmentOwnerSaveReqVO saveReqVO) {
        routeFlowConfigService.saveBatchRecordAttachmentOwners(saveReqVO);
        return success(true);
    }

    @GetMapping("/route-start-production-leader-production-lines")
    @Operation(summary = "获得工艺路线工序开始生产组长可负责范围")
    @Parameter(name = "routeId", description = "工艺路线编号", required = true)
    @Parameter(name = "routeVersionId", description = "候选路线版本编号")
    @PreAuthorize("@ss.hasPermission('mes:pro-route:batch-record-config:query')")
    public CommonResult<List<MesProRouteStartProductionLeaderProductionLineRespVO>>
    getRouteStartProductionLeaderProductionLines(
            @RequestParam("routeId") Long routeId,
            @RequestParam(value = "routeVersionId", required = false) Long routeVersionId) {
        return success(routeFlowConfigService.getRouteStartProductionLeaderProductionLines(routeId, routeVersionId));
    }

    @GetMapping("/route-start-production-leaders")
    @Operation(summary = "获得工艺路线工序开始生产组长配置")
    @Parameter(name = "routeId", description = "工艺路线编号", required = true)
    @Parameter(name = "routeVersionId", description = "候选路线版本编号")
    @PreAuthorize("@ss.hasPermission('mes:pro-route:batch-record-config:query')")
    public CommonResult<List<MesProRouteStartProductionLeaderRespVO>> getRouteStartProductionLeaders(
            @RequestParam("routeId") Long routeId,
            @RequestParam(value = "routeVersionId", required = false) Long routeVersionId) {
        return success(routeFlowConfigService.getRouteStartProductionLeaders(routeId, routeVersionId));
    }

    @PostMapping("/route-start-production-leaders/save")
    @Operation(summary = "保存工艺路线工序开始生产组长配置")
    @PreAuthorize("@ss.hasPermission('mes:pro-route:batch-record-config:update')")
    public CommonResult<Boolean> saveRouteStartProductionLeaders(
            @Valid @RequestBody MesProRouteStartProductionLeaderSaveReqVO saveReqVO) {
        routeFlowConfigService.saveRouteStartProductionLeaders(saveReqVO);
        return success(true);
    }

}
