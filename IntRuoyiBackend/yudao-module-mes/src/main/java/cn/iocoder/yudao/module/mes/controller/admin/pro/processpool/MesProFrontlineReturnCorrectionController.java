package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.FrontlineOwnProductionReturnCorrectionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.FrontlineOwnPqcReturnCorrectionReqVO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesFrontlineReturnCorrectionService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolProductionReportCorrectionService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolPqcInspectionCorrectionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name="管理后台 - MES 一线本人退回更正")
@RestController @RequestMapping("/mes/pro/frontline-return-correction") @Validated @RequiredArgsConstructor
public class MesProFrontlineReturnCorrectionController {
    private final MesFrontlineReturnCorrectionService returns;
    private final MesProcessPoolProductionReportCorrectionService production;
    private final MesProcessPoolPqcInspectionCorrectionService pqc;

    @GetMapping("/list") @PreAuthorize("@ss.hasPermission('mes:pro-feedback:query')")
    public CommonResult<List<MesFrontlineReturnCorrectionService.ReturnRow>> list(@RequestParam String leaderType) {
        return success(returns.listOwnReturned(getLoginUserId(),leaderType));
    }
    @GetMapping("/detail") @PreAuthorize("@ss.hasPermission('mes:pro-feedback:query')")
    public CommonResult<MesFrontlineReturnCorrectionService.ReturnDetail> detail(
            @RequestParam Long eventId,@RequestParam Long activeOrderId,@RequestParam Long rejectedReviewId,
            @RequestParam Long expectedRevisionId,@RequestParam String leaderType) {
        return success(returns.getOwnReturned(eventId,activeOrderId,rejectedReviewId,expectedRevisionId,getLoginUserId(),leaderType));
    }
    @PostMapping("/production/resubmit") @PreAuthorize("@ss.hasPermission('mes:pro-feedback:create')")
    public CommonResult<MesFrontlineReturnCorrectionService.CorrectionResult> productionResubmit(@Valid @RequestBody FrontlineOwnProductionReturnCorrectionReqVO request) {
        return success(production.correctOwnReturned(request.getCorrection().toCommand().setActorUserId(getLoginUserId()),
                request.getActiveOrderId(),request.getRejectedReviewId(),request.getExpectedRevisionId()));
    }
    @PostMapping("/pqc/resubmit") @PreAuthorize("@ss.hasPermission('mes:pro-feedback:create')")
    public CommonResult<MesFrontlineReturnCorrectionService.CorrectionResult> pqcResubmit(@Valid @RequestBody FrontlineOwnPqcReturnCorrectionReqVO request) {
        return success(pqc.correctOwnReturned(request.getCorrection().toCommand().setActorUserId(getLoginUserId()),
                request.getActiveOrderId(),request.getRejectedReviewId(),request.getExpectedRevisionId()));
    }
}
