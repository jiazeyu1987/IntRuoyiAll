package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeviationHandlingRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeviationHandlingSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeviationHandlingSignReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeviationHandlingCloseReqVO;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrDeviationHandlingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - eDHR偏差处理")
@RestController
@RequestMapping("/mes/pro/edhr-deviation-handling")
public class MesProEdhrDeviationHandlingController {

    @Resource
    private cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrDeviationAccessService accessService;

    @Resource
    private MesProEdhrDeviationHandlingService handlingService;

    @GetMapping("/get")
    @Operation(summary = "获取偏差处理记录")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<MesProEdhrDeviationHandlingRespVO> get(@RequestParam("deviationId") Long deviationId) {
        accessService.requireRead(deviationId);
        return success(handlingService.get(deviationId));
    }

    @PostMapping("/save")
    @Operation(summary = "保存偏差处理记录")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-deviation:handle')")
    public CommonResult<MesProEdhrDeviationHandlingRespVO> save(
            @RequestParam("deviationId") Long deviationId,
            @Valid @RequestBody MesProEdhrDeviationHandlingSaveReqVO reqVO) {
        return success(handlingService.save(SecurityFrameworkUtils.getLoginUserId(), deviationId, reqVO));
    }

    @PostMapping("/submit-investigation")
    @Operation(summary = "接收人正式提交调查记录")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-deviation:handle')")
    public CommonResult<MesProEdhrDeviationHandlingRespVO> submitInvestigation(
            @Valid @RequestBody cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeviationHandlingSubmitReqVO req) {
        return success(handlingService.submitInvestigation(SecurityFrameworkUtils.getLoginUserId(), req));
    }

    @PostMapping("/corrective-submit")
    @Operation(summary = "整改责任人提交本人整改")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<MesProEdhrDeviationHandlingRespVO> submitCorrective(
            @Valid @RequestBody cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeviationCorrectiveReqVO req) {
        return success(handlingService.submitCorrective(SecurityFrameworkUtils.getLoginUserId(), req));
    }

    @PostMapping("/corrective-verify")
    @Operation(summary = "指定核验人验证或退回整改")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-deviation:verify')")
    public CommonResult<MesProEdhrDeviationHandlingRespVO> verifyCorrective(
            @Valid @RequestBody cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeviationCorrectiveVerifyReqVO req) {
        return success(handlingService.verifyCorrective(SecurityFrameworkUtils.getLoginUserId(), req));
    }

    @PostMapping("/sign")
    @Operation(summary = "签署偏差处理节点")
    @PreAuthorize("@ss.hasAnyPermissions('mes:pro-edhr-deviation:handle','mes:pro-edhr-deviation:verify','mes:pro-edhr-deviation:department-confirm','mes:pro-edhr-deviation:qa-close','mes:pro-edhr-deviation:quality-approve','mes:pro-edhr-deviation:critical-management-approve')")
    public CommonResult<Long> sign(@Valid @RequestBody MesProEdhrDeviationHandlingSignReqVO reqVO) {
        return success(handlingService.sign(SecurityFrameworkUtils.getLoginUserId(), reqVO.getDeviationId(),
                reqVO.getNode(), reqVO.getPassword(), reqVO.getComment(),
                reqVO.getExpectedContentVersion(), reqVO.getExpectedContentHash()));
    }

    @PostMapping("/close")
    @Operation(summary = "常规关闭偏差")
    @PreAuthorize("@ss.hasAnyPermissions('mes:pro-edhr-deviation:qa-close','mes:pro-edhr-deviation:quality-approve')")
    public CommonResult<Boolean> close(@Valid @RequestBody MesProEdhrDeviationHandlingCloseReqVO reqVO) {
        handlingService.closeNormally(SecurityFrameworkUtils.getLoginUserId(), reqVO.getDeviationId(),
                reqVO.getExpectedContentVersion(), reqVO.getExpectedContentHash());
        return success(true);
    }
}
