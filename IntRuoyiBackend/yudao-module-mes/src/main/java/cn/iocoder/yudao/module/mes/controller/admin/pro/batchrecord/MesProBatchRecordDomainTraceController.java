package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordDomainTraceDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordDomainTracePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordDomainTracePageRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordDomainTraceVerifyReqVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordDomainTraceService;
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

@Tag(name = "管理后台 - MES eDHR 主数据追溯")
@RestController
@RequestMapping("/mes/pro/batch-record-execution/domain-trace")
@Validated
public class MesProBatchRecordDomainTraceController {

    @Resource
    private MesProBatchRecordDomainTraceService domainTraceService;

    @GetMapping("/detail")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-execution:domain-trace-query')")
    public CommonResult<MesProBatchRecordDomainTraceDetailRespVO> detail(@RequestParam("executionId") Long executionId) {
        return success(domainTraceService.getTraceDetail(executionId));
    }

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-execution:domain-trace-query')")
    public CommonResult<PageResult<MesProBatchRecordDomainTracePageRespVO>> page(
            @Valid MesProBatchRecordDomainTracePageReqVO pageReqVO) {
        return success(domainTraceService.getTracePage(pageReqVO));
    }

    @PostMapping("/verify")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-execution:domain-trace-verify')")
    public CommonResult<MesProBatchRecordDomainTraceDetailRespVO> verify(
            @Valid @RequestBody MesProBatchRecordDomainTraceVerifyReqVO reqVO) {
        return success(domainTraceService.verify(reqVO));
    }
}
