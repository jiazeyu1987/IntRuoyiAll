package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReportExportAuditPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReportExportAuditReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReportExportAuditRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReportQueryReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReportQueryRespVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReportService;
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

@Tag(name = "管理后台 - MES eDHR 报表查询")
@RestController
@RequestMapping("/mes/pro/edhr-report-query")
@Validated
public class MesProEdhrReportQueryController {

    @Resource
    private MesProEdhrReportService reportService;

    @PostMapping("/run")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-report:query')")
    public CommonResult<MesProEdhrReportQueryRespVO> runReportQuery(
            @Valid @RequestBody MesProEdhrReportQueryReqVO reqVO) {
        return success(reportService.runReportQuery(reqVO));
    }

    @PostMapping("/export-audit")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-report:export')")
    public CommonResult<MesProEdhrReportExportAuditRespVO> recordExportAudit(
            @Valid @RequestBody MesProEdhrReportExportAuditReqVO reqVO) {
        return success(reportService.recordExportAudit(reqVO));
    }

    @GetMapping("/export-audit/page")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-report:query')")
    public CommonResult<PageResult<MesProEdhrReportExportAuditRespVO>> getExportAuditPage(
            @Valid MesProEdhrReportExportAuditPageReqVO reqVO) {
        return success(reportService.getExportAuditPage(reqVO));
    }
}
