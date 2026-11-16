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
    @Resource
    private cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesReverseTraceOperationLog operationLog;

    @GetMapping("/catalog")
    @io.swagger.v3.oas.annotations.Operation(summary = "查看批记录反查条件")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-batch-execution:query')")
    public CommonResult<CatalogResponse> catalog(@Valid CatalogRequest request) {
        return success(reverseTraceService.getCatalog(request));
    }

    @PostMapping("/query")
    @io.swagger.v3.oas.annotations.Operation(summary = "反查关联批次")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-batch-execution:query')")
    public CommonResult<QueryResponse> query(@Valid @RequestBody QueryRequest request) {
        QueryResponse result = reverseTraceService.query(request);
        // A blocked query may have no authorized formal order source. Keep its original
        // diagnostic response; the request access log records this unsuccessful attempt.
        if ("BLOCKED".equals(result.getQueryStatus())) return success(result);
        operationLog.record(request.getAnchorBatchExecutionId(), "REVERSE_TRACE_QUERY",
                java.util.Map.of("conditionCount", request.getConditions().size(),
                        "queryStatus", java.util.Objects.toString(result.getQueryStatus(), ""),
                        "coverageStatus", java.util.Objects.toString(result.getCoverageStatus(), ""),
                        "queryHash", java.util.Objects.toString(result.getQueryHash(), "")));
        return success(result);
    }

    @PostMapping("/evidence")
    @io.swagger.v3.oas.annotations.Operation(summary = "查看反查命中记录")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-batch-execution:query')")
    public CommonResult<EvidenceResponse> evidence(@Valid @RequestBody EvidenceRequest request) {
        EvidenceResponse result = reverseTraceService.evidence(request);
        if ("BLOCKED".equals(result.getEvidenceStatus())) return success(result);
        operationLog.record(request.getAnchorBatchExecutionId(), "REVERSE_TRACE_EVIDENCE",
                java.util.Map.of("targetBatchExecutionId", request.getTargetBatchExecutionId(),
                        "evidenceStatus", java.util.Objects.toString(result.getEvidenceStatus(), ""),
                        "queryHash", java.util.Objects.toString(result.getQueryHash(), "")));
        return success(result);
    }
}
