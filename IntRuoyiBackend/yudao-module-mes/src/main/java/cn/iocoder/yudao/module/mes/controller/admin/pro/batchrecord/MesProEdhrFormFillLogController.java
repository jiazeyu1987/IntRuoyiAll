package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormFillLogDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormFillLogPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormFillLogPageRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProProductionReportRevisionLogDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProProductionReportRevisionLogPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProProductionReportRevisionLogPageRespVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrFormFillLogService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolProductionReportRevisionLogBO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolProductionReportRevisionLogService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/mes/pro/batch-record-execution/form-fill-log")
@Validated
public class MesProEdhrFormFillLogController {

    @Resource
    private MesProEdhrFormFillLogService formFillLogService;
    @Resource
    private MesProcessPoolProductionReportRevisionLogService productionReportRevisionLogService;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-form-fill-log:query')")
    public CommonResult<PageResult<MesProEdhrFormFillLogPageRespVO>> getPage(
            @Valid MesProEdhrFormFillLogPageReqVO reqVO) {
        return success(formFillLogService.getPage(reqVO));
    }

    @GetMapping("/detail")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-form-fill-log:query')")
    public CommonResult<MesProEdhrFormFillLogDetailRespVO> getDetail(
            @RequestParam("auditBatchId") Long auditBatchId) {
        return success(formFillLogService.getDetail(auditBatchId));
    }

    @GetMapping("/production-report-revision/page")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-form-fill-log:query')")
    public CommonResult<PageResult<MesProProductionReportRevisionLogPageRespVO>>
    getProductionReportRevisionPage(@Valid MesProProductionReportRevisionLogPageReqVO reqVO) {
        PageResult<MesProcessPoolProductionReportRevisionLogBO> page =
                productionReportRevisionLogService.getProductionReportRevisionPage(
                        reqVO, SecurityFrameworkUtils.getLoginUserId());
        return success(new PageResult<>(page.getList().stream()
                .map(MesProProductionReportRevisionLogPageRespVO::from)
                .toList(), page.getTotal()));
    }

    @GetMapping("/production-report-revision/detail")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-form-fill-log:query')")
    public CommonResult<MesProProductionReportRevisionLogDetailRespVO>
    getProductionReportRevisionDetail(@RequestParam("revisionId") Long revisionId) {
        return success(MesProProductionReportRevisionLogDetailRespVO.from(
                productionReportRevisionLogService.getProductionReportRevisionDetail(
                        revisionId, SecurityFrameworkUtils.getLoginUserId())));
    }
}
