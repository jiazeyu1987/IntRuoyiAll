package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFlowEventPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFlowEventRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFlowInterventionAddSignReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFlowInterventionAdminReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFlowInterventionPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFlowInterventionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFlowInterventionReturnReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFlowInterventionTransferReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFlowInterventionWithdrawReqVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrFlowInterventionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - eDHR流程干预管理")
@RestController
@RequestMapping("/mes/pro/edhr-flow-intervention")
@Validated
public class MesProEdhrFlowInterventionController {

    @Resource
    private MesProEdhrFlowInterventionService flowInterventionService;

    @GetMapping("/page")
    @Operation(summary = "分页查询 eDHR 流程干预")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-flow-intervention:query')")
    public CommonResult<PageResult<MesProEdhrFlowInterventionRespVO>> getPage(
            @Valid MesProEdhrFlowInterventionPageReqVO reqVO) {
        return success(flowInterventionService.getPage(reqVO));
    }

    @GetMapping("/event/page")
    @Operation(summary = "分页查询 eDHR 流程日志")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-flow-intervention:event-query')")
    public CommonResult<PageResult<MesProEdhrFlowEventRespVO>> getEventPage(
            @Valid MesProEdhrFlowEventPageReqVO reqVO) {
        return success(flowInterventionService.getEventPage(reqVO));
    }

    @PostMapping("/return")
    @Operation(summary = "记录 eDHR 流程退回干预")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-flow-intervention:return')")
    public CommonResult<MesProEdhrFlowInterventionRespVO> returnBack(
            @Valid @RequestBody MesProEdhrFlowInterventionReturnReqVO reqVO) {
        return success(flowInterventionService.returnBack(reqVO));
    }

    @PostMapping("/withdraw")
    @Operation(summary = "记录 eDHR 流程撤回干预")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-flow-intervention:withdraw')")
    public CommonResult<MesProEdhrFlowInterventionRespVO> withdraw(
            @Valid @RequestBody MesProEdhrFlowInterventionWithdrawReqVO reqVO) {
        return success(flowInterventionService.withdraw(reqVO));
    }

    @PostMapping("/transfer")
    @Operation(summary = "记录 eDHR 流程转办干预")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-flow-intervention:transfer')")
    public CommonResult<MesProEdhrFlowInterventionRespVO> transfer(
            @Valid @RequestBody MesProEdhrFlowInterventionTransferReqVO reqVO) {
        return success(flowInterventionService.transfer(reqVO));
    }

    @PostMapping("/add-sign")
    @Operation(summary = "记录 eDHR 流程加签干预")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-flow-intervention:add-sign')")
    public CommonResult<MesProEdhrFlowInterventionRespVO> addSign(
            @Valid @RequestBody MesProEdhrFlowInterventionAddSignReqVO reqVO) {
        return success(flowInterventionService.addSign(reqVO));
    }

    @PostMapping("/admin-intervene")
    @Operation(summary = "记录 eDHR 管理员流程干预")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-flow-intervention:admin-intervene')")
    public CommonResult<MesProEdhrFlowInterventionRespVO> adminIntervene(
            @Valid @RequestBody MesProEdhrFlowInterventionAdminReqVO reqVO) {
        return success(flowInterventionService.adminIntervene(reqVO));
    }
}
