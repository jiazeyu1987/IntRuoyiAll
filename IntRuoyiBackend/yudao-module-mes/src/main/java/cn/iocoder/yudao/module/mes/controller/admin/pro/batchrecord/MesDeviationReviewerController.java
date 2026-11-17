package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesDeviationReviewerRespVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesDeviationReviewerCandidateService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrDeviationHandlingService;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesDeviationReviewReturnReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeviationHandlingRespVO;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@Validated
@RequestMapping("/mes/pro/edhr-deviation-review")
public class MesDeviationReviewerController {
    @Resource private MesDeviationReviewerCandidateService candidateService;
    @Resource private MesProEdhrDeviationHandlingService handlingService;

    @GetMapping("/reviewer-candidates")
    @Operation(summary = "按偏差签署职责查询合格候选人")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-deviation:handle')")
    public CommonResult<List<MesDeviationReviewerRespVO>> list(@RequestParam @NotBlank String node) {
        return success(candidateService.list(node));
    }

    @PostMapping("/return")
    @Operation(summary = "审核责任人按当前版本退回偏差调查")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<MesProEdhrDeviationHandlingRespVO> returnToInvestigation(
            @Valid @RequestBody MesDeviationReviewReturnReqVO req) {
        return success(handlingService.returnToInvestigation(SecurityFrameworkUtils.getLoginUserId(), req));
    }
}
