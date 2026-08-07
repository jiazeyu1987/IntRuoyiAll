package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolEventRevisionUpdateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolProductionReportCorrectionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolProductionReportRevisionLogRespVO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolEventRevisionService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolProductionReportCorrectionService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolProductionReportRevisionLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - MES 工序池原始记录修改")
@RestController
@RequestMapping("/mes/pro/process-pool/event-revision")
@Validated
public class MesProProcessPoolEventRevisionController {

    private final MesProcessPoolEventRevisionService mesProcessPoolEventRevisionService;
    private final MesProcessPoolProductionReportCorrectionService productionReportCorrectionService;
    private final MesProcessPoolProductionReportRevisionLogService productionReportRevisionLogService;

    public MesProProcessPoolEventRevisionController(
            MesProcessPoolEventRevisionService mesProcessPoolEventRevisionService,
            MesProcessPoolProductionReportCorrectionService productionReportCorrectionService,
            MesProcessPoolProductionReportRevisionLogService productionReportRevisionLogService) {
        this.mesProcessPoolEventRevisionService = mesProcessPoolEventRevisionService;
        this.productionReportCorrectionService = productionReportCorrectionService;
        this.productionReportRevisionLogService = productionReportRevisionLogService;
    }

    @PostMapping("/update-original")
    @Operation(summary = "修改工序池原始记录并重新电子签名")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool:event-revision:update')")
    public CommonResult<Long> updateOriginalRecord(@Valid @RequestBody ProcessPoolEventRevisionUpdateReqVO reqVO) {
        return success(mesProcessPoolEventRevisionService.updateOriginalRecord(reqVO.toBO()));
    }

    @PostMapping("/correct-production-report")
    @Operation(summary = "按业务字段修改生产报工并使用当前登录人重新电子签名")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool:event-revision:update')")
    public CommonResult<Long> correctProductionReport(
            @Valid @RequestBody ProcessPoolProductionReportCorrectionReqVO reqVO) {
        return success(productionReportCorrectionService.correct(
                reqVO.toCommand().setActorUserId(getLoginUserId())));
    }

    @GetMapping("/production-report-logs")
    @Operation(summary = "查询当前组长可见的生产报工修改记录")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:query')")
    public CommonResult<List<ProcessPoolProductionReportRevisionLogRespVO>> getProductionReportRevisionLogs(
            @RequestParam("eventId") Long eventId) {
        return success(productionReportRevisionLogService.getLogs(eventId, getLoginUserId()).stream()
                .map(ProcessPoolProductionReportRevisionLogRespVO::from)
                .toList());
    }
}
