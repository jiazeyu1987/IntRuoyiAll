package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrBatchRecordFormPermissionRuleSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrProcessFormPermissionRuleRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrProcessFormPermissionRuleSaveReqVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrProcessFormPermissionRuleService;
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

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - MES eDHR 工序表单权限规则")
@RestController
@RequestMapping("/mes/pro/edhr-process-form-permission-rule")
@Validated
public class MesProEdhrProcessFormPermissionRuleController {

    @Resource
    private MesProEdhrProcessFormPermissionRuleService processFormPermissionRuleService;

    @GetMapping("/get")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-process-form-permission-rule:query')")
    public CommonResult<MesProEdhrProcessFormPermissionRuleRespVO> getRule(
            @RequestParam("routeProcessId") Long routeProcessId,
            @RequestParam("batchRecordReportId") String batchRecordReportId) {
        return success(processFormPermissionRuleService.getRule(routeProcessId, batchRecordReportId));
    }

    @GetMapping("/get-by-report")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-process-form-permission-rule:query')")
    public CommonResult<MesProEdhrProcessFormPermissionRuleRespVO> getRuleByReport(
            @RequestParam("batchRecordReportId") String batchRecordReportId) {
        return success(processFormPermissionRuleService.getRuleByReport(batchRecordReportId));
    }

    @PostMapping("/save")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-process-form-permission-rule:update')")
    public CommonResult<MesProEdhrProcessFormPermissionRuleRespVO> saveRule(
            @Valid @RequestBody MesProEdhrProcessFormPermissionRuleSaveReqVO reqVO) {
        return success(processFormPermissionRuleService.saveRule(reqVO));
    }

    @PostMapping("/save-by-report")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-process-form-permission-rule:update')")
    public CommonResult<MesProEdhrProcessFormPermissionRuleRespVO> saveRuleByReport(
            @Valid @RequestBody MesProEdhrBatchRecordFormPermissionRuleSaveReqVO reqVO) {
        return success(processFormPermissionRuleService.saveRuleByReport(reqVO));
    }
}
