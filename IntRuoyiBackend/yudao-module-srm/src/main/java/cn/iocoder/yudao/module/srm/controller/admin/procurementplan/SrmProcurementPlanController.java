package cn.iocoder.yudao.module.srm.controller.admin.procurementplan;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.srm.controller.admin.procurementplan.vo.*;
import cn.iocoder.yudao.module.srm.service.procurement.SrmProcurementPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - SRM 采购计划")
@RestController
@RequestMapping("/srm/procurement-plan")
@Validated
public class SrmProcurementPlanController {

    @Resource
    private SrmProcurementPlanService procurementPlanService;

    @PostMapping("/create")
    @Operation(summary = "创建采购计划")
    @PreAuthorize("@ss.hasPermission('srm:procurement-plan:create')")
    public CommonResult<Long> createProcurementPlan(@Valid @RequestBody SrmProcurementPlanSaveReqVO createReqVO) {
        return success(procurementPlanService.createProcurementPlan(createReqVO));
    }

    @PutMapping("/submit")
    @Operation(summary = "提交采购计划")
    @PreAuthorize("@ss.hasPermission('srm:procurement-plan:submit')")
    public CommonResult<Boolean> submitProcurementPlan(@RequestParam("id") Long id) {
        procurementPlanService.submitProcurementPlan(id);
        return success(true);
    }

    @PutMapping("/approve")
    @Operation(summary = "通过采购计划")
    @PreAuthorize("@ss.hasPermission('srm:procurement-plan:audit')")
    public CommonResult<Boolean> approveProcurementPlan(@Valid @RequestBody SrmProcurementPlanAuditReqVO auditReqVO) {
        procurementPlanService.approveProcurementPlan(auditReqVO);
        return success(true);
    }

    @PutMapping("/reject")
    @Operation(summary = "驳回采购计划")
    @PreAuthorize("@ss.hasPermission('srm:procurement-plan:audit')")
    public CommonResult<Boolean> rejectProcurementPlan(@Valid @RequestBody SrmProcurementPlanAuditReqVO auditReqVO) {
        procurementPlanService.rejectProcurementPlan(auditReqVO);
        return success(true);
    }

    @PostMapping("/generate-sourcing")
    @Operation(summary = "生成招标或非招标寻源项目")
    @PreAuthorize("@ss.hasPermission('srm:procurement-plan:generate')")
    public CommonResult<SrmSourcingProjectRespVO> generateSourcingProject(@Valid @RequestBody SrmProcurementPlanGenerateReqVO generateReqVO) {
        return success(procurementPlanService.generateSourcingProject(generateReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得采购计划详情")
    @Parameter(name = "id", description = "采购计划编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('srm:procurement-plan:query')")
    public CommonResult<SrmProcurementPlanRespVO> getProcurementPlan(@RequestParam("id") Long id) {
        return success(procurementPlanService.getProcurementPlan(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得采购计划分页")
    @PreAuthorize("@ss.hasPermission('srm:procurement-plan:query')")
    public CommonResult<PageResult<SrmProcurementPlanRespVO>> getProcurementPlanPage(@Valid SrmProcurementPlanPageReqVO pageReqVO) {
        return success(procurementPlanService.getProcurementPlanPage(pageReqVO));
    }
}
