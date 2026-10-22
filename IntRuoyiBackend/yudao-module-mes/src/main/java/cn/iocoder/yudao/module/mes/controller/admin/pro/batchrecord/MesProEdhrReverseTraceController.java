package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.CatalogRequest;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.CatalogResponse;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.EvidenceRequest;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.EvidenceResponse;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.QueryRequest;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.QueryResponse;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceService;
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

@RestController
@RequestMapping("/mes/pro/edhr-batch-execution/reverse-trace")
@Validated
public class MesProEdhrReverseTraceController {

    @Resource
    private MesProEdhrReverseTraceService reverseTraceService;

    @GetMapping("/catalog")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-batch-execution:query')")
    public CommonResult<CatalogResponse> catalog(@Valid CatalogRequest request) {
        return success(reverseTraceService.getCatalog(request));
    }

    @PostMapping("/query")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-batch-execution:query')")
    public CommonResult<QueryResponse> query(@Valid @RequestBody QueryRequest request) {
        return success(reverseTraceService.query(request));
    }

    @PostMapping("/evidence")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-batch-execution:query')")
    public CommonResult<EvidenceResponse> evidence(@Valid @RequestBody EvidenceRequest request) {
        return success(reverseTraceService.evidence(request));
    }
}
