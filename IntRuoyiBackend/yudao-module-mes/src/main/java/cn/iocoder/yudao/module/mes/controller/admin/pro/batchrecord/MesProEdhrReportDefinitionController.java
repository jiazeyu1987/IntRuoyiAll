package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReportDefinitionPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReportDefinitionRespVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - MES eDHR 报表定义")
@RestController
@RequestMapping("/mes/pro/edhr-report-definition")
@Validated
public class MesProEdhrReportDefinitionController {

    @Resource
    private MesProEdhrReportService reportService;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-report:query')")
    public CommonResult<PageResult<MesProEdhrReportDefinitionRespVO>> getPage(
            @Valid MesProEdhrReportDefinitionPageReqVO reqVO) {
        return success(reportService.getDefinitionPage(reqVO));
    }

    @GetMapping("/detail")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-report:query')")
    public CommonResult<MesProEdhrReportDefinitionRespVO> getDetail(@RequestParam("id") Long id) {
        return success(reportService.getDefinitionDetail(id));
    }
}
