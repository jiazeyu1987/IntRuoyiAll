package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolFifoOrchestrationAllocateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolFifoOrchestrationAllocateRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProcessPoolFifoAllocationLineDO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolFifoAllocationResult;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolFifoOrchestrationCommand;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolFifoOrchestrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - MES 工序池 FIFO 编排")
@RestController
@RequestMapping("/mes/pro/process-pool/fifo-orchestration")
@Validated
public class MesProcessPoolFifoOrchestrationController {

    private final MesProcessPoolFifoOrchestrationService orchestrationService;

    public MesProcessPoolFifoOrchestrationController(
            MesProcessPoolFifoOrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    @PostMapping("/allocate-available-output")
    @Operation(summary = "按生产工单计划开始时间执行 FIFO 分配")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-fifo:allocate')")
    public CommonResult<ProcessPoolFifoOrchestrationAllocateRespVO> allocateAvailableOutput(
            @RequestBody @Valid ProcessPoolFifoOrchestrationAllocateReqVO reqVO) {
        MesProcessPoolFifoAllocationResult result = orchestrationService.allocateAvailableOutput(
                MesProcessPoolFifoOrchestrationCommand.builder()
                        .allocationBatchNo(reqVO.getAllocationBatchNo())
                        .sourceProcessId(reqVO.getSourceProcessId())
                        .targetRouteProcessId(reqVO.getTargetRouteProcessId())
                        .targetProcessId(reqVO.getTargetProcessId())
                        .targetWorkOrderIds(reqVO.getTargetWorkOrderIds())
                        .build());
        return success(toRespVO(result));
    }

    private ProcessPoolFifoOrchestrationAllocateRespVO toRespVO(
            MesProcessPoolFifoAllocationResult result) {
        return ProcessPoolFifoOrchestrationAllocateRespVO.builder()
                .totalAllocatedQuantity(result.getTotalAllocatedQuantity())
                .lines(toLineRespVOs(result.getLines()))
                .build();
    }

    private List<ProcessPoolFifoOrchestrationAllocateRespVO.Line> toLineRespVOs(
            List<MesProcessPoolFifoAllocationLineDO> lines) {
        return lines.stream()
                .map(line -> ProcessPoolFifoOrchestrationAllocateRespVO.Line.builder()
                        .sourceQuantityFragmentId(line.getSourceQuantityFragmentId())
                        .sourceEventId(line.getSourceEventId())
                        .targetWorkOrderId(line.getTargetWorkOrderId())
                        .targetWorkOrderCode(line.getTargetWorkOrderCode())
                        .allocatedQuantity(line.getAllocatedQuantity())
                        .allocationStatus(line.getAllocationStatus())
                        .build())
                .toList();
    }
}
