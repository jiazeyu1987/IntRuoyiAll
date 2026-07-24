package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormFillLogDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormFillLogPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormFillLogPageRespVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrFormFillLogService;
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
}
