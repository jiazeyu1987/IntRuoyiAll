package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPrintTaskConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPrintTaskCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPrintTaskMarkFailedReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPrintTaskPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPrintTaskRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPrintExportAuditRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPrintHistoryCopyReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPrintHistoryCopyRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPrintHistoryExportReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReprintApplyReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReprintRequestRespVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrLabelPrintService;
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

@Tag(name = "管理后台 - MES eDHR 打印任务")
@RestController
@RequestMapping("/mes/pro/edhr-print-task")
@Validated
public class MesProEdhrPrintTaskController {

    @Resource
    private MesProEdhrLabelPrintService labelPrintService;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-print-task:query')")
    public CommonResult<PageResult<MesProEdhrPrintTaskRespVO>> getPage(
            @Valid MesProEdhrPrintTaskPageReqVO reqVO) {
        return success(labelPrintService.getPrintTaskPage(reqVO));
    }

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-print-task:create')")
    public CommonResult<MesProEdhrPrintTaskRespVO> create(
            @Valid @RequestBody MesProEdhrPrintTaskCreateReqVO reqVO) {
        return success(labelPrintService.createPrintTask(reqVO));
    }

    @PostMapping("/mark-failed")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-print-task:mark-failed')")
    public CommonResult<MesProEdhrPrintTaskRespVO> markFailed(
            @Valid @RequestBody MesProEdhrPrintTaskMarkFailedReqVO reqVO) {
        return success(labelPrintService.markPrintTaskFailed(reqVO));
    }

    @PostMapping("/confirm")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-print-task:confirm')")
    public CommonResult<MesProEdhrPrintTaskRespVO> confirm(
            @Valid @RequestBody MesProEdhrPrintTaskConfirmReqVO reqVO) {
        return success(labelPrintService.confirmPrintTask(reqVO));
    }

    @PostMapping("/reprint/apply")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-print-task:reprint')")
    public CommonResult<MesProEdhrReprintRequestRespVO> applyReprint(
            @Valid @RequestBody MesProEdhrReprintApplyReqVO reqVO) {
        return success(labelPrintService.applyReprint(reqVO));
    }

    @PostMapping("/history-copy")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-print-task:history-copy')")
    public CommonResult<MesProEdhrPrintHistoryCopyRespVO> createVoidHistoryCopy(
            @Valid @RequestBody MesProEdhrPrintHistoryCopyReqVO reqVO) {
        return success(labelPrintService.createVoidHistoryCopy(reqVO));
    }

    @PostMapping("/export-history")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-print-task:export')")
    public CommonResult<MesProEdhrPrintExportAuditRespVO> exportPrintHistory(
            @Valid @RequestBody MesProEdhrPrintHistoryExportReqVO reqVO) {
        return success(labelPrintService.exportPrintHistory(reqVO));
    }
}
